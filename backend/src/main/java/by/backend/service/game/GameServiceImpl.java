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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import by.backend.model.dto.RelationshipPathDTO;

@Service
public class GameServiceImpl implements GameService {

    private final PersonRepository personRepository;
    private final HighScoreRepository highScoreRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;
    private final GameProperties gameProperties;
    private final AnalysisService analysisService;
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
                         AnalysisService analysisService) {
        this.personRepository = personRepository;
        this.highScoreRepository = highScoreRepository;
        this.relationshipService = relationshipService;
        this.personMapper = personMapper;
        this.messageSource = messageSource;
        this.gameProperties = gameProperties;
        this.analysisService = analysisService;
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
        GameQuestionDTO firstQuestion = generateQuestionInternal(difficulty, Collections.emptySet(), locale);
        if (firstQuestion == null) {
            log.error("startGame: İlk soru üretilemedi. Oyuncu: {}, Zorluk: {}", playerName, difficulty);
            throw new GameException(getMessage("game.error.start_failed_no_question", locale));
        }
        GameQuestionDTO questionToSend = firstQuestion.toBuilder().build();
        questionToSend.setCorrectAnswer(null);
        questionToSend.setRelationshipPath(null);

        return InitialGameDataDTO.builder()
                .firstQuestion(questionToSend)
                .playerName(playerName)
                .difficulty(difficulty)
                .build();
    }

    @Override
    @Transactional
    public AnswerResponseDTO answerQuestion(GameAnswerDTO answerDetails, Locale locale) {
        String sessionId = answerDetails.getSessionId();
        log.debug("Processing answer for session '{}', question '{}'", sessionId, answerDetails.getQuestionId());

        // Eğer sessionId null veya boş ise, bir session oluşturmaya gerek yok
        // Doğrudan soruyu doğrulama kısmına geçebiliriz
        GameSession session = null;
        if (sessionId != null && !sessionId.isEmpty()) {
            session = activeGameSessions.get(sessionId);

            if (session != null && session.isGameOver()) {
                log.warn("Answer received for already finished game session '{}'.", sessionId);
                activeGameSessions.remove(sessionId);
                return createGameOverResponse(session, "game.error.game_already_over", locale);
            }
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
            correctAnswerText = getMessage(correctAnswerKey, locale, p1.getFirstName(), p2.getFirstName());
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

                session.recordAnswer(new PlayerAnswer(answerDetails.getQuestionId(), answerDetails.getAnswer(), correctAnswerText, true), pointsEarned);

                log.info("Correct answer by '{}' for question '{}', earned {} points. New score: {}, streak: {}",
                    session.getPlayerName(), answerDetails.getQuestionId(), pointsEarned, session.getScore().get(), session.getCurrentStreak().get());

                updatedScore = session.getScore().get();
                updatedStreak = session.getCurrentStreak().get();
            } else {
                session.recordAnswer(new PlayerAnswer(answerDetails.getQuestionId(), answerDetails.getAnswer(), correctAnswerText, false), 0);

                log.info("Incorrect answer by '{}' for question '{}'. Score remains: {}, streak reset to 0",
                    session.getPlayerName(), answerDetails.getQuestionId(), session.getScore().get());

                updatedScore = session.getScore().get();
                updatedStreak = 0;
            }

            if (session.isGameOver()) {
                log.info("Game over for session '{}'. Final score: {}, questions answered: {}/{}",
                    sessionId, session.getScore().get(), session.getQuestionsAnswered().get(), session.getTotalQuestions());

                activeGameSessions.remove(sessionId);
                return createGameOverResponse(session, null, locale);
            }
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
        if (answerDetails.getAskedQuestionSignaturesInThisGame() != null) {
            Set<String> askedSignatures = new HashSet<>(answerDetails.getAskedQuestionSignaturesInThisGame());
            nextQuestion = generateNewQuestion(answerDetails.getDifficulty(), askedSignatures);

            if (nextQuestion != null) {
                // Don't send the correct answer to client
                GameQuestionDTO clientQuestion = nextQuestion.toBuilder().build();
                clientQuestion.setCorrectAnswer(null);
                clientQuestion.setRelationshipPath(null);
                nextQuestion = clientQuestion;
            }
        }

        // Build the response (both session and non-session mode)
        AnswerResponseDTO response = AnswerResponseDTO.builder()
                .correctAnswer(isCorrect)
                .correctAnswerText(correctAnswerText)
                .pointsEarned(pointsEarned)
                .updatedScore(updatedScore)
                .updatedStreak(updatedStreak)
                .nextQuestion(nextQuestion)
                .gameOver(false)
                .relationshipPath(relationshipPath)
                .build();

        return response;
    }

    private AnswerResponseDTO createGameOverResponse(GameSession session, String messageKey, Locale locale) {
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
        for (int attempt = 0; attempt < MAX_ATTEMPTS_IN_GENERATE_INTERNAL; attempt++) {
            Person p1 = candidates.get(random.nextInt(candidates.size()));
            Person p2 = candidates.get(random.nextInt(candidates.size()));

            if (p1.getId().equals(p2.getId())) {
                log.debug("Aynı kişiler (p1:{}, p2:{}) denk geldi, tekrar denenecek.", p1.getId(), p2.getId());
                continue;
            }

            String questionSignature = p1.getId() + "_" + p2.getId();

            if (askedSignatures.contains(questionSignature)) {
                 log.debug("Bu kişi çifti ({}-{}) daha önce sorulmuş, tekrar denenecek.", p1.getId(), p2.getId());
                continue;
            }

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

            GameQuestionDTO question = createQuestionDTO(p1, p2, descResult, difficulty);

            if (question.getOptions().size() < 2) {
                log.warn("Soru için yeterli seçenek üretilemedi (P1:{}, P2:{}, Doğru Cevap Key: {}, Seçenek Sayısı: {}).", p1.getId(), p2.getId(), question.getCorrectAnswer(), question.getOptions().size());
                continue;
            }

            log.info("Geçerli soru üretildi: ID {}, P1:{}, P2:{}, Zorluk: {}, Cevap Key: {}", question.getId(), p1.getId(), p2.getId(), difficulty, question.getCorrectAnswer());
            return question;
        }
        log.warn("{} denemeden sonra {} zorluğunda geçerli bir soru üretilemedi.", MAX_ATTEMPTS_IN_GENERATE_INTERNAL, difficulty);
        return null;
    }

    private List<String> generateOptions(Difficulty difficulty, String correctAnswerKey, List<String> acceptableCorrectAnswerKeys) {
        Set<String> optionKeys = new LinkedHashSet<>();
        if (correctAnswerKey != null) {
            optionKeys.add(correctAnswerKey);
        }

        Set<String> allPossibleKeys = new HashSet<>();
        // Genel ve temel anahtarlar
        allPossibleKeys.add("relationship.parent");
        allPossibleKeys.add("relationship.child");
        allPossibleKeys.add("relationship.spouse");
        allPossibleKeys.add("relationship.sibling");
        allPossibleKeys.add("relationship.grandparent");
        allPossibleKeys.add("relationship.grandchild");
        allPossibleKeys.add("relationship.aunt_uncle");
        allPossibleKeys.add("relationship.nephew_niece");
        allPossibleKeys.add("relationship.cousin");
        allPossibleKeys.add("relationship.inlaw.parent");
        allPossibleKeys.add("relationship.inlaw.sibling");
        allPossibleKeys.add("relationship.inlaw.father");
        allPossibleKeys.add("relationship.inlaw.mother");
        allPossibleKeys.add("relationship.no_relation");

        // Doğru cevapları ve kabul edilebilir alternatiflerini seçenek havuzundan çıkar
        optionKeys.forEach(allPossibleKeys::remove);
        if (acceptableCorrectAnswerKeys != null) {
            acceptableCorrectAnswerKeys.forEach(allPossibleKeys::remove);
        }

        List<String> remainingKeys = new ArrayList<>(allPossibleKeys);
        Collections.shuffle(remainingKeys);

        int optionsCount = gameProperties.getOptionsCount(difficulty);

        for (String key : remainingKeys) {
            if (optionKeys.size() >= optionsCount) break;
            optionKeys.add(key);
        }

        List<String> finalOptions = new ArrayList<>(optionKeys);
        Collections.shuffle(finalOptions);
        return finalOptions;
    }

    private boolean isUnwantedRelationshipKey(String messageKey) {
        if (messageKey == null) return true; // Null key istenmeyen olarak kabul edilebilir
        return messageKey.equals("relationship.indirect.relationship.distant") ||
               messageKey.equals("relationship.indirect.relationship.undefined") ||
               messageKey.equals("relationship.friend") || // Örnek: Arkadaş ilişkisi anahtarı
               messageKey.equals("relationship.colleague") || // Örnek: İş arkadaşı anahtarı
               messageKey.contains("acquaintance"); // Örnek: Tanıdık içeren anahtarlar
    }

    private boolean isAppropriateForDifficulty(RelationshipDescriptionResult relationshipResult, Difficulty gameDifficulty, Person p1, Person p2) {
        if (relationshipResult.getStatus() != RelationshipStatus.FOUND) {
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
                isAppropriate = relationshipComplexity <= 2;
                break;
            case MEDIUM:
                // Orta: Genişletilmiş ilişkiler (2. derece akraba, karmaşıklık 2-3)
                isAppropriate = relationshipComplexity >= 2 && relationshipComplexity <= 3;
                break;
            case HARD:
                // Zor: Karmaşık ilişkiler (3+ derece akraba, karmaşıklık 3+)
                isAppropriate = relationshipComplexity >= 3 && relationshipComplexity <= MAX_ALLOWED_PATH_FOR_GAME;
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
            messageKey.equals("relationship.spouse.is_spouse_of")) {
            return "direct";
        }

        if (messageKey.contains("sibling")) return "siblings";
        if (messageKey.contains("grandparent") || messageKey.contains("grandfather") || messageKey.contains("grandmother")) return "grandparent";
        if (messageKey.contains("grandchild")) return "grandchild";
        if (messageKey.contains("aunt") || messageKey.contains("uncle")) return "aunt_uncle";
        if (messageKey.contains("nephew") || messageKey.contains("niece")) return "nephew_niece";
        if (messageKey.contains("cousin")) return "cousin";
        if (messageKey.contains("inlaw")) return "inlaw";
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
                // Mevcut skoru güncelle
                existingHighScore.setScore(newScore);
                existingHighScore.setCorrectAnswers(scoreDetails.getCorrectAnswers() >= 0 ? scoreDetails.getCorrectAnswers() : 0);
                existingHighScore.setTotalQuestions(scoreDetails.getTotalQuestions() >= 0 ? scoreDetails.getTotalQuestions() : 0);
                existingHighScore.setMaxStreak(scoreDetails.getMaxStreak() >= 0 ? scoreDetails.getMaxStreak() : 0);
                existingHighScore.setPlayedAt(LocalDateTime.now());
                scoreToSaveOrUpdate = existingHighScore;
                newHighScoreBeaten = true;
                log.info("Oyuncu '{}' için {} zorluğundaki mevcut yüksek skor ({}) aşıldı. Yeni skor: {}", playerName, difficulty, existingHighScore.getScore(), newScore);
            } else {
                log.info("Oyuncu '{}' için {} zorluğundaki yeni skor ({}), mevcut en yüksek skordan ({}) düşük veya eşit. Skor tablosu güncellenmeyecek.", playerName, difficulty, newScore, existingHighScore.getScore());
                throw new GameException(getMessage("game.error.score_not_high_enough", LocaleContextHolder.getLocale(), existingHighScore.getScore()));
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
            HighScore savedScore = highScoreRepository.save(scoreToSaveOrUpdate);
            log.info("Oyun sonucu başarıyla {} (ID: {}), Oyuncu '{}'", existingHighScore != null && newHighScoreBeaten ? "güncellendi" : "kaydedildi", savedScore.getId(), savedScore.getPlayerName());
            GameResultDTO resultDTO = convertToGameResultDTO(savedScore);
            return resultDTO;
        } catch (Exception e) {
            log.error("HighScore kaydedilirken/güncellenirken hata oluştu: Oyuncu '{}'. Hata: {}", playerName, e.getMessage(), e);
            return GameResultDTO.builder()
                    .playerName(playerName)
                    .score(newScore)
                    .difficulty(difficulty)
                    .correctAnswers(scoreDetails.getCorrectAnswers())
                    .totalQuestions(scoreDetails.getTotalQuestions())
                    .maxStreak(scoreDetails.getMaxStreak())
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

    private GameQuestionDTO createQuestionDTO(Person p1, Person p2, RelationshipDescriptionResult descResult, Difficulty difficulty) {
        String questionSignature = p1.getId() + "_" + p2.getId();

        String correctAnswerKey = descResult.getMessageKey();
        List<String> optionKeys = generateOptions(difficulty, correctAnswerKey, descResult.getAcceptableMessageKeys());

        // DTO'ya kişi isimlerini ekle
        String person1FullName = p1.getFirstName() + " " + p1.getLastName();
        String person2FullName = p2.getFirstName() + " " + p2.getLastName();

        return GameQuestionDTO.builder()
                .id(questionSignature)
                .person1(person1FullName) // `person1` alanını kullanıyoruz
                .person2(person2FullName) // `person2` alanını kullanıyoruz
                .options(optionKeys)
                .correctAnswer(correctAnswerKey)
                .difficulty(difficulty)
                .timeLimit(gameProperties.getTimeLimit(difficulty))
                .person1Info(personMapper.personToPersonInfo(p1))
                .person2Info(personMapper.personToPersonInfo(p2))
                .relationshipPath(descResult.getPath())
                .build();
    }
} 