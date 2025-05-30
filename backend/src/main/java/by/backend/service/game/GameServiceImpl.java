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

@Service
public class GameServiceImpl implements GameService {

    private final PersonRepository personRepository;
    private final HighScoreRepository highScoreRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;
    private final Map<Difficulty, List<GameQuestionDTO>> preGeneratedQuestions;
    private final ScheduledExecutorService executorService;
    private final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private static final Logger log = LoggerFactory.getLogger(GameServiceImpl.class);

    private static final int PRE_GENERATED_QUESTIONS_COUNT = 20;
    private static final int MAX_ATTEMPTS_IN_GENERATE_INTERNAL = 50;
    private static final int MAX_ATTEMPTS_FOR_QUESTION_GENERATION = 10;
    private static final int DEFAULT_QUESTIONS_PER_GAME = 10;

    private volatile List<Person> activePersonsCache = Collections.emptyList();
    private static final long ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES = 5;
    private long lastCacheRefreshTimeMillis = System.currentTimeMillis();
    private final Object cacheLock = new Object();

    public GameServiceImpl(PersonRepository personRepository,
                         HighScoreRepository highScoreRepository,
                         RelationshipService relationshipService,
                         PersonMapper personMapper,
                         MessageSource messageSource) {
        this.personRepository = personRepository;
        this.highScoreRepository = highScoreRepository;
        this.relationshipService = relationshipService;
        this.personMapper = personMapper;
        this.messageSource = messageSource;
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
                                          .collect(Collectors.toSet()));
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
                            questions.stream().map(GameQuestionDTO::getId).collect(Collectors.toSet()));
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
    public InitialGameDataDTO startGame(String playerName, Difficulty difficulty) {
        log.info("Oyun başlatılıyor: Oyuncu '{}', Zorluk '{}'", playerName, difficulty);
        GameQuestionDTO firstQuestion = generateQuestionInternal(difficulty, Collections.emptySet());
        if (firstQuestion == null) {
            log.error("startGame: İlk soru üretilemedi. Oyuncu: {}, Zorluk: {}", playerName, difficulty);
            throw new GameException(getMessage("game.error.start_failed_no_question"));
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
    public AnswerResponseDTO answerQuestion(GameAnswerDTO answerDetails) {
        log.debug("Cevap işleniyor: Oyuncu '{}', Soru ID '{}', Cevap '{}'",
            answerDetails.getPlayerName(), answerDetails.getQuestionId(), answerDetails.getAnswer());

        if (answerDetails.getQuestionId() == null || answerDetails.getAnswer() == null || answerDetails.getDifficulty() == null) {
            log.warn("answerQuestion çağrısı eksik parametrelerle yapıldı: {}", answerDetails);
            throw new GameException(getMessage("game.error.missing_parameters_for_answer"));
        }

        Person person1 = null;
        Person person2 = null;
        RelationshipDescriptionResult relationshipCheckResult;

        try {
            String[] ids = answerDetails.getQuestionId().split("_");
            if (ids.length == 2) {
                Optional<Person> optP1 = personRepository.findById(Long.parseLong(ids[0]));
                Optional<Person> optP2 = personRepository.findById(Long.parseLong(ids[1]));
                if (optP1.isPresent() && optP2.isPresent()) {
                    person1 = optP1.get();
                    person2 = optP2.get();
                } else {
                    log.warn("Cevaplanan soru için kişiler bulunamadı: P1 ID {} veya P2 ID {} bulunamadı.", ids[0], ids[1]);
                    throw new GameException(getMessage("game.error.persons_not_found_for_question", answerDetails.getQuestionId()));
                }
            } else {
                 log.warn("Geçersiz soru ID formatı: {}. Format P1ID_P2ID şeklinde olmalı.", answerDetails.getQuestionId());
                 throw new GameException(getMessage("game.error.invalid_question_id_format", answerDetails.getQuestionId()));
            }
            
            if (person1 == null || person2 == null) {
                 log.error("answerQuestion: person1 veya person2 null geldi. Soru ID: {}", answerDetails.getQuestionId());
                 throw new GameException(getMessage("game.error.persons_not_found_for_question", answerDetails.getQuestionId()));
            }

            PersonSummaryDTO person1Summary = personMapper.toSummaryDTO(person1);
            PersonSummaryDTO person2Summary = personMapper.toSummaryDTO(person2);
            relationshipCheckResult = relationshipService.findRelationshipDescription(person1Summary, person2Summary);

            if (relationshipCheckResult.getStatus() != RelationshipStatus.FOUND) {
                log.error("Doğru cevap {} için sistemden geçerli bir ilişki açıklaması alınamadı. Durum: {}, Mesaj Anahtarı: {}",
                    answerDetails.getQuestionId(), relationshipCheckResult.getStatus(), relationshipCheckResult.getMessageKey());
                throw new GameException(getMessage("game.error.could_not_determine_correct_answer"));
            }

        } catch (NumberFormatException e) {
            log.warn("Soru ID'sindeki kişi IDleri ayrıştırılamadı: {}. Hata: {}", answerDetails.getQuestionId(), e.getMessage());
            throw new GameException(getMessage("game.error.invalid_question_id_parsing", answerDetails.getQuestionId()));
        } catch (GameException ge) {
            throw ge;
        } catch (Exception e) {
            log.error("answerQuestion içinde beklenmedik bir hata oluştu. Soru ID: {}, Hata: {}", answerDetails.getQuestionId(), e.getMessage(), e);
            throw new GameException(getMessage("game.error.internal_processing_answer"));
        }

        String userAnswer = normalizeAnswer(answerDetails.getAnswer());
        boolean isCorrect = false;

        String primaryCorrectAnswerKey = relationshipCheckResult.getMessageKey();
        String primaryCorrectAnswerLocalized = getMessage(primaryCorrectAnswerKey, person1.getFirstName(), person2.getFirstName());

        if (normalizeAnswer(primaryCorrectAnswerLocalized).equals(userAnswer)) {
            isCorrect = true;
        } else {
            List<String> acceptableKeys = relationshipCheckResult.getAcceptableMessageKeys();
            if (acceptableKeys != null) {
                for (String key : acceptableKeys) {
                    String acceptableAnswerLocalized = getMessage(key, person1.getFirstName(), person2.getFirstName());
                    if (normalizeAnswer(acceptableAnswerLocalized).equals(userAnswer)) {
                        isCorrect = true;
                        break;
                    }
                }
            }
        }

        int points = calculatePointsInternal(isCorrect, answerDetails.getDifficulty(), answerDetails.getTimeTakenInSeconds(), answerDetails.getCurrentStreak());
        int newStreak = isCorrect ? answerDetails.getCurrentStreak() + 1 : 0;
        int totalScore = answerDetails.getCurrentScore() + points;

        HighScore highScore = null;

        Set<String> askedQuestionsInThisGame = answerDetails.getAskedQuestionSignaturesInThisGame() != null ?
                new HashSet<>(answerDetails.getAskedQuestionSignaturesInThisGame()) : new HashSet<>();
        askedQuestionsInThisGame.add(answerDetails.getQuestionId());

        GameQuestionDTO nextQuestion = null;
        String gameEndMessage = null;
        boolean isGameEnd = false;

        if (answerDetails.getGameQuestionCount() + 1 >= DEFAULT_QUESTIONS_PER_GAME) {
            isGameEnd = true;
            gameEndMessage = getMessage("game.feedback.end_of_game_all_questions_answered");
            log.info("Oyun bitti (tüm sorular cevaplandı): Oyuncu '{}', Skor '{}'", answerDetails.getPlayerName(), totalScore);
            highScore = new HighScore(
                null,
                answerDetails.getPlayerName(),
                totalScore,
                answerDetails.getDifficulty(),
                isCorrect ? answerDetails.getCorrectAnswersCount() + 1 : answerDetails.getCorrectAnswersCount(),
                DEFAULT_QUESTIONS_PER_GAME,
                newStreak,
                LocalDateTime.now()
            );
            highScoreRepository.save(highScore);
        } else {
            nextQuestion = generateNewQuestion(answerDetails.getDifficulty(), askedQuestionsInThisGame);
            if (nextQuestion == null) {
                isGameEnd = true;
                gameEndMessage = getMessage("game.feedback.end_of_game_no_more_questions");
                log.warn("Oyun bitti (yeni soru üretilemedi): Oyuncu '{}'", answerDetails.getPlayerName());
                 highScore = new HighScore(
                    null,
                    answerDetails.getPlayerName(),
                    totalScore,
                    answerDetails.getDifficulty(),
                    isCorrect ? answerDetails.getCorrectAnswersCount() + 1 : answerDetails.getCorrectAnswersCount(),
                    answerDetails.getGameQuestionCount() + 1,
                    newStreak,
                    LocalDateTime.now()
                );
                highScoreRepository.save(highScore);
            }
        }

        AnswerResponseDTO.AnswerResponseDTOBuilder responseBuilder = AnswerResponseDTO.builder()
                .correctAnswer(isCorrect)
                .pointsEarned(points)
                .updatedStreak(newStreak)
                .gameOver(isGameEnd)
                .gameEndMessage(gameEndMessage)
                .updatedScore(totalScore)
                .askedQuestionSignaturesInThisGame(askedQuestionsInThisGame);

        if (isCorrect) {
            responseBuilder.correctAnswerText(getMessage(relationshipCheckResult.getMessageKey(), person1.getFirstName(), person2.getFirstName()));
            List<RelationshipStepDTO> path = relationshipService.getRelationshipPath(person1, person2);
            responseBuilder.relationshipPath(path);
        } else {
             responseBuilder.correctAnswerText(getMessage(relationshipCheckResult.getMessageKey(), person1.getFirstName(), person2.getFirstName()));
        }

        if (nextQuestion != null) {
            GameQuestionDTO questionToSend = nextQuestion.toBuilder().build();
            questionToSend.setCorrectAnswer(null);
            questionToSend.setRelationshipPath(null);
            responseBuilder.nextQuestion(questionToSend);
        }
        
        if (highScore != null) {
            responseBuilder.finalScoreId(highScore.getId());
            responseBuilder.finalResult(GameResultDTO.builder()
                                .playerName(highScore.getPlayerName())
                                .score(highScore.getScore())
                                .difficulty(highScore.getDifficulty())
                                .date(highScore.getPlayedAt().toLocalDate())
                                .correctAnswers(highScore.getCorrectAnswers())
                                .totalQuestions(highScore.getTotalQuestions())
                                .maxStreak(highScore.getMaxStreak())
                                .build());
        }

        log.debug("Cevap işlendi ve yanıt hazırlandı: {}", responseBuilder.build());
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
            GameQuestionDTO question = generateQuestionInternal(difficulty, askedQuestionSignatures);
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
    public GameQuestionDTO generateQuestion(Difficulty difficulty) {
        log.info("İsteğe bağlı soru üretiliyor, Zorluk: {}", difficulty);
        GameQuestionDTO question = generateQuestionInternal(difficulty, Collections.emptySet());
        if (question == null) {
            log.error("generateQuestion: Soru üretilemedi. Zorluk: {}", difficulty);
            throw new GameException(getMessage("game.error.generate_question_failed"));
        }
        GameQuestionDTO questionToSend = question.toBuilder().build();
        questionToSend.setCorrectAnswer(null);
        questionToSend.setRelationshipPath(null);
        return questionToSend;
    }

    private GameQuestionDTO generateQuestionInternal(Difficulty difficulty, Set<String> askedSignatures) {
        log.debug("generateQuestionInternal çağrıldı. Zorluk: {}, Önceden sorulanlar: {}", difficulty, askedSignatures.size());
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
            
            String actualRelationshipDescription = descResult.getLocalizedDescription(); 
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

            String questionTextValue = getMessage("game.question.text", p1.getFirstName() + " " + p1.getLastName(), p2.getLastName());
            List<String> options = generateOptions(difficulty, actualRelationshipDescription, actualRelationshipMessageKey, p1, p2, descResult.getAcceptableMessageKeys());

            if (options.size() < 2) { 
                log.warn("Soru için yeterli seçenek üretilemedi (P1:{}, P2:{}, Doğru Cevap Key: {}, Seçenek Sayısı: {}). Seçenekler: {}", p1.getId(), p2.getId(), actualRelationshipMessageKey, options.size(), options);
                continue; 
            }
            
            Collections.shuffle(options);

            GameQuestionDTO question = GameQuestionDTO.builder()
                    .id(questionSignature)
                    .questionText(questionTextValue)
                    .options(options)
                    .correctAnswer(actualRelationshipDescription) 
                    .difficulty(difficulty)
                    .person1Info(enrichPersonInfo(p1))
                    .person2Info(enrichPersonInfo(p2))
                    .build();
            log.info("Geçerli soru üretildi: ID {}, P1:{}, P2:{}, Zorluk: {}, Cevap Key: {}", question.getId(), p1.getId(), p2.getId(), difficulty, actualRelationshipMessageKey);
            return question;
        }
        log.warn("{} denemeden sonra {} zorluğunda geçerli bir soru üretilemedi.", MAX_ATTEMPTS_IN_GENERATE_INTERNAL, difficulty);
        return null;
    }

    private List<String> generateOptions(Difficulty difficulty, String correctAnswerText, String correctAnswerKey, Person p1, Person p2, List<String> acceptableCorrectAnswerKeys) {
        Set<String> options = new LinkedHashSet<>();
        options.add(correctAnswerText);

        // İlk olarak, gerçek ilişkilere dayalı yanlış seçenekler oluştur
        List<String> possibleWrongRelations = getPossibleRelations(difficulty, p1, p2, correctAnswerKey, acceptableCorrectAnswerKeys)
                .stream()
                .filter(relationText -> {
                    String lowerRelationText = relationText.toLowerCase();
                    boolean isUnwantedGeneric = lowerRelationText.contains("distant") || 
                                                lowerRelationText.contains("undefined") || 
                                                lowerRelationText.contains("uzak") || 
                                                lowerRelationText.contains("belirsiz") ||
                                                lowerRelationText.contains("tanımsız") || 
                                                lowerRelationText.contains("ilişki bulunamadı");
                    return !isUnwantedGeneric;
                })
                .collect(Collectors.toList());

        Collections.shuffle(possibleWrongRelations);

        for (String wrongRelation : possibleWrongRelations) {
            if (options.size() >= 4) break; // Hedef 4 seçenek
            if (!options.contains(wrongRelation)) {
                options.add(wrongRelation);
            }
        }
        
        // Eğer yeterli seçenek yoksa, zorluk seviyesine göre özel yanıltıcılar ekle
        if (options.size() < 4) {
            Set<String> difficultyBasedDistractorKeys = new HashSet<>();
            String relationshipCategory = getRelationshipCategory(correctAnswerKey);
            
            switch (difficulty) {
                case EASY:
                    // Kolay sorular için temel ilişkiler
                    if (!relationshipCategory.equals("direct") && !relationshipCategory.equals("siblings")) {
                        difficultyBasedDistractorKeys.add("relationship.parent_child.parent");
                        difficultyBasedDistractorKeys.add("relationship.parent_child.child");
                        difficultyBasedDistractorKeys.add("relationship.spouse.is_spouse_of");
                        difficultyBasedDistractorKeys.add("relationship.sibling.is_sibling_of");
                    }
                    break;
                    
                case MEDIUM:
                    // Orta zorluk için genişletilmiş aile ilişkileri
                    if (!relationshipCategory.equals("grandparent") && !relationshipCategory.equals("grandchild") &&
                        !relationshipCategory.equals("aunt_uncle") && !relationshipCategory.equals("nephew_niece")) {
                        difficultyBasedDistractorKeys.add("relationship.grandparent");
                        difficultyBasedDistractorKeys.add("relationship.grandchild");
                        difficultyBasedDistractorKeys.add("relationship.aunt_uncle");
                        difficultyBasedDistractorKeys.add("relationship.nephew_niece");
                    }
                    break;
                    
                case HARD:
                    // Zor sorular için karmaşık ilişkiler
                    if (!relationshipCategory.equals("cousin") && !relationshipCategory.equals("inlaw")) {
                        difficultyBasedDistractorKeys.add("relationship.cousin");
                        difficultyBasedDistractorKeys.add("relationship.inlaw.parent");
                        difficultyBasedDistractorKeys.add("relationship.inlaw.child");
                        difficultyBasedDistractorKeys.add("relationship.inlaw.sibling");
                    }
                    break;
            }
            
            // Yanıltıcı seçenekleri ekle
            for (String key : difficultyBasedDistractorKeys) {
                if (options.size() >= 4) break;
                if (!key.equals(correctAnswerKey) && 
                    (acceptableCorrectAnswerKeys == null || !acceptableCorrectAnswerKeys.contains(key))) {
                    try {
                        String localizedDistractor = getMessage(key, p1.getFirstName(), p2.getFirstName());
                        if (!options.contains(localizedDistractor) && !localizedDistractor.equals(correctAnswerText)) {
                            options.add(localizedDistractor);
                        }
                    } catch (Exception e) {
                        log.debug("Yanıltıcı seçenek oluşturulamadı: {}", key);
                    }
                }
            }
        }

        // Hala yeterli seçenek yoksa, genel yanıltıcılar ekle
        if (options.size() < 4) {
            String noRelationDistractor = getMessage("game.distractor.no_relation");
            if (!options.contains(noRelationDistractor) && !noRelationDistractor.equals(correctAnswerText)) {
                options.add(noRelationDistractor);
            }
        }
        
        // Son çare olarak, jenerik yanıltıcılar ekle
        int placeholderCount = 1;
        while (options.size() < 4 && placeholderCount <= 5) {
            String placeholderOption = getMessage("game.distractor.other_relative_placeholder", String.valueOf(placeholderCount));
            if (!options.contains(placeholderOption)) {
                options.add(placeholderOption);
            } else {
                placeholderOption = getMessage("game.distractor.other_relative_placeholder", "Alt-" + String.valueOf(placeholderCount));
                if (!options.contains(placeholderOption)) {
                    options.add(placeholderOption);
                }
            }
            placeholderCount++;
        }
        
        // En az 2 seçenek olduğundan emin ol
        if (options.size() < 2) {
            log.warn("Soru için yeterli seçenek üretilemedi: P1:{}, P2:{}, Doğru Cevap: {}", p1.getId(), p2.getId(), correctAnswerText);
            options.add("İlişki yok");
        }
        
        // Seçenekleri karıştır
        List<String> shuffledOptions = new ArrayList<>(options);
        Collections.shuffle(shuffledOptions);
        
        log.info("Nihai seçenekler (P1:{}, P2:{}, Key:'{}', Doğru:'{}') - Toplam: {}", 
            p1.getId(), p2.getId(), correctAnswerKey, correctAnswerText, shuffledOptions.size());
        
        return shuffledOptions;
    }

    private List<String> getPossibleRelations(Difficulty difficulty, Person p1, Person p2, String actualCorrectAnswerKey, List<String> acceptableCorrectAnswerKeys) {
        Set<String> possibleRelationTexts = new HashSet<>();

        // Mevcut kişilerin tüm olası ilişkilerini al
        // Bu kısım, tüm olası ilişkileri bulmak için bir stratejiye ihtiyaç duyar.
        // Örneğin, rastgele başka kişilerle olan ilişkilerini sorgulayabilir veya
        // belirli bir derinliğe kadar olan tüm ilişkileri inceleyebilir.
        // Şimdilik, sadece p1 ve p2 arasındaki asıl ilişkiyi döndürelim (bu doğru değil, seçenekler için daha fazlası lazım)

        PersonSummaryDTO p1Summary = personMapper.toSummaryDTO(p1);
        PersonSummaryDTO p2Summary = personMapper.toSummaryDTO(p2);
        RelationshipDescriptionResult actualRelation = relationshipService.findRelationshipDescription(p1Summary, p2Summary);
        
        if (actualRelation.getStatus() == RelationshipStatus.FOUND) {
            possibleRelationTexts.add(actualRelation.getLocalizedDescription());
        }
        // ... (Daha fazla yanlış seçenek üretme mantığı eklenecek) ...
        // Örnek olarak birkaç genel yanlış seçenek ekleyelim (bu kısım geliştirilmeli)
        possibleRelationTexts.add(getMessage("relationship.sibling", p1.getFirstName(), p2.getFirstName()));
        possibleRelationTexts.add(getMessage("relationship.parent", p1.getFirstName(), p2.getFirstName()));
        possibleRelationTexts.add(getMessage("relationship.child", p1.getFirstName(), p2.getFirstName()));
        possibleRelationTexts.add(getMessage("relationship.spouse", p1.getFirstName(), p2.getFirstName()));
        
        log.debug("Potansiyel yanlış ilişkiler bulundu (filtrelenmiş olabilir): {}", possibleRelationTexts.size());
        return new ArrayList<>(possibleRelationTexts);
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

    private Map<String, Object> enrichPersonInfo(Person person) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", person.getId());
        info.put("name", person.getFirstName() + " " + person.getLastName());
        info.put("gender", person.getGender() != null ? person.getGender().name() : "Bilinmiyor");
        info.put("birthYear", person.getBirthDate() != null ? person.getBirthDate().getYear() : null);
        info.put("deathYear", person.getDeathDate() != null ? person.getDeathDate().getYear() : null);
        return info;
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
            throw new GameException("Geçerli bir oyuncu adı gereklidir.");
        }
        
        if (scoreDetails.getDifficulty() == null) {
            log.warn("Zorluk seviyesi olmadan skor kaydedilemez");
            throw new GameException("Zorluk seviyesi gereklidir.");
        }
        
        if (scoreDetails.getScore() < 0) {
            log.warn("Negatif skor kaydedilemez: {}", scoreDetails.getScore());
            throw new GameException("Skor negatif olamaz.");
        }

        HighScore highScore = new HighScore();
        highScore.setPlayerName(scoreDetails.getPlayerName().trim());
        highScore.setScore(scoreDetails.getScore());
        highScore.setDifficulty(scoreDetails.getDifficulty());
        highScore.setCorrectAnswers(scoreDetails.getCorrectAnswers() >= 0 ? scoreDetails.getCorrectAnswers() : 0);
        highScore.setTotalQuestions(scoreDetails.getTotalQuestions() >= 0 ? scoreDetails.getTotalQuestions() : 0);
        highScore.setMaxStreak(scoreDetails.getMaxStreak() >= 0 ? scoreDetails.getMaxStreak() : 0);
        highScore.setPlayedAt(LocalDateTime.now());

        try {
            HighScore savedScore = highScoreRepository.save(highScore);
            log.info("Oyun sonucu başarıyla kaydedildi: ID {}, Oyuncu '{}'", savedScore.getId(), savedScore.getPlayerName());
            
            return GameResultDTO.builder()
                    .playerName(savedScore.getPlayerName())
                    .score(savedScore.getScore())
                    .difficulty(savedScore.getDifficulty())
                    .correctAnswers(savedScore.getCorrectAnswers())
                    .totalQuestions(savedScore.getTotalQuestions())
                    .maxStreak(savedScore.getMaxStreak())
                    .gameOver(true)
                    .build();
        } catch (Exception e) {
            log.error("HighScore kaydedilirken hata oluştu: Oyuncu '{}'. Hata: {}", scoreDetails.getPlayerName(), e.getMessage(), e);
            return GameResultDTO.builder()
                    .playerName(scoreDetails.getPlayerName())
                    .score(scoreDetails.getScore())
                    .difficulty(scoreDetails.getDifficulty())
                    .correctAnswers(scoreDetails.getCorrectAnswers())
                    .totalQuestions(scoreDetails.getTotalQuestions())
                    .maxStreak(scoreDetails.getMaxStreak())
                    .gameOver(true)
                    .build();
        }
    }

    private String getMessage(String code, Object... args) {
        try {
            return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            log.warn("No message found for code: {} (Args: {})", code, Arrays.toString(args));
            return code + " (Args: " + Arrays.toString(args) + ")"; 
        }
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
                                      .collect(Collectors.toSet()));
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
} 