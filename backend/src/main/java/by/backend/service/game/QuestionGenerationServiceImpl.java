package by.backend.service.game;

import by.backend.config.GameProperties;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.dto.PersonInfoDTO;
import by.backend.model.entity.Person;
import by.backend.model.enums.Difficulty;
import by.backend.model.enums.RelationshipStatus;
import by.backend.repository.PersonRepository;
import by.backend.service.relationship.RelationshipService;
import by.backend.mapper.PersonMapper;
import by.backend.service.description.RelationshipDescriptionResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;


@Service
@Slf4j
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    // Magic numbers converted to constants
    private static final int MAX_ATTEMPTS_IN_GENERATE_INTERNAL = 200;
    private static final long ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES = 5;
    private static final int MIN_PERSONS_FOR_QUESTIONS = 2;
    private static final int MIN_FILTERED_PERSONS_THRESHOLD = 5;
    private static final int MIN_OPTIONS_COUNT = 2;
    private static final int MAX_PATH_LENGTH = 6;
    private static final int YEARS_BACK_FOR_DEATH_DATE = 1;
    private static final double PATH_BONUS_MULTIPLIER = 0.08;
    private static final double MAX_PATH_BONUS = 0.3;
    private static final double MAX_COMPLEXITY_SCORE = 1.0;
 
    
    // Additional constants for magic numbers
    private static final int DEFAULT_PATH_LENGTH = 3;
    private static final int COUSIN_SECOND_PATH_LENGTH = 4;
    private static final int COUSIN_THIRD_PATH_LENGTH = 5;
    private static final int INLAW_COMPLEX_PATH_LENGTH = 4;
    private static final int STEP_PARENT_CHILD_PATH_LENGTH = 2;
    private static final int DISTANT_CLOSE_PATH_LENGTH = 3;
    private static final int DISTANT_MODERATE_PATH_LENGTH = 4;
    private static final int DISTANT_FAR_PATH_LENGTH = 5;
    private static final double COMPLEXITY_SCORE_THRESHOLD_07 = 0.7;
    private static final double COMPLEXITY_SCORE_THRESHOLD_06 = 0.6;
    private static final double MATERNAL_PATERNAL_BONUS = 0.05;
    private static final double REMOVED_RELATIONSHIP_BONUS = 0.15;
    private static final double HALF_STEP_BONUS = 0.1;
    
    // String constants
    private static final String GAME_PREFIX = "game.";
    private static final String RELATIONSHIP_PREFIX = "relationship.";
    private static final String GAME_ANSWER_PREFIX = "game.answer.";
    private static final String GAME_QUESTION_FORMAT_KEY = "game.question.format";
    private static final String RELATIONSHIP_NOT_FOUND = "relationship.not_found";
    private static final String UNDERSCORE = "_";
    static final String DOT = ".";
    
    // Category constants
    private static final String CATEGORY_DIRECT = "direct";
    private static final String CATEGORY_SIBLINGS = "siblings";
    private static final String CATEGORY_GRANDPARENT = "grandparent";
    private static final String CATEGORY_GRANDCHILD = "grandchild";
    private static final String CATEGORY_AUNT_UNCLE = "aunt_uncle";
    private static final String CATEGORY_NEPHEW_NIECE = "nephew_niece";
    private static final String CATEGORY_COUSIN = "cousin";
    private static final String CATEGORY_INLAW = "inlaw";
    private static final String CATEGORY_STEP = "step";
    private static final String CATEGORY_DISTANT = "distant";
    private static final String CATEGORY_SELF = "self";
    private static final String CATEGORY_NONE = "none";
    private static final String CATEGORY_OTHER = "other";
    private static final String CATEGORY_UNDEFINED = "undefined";
    
    // Relationship qualifier constants
    private static final String COMPLEX = "complex";
    private static final String SECOND = "second";
    private static final String THIRD = "third";
    private static final String SPOUSE_SIBLING_SPOUSE = "spouse_sibling_spouse";
    private static final String MATERNAL = "maternal";
    private static final String PATERNAL = "paternal";
    
    // Search term constants
    private static final String GRANDPARENT_TERM = "grandparent";
    private static final String GRANDCHILD_TERM = "grandchild";
    private static final String AUNT_TERM = "aunt";
    private static final String UNCLE_TERM = "uncle";
    private static final String NEPHEW_TERM = "nephew";
    private static final String NIECE_TERM = "niece";
    private static final String COUSIN_TERM = "cousin";
    private static final String INLAW_TERM = "inlaw";
    private static final String STEP_TERM = "step";
    private static final String ITSELF_TERM = "itself";
    private static final String NOT_FOUND_TERM = "not_found";
    private static final String SIBLING_TERM = ".sibling";
    private static final String SPOUSE_TERM = ".spouse";

    private final PersonRepository personRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;
    private final GameProperties gameProperties;
    final RelationshipDescriptionResolver relationshipDescriptionResolver;
    private final Random random = new Random();

    private final List<Person> activePersonsCache = new CopyOnWriteArrayList<>();
    private long lastCacheRefreshTimeMillis = System.currentTimeMillis();
    private final Object cacheLock = new Object();
    
    // Yeni: Soru önbellek sistemi
    private final Map<Difficulty, Queue<GameQuestionDTO>> questionCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE_PER_DIFFICULTY = 50;

    public QuestionGenerationServiceImpl(PersonRepository personRepository,
                                       RelationshipService relationshipService,
                                       PersonMapper personMapper,
                                       MessageSource messageSource,
                                       GameProperties gameProperties,
                                       RelationshipDescriptionResolver relationshipDescriptionResolver) {
        this.personRepository = personRepository;
        this.relationshipService = relationshipService;
        this.personMapper = personMapper;
        this.messageSource = messageSource;
        this.gameProperties = gameProperties;
        this.relationshipDescriptionResolver = relationshipDescriptionResolver;
    }
    
    @PostConstruct
    public void init() {
        log.info("QuestionGenerationService başlatılıyor...");
        CompletableFuture.runAsync(() -> {
            try {
                refreshActivePersonsCache();
                initializeQuestionCache();
            } catch (Exception e) {
                log.error("Aktif kişi önbelleği ilk doldurma sırasında hata oluştu.", e);
            }
        });
    }
    
    private void initializeQuestionCache() {
        log.info("Soru önbelleği başlatılıyor...");
        for (Difficulty difficulty : Difficulty.values()) {
            questionCache.put(difficulty, new ConcurrentLinkedQueue<>());
            fillQuestionCache(difficulty);
        }
        log.info("Soru önbelleği başlatıldı.");
    }
    
    private void fillQuestionCache(Difficulty difficulty) {
        Queue<GameQuestionDTO> cache = questionCache.get(difficulty);
        Locale defaultLocale = Locale.of("tr");
        
        while (cache.size() < CACHE_SIZE_PER_DIFFICULTY) {
            GameQuestionDTO question = generateQuestionInternal(difficulty, Collections.emptySet(), defaultLocale);
            if (question != null) {
                cache.offer(question);
                log.debug("Soru {} zorluğu önbelleğe eklendi. Önbellek boyutu: {}", difficulty, cache.size());
            } else {
                log.warn("Soru {} zorluğu için üretilemedi, önbellek doldurma durduruluyor.", difficulty);
                break;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GameQuestionDTO generateQuestion(Difficulty difficulty, Set<String> askedSignatures, Locale locale) {
        // Gerçek ilişki tabanlı soru üretimi (tüm zorluk seviyeleri için)
        GameQuestionDTO question = generateQuestionInternal(difficulty, askedSignatures, locale);
        
        if (question != null) {
            log.info("Soru üretildi (Zorluk: {}): {} -> {}. Cevap: {}", 
                    difficulty, extractFirstName(question.getPerson1()), 
                    extractFirstName(question.getPerson2()), question.getCorrectAnswer());
            return question;
        }
        
        // Sadece gerçek soru üretilemezse fallback
        log.warn("Gerçek soru üretilemedi, fallback devreye giriyor. Zorluk: {}", difficulty);
        return createFallbackQuestion();
    }
    
    
    
    private GameQuestionDTO createFallbackQuestion() {
        log.warn("Gerçek soru üretilemedi, fallback soru oluşturuluyor");
        
        // Basit bir fallback soru oluştur
        String questionId = "fallback_" + System.currentTimeMillis();
        String questionText = "Kim kimin yakın akrabasıdır?";
        
        List<String> options = Arrays.asList(
            "Annesi", 
            "Babası", 
            "Kardeşi", 
            "Eşi"
        );
        
        return GameQuestionDTO.builder()
            .id(questionId)
            .questionText(questionText)
            .options(options)
            .correctAnswer("Annesi")
            .difficulty(Difficulty.EASY)
            .timeLimit(gameProperties.getQuestionTimeLimit(Difficulty.EASY))
            .build();
    }

    private GameQuestionDTO generateQuestionInternal(Difficulty difficulty, Set<String> askedSignatures, Locale locale) {
        long startTime = System.currentTimeMillis();
        log.debug("Zorluk '{}' için soru üretimi başlıyor. Zaten sorulanlar: {}", difficulty, askedSignatures.size());

        List<Person> activePersons = getActivePersonsForQuestionGeneration();
        if (!hasMinimumPersons(activePersons)) {
            return null;
        }

        QuestionGenerationContext context = new QuestionGenerationContext(0, 0);
        
        for (int attempt = 0; attempt < MAX_ATTEMPTS_IN_GENERATE_INTERNAL; attempt++) {
            GameQuestionDTO question = attemptQuestionGeneration(activePersons, askedSignatures, difficulty, locale, context);
            if (question != null) {
                logSuccessfulGeneration(difficulty, question, startTime, attempt);
                return question;
            }
        }

        logFailedGeneration(difficulty, startTime, context);
        return null;
    }

    private boolean hasMinimumPersons(List<Person> activePersons) {
        if (activePersons.size() < MIN_PERSONS_FOR_QUESTIONS) {
            log.warn("Soru üretmek için yeterli aktif kişi yok (gereken en az {}, bulunan: {}).", 
                    MIN_PERSONS_FOR_QUESTIONS, activePersons.size());
            return false;
        }
        return true;
    }

    private GameQuestionDTO attemptQuestionGeneration(List<Person> activePersons, Set<String> askedSignatures, 
                                                     Difficulty difficulty, Locale locale, QuestionGenerationContext context) {
        PersonPair personPair = selectRandomPersonPair(activePersons);
        
        if (personPair.isSamePerson() || isQuestionAlreadyAsked(personPair, askedSignatures)) {
            return null;
        }

        try {
            return processRelationshipForQuestion(personPair, difficulty, locale, context);
        } catch (Exception e) {
            log.error("Soru üretimi sırasında hata (p1={}, p2={}): {}", 
                     personPair.getPerson1().getId(), personPair.getPerson2().getId(), e.getMessage(), e);
            return null;
        }
    }

    private PersonPair selectRandomPersonPair(List<Person> activePersons) {
        // %60 aile bağlantılı, %30 nesil farklı, %10 tamamen rastgele seçim 
        // (eş sorularını azaltmak için nesil farklı seçimi artırdık)
        double selector = random.nextDouble();
        
        if (selector < 0.60) { // Aile bağlantılı seçimi azalt
            PersonPair familyPair = selectFamilyConnectedPair(activePersons);
            if (familyPair != null && !familyPair.isSamePerson()) {
                return familyPair;
            }
        } else if (selector < 0.90) { // Nesil farklı seçimi artır
            PersonPair generationPair = selectGenerationDifferentPair(activePersons);
            if (generationPair != null && !generationPair.isSamePerson()) {
                return generationPair;
            }
        }
        
        // Fallback: tamamen rastgele
        return selectCompletelyRandomPair(activePersons);
    }
    
    private PersonPair selectCompletelyRandomPair(List<Person> activePersons) {
        Person p1 = activePersons.get(random.nextInt(activePersons.size()));
        Person p2 = activePersons.get(random.nextInt(activePersons.size()));
        return new PersonPair(p1, p2);
    }
    
    private PersonPair selectFamilyConnectedPair(List<Person> activePersons) {
        Set<Long> familyNetworkIds = new HashSet<>();
        Person basePerson = activePersons.get(random.nextInt(activePersons.size()));
        addFamilyNetworkIds(basePerson, familyNetworkIds, 3);  // Daha derin ilişki arayışı için depth 3
        
        List<Person> connectedPersons = activePersons.stream()
            .filter(p -> familyNetworkIds.contains(p.getId()) && !p.getId().equals(basePerson.getId()))
            .toList();
        
        if (connectedPersons.isEmpty()) {
            return null;
        }
        
        // Karmaşık ilişkiler için daha uzak akrabaları önceliklendir
        List<Person> prioritizedPersons = connectedPersons.stream()
            .sorted((p1, p2) -> {
                // Nesil farkı olan kişileri öncelikle
                int generationDiff1 = Math.abs((p1.getBirthDate() != null ? p1.getBirthDate().getYear() : 1990) - 
                                              (basePerson.getBirthDate() != null ? basePerson.getBirthDate().getYear() : 1990));
                int generationDiff2 = Math.abs((p2.getBirthDate() != null ? p2.getBirthDate().getYear() : 1990) - 
                                              (basePerson.getBirthDate() != null ? basePerson.getBirthDate().getYear() : 1990));
                return Integer.compare(generationDiff2, generationDiff1); // Daha fazla nesil farkı olanları önce
            })
            .toList();
        
        Person targetPerson = prioritizedPersons.get(random.nextInt(Math.min(prioritizedPersons.size(), 5))); // İlk 5 arasından seç
        return new PersonPair(basePerson, targetPerson);
    }
    
    private PersonPair selectGenerationDifferentPair(List<Person> activePersons) {
        // Doğum yılları farklı olan (30+ yaş farkı) kişiler seç
        for (int attempt = 0; attempt < 10; attempt++) {
            Person p1 = activePersons.get(random.nextInt(activePersons.size()));
            Person p2 = activePersons.get(random.nextInt(activePersons.size()));
            
            if (p1.getBirthDate() != null && p2.getBirthDate() != null) {
                int p1BirthYear = p1.getBirthDate().getYear();
                int p2BirthYear = p2.getBirthDate().getYear();
                int ageDiff = Math.abs(p1BirthYear - p2BirthYear);
                if (ageDiff >= 25) { // Minimum 25 yaş farkı (potansiyel dede-torun)
                    log.info("👴👶 Nesil farklı seçim: {} ({}) -> {} ({}), yaş farkı: {}", 
                             p1.getFirstName(), p1BirthYear, 
                             p2.getFirstName(), p2BirthYear, ageDiff);
                    return new PersonPair(p1, p2);
                }
            }
        }
        // Bulamazsa rastgele seç
        log.info("⚡ Nesil farklı bulunamadı, rastgele seçim yapılıyor");
        return selectCompletelyRandomPair(activePersons);
    }
    
    private void addFamilyNetworkIds(Person person, Set<Long> networkIds, int maxDepth) {
        if (maxDepth <= 0 || networkIds.contains(person.getId())) {
            return;
        }
        
        networkIds.add(person.getId());
        
        // LAZY loading sorununu çözmek için try-catch kullan
        try {
            // Ebeveynler, çocuklar, eş, kardeşler ekle
            person.getParents().forEach(parent -> addFamilyNetworkIds(parent, networkIds, maxDepth - 1));
            person.getChildren().forEach(child -> addFamilyNetworkIds(child, networkIds, maxDepth - 1));
            person.getSpouseOptional().ifPresent(spouse -> addFamilyNetworkIds(spouse, networkIds, maxDepth - 1));
            person.getSiblings().forEach(sibling -> addFamilyNetworkIds(sibling, networkIds, maxDepth - 1));
        } catch (Exception e) {
            // Hibernate session sorunları için sessizce atla
            log.debug("LAZY loading hatası için aile ağı genişletme atlandı: {}", e.getMessage());
        }
    }

    private boolean isQuestionAlreadyAsked(PersonPair personPair, Set<String> askedSignatures) {
        String questionId = createQuestionId(personPair.getPerson1(), personPair.getPerson2());
        String reverseQuestionId = createQuestionId(personPair.getPerson2(), personPair.getPerson1());
        return askedSignatures.contains(questionId) || askedSignatures.contains(reverseQuestionId);
    }

    private String createQuestionId(Person p1, Person p2) {
        return p1.getId() + UNDERSCORE + p2.getId();
    }

    private GameQuestionDTO processRelationshipForQuestion(PersonPair personPair, Difficulty difficulty, 
                                                         Locale locale, QuestionGenerationContext context) {

        RelationshipDescriptionResult relationshipResult = relationshipService.findRelationshipDescription(
            personMapper.toSummaryDTO(personPair.getPerson1()), 
            personMapper.toSummaryDTO(personPair.getPerson2())
        );

        if (relationshipResult == null) {
            return null;
        }
        
        context.incrementFoundRelationships();
        
        if (relationshipResult.getStatus() != RelationshipStatus.FOUND) {
            return null;
        }

        if (!isAppropriateForDifficulty(relationshipResult, difficulty, personPair.getPerson1(), personPair.getPerson2())) {
            return null;
        }

        GameQuestionDTO question = createQuestionDTO(personPair.getPerson1(), personPair.getPerson2(), 
                                                    relationshipResult, difficulty, locale);
        
        if (question != null && isQuestionQualityGood(question)) {
            context.incrementValidQuestions();
            return question;
        }
        
        return null;
    }

    private void logSuccessfulGeneration(Difficulty difficulty, GameQuestionDTO question, long startTime, int attempt) {
        long duration = System.currentTimeMillis() - startTime;
        log.info("Soru üretildi (Zorluk: {}): {} -> {}. Cevap: {}. Süre: {}ms (deneme: {}/{})",
                difficulty, extractFirstName(question.getPerson1()), extractFirstName(question.getPerson2()), 
                question.getCorrectAnswer(), duration, 
                attempt + 1, MAX_ATTEMPTS_IN_GENERATE_INTERNAL);
    }

    private void logFailedGeneration(Difficulty difficulty, long startTime, QuestionGenerationContext context) {
        log.warn("Maksimum deneme sayısına ulaşıldı ({}) ancak '{}' zorluğu için uygun soru üretilemedi. Süre: {}ms. " +
                "Bulunan ilişkiler: {}, Geçerli sorular: {}", 
                MAX_ATTEMPTS_IN_GENERATE_INTERNAL, difficulty, System.currentTimeMillis() - startTime, 
                context.getFoundRelationships(), context.getValidQuestions());
    }

    private String extractFirstName(String fullName) {
        return fullName != null && fullName.contains(" ") ? fullName.split(" ")[0] : fullName;
    }

    // Inner classes for better code organization
    private static class PersonPair {
        private final Person person1;
        private final Person person2;

        public PersonPair(Person person1, Person person2) {
            this.person1 = person1;
            this.person2 = person2;
        }

        public Person getPerson1() { return person1; }
        public Person getPerson2() { return person2; }
        
        public boolean isSamePerson() {
            return person1.getId().equals(person2.getId());
        }
    }

    private static class QuestionGenerationContext {
        private int foundRelationships;
        private int validQuestions;

        public QuestionGenerationContext(int foundRelationships, int validQuestions) {
            this.foundRelationships = foundRelationships;
            this.validQuestions = validQuestions;
        }

        public void incrementFoundRelationships() { foundRelationships++; }
        public void incrementValidQuestions() { validQuestions++; }
        public int getFoundRelationships() { return foundRelationships; }
        public int getValidQuestions() { return validQuestions; }
    }

    @Override
    public List<GameQuestionDTO> generateCandidateQuestions(Difficulty difficulty, int count, Set<String> askedSignatures, Locale locale) {
        List<GameQuestionDTO> candidates = new ArrayList<>();
        Set<String> currentAskedSignatures = new HashSet<>(askedSignatures);

        for (int i = 0; i < count; i++) {
            GameQuestionDTO question = generateQuestion(difficulty, currentAskedSignatures, locale);
            if (question != null) {
                candidates.add(question);
                currentAskedSignatures.add(question.getId());
                currentAskedSignatures.add(question.getId().split("_")[1] + "_" + question.getId().split("_")[0]);
            } else {
                log.warn("{} zorluğu için aday soru üretilemedi (istenilen: {}, üretilen: {}).", difficulty, count, candidates.size());
                break; 
            }
        }
        return candidates;
    }
    
    private GameQuestionDTO createQuestionDTO(Person p1, Person p2, RelationshipDescriptionResult descResult, Difficulty difficulty, Locale locale) {
        log.info("Creating question DTO for {} -> {}, path steps: {}", 
                 p1.getFirstName(), p2.getFirstName(), 
                 descResult.getPath() != null ? descResult.getPath().size() : 0);
        String person1Name = p1.getFirstName() + " " + p1.getLastName();
        String person2Name = p2.getFirstName() + " " + p2.getLastName();
        String questionText = getMessage(GAME_QUESTION_FORMAT_KEY, locale, true, person1Name, person2Name);

        String longAnswerKey = descResult.getMessageKey();
        if (longAnswerKey == null || longAnswerKey.isEmpty() || longAnswerKey.startsWith(RELATIONSHIP_NOT_FOUND)) {
            return null;
        }

        // Cinsiyete göre spesifik cevap oluşturulurken p1'in cinsiyeti kullanılmalı
        String specificAnswerKey = generateSpecificAnswerKey(longAnswerKey, p1);
        String simpleCorrectAnswer = getMessage(specificAnswerKey, locale);

        // Eğer spesifik çeviri bulunamazsa genel anahtarı dene
        if (simpleCorrectAnswer.equals(specificAnswerKey) || simpleCorrectAnswer.startsWith(GAME_ANSWER_PREFIX)) {
            String generalAnswerKey = longAnswerKey.replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
            simpleCorrectAnswer = getMessage(generalAnswerKey, locale);
            
            if (simpleCorrectAnswer.equals(generalAnswerKey) || simpleCorrectAnswer.startsWith(GAME_ANSWER_PREFIX)) {
                log.warn("Oyun cevabı için çeviri bulunamadı, orijinal: {}, spesifik: {}, genel: {}. Soru atlanıyor.", 
                        longAnswerKey, specificAnswerKey, generalAnswerKey);
                return null;
            }
            specificAnswerKey = generalAnswerKey;
        }

        Set<String> acceptableKeys = descResult.getAcceptableMessageKeys() != null ?
            new HashSet<>(descResult.getAcceptableMessageKeys()) : null;
        List<String> options = generateOptionsEnhanced(difficulty, specificAnswerKey, acceptableKeys, locale, p1);

        if (options.size() < getOptionCountForDifficulty(difficulty)) {
            log.warn("Soru için yeterli seçenek üretilemedi. Doğru cevap anahtarı: {}, üretilen seçenek sayısı: {}",
                    specificAnswerKey, options.size());
            return null;
        }

        return GameQuestionDTO.builder()
                .id(createQuestionId(p1, p2))
                .questionText(questionText)
                .person1(person1Name)
                .person2(person2Name)
                .options(options)
                .correctAnswer(simpleCorrectAnswer)
                .difficulty(difficulty)
                .person1Info(personMapper.personToPersonInfo(p1))
                .person2Info(personMapper.personToPersonInfo(p2))
                .relationshipPath(descResult.getPath())
                .build();
    }

    /**
     * Cinsiyete göre spesifik cevap anahtarı oluşturur
     */
    private String generateSpecificAnswerKey(String relationshipKey, Person targetPerson) {
        String baseKey = relationshipKey.replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
        String gender = targetPerson.getGender().name();
        
        // SPESİFİK KUZEN İLİŞKİLERİ
        if (relationshipKey.contains("cousin.paternal_uncle_son")) {
            return GAME_ANSWER_PREFIX + "paternal_uncle_son";
        } else if (relationshipKey.contains("cousin.paternal_uncle_daughter")) {
            return GAME_ANSWER_PREFIX + "paternal_uncle_daughter";
        } else if (relationshipKey.contains("cousin.maternal_uncle_son")) {
            return GAME_ANSWER_PREFIX + "maternal_uncle_son";
        } else if (relationshipKey.contains("cousin.maternal_uncle_daughter")) {
            return GAME_ANSWER_PREFIX + "maternal_uncle_daughter";
        } else if (relationshipKey.contains("cousin.paternal_aunt_son")) {
            return GAME_ANSWER_PREFIX + "paternal_aunt_son";
        } else if (relationshipKey.contains("cousin.paternal_aunt_daughter")) {
            return GAME_ANSWER_PREFIX + "paternal_aunt_daughter";
        } else if (relationshipKey.contains("cousin.maternal_aunt_son")) {
            return GAME_ANSWER_PREFIX + "maternal_aunt_son";
        } else if (relationshipKey.contains("cousin.maternal_aunt_daughter")) {
            return GAME_ANSWER_PREFIX + "maternal_aunt_daughter";
        }
        
        // BÜYÜK AİLE İLİŞKİLERİ
        if (relationshipKey.contains("grand_uncle.paternal")) {
            return GAME_ANSWER_PREFIX + "grand_uncle_paternal";
        } else if (relationshipKey.contains("grand_uncle.maternal")) {
            return GAME_ANSWER_PREFIX + "grand_uncle_maternal";
        } else if (relationshipKey.contains("grand_aunt.paternal")) {
            return GAME_ANSWER_PREFIX + "grand_aunt_paternal";
        } else if (relationshipKey.contains("grand_aunt.maternal")) {
            return GAME_ANSWER_PREFIX + "grand_aunt_maternal";
        } else if (relationshipKey.contains("great_grandparent.male")) {
            return GAME_ANSWER_PREFIX + "great_grandfather";
        } else if (relationshipKey.contains("great_grandparent.female")) {
            return GAME_ANSWER_PREFIX + "great_grandmother";
        }
        
        // Ana parent/child ilişkileri için cinsiyete göre spesifik anahtarlar
        if (relationshipKey.contains("father")) {
            return GAME_ANSWER_PREFIX + "father";
        } else if (relationshipKey.contains("mother")) {
            return GAME_ANSWER_PREFIX + "mother";
        } else if (relationshipKey.contains("son")) {
            return GAME_ANSWER_PREFIX + "son";
        } else if (relationshipKey.contains("daughter")) {
            return GAME_ANSWER_PREFIX + "daughter";
        } else if (relationshipKey.contains("brother")) {
            return GAME_ANSWER_PREFIX + "brother";
        } else if (relationshipKey.contains("sister")) {
            return GAME_ANSWER_PREFIX + "sister";
        } else if (relationshipKey.contains("grandfather")) {
            return GAME_ANSWER_PREFIX + "grandfather";
        } else if (relationshipKey.contains("grandmother")) {
            return GAME_ANSWER_PREFIX + "grandmother";
        } else if (relationshipKey.contains("grandson")) {
            return GAME_ANSWER_PREFIX + "grandson";
        } else if (relationshipKey.contains("granddaughter")) {
            return GAME_ANSWER_PREFIX + "granddaughter";
        } else if (relationshipKey.contains("nephew")) {
            return GAME_ANSWER_PREFIX + "nephew";
        } else if (relationshipKey.contains("niece")) {
            return GAME_ANSWER_PREFIX + "niece";
        } else if (relationshipKey.contains("spouse")) {
            return GAME_ANSWER_PREFIX + "spouse";
        }
        
        // Cinsiyet tabanlı genel ilişkiler için
        if (relationshipKey.contains("parent")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "father" : GAME_ANSWER_PREFIX + "mother";
        } else if (relationshipKey.contains("child")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "son" : GAME_ANSWER_PREFIX + "daughter";
        } else if (relationshipKey.contains("sibling")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "brother" : GAME_ANSWER_PREFIX + "sister";
        } else if (relationshipKey.contains("grandparent")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "grandfather" : GAME_ANSWER_PREFIX + "grandmother";
        } else if (relationshipKey.contains("grandchild")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "grandson" : GAME_ANSWER_PREFIX + "granddaughter";
        } else if (relationshipKey.contains("cousin")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "cousin_male" : GAME_ANSWER_PREFIX + "cousin_female";
        } else if (relationshipKey.contains("uncle") || relationshipKey.contains("aunt")) {
            return "MALE".equals(gender) ? GAME_ANSWER_PREFIX + "uncle" : GAME_ANSWER_PREFIX + "aunt";
        }
        
        return baseKey;
    }

    private List<String> generateOptionsEnhanced(Difficulty difficulty, String shortCorrectAnswerKey, Set<String> acceptableLongAnswerKeys, Locale locale, Person targetPerson) {
        Set<String> options = new HashSet<>();
        String correctAnswerText = getMessage(shortCorrectAnswerKey, locale);
        options.add(correctAnswerText);

        addAcceptableOptionsEnhanced(acceptableLongAnswerKeys, shortCorrectAnswerKey, options, locale, targetPerson);
        
        int requiredOptions = getOptionCountForDifficulty(difficulty);
        addSmartDistractorOptions(options, shortCorrectAnswerKey, acceptableLongAnswerKeys, requiredOptions, locale, difficulty, targetPerson);
        
        addFallbackOptionsIfNeeded(options, requiredOptions);

        List<String> finalOptions = new ArrayList<>(options);
        Collections.shuffle(finalOptions);
        return finalOptions;
    }

    private List<String> generateOptions(Difficulty difficulty, String shortCorrectAnswerKey, Set<String> acceptableLongAnswerKeys, Locale locale) {
        Set<String> options = new HashSet<>();
        String correctAnswerText = getMessage(shortCorrectAnswerKey, locale);
        options.add(correctAnswerText);

        addAcceptableOptions(acceptableLongAnswerKeys, shortCorrectAnswerKey, options, locale);
        
        int requiredOptions = getOptionCountForDifficulty(difficulty);
        addRandomDistractorOptions(options, shortCorrectAnswerKey, acceptableLongAnswerKeys, requiredOptions, locale);
        
        addFallbackOptionsIfNeeded(options, requiredOptions);

        List<String> finalOptions = new ArrayList<>(options);
        Collections.shuffle(finalOptions);
        return finalOptions;
    }

    private void addAcceptableOptionsEnhanced(Set<String> acceptableLongAnswerKeys, String shortCorrectAnswerKey, Set<String> options, Locale locale, Person targetPerson) {
        if (acceptableLongAnswerKeys != null) {
            for (String longKey : acceptableLongAnswerKeys) {
                String shortKey = generateSpecificAnswerKey(longKey, targetPerson);
                if (!shortKey.equals(shortCorrectAnswerKey)) {
                    String optionText = getMessage(shortKey, locale);
                    if (!optionText.equals(shortKey) && !optionText.startsWith(GAME_ANSWER_PREFIX)) {
                        options.add(optionText);
                    }
                }
            }
        }
    }

    private void addAcceptableOptions(Set<String> acceptableLongAnswerKeys, String shortCorrectAnswerKey, Set<String> options, Locale locale) {
        if (acceptableLongAnswerKeys != null) {
            for (String longKey : acceptableLongAnswerKeys) {
                String shortKey = longKey.replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
                if (!shortKey.equals(shortCorrectAnswerKey)) {
                    options.add(getMessage(shortKey, locale));
                }
            }
        }
    }

    private void addRandomDistractorOptions(Set<String> options, String shortCorrectAnswerKey, 
                                           Set<String> acceptableLongAnswerKeys, int requiredOptions, Locale locale) {
        List<String> allRelationshipKeys = getAllRelationshipKeys();
        Collections.shuffle(allRelationshipKeys);

        int attempts = 0;
        while (options.size() < requiredOptions && attempts < allRelationshipKeys.size()) {
            String randomLongKey = allRelationshipKeys.get(attempts);
            String randomShortKey = randomLongKey.replace(RELATIONSHIP_PREFIX, GAME_ANSWER_PREFIX);
            
            if (isValidDistractorOption(randomShortKey, shortCorrectAnswerKey, acceptableLongAnswerKeys, randomLongKey)) {
                String distractorText = getMessage(randomShortKey, locale);
                if (isTranslatedAndUnique(distractorText, randomShortKey, options)) {
                    options.add(distractorText);
                } else {
                    log.trace("Atlanan çevrilmemiş veya sorunlu oyun şıkkı anahtarı: {}", randomShortKey);
                }
            }
            attempts++;
        }
    }

    /**
     * Zorluk seviyesine göre akıllı çeldirici seçenekler oluşturur
     */
    private void addSmartDistractorOptions(Set<String> options, String shortCorrectAnswerKey, 
                                          Set<String> acceptableLongAnswerKeys, int requiredOptions, 
                                          Locale locale, Difficulty difficulty, Person targetPerson) {
        
        // Önce doğru cevabın türünü belirle
        String correctType = determineRelationshipType(shortCorrectAnswerKey);
        
        // Zorluk seviyesine göre çeldirici stratejisi belirle
        List<String> smartDistractors = generateSmartDistractorsByDifficulty(correctType, difficulty, targetPerson);
        Collections.shuffle(smartDistractors);
        
        // Akıllı çeldiricileri ekle
        for (String distractorKey : smartDistractors) {
            if (options.size() >= requiredOptions) break;
            
            if (isValidDistractorOption(distractorKey, shortCorrectAnswerKey, acceptableLongAnswerKeys, null)) {
                String distractorText = getMessage(distractorKey, locale);
                if (isTranslatedAndUnique(distractorText, distractorKey, options)) {
                    options.add(distractorText);
                }
            }
        }
        
        // Eğer hala yeterli seçenek yoksa rastgele ekle
        if (options.size() < requiredOptions) {
            addRandomDistractorOptions(options, shortCorrectAnswerKey, acceptableLongAnswerKeys, requiredOptions, locale);
        }
    }

    /**
     * İlişki türünü belirler
     */
    private String determineRelationshipType(String answerKey) {
        // İngilizce terimler
        if (answerKey.contains("father") || answerKey.contains("mother")) return "parent";
        if (answerKey.contains("son") || answerKey.contains("daughter")) return "child";
        if (answerKey.contains("brother") || answerKey.contains("sister")) return "sibling";
        if (answerKey.contains("grandfather") || answerKey.contains("grandmother")) return "grandparent";
        if (answerKey.contains("grandson") || answerKey.contains("granddaughter")) return "grandchild";
        if (answerKey.contains("uncle") || answerKey.contains("aunt")) return "uncle_aunt";
        if (answerKey.contains("nephew") || answerKey.contains("niece")) return "nephew_niece";
        if (answerKey.contains("cousin")) return "cousin";
        if (answerKey.contains("spouse")) return "spouse";
        
        // Türkçe terimler
        if (answerKey.contains("anne") || answerKey.contains("baba")) return "parent";
        if (answerKey.contains("oğul") || answerKey.contains("kız")) return "child";
        if (answerKey.contains("kardeş") || answerKey.contains("erkek_kardeş") || answerKey.contains("kız_kardeş")) return "sibling";
        if (answerKey.contains("büyükanne") || answerKey.contains("büyükbaba") || answerKey.contains("nene") || answerKey.contains("dede")) return "grandparent";
        if (answerKey.contains("torun")) return "grandchild";
        if (answerKey.contains("amca") || answerKey.contains("dayı") || answerKey.contains("hala") || answerKey.contains("teyze")) return "uncle_aunt";
        if (answerKey.contains("yeğen")) return "nephew_niece";
        if (answerKey.contains("kuzen")) return "cousin";
        if (answerKey.contains("eş")) return "spouse";
        
        // Karmaşık kayın ilişkileri
        if (answerKey.contains("kaynana") || answerKey.contains("kaynata") || answerKey.contains("kaynbiraderi") || 
            answerKey.contains("baldız") || answerKey.contains("görümce") || answerKey.contains("enişte") ||
            answerKey.contains("gelin") || answerKey.contains("damat")) return "inlaw";
        
        // Çok karmaşık ilişkiler
        if (answerKey.contains("bacanak") || answerKey.contains("elti")) return "complex_inlaw";
        
        // Üvey ilişkiler
        if (answerKey.contains("üvey")) return "step";
        
        return "other";
    }

    /**
     * Zorluk seviyesine göre akıllı çeldiriciler oluşturur
     */
    private List<String> generateSmartDistractorsByDifficulty(String correctType, Difficulty difficulty, Person targetPerson) {
                    boolean isMale = targetPerson.getGender() == by.backend.model.enums.Gender.MALE;
        
        return switch (difficulty) {
            case EASY -> generateEasyDistractors(correctType, isMale);
            case MEDIUM -> generateMediumDistractors(correctType, isMale);
            case HARD -> generateHardDistractors(correctType, isMale);
            default -> Collections.emptyList();
        };
    }

    private List<String> generateEasyDistractors(String correctType, boolean isMale) {
        List<String> distractors = new ArrayList<>();
        
        // Farklı kategoriden temel ilişkiler ekle
        if (!correctType.equals(CATEGORY_DIRECT)) distractors.add("parent");
        if (!correctType.equals(CATEGORY_DIRECT)) distractors.add("child");
        if (!correctType.equals(CATEGORY_SIBLINGS)) distractors.add("sibling");
        
        // Cinsiyetle uyumlu temel ilişkiler ekle
        if (isMale) {
            distractors.add("uncle"); // Amca/Dayı
            distractors.add("nephew"); // Erkek yeğen
        } else {
            distractors.add("aunt"); // Teyze/Hala
            distractors.add("niece"); // Kız yeğen
        }
        
        Collections.shuffle(distractors);
        return distractors;
    }

    private List<String> generateMediumDistractors(String correctType, boolean isMale) {
        List<String> distractors = new ArrayList<>();
        
        // Kuzen ve torun ilişkileri
        if (!correctType.equals(CATEGORY_COUSIN)) distractors.add("cousin");
        if (!correctType.equals(CATEGORY_GRANDCHILD)) distractors.add("grandchild");
        if (!correctType.equals(CATEGORY_GRANDPARENT)) distractors.add("grandparent");

        // Cinsiyete göre daha uzak ilişkiler
        if (isMale) {
            distractors.add("grandpa");
            distractors.add("brother_in_law");
        } else {
            distractors.add("grandma");
            distractors.add("sister_in_law");
        }
        
        Collections.shuffle(distractors);
        return distractors;
    }

    private List<String> generateHardDistractors(String correctType, boolean isMale) {
        List<String> distractors = new ArrayList<>();

        // Karmaşık kayın ve uzak akrabalık terimleri ekle
        distractors.add("ikinci_kuzen");
        distractors.add("üvey_kardeş");
        distractors.add("dünür");
        distractors.add("elti");
        distractors.add("bacanak");

        // Cinsiyete göre karmaşık çeldiriciler
        if (isMale) {
            distractors.add("kayınpeder");
            distractors.add("kayınbirader");
            distractors.add("enişte");
            distractors.add("bacanak"); // Tekrar eklense de Set ile benzersiz olacak
        } else {
            distractors.add("kayınvalide");
            distractors.add("görümce");
            distractors.add("baldız");
            distractors.add("elti"); // Tekrar eklense de Set ile benzersiz olacak
        }

        // Çok uzak ve jenerik terimler
        distractors.add("büyük amca");
        distractors.add("ikinci dereceden kuzen");

        Collections.shuffle(distractors);
        return distractors.stream().distinct().limit(5).collect(Collectors.toList());
    }

    private List<String> getAllRelationshipKeys() {
        return new ArrayList<>(List.of(
            "relationship.parent", "relationship.child", "relationship.sibling",
            "relationship.grandparent", "relationship.grandchild", "relationship.aunt_uncle",
            "relationship.nephew_niece", "relationship.cousin", "relationship.spouse",
            "relationship.sibling_spouse", "relationship.spouse_sibling"
        ));
    }

    private boolean isValidDistractorOption(String randomShortKey, String shortCorrectAnswerKey, 
                                           Set<String> acceptableLongAnswerKeys, String randomLongKey) {
        return !randomShortKey.equals(shortCorrectAnswerKey) && 
               (acceptableLongAnswerKeys == null || !acceptableLongAnswerKeys.contains(randomLongKey));
    }

    private boolean isTranslatedAndUnique(String distractorText, String randomShortKey, Set<String> options) {
        return !distractorText.equals(randomShortKey) && 
               !distractorText.startsWith(GAME_ANSWER_PREFIX) && 
               !options.contains(distractorText);
    }

    private void addFallbackOptionsIfNeeded(Set<String> options, int requiredOptions) {
        if (options.size() < requiredOptions) {
            log.warn("Yeterli sayıda benzersiz şık üretilemedi. Gereken: {}, Üretilen: {}. " +
                    "Sistemi daha iyi veri ile geliştirme gerekiyor.", 
                    requiredOptions, options.size());
            
            // Sabit fallback seçenekleri kaldırıldı
            // Gerçek soru üretimi sistemi geliştirilmeli
            // Şimdilik eksik seçeneklerle devam ediyoruz
        }
    }
    


    private boolean isAppropriateForDifficulty(RelationshipDescriptionResult result, Difficulty difficulty, Person p1, Person p2) {
        if (result.getStatus() != RelationshipStatus.FOUND || p1 == null || p2 == null) {
            return false;
        }

        String messageKey = result.getMessageKey();
        String category = getRelationshipCategory(messageKey);
        
        switch (difficulty) {
            case EASY:
                return isEasyLevelRelationship(messageKey, category);
            case MEDIUM:
                return isMediumLevelRelationship(messageKey, category);
            case HARD:
                return isHardLevelRelationship(messageKey, category);
            default:
                return false;
        }
    }
    
    private boolean isEasyLevelRelationship(String messageKey, String category) {
        // EASY seviye: Sadece temel doğrudan ilişkiler - daha esnek yapalım
        return CATEGORY_DIRECT.equals(category) || 
               CATEGORY_SIBLINGS.equals(category) ||
               messageKey.contains("spouse") ||
               messageKey.contains("parent") ||
               messageKey.contains("child") ||
               messageKey.contains("sibling") ||
               messageKey.contains("father") ||
               messageKey.contains("mother") ||
               messageKey.contains("son") ||
               messageKey.contains("daughter") ||
               messageKey.contains("brother") ||
               messageKey.contains("sister");
    }
    
    private boolean isMediumLevelRelationship(String messageKey, String category) {
        // MEDIUM seviye: Temel ilişkiler + 1-2 adım uzak ilişkiler - çok daha esnek
        if (isEasyLevelRelationship(messageKey, category)) {
            return true;
        }
        
        return CATEGORY_GRANDPARENT.equals(category) || 
               CATEGORY_GRANDCHILD.equals(category) ||
               CATEGORY_AUNT_UNCLE.equals(category) || 
               CATEGORY_NEPHEW_NIECE.equals(category) ||
               CATEGORY_COUSIN.equals(category) ||
               messageKey.contains("grandfather") ||
               messageKey.contains("grandmother") ||
               messageKey.contains("grandson") ||
               messageKey.contains("granddaughter") ||
               messageKey.contains("uncle") ||
               messageKey.contains("aunt") ||
               messageKey.contains("nephew") ||
               messageKey.contains("niece") ||
               messageKey.contains("cousin") ||
               messageKey.contains("grandparent") ||
               messageKey.contains("grandchild") ||
               // Türkçe anahtar kelimeler de ekleyelim
               messageKey.contains("dede") ||
               messageKey.contains("nine") ||
               messageKey.contains("torun") ||
               messageKey.contains("amca") ||
               messageKey.contains("dayı") ||
               messageKey.contains("teyze") ||
               messageKey.contains("hala") ||
               messageKey.contains("yeğen") ||
               messageKey.contains("kuzen");
    }

    private boolean isHardLevelRelationship(String messageKey, String category) {
        if (messageKey == null) return false;
        
        // Sadece gerçekten karmaşık ilişkileri HARD olarak kabul et
        return messageKey.contains("great_grand") ||
               messageKey.contains("cousin.second") ||
               messageKey.contains("cousin.removed") ||
               messageKey.contains("elti") ||
               messageKey.contains("bacanak") ||
               messageKey.contains("dunur") ||
               messageKey.contains("kayin") || // kayınpeder/valide/birader
               messageKey.contains("gorumce") ||
               messageKey.contains("baldiz") ||
               messageKey.contains("inlaw.complex") ||
               messageKey.contains("distant");
    }
    
    /**
     * Türkçe'ye özgü karmaşık aile terimleri kontrolü
     */
    private boolean hasTurkishComplexTerms(String messageKey) {
        return messageKey.contains("bacanak") || messageKey.contains("elti");
    }

    private String getRelationshipCategory(String messageKey) {
        if (messageKey == null) {
            return CATEGORY_UNDEFINED;
        }
        
        String key = messageKey.toLowerCase(Locale.ROOT);
        
        // Direct relationship check
        if (isDirectRelationship(key)) {
            return CATEGORY_DIRECT;
        }
        
        // Family relationship checks
        if (key.contains(SIBLING_TERM)) return CATEGORY_SIBLINGS;
        if (key.contains(GRANDPARENT_TERM)) return CATEGORY_GRANDPARENT;
        if (key.contains(GRANDCHILD_TERM)) return CATEGORY_GRANDCHILD;
        if (isAuntUncleRelationship(key)) return CATEGORY_AUNT_UNCLE;
        if (isNephewNieceRelationship(key)) return CATEGORY_NEPHEW_NIECE;
        if (key.contains(COUSIN_TERM)) return CATEGORY_COUSIN;
        if (key.contains(INLAW_TERM)) return CATEGORY_INLAW;
        if (key.contains(STEP_TERM)) return CATEGORY_STEP;
        
        // Distant relationship kontrolü ekleyelim
        if (key.contains("distant") || key.contains("complex") || key.contains("remote")) return CATEGORY_DISTANT;
        
        // Special cases
        if (key.contains(ITSELF_TERM)) return CATEGORY_SELF;
        if (key.contains(NOT_FOUND_TERM)) return CATEGORY_NONE;
        
        return CATEGORY_OTHER;
    }
    
    private boolean isDirectRelationship(String key) {
        return key.startsWith("relationship.direct") || 
               key.startsWith("relationship.reverse") || 
               key.contains(SPOUSE_TERM);
    }
    
    private boolean isAuntUncleRelationship(String key) {
        return key.contains(AUNT_TERM) || key.contains(UNCLE_TERM);
    }
    
    private boolean isNephewNieceRelationship(String key) {
        return key.contains(NEPHEW_TERM) || key.contains(NIECE_TERM);
    }
    

    
    private boolean isQuestionQualityGood(GameQuestionDTO question) {
        return isQuestionValid(question) && 
               isQuestionTextTranslated(question) && 
               isCorrectAnswerTranslated(question) && 
               areOptionsValid(question) && 
               isCorrectAnswerInOptions(question) && 
               !hasForbiddenTerms(question);
    }
    
    private boolean isQuestionValid(GameQuestionDTO question) {
        if (question == null || question.getQuestionText() == null || question.getQuestionText().trim().isEmpty()) {
            log.warn("Question object or text is null/empty.");
            return false;
        }
        return true;
    }
    
    private boolean isQuestionTextTranslated(GameQuestionDTO question) {
        if (question.getQuestionText().startsWith(GAME_PREFIX) || question.getQuestionText().startsWith(RELATIONSHIP_PREFIX)) {
            log.warn("Kritik Hata: Soru metni çevrilmemiş bir anahtar içeriyor: '{}'", question.getQuestionText());
            return false;
        }
        return true;
    }
    
    private boolean isCorrectAnswerTranslated(GameQuestionDTO question) {
        if (question.getCorrectAnswer().startsWith(RELATIONSHIP_PREFIX) || question.getCorrectAnswer().startsWith(GAME_PREFIX)) {
            log.warn("Kritik Hata: Doğru cevap çevrilmemiş bir anahtar içeriyor: '{}'", question.getCorrectAnswer());
            return false;
        }
        return true;
    }
    
    private boolean areOptionsValid(GameQuestionDTO question) {
        if (question.getOptions() == null || question.getOptions().size() < MIN_OPTIONS_COUNT) {
            log.warn("Yeterli seçenek üretilemedi. Bulunan: {}",
                    question.getOptions() != null ? question.getOptions().size() : "null");
            return false;
        }

        long untranslatedOptions = question.getOptions().stream()
                .filter(opt -> opt.startsWith(RELATIONSHIP_PREFIX) || opt.startsWith(GAME_PREFIX))
                .count();

        if (untranslatedOptions > 0) {
            log.warn("{} adet çevrilmemiş seçenek bulundu. Soru ID: {}", untranslatedOptions, question.getId());
            return false;
        }
        return true;
    }
    
    private boolean isCorrectAnswerInOptions(GameQuestionDTO question) {
        if (!question.getOptions().contains(question.getCorrectAnswer())) {
            log.warn("Doğru cevap '{}' seçenekler listesinde bulunamadı: {}. Bu soru atlanacak.",
                    question.getCorrectAnswer(), question.getOptions());
            return false;
        }
        return true;
    }
    
    private boolean hasForbiddenTerms(GameQuestionDTO question) {
        Set<String> forbiddenTerms = Set.of("İlişki Belirsiz", "İlişki Tanımlanamadı");
        if (question.getOptions().stream().anyMatch(forbiddenTerms::contains)) {
            log.warn("Seçeneklerden biri yasaklı bir terim içeriyor: {}", question.getOptions());
            return true;
        }
        return false;
    }
    
    private List<Person> getActivePersonsForQuestionGeneration() {
        if (activePersonsCache.isEmpty() || System.currentTimeMillis() - lastCacheRefreshTimeMillis > TimeUnit.MINUTES.toMillis(ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES)) {
            synchronized(cacheLock) {
                if (activePersonsCache.isEmpty() || System.currentTimeMillis() - lastCacheRefreshTimeMillis > TimeUnit.MINUTES.toMillis(ACTIVE_PERSONS_CACHE_REFRESH_INTERVAL_MINUTES)) {
                    refreshActivePersonsCache();
                }
            }
        }
        return activePersonsCache;
    }

    protected void refreshActivePersonsCache() {
        log.info("Aktif kişi önbelleği yenileniyor...");
        long startTime = System.currentTimeMillis();
        try {
            List<Person> allPersons = personRepository.findAll();
            List<Person> filteredPersons = allPersons.stream()
                .filter(p -> (p.getDeathDate() == null || p.getDeathDate().isAfter(LocalDate.now().minusYears(YEARS_BACK_FOR_DEATH_DATE))) &&
                             (p.getBirthDate() != null || (p.getFirstName() != null && !p.getFirstName().trim().isEmpty())))
                .toList();

            if (filteredPersons.size() < MIN_FILTERED_PERSONS_THRESHOLD) {
                log.warn("Filtrelenmiş aktif kişi sayısı çok az ({}), tüm kişileri dahil ediyorum", filteredPersons.size());
                this.activePersonsCache.clear();
                this.activePersonsCache.addAll(allPersons);
            } else {
                this.activePersonsCache.clear();
                this.activePersonsCache.addAll(filteredPersons);
            }
            
            log.info("Aktif kişi önbelleği {} kişi ile yenilendi. Süre: {}ms", this.activePersonsCache.size(), (System.currentTimeMillis() - startTime));
            lastCacheRefreshTimeMillis = System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Aktif kişi önbelleği yenilenirken hata oluştu: {}", e.getMessage(), e);
        }
    }

    private String getMessage(String code, Locale locale, boolean withArgs, Object... args) {
        try {
            if (withArgs) {
                return messageSource.getMessage(code, args, locale);
            }
            return messageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException _) {
            log.trace("Message key not found: {}", code);
            return code; // Return key if not found
        }
    }

    private String getMessage(String code, Locale locale) {
        return getMessage(code, locale, true, "...", "...");
    }

    private int getOptionCountForDifficulty(Difficulty difficulty) {
        return gameProperties.getOptionsCount(difficulty);
    }
} 
