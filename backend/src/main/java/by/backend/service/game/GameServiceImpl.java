package by.backend.service.game;

import by.backend.exception.GameException;
import by.backend.model.dto.*;
import by.backend.model.entity.HighScore;
import by.backend.model.entity.Person;
import by.backend.model.enums.Difficulty;
import by.backend.model.enums.RelationshipStatus;
import by.backend.repository.HighScoreRepository;
import by.backend.repository.PersonRepository;
import by.backend.service.relationship.RelationshipService;
import by.backend.service.cache.RelationshipCache;
import by.backend.service.graph.FamilyGraphService;
import by.backend.mapper.PersonMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import by.backend.config.GameProperties;
import by.backend.service.game.AnalysisService;
import by.backend.service.game.session.GameSession;
import by.backend.service.game.session.PlayerAnswer;
import by.backend.service.game.GameAnalysisService;
import by.backend.model.dto.RelationshipPathDTO;
import java.util.UUID;

@Service
public class GameServiceImpl implements GameService {

    private final PersonRepository personRepository;
    private final HighScoreRepository highScoreRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;
    private final GameProperties gameProperties;
    private final AnalysisService analysisService;
    private final GameAnalysisService gameAnalysisService;
    private final RelationshipCache relationshipCache;
    private final FamilyGraphService familyGraphService;
    private final Map<Difficulty, List<GameQuestionDTO>> preGeneratedQuestions;
    private final ScheduledExecutorService executorService;
    private final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);

    private static final int PRE_GENERATED_QUESTIONS_COUNT = 20;
    private static final int MAX_ATTEMPTS_IN_GENERATE_INTERNAL = 50;
    private static final int MAX_ATTEMPTS_FOR_QUESTION_GENERATION = 10;

    private volatile List<Person> activePersonsCache = Collections.emptyList();
    private static final long ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES = 5;
    private long lastCacheRefreshTimeMillis = System.currentTimeMillis();
    private final Object cacheLock = new Object();

    private final ConcurrentMap<String, GameSession> activeGameSessions = new ConcurrentHashMap<>();

    public GameServiceImpl(PersonRepository personRepository,
                         HighScoreRepository highScoreRepository,
                         RelationshipService relationshipService,
                         PersonMapper personMapper,
                         MessageSource messageSource,
                         GameProperties gameProperties,
                         AnalysisService analysisService,
                         GameAnalysisService gameAnalysisService,
                         RelationshipCache relationshipCache,
                         FamilyGraphService familyGraphService) {
        this.personRepository = personRepository;
        this.highScoreRepository = highScoreRepository;
        this.relationshipService = relationshipService;
        this.personMapper = personMapper;
        this.messageSource = messageSource;
        this.gameProperties = gameProperties;
        this.analysisService = analysisService;
        this.gameAnalysisService = gameAnalysisService;
        this.relationshipCache = relationshipCache;
        this.familyGraphService = familyGraphService;
        this.preGeneratedQuestions = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            preGeneratedQuestions.put(difficulty, Collections.synchronizedList(new ArrayList<>()));
        }
        this.executorService = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    }

    @PostConstruct
    public void init() {
        log.info("GameService başlatılıyor, başlangıç işlemleri...");

        CompletableFuture.runAsync(this::refreshActivePersonsCache).thenRun(() -> {
            log.info("Aktif kişi önbelleği ilk kez dolduruldu. Boyut: {}", activePersonsCache.size());
            if (!this.activePersonsCache.isEmpty()) {
                for (Difficulty difficulty : Difficulty.values()) {
                    CompletableFuture.runAsync(() -> generateInitialQuestions(difficulty));
                }
            } else {
                log.warn("Aktif kişi önbelleği boş olduğu için başlangıç soruları üretilemiyor.");
            }
        }).exceptionally(ex -> {
            log.error("Aktif kişi önbelleği ilk doldurma sırasında hata oluştu: {}", ex.getMessage(), ex);
            return null;
        });

        executorService.scheduleWithFixedDelay(this::refreshAllQuestions, ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES, ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
        executorService.scheduleWithFixedDelay(this::refreshActivePersonsCache,
                                               ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES,
                                               ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES,
                                               TimeUnit.MINUTES);
        log.info("Periyodik görevler (soru ve kişi önbelleği yenileme) ayarlandı. Yenileme aralığı: {} dakika.", ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES);
    }

    @Async
    protected void generateInitialQuestions(Difficulty difficulty) {
        log.info("{} zorluğu için başlangıç soruları üretimi başladı...", difficulty);
        try {
            List<GameQuestionDTO> questionsForDifficulty = preGeneratedQuestions.get(difficulty);
            int count = 0;
            long startTime = System.currentTimeMillis();
            while (questionsForDifficulty.size() < PRE_GENERATED_QUESTIONS_COUNT) {
                if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(30)) {
                    log.warn("{} zorluğu için başlangıç soru üretimi 30 saniyeyi aştı, durduruluyor. Mevcut: {}", difficulty, questionsForDifficulty.size());
                    break;
                }
                GameQuestionDTO question = generateQuestionInternal(difficulty,
                    questionsForDifficulty.stream()
                                          .map(GameQuestionDTO::getId)
                                          .collect(Collectors.toSet()), LocaleContextHolder.getLocale());
                if (question != null) {
                    questionsForDifficulty.add(question);
                    count++;
                } else {
                    log.warn("{} zorluğu için başlangıç soru üretiminde null soru geldi, döngüden çıkılabilir.", difficulty);
                    break;
                }
            }
            log.info("{} zorluğu için {} adet başlangıç sorusu üretildi (Toplam: {}). Süre: {}ms",
                difficulty, count, questionsForDifficulty.size(), System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("{} zorluğu için başlangıç soruları üretilirken hata: {}", difficulty, e.getMessage(), e);
        }
    }

    private void refreshAllQuestions() {
        log.info("Tüm zorluk seviyeleri için önceden üretilmiş soruların periyodik yenilenmesi başlatılıyor...");
        for (Difficulty difficulty : Difficulty.values()) {
            CompletableFuture.runAsync(() -> {
                log.debug("{} zorluğu için soru yenileme işlemi başladı.", difficulty);
                try {
                    List<GameQuestionDTO> questions = preGeneratedQuestions.get(difficulty);
                    int initialSize = questions.size();
                    int addedCount = 0;
                    long startTime = System.currentTimeMillis();
                    while (questions.size() < PRE_GENERATED_QUESTIONS_COUNT) {
                        if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(30)) {
                            log.warn("{} zorluğu için soru yenileme 30 saniyeyi aştı, durduruluyor. Mevcut: {}", difficulty, questions.size());
                            break;
                        }
                        GameQuestionDTO question = generateQuestionInternal(difficulty,
                            questions.stream().map(GameQuestionDTO::getId).collect(Collectors.toSet()), LocaleContextHolder.getLocale());
                        if (question != null) {
                            questions.add(question);
                            addedCount++;
                        } else {
                            log.warn("{} zorluğu için soru yenileme sırasında yeni soru üretilemedi (null döndü), mevcut boyut: {}.", difficulty, questions.size());
                            break;
                        }
                    }
                    if (addedCount > 0) {
                        log.info("{} zorluğu için sorular yenilendi. {} yeni soru eklendi. Yeni boyut: {}. Süre: {}ms",
                            difficulty, addedCount, questions.size(), System.currentTimeMillis() - startTime);
                    } else if (initialSize < PRE_GENERATED_QUESTIONS_COUNT) {
                        log.info("{} zorluğu için soru yenileme denendi ancak yeni soru eklenemedi. Mevcut boyut: {}.", difficulty, questions.size());
                    }
                } catch (Exception e) {
                    log.error("{} zorluğu için sorular yenilenirken hata oluştu: {}", difficulty, e.getMessage(), e);
                }
            });
        }
    }

    @Override
    @Transactional
    public InitialGameDataDTO startGame(String playerName, Difficulty difficulty, Locale locale) {
        log.info("Starting game for player '{}' with difficulty '{}'", playerName, difficulty);

        GameQuestionDTO firstQuestion = null;
        Difficulty currentDifficulty = difficulty;
        int attempts = 0;
        while (firstQuestion == null && attempts < Difficulty.values().length) {
            firstQuestion = generateQuestionInternal(currentDifficulty, Collections.emptySet(), locale);
            if (firstQuestion == null) {
                log.warn("startGame: {} zorluğunda soru üretilemedi, bir alt zorluk deneniyor.", currentDifficulty);
                currentDifficulty = getPreviousDifficulty(currentDifficulty);
                if (currentDifficulty == null) {
                    log.error("startGame: Tüm zorluk seviyeleri denendi ancak soru üretilemedi.");
                    break;
                }
            }
            attempts++;
        }

        if (firstQuestion == null) {
            log.error("startGame: İlk soru üretilemedi. Oyuncu: {}, İstenen Zorluk: {}", playerName, difficulty);
            throw new GameException(getMessage("game.error.start_failed_no_question", locale));
        }

        log.info("Oyun {} zorluğunda başlatıldı (istenen: {})", firstQuestion.getDifficulty(), difficulty);

        String sessionId = UUID.randomUUID().toString();
        // GameProperties'den değerler okunuyor
        long gameDurationInSeconds = gameProperties.getDurationInSeconds().get(firstQuestion.getDifficulty());
        int totalQuestions = gameProperties.getQuestionsPerGame();

        GameSession newSession = new GameSession(
                sessionId,
                playerName,
                firstQuestion.getDifficulty(),
                gameDurationInSeconds,
                totalQuestions,
                new ArrayList<>() // Sorular oyun sırasında tek tek üretildiği için başlangıç listesi boş.
        );
        
        // İlk soruyu session'da kaydet
        newSession.getAskedQuestionSignatures().add(firstQuestion.getId());
        
        activeGameSessions.put(newSession.getSessionId(), newSession);
        log.info("New game session created with ID '{}' for player '{}', first question: {}", 
                newSession.getSessionId(), playerName, firstQuestion.getId());

        GameQuestionDTO questionToSend = firstQuestion.toBuilder().build();
        questionToSend.setCorrectAnswer(null);
        // relationshipPath'i null yapmıyoruz, çünkü frontend'de harita göstermek için gerekli

        return InitialGameDataDTO.builder()
                .sessionId(newSession.getSessionId())
                .firstQuestion(questionToSend)
                .playerName(playerName)
                .difficulty(firstQuestion.getDifficulty())
                .gameDurationInSeconds(gameDurationInSeconds)
                .totalQuestions(totalQuestions)
                .build();
    }

    private Difficulty getPreviousDifficulty(Difficulty difficulty) {
        if (difficulty == null) return null;
        int currentOrdinal = difficulty.ordinal();
        if (currentOrdinal > 0) {
            return Difficulty.values()[currentOrdinal - 1];
        }
        return null; // En düşük seviyede zaten
    }

    @Override
    @Transactional
    public AnswerResponseDTO answerQuestion(GameAnswerDTO answerDetails, Locale locale) {
        String sessionId = answerDetails.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            log.warn("answerQuestion: Session ID is missing in the request.");
            throw new GameException(getMessage("game.error.session_not_found", locale));
        }

        log.debug("Processing answer for session '{}', question '{}'", sessionId, answerDetails.getQuestionId());

        GameSession session = activeGameSessions.get(sessionId);
        if (session == null) {
            log.warn("No active game session found for ID '{}'. It might have expired or never existed.", sessionId);
            throw new GameException(getMessage("game.error.session_not_found", locale));
        }
        
        if (session.isGameOver()) {
            log.warn("Answer received for already finished game session '{}'.", sessionId);
            activeGameSessions.remove(sessionId);
            return createGameOverResponse(session, "game.error.game_already_over", locale);
        }

        // --- Start of inlined validation logic ---
        String correctAnswerText;
        boolean isCorrect;
        RelationshipPathDTO relationshipPath = null;
        try {
            String[] ids = answerDetails.getQuestionId().split("_");
            if (ids.length != 2) throw new GameException("Invalid Question ID format");

            Person p1 = personRepository.findById(Long.parseLong(ids[0])).orElseThrow(() -> new GameException("Person 1 not found"));
            Person p2 = personRepository.findById(Long.parseLong(ids[1])).orElseThrow(() -> new GameException("Person 2 not found"));

            RelationshipDescriptionResult result = relationshipService.findRelationshipDescription(personMapper.toSummaryDTO(p1), personMapper.toSummaryDTO(p2));
            if (result.getStatus() != RelationshipStatus.FOUND) {
                throw new GameException("Could not determine relationship for question ID: " + answerDetails.getQuestionId());
            }

            String correctAnswerKey = result.getMessageKey();
            correctAnswerText = getMessage(correctAnswerKey, locale);
            isCorrect = normalizeAnswer(correctAnswerText).equals(normalizeAnswer(answerDetails.getAnswer()));

            // On correct answer, include the path for graph visualization
            if(isCorrect) {
                List<RelationshipStepDTO> path = result.getPath();
                // Manuel olarak RelationshipPathDTO nesnesi oluştur
                relationshipPath = new RelationshipPathDTO();
                relationshipPath.setSteps(path);
                relationshipPath.setDescription(correctAnswerText);
            }

        } catch (Exception e) {
            log.error("Error validating answer for questionId '{}': {}", answerDetails.getQuestionId(), e.getMessage(), e);
            throw new GameException(getMessage("game.error.could_not_determine_correct_answer", locale));
        }
        // --- End of inlined validation logic ---

        int pointsEarned = 0;
        Integer currentScore = answerDetails.getCurrentScore();
        int updatedScore = currentScore != null ? currentScore : 0;
        int updatedStreak = 0;

        // Eğer bir oturum varsa, puanı ve streak'i güncelleyelim
        if (session != null) {
            if (isCorrect) {
                Long timeTaken = answerDetails.getTimeTakenInSeconds();
                pointsEarned = calculatePointsInternal(true, answerDetails.getDifficulty(),
                    timeTaken != null ? timeTaken : 0L,
                    session.getCurrentStreak().get());

                session.recordAnswer(new PlayerAnswer(answerDetails.getQuestionId(), "family", answerDetails.getAnswer(), true), pointsEarned);

                log.info("Correct answer by '{}' for question '{}', earned {} points. New score: {}, streak: {}",
                    session.getPlayerName(), answerDetails.getQuestionId(), pointsEarned, session.getScore().get(), session.getCurrentStreak().get());

                updatedScore = session.getScore().get();
                updatedStreak = session.getCurrentStreak().get();
            } else {
                session.recordAnswer(new PlayerAnswer(answerDetails.getQuestionId(), "family", answerDetails.getAnswer(), false), 0);

                log.info("Incorrect answer by '{}' for question '{}'. Score remains: {}, streak reset to 0",
                    session.getPlayerName(), answerDetails.getQuestionId(), session.getScore().get());

                updatedScore = session.getScore().get();
                updatedStreak = 0;
            }

            // Oyun bitme kontrolünü erken yapmıyoruz, sonraki soru üretildikten sonra yapacağız
        } else {
            // Oturum yoksa, sadece istemci tarafındaki değerleri kullanalım
            if (isCorrect) {
                Long timeTaken = answerDetails.getTimeTakenInSeconds();
                Integer currentStreak = answerDetails.getCurrentStreak();
                pointsEarned = calculatePointsInternal(true, answerDetails.getDifficulty(),
                    timeTaken != null ? timeTaken : 0L,
                    currentStreak != null ? currentStreak : 0);

                currentScore = answerDetails.getCurrentScore();
                updatedScore = currentScore != null ? currentScore + pointsEarned : pointsEarned;
                updatedStreak = currentStreak != null ? currentStreak + 1 : 1;

                log.info("Correct answer by '{}' (no session) for question '{}', earned {} points. New score: {}, streak: {}",
                    answerDetails.getPlayerName(), answerDetails.getQuestionId(), pointsEarned, updatedScore, updatedStreak);
            } else {
                log.info("Incorrect answer by '{}' (no session) for question '{}'. Score remains: {}, streak reset to 0",
                    answerDetails.getPlayerName(), answerDetails.getQuestionId(), updatedScore);

                updatedStreak = 0;
            }
        }

        // Next question generation (both session and non-session mode)
        GameQuestionDTO nextQuestion = null;
        boolean shouldGenerateNext = true;
        
        // Session tabanlı oyunlarda kontroller
        if (session != null) {
            // Süre kontrolü
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - session.getStartTime()) / 1000;
            if (elapsedSeconds >= session.getGameDurationInSeconds()) {
                shouldGenerateNext = false;
                log.info("Oyun süresi doldu, yeni soru üretilmiyor. Session: {}, Geçen süre: {}s", sessionId, elapsedSeconds);
            }
            
            // Soru sayısı kontrolü
            if (session.getQuestionsAnswered().get() >= session.getTotalQuestions()) {
                shouldGenerateNext = false;
                log.info("Toplam soru sayısına ulaşıldı, yeni soru üretilmiyor. Session: {}, Cevaplanan: {}/{}", 
                        sessionId, session.getQuestionsAnswered().get(), session.getTotalQuestions());
            }
        }
        
        if (shouldGenerateNext) {
            Set<String> askedSignatures = new HashSet<>();
            
            // Session varsa session'dan sorulan soruları ekle
            if (session != null) {
                askedSignatures.addAll(session.getAskedQuestionSignatures());
            }
            
            // Frontend'den gelen soruları da ekle (fallback için)
            if (answerDetails.getAskedQuestionSignaturesInThisGame() != null) {
                askedSignatures.addAll(answerDetails.getAskedQuestionSignaturesInThisGame());
            }
            
            nextQuestion = generateNewQuestion(answerDetails.getDifficulty(), askedSignatures);

            if (nextQuestion != null) {
                // Don't send the correct answer to client
                GameQuestionDTO clientQuestion = nextQuestion.toBuilder().build();
                clientQuestion.setCorrectAnswer(null);
                // relationshipPath'i null yapmıyoruz, çünkü frontend'de harita göstermek için gerekli
                nextQuestion = clientQuestion;
                
                // Session'da yeni soruyu kaydet
                if (session != null) {
                    session.getAskedQuestionSignatures().add(nextQuestion.getId());
                }
                log.info("Yeni soru üretildi. Session: {}, Soru ID: {}, Toplam soru: {}", 
                        sessionId, nextQuestion.getId(), session != null ? session.getAskedQuestionSignatures().size() : "N/A");
            } else {
                log.warn("Yeni soru üretilemedi, oyunu bitirme sinyali göndereceğiz. Session: {}", sessionId);
            }
        }

        // Oyun bitme kontrolü - sadece yeni soru üretilemediğinde ya da session limitine ulaşıldığında
        boolean isGameOver = false;
        if (session != null) {
            // Session varsa: toplam soru sayısına ulaşıldıysa ya da süre dolduya ya da yeni soru üretilemediğinde oyun biter
            isGameOver = session.areAllQuestionsAnswered() || session.isTimeUp() || nextQuestion == null;
            
            if (isGameOver) {
                log.info("Game over for session '{}'. Final score: {}, questions answered: {}/{}",
                    sessionId, session.getScore().get(), session.getQuestionsAnswered().get(), session.getTotalQuestions());
            }
        } else if (nextQuestion == null) {
            isGameOver = true;
        }
        
        // Build the response (both session and non-session mode)
        AnswerResponseDTO response = AnswerResponseDTO.builder()
                .correctAnswer(isCorrect)
                .correctAnswerText(correctAnswerText)
                .pointsEarned(pointsEarned)
                .updatedScore(updatedScore)
                .updatedStreak(updatedStreak)
                .nextQuestion(nextQuestion)
                .gameOver(isGameOver)
                .relationshipPath(relationshipPath)
                .build();

        // Oyun bitti ise session'ı temizle
        if (isGameOver && session != null) {
            activeGameSessions.remove(sessionId);
            log.info("Oyun bitti, session temizlendi: {}", sessionId);
        }

        return response;
    }

    private AnswerResponseDTO createGameOverResponse(GameSession session, String messageKey, Locale locale) {
        log.info("Game over response for session: {}", session.getSessionId());
        AnalysisResultDTO analysis = analysisService.analyze(session.getPlayerAnswers(), locale);

        GameResultDTO finalResult = GameResultDTO.builder()
                .playerName(session.getPlayerName())
                .score(session.getScore().get())
                .difficulty(session.getDifficulty())
                .date(LocalDate.now())
                .correctAnswers(session.getScore().get() > 0 ? session.getQuestionsAnswered().get() : 0) // Approximation
                .totalQuestions(session.getTotalQuestions())
                .maxStreak(session.getMaxStreak().get())
                .build();

        var responseBuilder = AnswerResponseDTO.builder()
                .correctAnswer(false)
                .pointsEarned(0)
                .updatedScore(session.getScore().get())
                .updatedStreak(session.getCurrentStreak().get())
                .gameOver(true)
                .finalResult(finalResult)
                .analysisResult(analysis);

        if (messageKey != null) {
            responseBuilder.gameEndMessage(getMessage(messageKey, locale));
        }

        return responseBuilder.build();
    }

    private String normalizeAnswer(String answer) {
        return answer.trim().toLowerCase();
    }

    private GameQuestionDTO generateNewQuestion(Difficulty difficulty, Set<String> askedQuestionSignatures) {
        log.debug("{} zorluğunda yeni soru üretiliyor, şu ana kadar sorulanlar: {}", difficulty, askedQuestionSignatures.size());
        List<GameQuestionDTO> questionPool = preGeneratedQuestions.get(difficulty);

        synchronized (questionPool) {
            Iterator<GameQuestionDTO> iterator = questionPool.iterator();
            while (iterator.hasNext()) {
                GameQuestionDTO q = iterator.next();
                if (!askedQuestionSignatures.contains(q.getId())) {
                    iterator.remove();
                    log.info("Havuzdan soru alındı: ID {}. Havuz boyutu: {}", q.getId(), questionPool.size());
                    if (questionPool.size() < PRE_GENERATED_QUESTIONS_COUNT / 2) {
                        int questionsToGenerate = PRE_GENERATED_QUESTIONS_COUNT - questionPool.size();
                        CompletableFuture.runAsync(() -> refreshSpecificDifficultyQuestions(difficulty, questionsToGenerate));
                    }
                    return q;
                }
            }
        }
        log.info("Havuzda {} zorluğu için uygun yeni soru bulunamadı (veya hepsi sorulmuş). Dinamik olarak üretilecek.", difficulty);
        for (int attempt = 0; attempt < MAX_ATTEMPTS_FOR_QUESTION_GENERATION; attempt++) {
            GameQuestionDTO question = generateQuestionInternal(difficulty, askedQuestionSignatures, LocaleContextHolder.getLocale());
            if (question != null) {
                log.info("Dinamik olarak yeni soru üretildi: ID {}", question.getId());
                return question;
            }
            log.warn("Dinamik soru üretme denemesi {} başarısız oldu.", attempt + 1);
        }
        log.error("{} zorluğunda yeni soru üretilemedi ({} denemeden sonra).", difficulty, MAX_ATTEMPTS_FOR_QUESTION_GENERATION);
        return null;
    }

    @Override
    public GameQuestionDTO generateQuestion(Difficulty difficulty, Locale locale) {
        log.info("İsteğe bağlı soru üretiliyor, Zorluk: {}", difficulty);
        GameQuestionDTO question = generateQuestionInternal(difficulty, Collections.emptySet(), locale);
        if (question == null) {
            log.error("generateQuestion: Soru üretilemedi. Zorluk: {}", difficulty);
            throw new GameException(getMessage("game.error.generate_question_failed", locale));
        }
        GameQuestionDTO questionToSend = question.toBuilder().build();
        questionToSend.setCorrectAnswer(null);
        questionToSend.setRelationshipPath(null);
        return questionToSend;
    }

    private GameQuestionDTO generateQuestionInternal(Difficulty difficulty, Set<String> askedSignatures, Locale locale) {
        log.debug("generateQuestionInternal çağrıldı. Zorluk: {}, Önceden sorulanlar: {}, Yerel: {}", difficulty, askedSignatures.size(), locale.toLanguageTag());
        List<Person> candidates = getActivePersonsForQuestionGeneration();
        if (candidates.size() < 2) {
            log.warn("Soru üretmek için yeterli aktif kişi yok ({}).", candidates.size());
            return null;
        }

        Random random = new Random();
        Set<String> attemptedPairs = new HashSet<>(); // Bu session'da denenen çiftleri takip et
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS_IN_GENERATE_INTERNAL; attempt++) {
            Person p1 = candidates.get(random.nextInt(candidates.size()));
            Person p2 = candidates.get(random.nextInt(candidates.size()));

            if (p1.getId().equals(p2.getId())) {
                log.debug("Aynı kişiler (p1:{}, p2:{}) denk geldi, tekrar denenecek.", p1.getId(), p2.getId());
                continue;
            }

            // Her iki yöndeki soruları da kontrol et (P1->P2 ve P2->P1)
            String questionSignature1 = p1.getId() + "_" + p2.getId();
            String questionSignature2 = p2.getId() + "_" + p1.getId();
            String attemptSignature = Math.min(p1.getId(), p2.getId()) + "_" + Math.max(p1.getId(), p2.getId());

            if (askedSignatures.contains(questionSignature1) || askedSignatures.contains(questionSignature2)) {
                 log.debug("Bu kişi çifti ({}-{}) daha önce sorulmuş, tekrar denenecek.", p1.getId(), p2.getId());
                continue;
            }
            
            if (attemptedPairs.contains(attemptSignature)) {
                log.debug("Bu kişi çifti ({}-{}) bu oturumda denendi, tekrar denenecek.", p1.getId(), p2.getId());
                continue;
            }
            attemptedPairs.add(attemptSignature);

            PersonSummaryDTO p1Summary = personMapper.toSummaryDTO(p1);
            PersonSummaryDTO p2Summary = personMapper.toSummaryDTO(p2);
            RelationshipDescriptionResult descResult = relationshipService.findRelationshipDescription(p1Summary, p2Summary);

            if (descResult.getStatus() != RelationshipStatus.FOUND) {
                log.debug("İlişki durumu 'FOUND' değil (P1:{}, P2:{}, Durum: {}), tekrar denenecek.", p1.getId(), p2.getId(), descResult.getStatus());
                continue;
            }
            
            String actualRelationshipMessageKey = descResult.getMessageKey();

            // İstenmeyen genel veya belirsiz ilişki türlerini filtrele
            if (isUnwantedRelationshipKey(actualRelationshipMessageKey)) {
                log.debug("İstenmeyen ilişki anahtarı ({}) P1:{}, P2:{} için soru üretimi atlanıyor.", actualRelationshipMessageKey, p1.getId(), p2.getId());
                continue;
            }

            if (!isAppropriateForDifficulty(descResult, difficulty, p1, p2)) {
                 log.debug("İlişki ({}) P1:{}, P2:{} zorluk seviyesi '{}' için uygun değil, tekrar denenecek.", actualRelationshipMessageKey, p1.getId(), p2.getId(), difficulty);
                continue;
            }

            GameQuestionDTO question = createQuestionDTO(p1, p2, descResult, difficulty, locale);

            if (question.getOptions().size() < 2) {
                log.warn("Soru için yeterli seçenek üretilemedi (P1:{}, P2:{}, Doğru Cevap: {}, Seçenek Sayısı: {}).", p1.getId(), p2.getId(), question.getCorrectAnswer(), question.getOptions().size());
                continue;
            }
            
            // Soru kalitesini kontrol et
            if (!isQuestionQualityGood(question, descResult)) {
                log.debug("Soru kalitesi yetersiz (P1:{}, P2:{}), tekrar denenecek.", p1.getId(), p2.getId());
                continue;
            }

            log.info("Geçerli soru üretildi: ID {}, P1:{}, P2:{}, Zorluk: {}, Cevap: {}", question.getId(), p1.getId(), p2.getId(), difficulty, question.getCorrectAnswer());
            return question;
        }
        log.warn("{} denemeden sonra {} zorluğunda geçerli bir soru üretilemedi.", MAX_ATTEMPTS_IN_GENERATE_INTERNAL, difficulty);
        return null;
    }

    private List<String> generateOptions(Difficulty difficulty, String correctAnswerKey, List<String> acceptableCorrectAnswerKeys, Locale locale, Person p1, Person p2) {
        // Cinsiyete göre seçenek üretmek için p1 ve p2 eklendi.
        Set<String> optionKeys = new LinkedHashSet<>();
        if (correctAnswerKey != null) {
            optionKeys.add(correctAnswerKey);
        }

        Set<String> allPossibleKeys = new HashSet<>();
        
        // Cinsiyetleri al
        boolean p1IsMale = "MALE".equalsIgnoreCase(p1.getGender().name());
        boolean p2IsMale = "MALE".equalsIgnoreCase(p2.getGender().name());

        // ========== CİNSİYETE GÖRE FİLTRELENMİŞ KESİN İLİŞKİ TANIMLARI ==========

        // p1 (sorulan kişi) erkek ise eklenebilecek erkek rolleri
        if (p1IsMale) {
            allPossibleKeys.add("relationship.parent.father");
            allPossibleKeys.add("relationship.child.son");
            allPossibleKeys.add("relationship.sibling.brother");
            allPossibleKeys.add("relationship.grandparent.grandfather");
            allPossibleKeys.add("relationship.grandchild.grandson");
            allPossibleKeys.add("relationship.maternal_uncle");
            allPossibleKeys.add("relationship.paternal_uncle");
            allPossibleKeys.add("relationship.nephew");
            allPossibleKeys.add("relationship.cousin.male");
            allPossibleKeys.add("relationship.inlaw.father");
            allPossibleKeys.add("relationship.inlaw.brother");
            allPossibleKeys.add("relationship.sibling_spouse.male"); // Enişte
        }
        // p1 (sorulan kişi) kadın ise eklenebilecek kadın rolleri
        else {
            allPossibleKeys.add("relationship.parent.mother");
            allPossibleKeys.add("relationship.child.daughter");
            allPossibleKeys.add("relationship.sibling.sister");
            allPossibleKeys.add("relationship.grandparent.grandmother");
            allPossibleKeys.add("relationship.grandchild.granddaughter");
            allPossibleKeys.add("relationship.maternal_aunt");
            allPossibleKeys.add("relationship.paternal_aunt");
            allPossibleKeys.add("relationship.niece");
            allPossibleKeys.add("relationship.cousin.female");
            allPossibleKeys.add("relationship.inlaw.mother");
            allPossibleKeys.add("relationship.inlaw.sister_of_wife");
            allPossibleKeys.add("relationship.inlaw.sister_of_husband");
            allPossibleKeys.add("relationship.sibling_spouse.female"); // Yenge
        }
        
        // Cinsiyetten bağımsız veya her iki cinsiyete de uygulanabilenler
        allPossibleKeys.add("relationship.spouse");
        
        // p2'ye (referans alınan kişi) göre damat/gelin ekle
        if(p2IsMale) {
            allPossibleKeys.add("relationship.inlaw.daughter"); // Gelini
        } else {
            allPossibleKeys.add("relationship.inlaw.son"); // Damadı
        }

        // Eş kardeşinin eşi (Bacanak/Elti) - Sadece Hard seviyede
        if (difficulty == Difficulty.HARD) {
            if (p1IsMale) {
                 allPossibleKeys.add("relationship.spouse_sibling_spouse.bacanak");
            } else {
                 allPossibleKeys.add("relationship.spouse_sibling_spouse.elti");
            }
        }

        // Zorluk seviyesine göre ek seçenekler
        if (difficulty == Difficulty.HARD) {
            allPossibleKeys.add("relationship.distant_relative");
        }
        
        // Her zaman "İlişki yok" seçeneği ekle
        allPossibleKeys.add("game.distractor.no_relation");

        // Doğru cevapları ve kabul edilebilir alternatiflerini seçenek havuzundan çıkar
        optionKeys.forEach(allPossibleKeys::remove);
        if (acceptableCorrectAnswerKeys != null) {
            acceptableCorrectAnswerKeys.forEach(allPossibleKeys::remove);
        }

        // Kategoriye uygun seçenekleri öncelendir
        List<String> prioritizedKeys = new ArrayList<>();
        List<String> remainingKeys = new ArrayList<>();
        
        for (String key : allPossibleKeys) {
            String keyCategory = getRelationshipCategory(key);
            if (keyCategory.equals(correctCategory) || isRelatedCategory(correctCategory, keyCategory)) {
                prioritizedKeys.add(key);
            } else {
                remainingKeys.add(key);
            }
        }
        
        Collections.shuffle(prioritizedKeys);
        Collections.shuffle(remainingKeys);
        
        // Önce benzer kategorilerden, sonra diğerlerinden seç
        List<String> allKeys = new ArrayList<>(prioritizedKeys);
        allKeys.addAll(remainingKeys);

        int optionsCount = gameProperties.getOptionsCount(difficulty);

        for (String key : allKeys) {
            if (optionKeys.size() >= optionsCount) break;
            optionKeys.add(key);
        }

        // Anahtarları çevirilen metinlere dönüştür ve çeviri kalitesini kontrol et
        List<String> translatedOptions = new ArrayList<>();
        for (String key : optionKeys) {
            String translatedText = getMessage(key, locale);
            // Çevrilmemiş key'leri atlama
            if (!translatedText.equals(key) && !translatedText.startsWith("relationship.")) {
                translatedOptions.add(translatedText);
            }
        }
        
        // Minimum seçenek sayısını garanti et
        while (translatedOptions.size() < 3) {
            translatedOptions.add("İlişki yok");
        }

        Collections.shuffle(translatedOptions);
        return translatedOptions;
    }
    
    /**
     * İki kategori arasında benzerlik olup olmadığını kontrol eder
     */
    private boolean isRelatedCategory(String category1, String category2) {
        if (category1 == null || category2 == null) return false;
        
        // Benzer kategoriler
        Set<String> familyCore = Set.of("direct", "siblings");
        Set<String> familyExtended = Set.of("grandparent", "grandchild", "aunt_uncle", "nephew_niece");
        Set<String> familyInlaw = Set.of("inlaw");
        Set<String> familyCousin = Set.of("cousin");
        
        if (familyCore.contains(category1) && familyCore.contains(category2)) return true;
        if (familyExtended.contains(category1) && familyExtended.contains(category2)) return true;
        if (familyInlaw.contains(category1) && familyInlaw.contains(category2)) return true;
        if (familyCousin.contains(category1) && familyCousin.contains(category2)) return true;
        
        return false;
    }

    private boolean isUnwantedRelationshipKey(String messageKey) {
        if (messageKey == null) return true;
        
        // İlişki bulunamayan veya belirsiz durumlar
        if (messageKey.equals("relationship.not_found") ||
            messageKey.equals("relationship.distant_relative") ||
            messageKey.equals("relationship.error") ||
            messageKey.equals("relationship.unknown") ||
            messageKey.contains("indirect") ||
            messageKey.contains("friend") ||
            messageKey.contains("colleague") ||
            messageKey.contains("acquaintance")) {
            return true;
        }
        
        // SADECE MUĞLAK/TEKNİK TERİMLER YASAK - TÜRK AİLE YAPISINA UYGUN TERİMLER İZİNLİ
        if (messageKey.contains("inlaw.sibling") ||   // Genel "kayın kardeş" - muğlak
            messageKey.contains("inlaw.child") ||     // Genel "kayın çocuk" - muğlak  
            messageKey.contains("inlaw.parent")) {    // Genel "kayın ebeveyn" - muğlak
            return true;
        }
        
        // Spesifik ve kesin tanımlı Türk aile yapısı terimleri İZİNLİ:
        // - relationship.inlaw.sister_of_wife (Baldızı)
        // - relationship.inlaw.sister_of_husband (Görümcesi)  
        // - relationship.spouse_sibling_spouse.bacanak (Bacanağı)
        // - relationship.spouse_sibling_spouse.elti (Eltisi)
        // - relationship.sibling_spouse.male (Eniştesi)
        // - relationship.sibling_spouse.female (Yengesi)
        
        // GENEL/BELİRSİZ TERİMLER YASAK
        if (messageKey.contains("aunt_uncle") ||   // Amca/Dayı/Hala/Teyze - belirsiz
            messageKey.contains("nephew_niece") || // Yeğeni - cinsiyet belirsiz
            messageKey.contains("grandparent") ||  // Büyükebeveyn - cinsiyet belirsiz
            messageKey.contains("grandchild")) {   // Torun - cinsiyet belirsiz
            return true;
        }
        
        return false;
    }

    private boolean isAppropriateForDifficulty(RelationshipDescriptionResult relationshipResult, Difficulty gameDifficulty, Person p1, Person p2) {
        if (relationshipResult.getStatus() != RelationshipStatus.FOUND) {
            return false;
        }
        
        if (p1 == null || p2 == null) {
            log.warn("isAppropriateForDifficulty called with null parameters: p1={}, p2={}", p1, p2);
            return false;
        }

        String messageKey = relationshipResult.getMessageKey();
        int determinedPathLength;
        String relationshipCategory = getRelationshipCategory(messageKey);
        String logDetails = String.format("P1:%d, P2:%d, Key:'%s', Category:%s, DirectType:%s",
                p1.getId(), p2.getId(), messageKey, relationshipCategory, relationshipResult.getDirectTypeIfApplicable());

        if (relationshipResult.getDirectTypeIfApplicable() != null) {
            determinedPathLength = 1;
        } else {
            List<RelationshipStepDTO> actualPath = relationshipService.getRelationshipPath(p1, p2);
            if (actualPath != null && !actualPath.isEmpty()) {
                determinedPathLength = actualPath.size();
            } else {
                // Kategori tabanlı ilişki tahmini
                switch (relationshipCategory) {
                    case "direct":
                        determinedPathLength = 1;
                        break;
                    case "siblings":
                        determinedPathLength = 2;
                        break;
                    case "grandparent":
                    case "grandchild":
                        determinedPathLength = 2;
                        break;
                    case "aunt_uncle":
                    case "nephew_niece":
                        determinedPathLength = 3;
                        break;
                    case "cousin":
                        determinedPathLength = 4;
                        break;
                    case "inlaw":
                        determinedPathLength = messageKey.contains(".parent") || messageKey.contains(".child") ? 2 : 3;
                        break;
                    case "step":
                        determinedPathLength = messageKey.contains(".parent") || messageKey.contains(".child") ? 1 : 2;
                        break;
                    case "distant":
                        determinedPathLength = 4;
                        break;
                    case "undefined":
                    case "error":
                        determinedPathLength = 5;
                        log.warn("Belirlenmesi zor ilişki: {} - Zorluk 5 olarak kabul edildi", logDetails);
                        break;
                    default:
                        determinedPathLength = 3;
                        log.warn("İlişki kategorisi belirlenemedi: {} - Orta zorluk (3) olarak kabul edildi", logDetails);
                }
            }
        }

        if (determinedPathLength == 0) {
            log.warn("Path length for {} was 0 despite FOUND status. Correcting to 1.", logDetails);
            determinedPathLength = 1;
        }

        final int MAX_ALLOWED_PATH_FOR_GAME = 5;
        if (determinedPathLength > MAX_ALLOWED_PATH_FOR_GAME) {
            log.warn("Path length {} for {} exceeds MAX_ALLOWED_PATH_FOR_GAME {}. Clamping to MAX.", determinedPathLength, logDetails, MAX_ALLOWED_PATH_FOR_GAME);
            determinedPathLength = MAX_ALLOWED_PATH_FOR_GAME;
        }

        int relationshipComplexity = calculateRelationshipComplexity(relationshipCategory, determinedPathLength);
        log.info("İlişki karmaşıklığı: P1:{}, P2:{}, İlişki:'{}', Kategori:{}, Yol:{}, Karmaşıklık:{}",
            p1.getId(), p2.getId(), messageKey, relationshipCategory, determinedPathLength, relationshipComplexity);

        boolean isAppropriate = false;
        switch (gameDifficulty) {
            case EASY:
                // Kolay: Temel ilişkiler (1. derece akraba, karmaşıklık 1-2)
                // Doğrudan aile bağları: ebeveyn, çocuk, eş, kardeş, büyükanne/büyükbaba
                isAppropriate = relationshipComplexity <= 2 && 
                    (relationshipCategory.equals("direct") || 
                     relationshipCategory.equals("siblings") || 
                     relationshipCategory.equals("grandparent") || 
                     relationshipCategory.equals("grandchild"));
                break;
            case MEDIUM:
                // Orta: Genişletilmiş ilişkiler (amca, dayı, hala, teyze, yeğen, kuzen)
                isAppropriate = (relationshipComplexity >= 2 && relationshipComplexity <= 4) && 
                    (relationshipCategory.equals("aunt_uncle") || 
                     relationshipCategory.equals("nephew_niece") || 
                     relationshipCategory.equals("cousin") ||
                     relationshipCategory.equals("siblings") ||
                     relationshipCategory.equals("grandparent") ||
                     relationshipCategory.equals("grandchild"));
                break;
            case HARD:
                // Zor: Karmaşık ilişkiler (kayın ilişkileri, uzak akrabalar)
                isAppropriate = relationshipComplexity >= 2 && relationshipComplexity <= MAX_ALLOWED_PATH_FOR_GAME;
                break;
            default:
                isAppropriate = false;
                break;
        }
        log.debug("P1:{}, P2:{}, Key:{}, İlişki Kategorisi:{}, Yol:{}, Karmaşıklık:{}, Zorluk:{}, Uygun:{}",
            p1.getId(), p2.getId(), messageKey, relationshipCategory, determinedPathLength, relationshipComplexity, gameDifficulty, isAppropriate);
        return isAppropriate;
    }

    /**
     * İlişki anahtarından kategori belirler
     */
    private String getRelationshipCategory(String messageKey) {
        if (messageKey == null) return "undefined";

        if (messageKey.startsWith("relationship.direct.") ||
            messageKey.equals("relationship.parent_child.parent") ||
            messageKey.equals("relationship.parent_child.child") ||
            messageKey.equals("relationship.spouse.is_spouse_of") ||
            messageKey.equals("relationship.parent") ||
            messageKey.equals("relationship.child") ||
            messageKey.equals("relationship.spouse")) {
            return "direct";
        }

        if (messageKey.contains("sibling")) return "siblings";
        if (messageKey.contains("grandparent") || messageKey.contains("grandfather") || messageKey.contains("grandmother")) return "grandparent";
        if (messageKey.contains("grandchild")) return "grandchild";
        
        // Anne ve baba tarafı akrabalar
        if (messageKey.equals("relationship.maternal_uncle") || 
            messageKey.equals("relationship.paternal_uncle") ||
            messageKey.equals("relationship.maternal_aunt") ||
            messageKey.equals("relationship.paternal_aunt") ||
            messageKey.contains("aunt") || messageKey.contains("uncle")) {
            return "aunt_uncle";
        }
        
        if (messageKey.contains("nephew") || messageKey.contains("niece")) return "nephew_niece";
        if (messageKey.contains("cousin")) return "cousin";
        
        // Kayın ilişkileri ve özel durumlar - Türk aile yapısına uygun  
        if (messageKey.contains("inlaw") || 
            messageKey.contains("sibling_spouse") ||           // Enişte/Yenge
            messageKey.contains("spouse_sibling_spouse")) {    // Bacanak/Elti
            return "inlaw";
        }
        
        if (messageKey.contains("step")) return "step";
        if (messageKey.contains("distant")) return "distant";
        if (messageKey.equals("relationship.not_found")) return "not_found";
        if (messageKey.contains("error")) return "error";

        return "other";
    }

    /**
     * İlişki karmaşıklığını hesaplar (yol uzunluğu ve ilişki kategorisine göre)
     */
    private int calculateRelationshipComplexity(String relationshipCategory, int pathLength) {
        int baseComplexity = pathLength;

        // Bazı kategoriler için karmaşıklık ayarlamaları
        switch (relationshipCategory) {
            case "direct":
                return 1; // Doğrudan ilişkiler her zaman 1 (en kolay)
            case "siblings":
                return 2; // Kardeş ilişkileri 2
            case "inlaw":
                return Math.min(pathLength + 1, 5); // Kayın ilişkileri biraz daha karmaşık
            case "step":
                return Math.min(pathLength + 1, 4); // Üvey ilişkiler biraz daha karmaşık
            case "distant":
                return 4; // Uzak ilişkiler
            case "undefined":
            case "error":
            case "not_found":
                return 5; // En karmaşık (belirlenemeyen, hatalı, bulunamayan)
            default:
                return baseComplexity; // Diğerleri için yol uzunluğu baz alınır
        }
    }

    private int calculatePointsInternal(boolean isCorrect, Difficulty difficulty, long timeTakenInSeconds, int currentStreakValue) {
        if (!isCorrect) return 0;

        int basePoints = switch (difficulty) {
            case EASY -> 10;
            case MEDIUM -> 15;
            case HARD -> 25;
        };

        double timeMultiplier = 1.0;
        if (timeTakenInSeconds <= 3) timeMultiplier = 1.8;
        else if (timeTakenInSeconds <= 7) timeMultiplier = 1.4;
        else if (timeTakenInSeconds <= 12) timeMultiplier = 1.1;
        else if (timeTakenInSeconds > 25) timeMultiplier = 0.6;
        else if (timeTakenInSeconds > 18) timeMultiplier = 0.8;

        int streakBonus = 0;
        int newStreak = currentStreakValue + 1;
        if (newStreak >= 2) {
            streakBonus = (newStreak / 2) * (difficulty == Difficulty.HARD ? 5 : (difficulty == Difficulty.MEDIUM ? 3 : 2));
        }

        int totalPoints = (int)(basePoints * timeMultiplier) + streakBonus;
        return Math.max(difficulty == Difficulty.EASY ? 3 : (difficulty == Difficulty.MEDIUM ? 5 : 7), totalPoints);
    }

    @Override
    @Transactional
    public GameResultDTO recordGameResult(RecordScoreRequestDTO scoreDetails) {
        log.info("Oyun sonucu kaydediliyor: Oyuncu '{}', Skor '{}', Zorluk '{}'",
                scoreDetails.getPlayerName(), scoreDetails.getScore(), scoreDetails.getDifficulty());

        if (scoreDetails.getPlayerName() == null || scoreDetails.getPlayerName().trim().isEmpty()) {
            log.warn("Geçersiz oyuncu adı ile skor kaydı yapılamaz: {}", scoreDetails.getPlayerName());
            throw new GameException(getMessage("game.error.invalid_player_name", LocaleContextHolder.getLocale()));
        }

        if (scoreDetails.getDifficulty() == null) {
            log.warn("Zorluk seviyesi olmadan skor kaydedilemez");
            throw new GameException(getMessage("game.error.difficulty_required", LocaleContextHolder.getLocale()));
        }

        if (scoreDetails.getScore() < 0) {
            log.warn("Negatif skor kaydedilemez: {}", scoreDetails.getScore());
            throw new GameException(getMessage("game.error.negative_score", LocaleContextHolder.getLocale()));
        }

        String playerName = scoreDetails.getPlayerName().trim();
        Difficulty difficulty = scoreDetails.getDifficulty();
        int newScore = scoreDetails.getScore();

        HighScore existingHighScore = highScoreRepository.findTopByPlayerNameAndDifficultyOrderByScoreDesc(playerName, difficulty);

        HighScore scoreToSaveOrUpdate;
        boolean newHighScoreBeaten = false;

        if (existingHighScore != null) {
            if (newScore > existingHighScore.getScore()) {
                // Mevcut yüksek skoru güncelle
                existingHighScore.setScore(newScore);
                existingHighScore.setCorrectAnswers(scoreDetails.getCorrectAnswers() >= 0 ? scoreDetails.getCorrectAnswers() : 0);
                existingHighScore.setTotalQuestions(scoreDetails.getTotalQuestions() >= 0 ? scoreDetails.getTotalQuestions() : 0);
                existingHighScore.setMaxStreak(scoreDetails.getMaxStreak() >= 0 ? scoreDetails.getMaxStreak() : 0);
                existingHighScore.setPlayedAt(LocalDateTime.now());
                scoreToSaveOrUpdate = existingHighScore;
                newHighScoreBeaten = true;
                log.info("Oyuncu '{}' için {} zorluğundaki mevcut yüksek skor ({}) aşıldı. Yeni skor: {}", playerName, difficulty, existingHighScore.getScore(), newScore);
            } else {
                // Düşük skor olsa da oyun sonucunu kaydet, sadece yüksek skor tablosunu güncelleme
                log.info("Oyuncu '{}' için {} zorluğundaki yeni skor ({}), mevcut en yüksek skordan ({}) düşük veya eşit. Yüksek skor tablosu güncellenmeyecek ancak oyun sonucu kaydediliyor.", playerName, difficulty, newScore, existingHighScore.getScore());
                // Mevcut en yüksek skoru referans olarak kullan ama yeni GameResultDTO döndür
                scoreToSaveOrUpdate = null;
                newHighScoreBeaten = false;
            }
        } else {
            // Bu oyuncu ve zorluk için ilk skor
            HighScore newHighScoreEntry = new HighScore();
            newHighScoreEntry.setPlayerName(playerName);
            newHighScoreEntry.setScore(newScore);
            newHighScoreEntry.setDifficulty(difficulty);
            newHighScoreEntry.setCorrectAnswers(scoreDetails.getCorrectAnswers() >= 0 ? scoreDetails.getCorrectAnswers() : 0);
            newHighScoreEntry.setTotalQuestions(scoreDetails.getTotalQuestions() >= 0 ? scoreDetails.getTotalQuestions() : 0);
            newHighScoreEntry.setMaxStreak(scoreDetails.getMaxStreak() >= 0 ? scoreDetails.getMaxStreak() : 0);
            newHighScoreEntry.setPlayedAt(LocalDateTime.now());
            scoreToSaveOrUpdate = newHighScoreEntry;
            newHighScoreBeaten = true; // Yeni bir en yüksek skor
            log.info("Oyuncu '{}' için {} zorluğunda ilk skor kaydediliyor: {}", playerName, difficulty, newScore);
        }

        try {
            if (scoreToSaveOrUpdate != null) {
                HighScore savedScore = highScoreRepository.save(scoreToSaveOrUpdate);
                log.info("Oyun sonucu başarıyla {} (ID: {}), Oyuncu '{}'", existingHighScore != null && newHighScoreBeaten ? "güncellendi" : "kaydedildi", savedScore.getId(), savedScore.getPlayerName());
                return convertToGameResultDTO(savedScore);
            } else {
                // Yüksek skor tablosu güncellenmedi ama oyun sonucu kaydedildi
                log.info("Oyuncu '{}' için {} zorluğunda oyun sonucu başarıyla kaydedildi (skor: {})", playerName, difficulty, newScore);
                return GameResultDTO.builder()
                        .playerName(playerName)
                        .score(newScore)
                        .difficulty(difficulty)
                        .date(LocalDate.now())
                        .correctAnswers(scoreDetails.getCorrectAnswers() >= 0 ? scoreDetails.getCorrectAnswers() : 0)
                        .totalQuestions(scoreDetails.getTotalQuestions() >= 0 ? scoreDetails.getTotalQuestions() : 0)
                        .maxStreak(scoreDetails.getMaxStreak() >= 0 ? scoreDetails.getMaxStreak() : 0)
                        .build();
            }
        } catch (Exception e) {
            log.error("HighScore kaydedilirken/güncellenirken hata oluştu: Oyuncu '{}'. Hata: {}", playerName, e.getMessage(), e);
            return GameResultDTO.builder()
                    .playerName(playerName)
                    .score(newScore)
                    .difficulty(difficulty)
                    .date(LocalDate.now())
                    .correctAnswers(scoreDetails.getCorrectAnswers() >= 0 ? scoreDetails.getCorrectAnswers() : 0)
                    .totalQuestions(scoreDetails.getTotalQuestions() >= 0 ? scoreDetails.getTotalQuestions() : 0)
                    .maxStreak(scoreDetails.getMaxStreak() >= 0 ? scoreDetails.getMaxStreak() : 0)
                    .build();
        }
    }

    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Missing message for key '{}' and locale '{}'", code, locale.toLanguageTag());
            return code; // Fallback to the code itself
        }
    }

    private String getMessage(String code, Locale locale) {
        return getMessage(code, locale, (Object[]) null);
    }

    protected synchronized void refreshActivePersonsCache() {
        log.info("Aktif kişi önbelleği yenileniyor...");
        long startTime = System.currentTimeMillis();
        try {
            List<Person> allPersons = personRepository.findAll();
            List<Person> filteredPersons = allPersons.stream()
                .filter(p -> p.getBirthDate() != null && (p.getDeathDate() == null || p.getDeathDate().isAfter(LocalDate.now().minusYears(1))))
                .collect(Collectors.toList());

            if (filteredPersons.isEmpty()) {
                log.warn("Önbelleği yenilemek için aktif kişi bulunamadı. Önbellek boş olacak.");
            }
            this.activePersonsCache = Collections.unmodifiableList(new ArrayList<>(filteredPersons));
            log.info("Aktif kişi önbelleği {} kişi ile yenilendi. Süre: {}ms",
                     this.activePersonsCache.size(), (System.currentTimeMillis() - startTime));
            lastCacheRefreshTimeMillis = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Aktif kişi önbelleği yenilenirken hata oluştu: {}", e.getMessage(), e);
        }
    }

    @Override
    @Cacheable(value = "highScoresByDifficulty", unless = "#result == null")
    @Transactional(readOnly = true)
    public Map<Difficulty, List<GameResultDTO>> getHighScores() {
        log.info("Tüm zorluk seviyeleri için yüksek skorlar istendi");
        Map<Difficulty, List<GameResultDTO>> result = new EnumMap<>(Difficulty.class);
        try {
            for (Difficulty difficulty : Difficulty.values()) {
                log.debug("{} zorluğu için yüksek skorlar alınıyor", difficulty);
                List<HighScore> highScores = highScoreRepository.findTop10ByDifficultyOrderByScoreDesc(difficulty);
                List<GameResultDTO> dtoList = highScores.stream()
                    .map(this::convertToGameResultDTO)
                    .collect(Collectors.toList());
                result.put(difficulty, dtoList);
                log.debug("{} zorluğu için {} adet yüksek skor bulundu", difficulty, dtoList.size());
            }
            log.info("Yüksek skorlar başarıyla getirildi");
            return result;
        } catch (Exception e) {
            log.error("Yüksek skorlar getirilirken hata oluştu: {}", e.getMessage(), e);
            Map<Difficulty, List<GameResultDTO>> emptyResult = new EnumMap<>(Difficulty.class);
            for (Difficulty difficulty : Difficulty.values()) {
                emptyResult.put(difficulty, new ArrayList<>());
            }
            return emptyResult;
        }
    }

    private GameResultDTO convertToGameResultDTO(HighScore score) {
        if (score == null) return null;
        return GameResultDTO.builder()
                .playerName(score.getPlayerName())
                .score(score.getScore())
                .difficulty(score.getDifficulty())
                .date(score.getPlayedAt().toLocalDate())
                .correctAnswers(score.getCorrectAnswers())
                .totalQuestions(score.getTotalQuestions())
                .maxStreak(score.getMaxStreak())
                .build();
    }

    protected void refreshSpecificDifficultyQuestions(Difficulty difficulty, int numberOfQuestionsToGenerate) {
        log.info("{} zorluğu için {} adet eksik soru üretimi tetiklendi.", difficulty, numberOfQuestionsToGenerate);
        List<GameQuestionDTO> questionsForDifficulty = preGeneratedQuestions.get(difficulty);
        if (questionsForDifficulty == null) {
            log.error("refreshSpecificDifficultyQuestions: {} zorluğu için soru listesi null! Bu olmamalı.", difficulty);
            questionsForDifficulty = Collections.synchronizedList(new ArrayList<>());
            preGeneratedQuestions.put(difficulty, questionsForDifficulty);
        }

        int generatedCount = 0;
        long startTime = System.currentTimeMillis();
        while (questionsForDifficulty.size() < PRE_GENERATED_QUESTIONS_COUNT && generatedCount < numberOfQuestionsToGenerate) {
            if (System.currentTimeMillis() - startTime > TimeUnit.SECONDS.toMillis(20)) {
                log.warn("{} zorluğu için {} adet soru üretimi 20 saniyeyi aştı, durduruluyor. Mevcut: {}, İstenen Ek: {}",
                         difficulty, generatedCount, questionsForDifficulty.size(), numberOfQuestionsToGenerate);
                break;
            }
            GameQuestionDTO question = generateQuestionInternal(difficulty,
                questionsForDifficulty.stream()
                                      .map(GameQuestionDTO::getId)
                                      .collect(Collectors.toSet()), LocaleContextHolder.getLocale());
            if (question != null) {
                synchronized (questionsForDifficulty) {
                    if (questionsForDifficulty.size() < PRE_GENERATED_QUESTIONS_COUNT &&
                        !questionsForDifficulty.stream().anyMatch(q -> q.getId().equals(question.getId()))) {
                        questionsForDifficulty.add(question);
                        generatedCount++;
                    }
                }
            } else {
                log.warn("{} zorluğu için refresh sırasında null soru geldi.", difficulty);
                break;
            }
        }
        if (generatedCount > 0) {
            log.info("{} zorluğu için {} adet eksik soru üretildi (Toplam havuzda: {}). Süre: {}ms",
                difficulty, generatedCount, questionsForDifficulty.size(), System.currentTimeMillis() - startTime);
        }
    }

    private List<Person> getActivePersonsForQuestionGeneration() {
        List<Person> currentCache = this.activePersonsCache;
        if (currentCache.isEmpty() || System.currentTimeMillis() - lastCacheRefreshTimeMillis > TimeUnit.MINUTES.toMillis(ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES / 2L)) {
            synchronized(cacheLock) {
                currentCache = this.activePersonsCache;
                if (currentCache.isEmpty() || System.currentTimeMillis() - lastCacheRefreshTimeMillis > TimeUnit.MINUTES.toMillis(ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES / 2L)) {
                    log.info("Aktif kişi önbelleği boş veya eski, senkronize olarak yenileniyor...");
                    refreshActivePersonsCache();
                    currentCache = this.activePersonsCache;
                }
            }
        }
        if (currentCache.isEmpty()) {
            log.warn("Aktif kişi önbelleği yenileme sonrası hala boş.");
        }
        return currentCache;
    }

    private GameQuestionDTO createQuestionDTO(Person p1, Person p2, RelationshipDescriptionResult descResult, Difficulty difficulty, Locale locale) {
        String questionText = getMessage("game.question.format", locale, p1.getFirstName(), p2.getFirstName());
        String correctAnswerKey = descResult.getMessageKey();
        List<String> acceptableAnswerKeys = descResult.getAcceptableMessageKeys();

        List<String> options = generateOptions(difficulty, correctAnswerKey, acceptableAnswerKeys, locale, p1, p2);

        String correctAnswerText = getMessage(correctAnswerKey, locale);
        
        // Path'i zorluk seviyesine göre sadeleştir
        List<RelationshipStepDTO> relationshipPath = relationshipService.getRelationshipPath(p1, p2);
        List<RelationshipStepDTO> filteredPath = filterPathByDifficulty(relationshipPath, difficulty);

        return GameQuestionDTO.builder()
                .id(descResult.getMessageKey())
                .questionText(questionText)
                .person1(p1.getFirstName() + " " + p1.getLastName())
                .person2(p2.getFirstName() + " " + p2.getLastName())
                .options(options)
                .correctAnswer(correctAnswerText)
                .difficulty(difficulty)
                .timeLimit(gameProperties.getTimeLimit(difficulty))
                .person1Info(personMapper.personToPersonInfo(p1))
                .person2Info(personMapper.personToPersonInfo(p2))
                .relationshipPath(filteredPath)
                .build();
    }
    
    /**
     * Çevrilmemiş message key'ler için SADECE KEŞİN fallback çeviriler
     * Muğlak terimler kullanılmaz!
     */
    private String getFallbackTranslation(String messageKey, Locale locale) {
        if (messageKey == null) return "İlişki Tanımlanamadı";
        
        // SADECE KEŞİN KAYIN İLİŞKİLERİ
        if (messageKey.contains("inlaw.son")) return "Damadı";
        if (messageKey.contains("inlaw.daughter")) return "Gelini";
        if (messageKey.contains("inlaw.brother")) return "Kayınbiraderi";
        if (messageKey.contains("inlaw.sister")) return "Görümcesi";  // NET: sadece görümce
        if (messageKey.contains("inlaw.father")) return "Kayınpederi";
        if (messageKey.contains("inlaw.mother")) return "Kayınvalidesi";
        
        // KEŞİN BÜYÜKEBEVEYN/TORUN İLİŞKİLERİ
        if (messageKey.contains("grandfather")) return "Dedesi";
        if (messageKey.contains("grandmother")) return "Nenesi";
        if (messageKey.contains("grandson")) return "Erkek Torunu";
        if (messageKey.contains("granddaughter")) return "Kız Torunu";
        
        // NET AMCA/DAYΙ/HALA/TEYZE
        if (messageKey.contains("paternal_uncle")) return "Amcası";
        if (messageKey.contains("maternal_uncle")) return "Dayısı";
        if (messageKey.contains("paternal_aunt")) return "Halası";
        if (messageKey.contains("maternal_aunt")) return "Teyzesi";
        
        // KEŞİN TEMEL İLİŞKİLER
        if (messageKey.contains("nephew")) return "Erkek Yeğeni";
        if (messageKey.contains("niece")) return "Kız Yeğeni";
        if (messageKey.contains("cousin.male")) return "Erkek Kuzeni";
        if (messageKey.contains("cousin.female")) return "Kız Kuzeni";
        if (messageKey.contains("sibling.brother")) return "Erkek Kardeşi";
        if (messageKey.contains("sibling.sister")) return "Kız Kardeşi";
        if (messageKey.contains("parent.father")) return "Babası";
        if (messageKey.contains("parent.mother")) return "Annesi";
        if (messageKey.contains("child.son")) return "Oğlu";
        if (messageKey.contains("child.daughter")) return "Kızı";
        if (messageKey.contains("spouse")) return "Eşi";
        
        // FALLBACK: Genel belirsiz terimler KULLANILMAZ
        log.warn("Kesin tanım bulunamadı: {}", messageKey);
        return "İlişki Belirsiz";
    }

    /**
     * Zorluk derecesine göre ilişki yolunu filtreler
     * Artık "?" gösterimini kullanmıyoruz, tüm zorluk seviyelerinde gerçek ilişkileri gösteriyoruz
     */
    private List<RelationshipStepDTO> filterPathByDifficulty(List<RelationshipStepDTO> originalPath, Difficulty difficulty) {
        if (originalPath == null || originalPath.isEmpty()) {
            return originalPath;
        }

        switch (difficulty) {
            case EASY:
                // Kolay seviyede: Sadece doğrudan ilişkileri göster (en fazla 2 adım)
                if (originalPath.size() <= 2) {
                    return originalPath;
                } else {
                    // Çok uzun yol varsa kısalt, ancak anlamlı bağlantıları koru
                    return originalPath.subList(0, Math.min(originalPath.size(), 2));
                }

            case MEDIUM:
                // Orta seviyede: Orta uzunluktaki yolları göster (en fazla 4 adım)
                if (originalPath.size() <= 4) {
                    return originalPath;
                } else {
                    return originalPath.subList(0, Math.min(originalPath.size(), 4));
                }

            case HARD:
            default:
                // Zor seviyede: Tüm düğümler ve ilişki etiketlerini göster (sınırsız)
                return originalPath;
        }
    }
    
    /**
     * Soru kalitesini kontrol eder - SADECE KEŞİN İLİŞKİLER KABUL EDİLİR
     */
    private boolean isQuestionQualityGood(GameQuestionDTO question, RelationshipDescriptionResult descResult) {
        // Soru metninin geçerli olup olmadığını kontrol et
        if (question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
            log.warn("Soru metni boş");
            return false;
        }
        
        // Doğru cevabın teknik terim içerip içermediğini kontrol et
        String correctAnswer = question.getCorrectAnswer();
        if (correctAnswer.startsWith("relationship.") || correctAnswer.contains("inlaw.")) {
            log.warn("Doğru cevap çevrilmemiş teknik terim içeriyor: {}", correctAnswer);
            return false;
        }
        
        // MUĞLAK TERİMLERİN KULLANILIP KULLANILMADIĞINI KONTROL ET
        String[] forbiddenTerms = {
            "Baldızı", "Eltisi", "Bacanağı", "Kayını", 
            "Büyükbabası", "Büyükannesi", // "Dedesi", "Nenesi" kullanılmalı
            "Akrabası", "Belirsiz", "Uzak",
            "relationship.unknown", "relationship.distant"
        };
        
        for (String forbiddenTerm : forbiddenTerms) {
            if (correctAnswer.contains(forbiddenTerm)) {
                log.warn("Doğru cevap muğlak terim içeriyor: {} -> {}", forbiddenTerm, correctAnswer);
                return false;
            }
        }
        
        // Seçeneklerin kalitesini kontrol et
        for (String option : question.getOptions()) {
            if (option.startsWith("relationship.") || option.contains("inlaw.")) {
                log.warn("Seçenek çevrilmemiş teknik terim içeriyor: {}", option);
                return false;
            }
            
            // Muğlak terimler seçeneklerde de kabul edilmez
            for (String forbiddenTerm : forbiddenTerms) {
                if (option.contains(forbiddenTerm)) {
                    log.warn("Seçenek muğlak terim içeriyor: {} -> {}", forbiddenTerm, option);
                    return false;
                }
            }
        }
        
        // Minimum seçenek sayısını kontrol et
        if (question.getOptions().size() < 3) {
            log.warn("Yetersiz seçenek sayısı: {}", question.getOptions().size());
            return false;
        }
        
        // İlişki yolunun mantıklı olup olmadığını kontrol et
        if (question.getRelationshipPath() != null && question.getRelationshipPath().size() > 6) {
            log.warn("İlişki yolu çok uzun: {} adım", question.getRelationshipPath().size());
            return false;
        }
        
        return true;
    }

    @Override
    public GameAnalysisDTO getGameAnalysis(String sessionId) {
        log.info("Oyun analizi isteniyor, Session ID: {}", sessionId);
        
        GameSession session = activeGameSessions.get(sessionId);
        if (session == null) {
            log.warn("Session bulunamadı: {}", sessionId);
            throw new GameException("Oyun oturumu bulunamadı: " + sessionId);
        }
        
        try {
            GameAnalysisDTO analysis = gameAnalysisService.analyzeGameSession(session);
            log.info("Analiz başarıyla oluşturuldu, Session: {}", sessionId);
            return analysis;
        } catch (Exception e) {
            log.error("Analiz oluşturulurken hata: {}", e.getMessage(), e);
            throw new GameException("Analiz oluşturulurken hata: " + e.getMessage());
        }
    }

    @Override
    public GameAnalysisDTO endGame(String sessionId) {
        log.info("Oyun bitiriliyor, Session ID: {}", sessionId);
        
        GameSession session = activeGameSessions.get(sessionId);
        if (session == null) {
            log.warn("Session bulunamadı: {}", sessionId);
            throw new GameException("Oyun oturumu bulunamadı: " + sessionId);
        }
        
        try {
            // Session'ı pasif hale getir
            session.setActive(false);
            
            // Final analizi oluştur
            GameAnalysisDTO finalAnalysis = gameAnalysisService.analyzeGameSession(session);
            
            // Session'ı temizle
            activeGameSessions.remove(sessionId);
            
            log.info("Oyun başarıyla bitirildi ve analiz oluşturuldu, Session: {}", sessionId);
            return finalAnalysis;
        } catch (Exception e) {
            log.error("Oyun bitirilirken hata: {}", e.getMessage(), e);
            throw new GameException("Oyun bitirilirken hata: " + e.getMessage());
        }
    }
} 