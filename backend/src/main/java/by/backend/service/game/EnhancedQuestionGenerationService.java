package by.backend.service.game;

import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.dto.PersonInfoDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.entity.Person;
import by.backend.model.enums.Difficulty;
import by.backend.model.enums.Gender;
import by.backend.model.enums.TurkishFamilyRelationType;
import by.backend.model.enums.TurkishFamilyRelationType.FamilyGeneration;
import by.backend.model.enums.TurkishFamilyRelationType.RelationshipSide;
import by.backend.repository.PersonRepository;
import by.backend.service.description.RelationshipDescriptionResolver;
import by.backend.service.validation.FamilyRelationshipValidator;
import by.backend.service.validation.FamilyRelationshipValidator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gelişmiş aile ilişki sistemi kullanan soru üretim servisi
 * Yaş, cinsiyet ve mantık kontrolleri ile gerçekçi sorular üretir
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedQuestionGenerationService {

    private final PersonRepository personRepository;
    private final RelationshipDescriptionResolver relationshipDescriptionResolver;
    private final FamilyRelationshipValidator familyValidator;
    private final MessageSource messageSource;
    
    private final Random random = new Random();
    
    // Zorluk bazında ağırlıklar
    private static final Map<Difficulty, Map<FamilyGeneration, Double>> GENERATION_WEIGHTS = Map.of(
        Difficulty.EASY, Map.of(
            FamilyGeneration.PARENT, 0.4,
            FamilyGeneration.CHILD, 0.3,
            FamilyGeneration.SAME, 0.2,
            FamilyGeneration.GRANDPARENT, 0.1,
            FamilyGeneration.GRANDCHILD, 0.0
        ),
        Difficulty.MEDIUM, Map.of(
            FamilyGeneration.PARENT, 0.25,
            FamilyGeneration.CHILD, 0.25,
            FamilyGeneration.SAME, 0.25,
            FamilyGeneration.GRANDPARENT, 0.15,
            FamilyGeneration.GRANDCHILD, 0.1
        ),
        Difficulty.HARD, Map.of(
            FamilyGeneration.PARENT, 0.15,
            FamilyGeneration.CHILD, 0.15,
            FamilyGeneration.SAME, 0.35,
            FamilyGeneration.GRANDPARENT, 0.2,
            FamilyGeneration.GRANDCHILD, 0.15
        )
    );

    /**
     * Gelişmiş soru üretim metodu
     */
    public GameQuestionDTO generateEnhancedQuestion(Difficulty difficulty, Set<String> askedSignatures, Locale locale) {
        List<Person> activePersons = getActivePersonsWithAgeValidation();
        
        if (activePersons.size() < 2) {
            log.warn("Soru üretimi için yeterli kişi yok: {}", activePersons.size());
            return null;
        }

        for (int attempt = 0; attempt < 50; attempt++) {
            PersonPair pair = selectOptimalPersonPair(activePersons, difficulty);
            if (pair == null) continue;

            String questionId = createQuestionId(pair.person1, pair.person2);
            if (askedSignatures.contains(questionId)) continue;

            GameQuestionDTO question = generateQuestionForPair(pair, difficulty, locale);
            if (question != null && isQuestionValid(question)) {
                log.info("Gelişmiş soru üretildi: zorluk={}, kişiler={}+{}", 
                        difficulty, pair.person1.getFirstName(), pair.person2.getFirstName());
                return question;
            }
        }

        log.warn("Gelişmiş soru üretimi başarısız: difficulty={}", difficulty);
        return null;
    }

    /**
     * Yaş doğrulaması yapılmış aktif kişileri getirir
     */
    private List<Person> getActivePersonsWithAgeValidation() {
        return personRepository.findAll().stream()
                .filter(person -> person.getBirthDate() != null)
                .filter(person -> calculateAge(person) >= 0 && calculateAge(person) <= 120)
                .filter(person -> person.getFirstName() != null && !person.getFirstName().trim().isEmpty())
                .filter(person -> person.getLastName() != null && !person.getLastName().trim().isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Zorluk seviyesine göre optimal kişi çifti seçer
     */
    private PersonPair selectOptimalPersonPair(List<Person> persons, Difficulty difficulty) {
        List<PersonPair> validPairs = new ArrayList<>();
        
        for (int i = 0; i < persons.size() && validPairs.size() < 100; i++) {
            for (int j = i + 1; j < persons.size() && validPairs.size() < 100; j++) {
                Person p1 = persons.get(i);
                Person p2 = persons.get(j);
                
                if (isPairSuitableForDifficulty(p1, p2, difficulty)) {
                    validPairs.add(new PersonPair(p1, p2));
                }
            }
        }

        if (validPairs.isEmpty()) {
            return null;
        }

        // Zorluk seviyesine göre ağırlıklı seçim
        return selectWeightedPair(validPairs, difficulty);
    }

    /**
     * Kişi çiftinin zorluk seviyesine uygunluğunu kontrol eder
     */
    private boolean isPairSuitableForDifficulty(Person p1, Person p2, Difficulty difficulty) {
        int age1 = calculateAge(p1);
        int age2 = calculateAge(p2);
        int ageDifference = Math.abs(age1 - age2);

        return switch (difficulty) {
            case EASY -> ageDifference >= 15 && ageDifference <= 50; // Net yaş farkları
            case MEDIUM -> ageDifference >= 5 && ageDifference <= 60; // Orta seviye
            case HARD -> ageDifference >= 0 && ageDifference <= 80; // Tüm yaş aralıkları
        };
    }

    /**
     * Ağırlıklı kişi çifti seçimi
     */
    private PersonPair selectWeightedPair(List<PersonPair> validPairs, Difficulty difficulty) {
        if (validPairs.isEmpty()) return null;
        
        // Basit random seçim (ileride ağırlıklı algoritma eklenebilir)
        return validPairs.get(random.nextInt(validPairs.size()));
    }

    /**
     * Kişi çifti için soru üretir
     */
    private GameQuestionDTO generateQuestionForPair(PersonPair pair, Difficulty difficulty, Locale locale) {
        // İlişki analizi
        RelationshipDescriptionResult relationship = relationshipDescriptionResolver
                .resolveDescription(pair.person1, pair.person2, locale);

        if (relationship == null || relationship.getStatus() == null) {
            return null;
        }

        // Türkçe ilişki türünü belirle
        TurkishFamilyRelationType turkishRelation = determineTurkishRelationType(
                pair.person1, pair.person2, relationship);

        if (turkishRelation == null) {
            return null;
        }

        // Doğrulama
        ValidationResult validation = familyValidator.validateRelationship(
                pair.person1, pair.person2, turkishRelation);

        if (!validation.isValid()) {
            log.debug("İlişki doğrulama başarısız: {}", validation.getMessage());
            return null;
        }

        // Soru oluştur
        return buildGameQuestion(pair, turkishRelation, difficulty, locale);
    }

    /**
     * Türkçe ilişki türünü belirler
     */
    private TurkishFamilyRelationType determineTurkishRelationType(Person p1, Person p2, 
                                                                  RelationshipDescriptionResult relationship) {
        String messageKey = relationship.getMessageKey();
        Gender p2Gender = p2.getGender();

        if (messageKey == null) return null;

        // MessageKey'e göre Türkçe ilişki türü mapping
        return switch (messageKey) {
            case "relationship.parent" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.ANNE : TurkishFamilyRelationType.BABA;
            case "relationship.child" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.KIZ_COCUK : TurkishFamilyRelationType.ERKEK_COCUK;
            case "relationship.sibling" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.KIZ_KARDES : TurkishFamilyRelationType.ERKEK_KARDES;
            case "relationship.spouse" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.ES_KADIN : TurkishFamilyRelationType.ES_ERKEK;
            case "relationship.maternal_uncle" -> TurkishFamilyRelationType.DAYI;
            case "relationship.maternal_aunt" -> TurkishFamilyRelationType.TEYZE;
            case "relationship.paternal_uncle" -> TurkishFamilyRelationType.AMCA;
            case "relationship.paternal_aunt" -> TurkishFamilyRelationType.HALA;
            case "relationship.grandparent" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.BUYUKANNE : TurkishFamilyRelationType.BUYUKBABA;
            case "relationship.grandchild" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.KIZ_TORUN : TurkishFamilyRelationType.ERKEK_TORUN;
            case "relationship.nephew" -> TurkishFamilyRelationType.YEGEN_ERKEK;
            case "relationship.niece" -> TurkishFamilyRelationType.YEGEN_KIZ;
            case "relationship.cousin" -> p2Gender == Gender.KADIN ? 
                    TurkishFamilyRelationType.KUZEN_KIZ : TurkishFamilyRelationType.KUZEN_ERKEK;
            default -> null;
        };
    }

    /**
     * Oyun sorusunu oluşturur
     */
    private GameQuestionDTO buildGameQuestion(PersonPair pair, TurkishFamilyRelationType relationType, 
                                            Difficulty difficulty, Locale locale) {
        String questionText = String.format("%s, %s'nin nesidir?", 
                pair.person1.getFirstName() + " " + pair.person1.getLastName(),
                pair.person2.getFirstName() + " " + pair.person2.getLastName());

        String correctAnswer = relationType.getTurkishName();

        // Seçenekler oluştur
        List<String> options = generateSmartOptions(relationType, pair.person2, difficulty);
        
        return GameQuestionDTO.builder()
                .id(createQuestionId(pair.person1, pair.person2))
                .questionText(questionText)
                .person1(pair.person1.getFirstName() + " " + pair.person1.getLastName())
                .person2(pair.person2.getFirstName() + " " + pair.person2.getLastName())
                .person1Info(PersonInfoDTO.builder()
                        .id(pair.person1.getId())
                        .name(pair.person1.getFirstName() + " " + pair.person1.getLastName())
                        .gender(pair.person1.getGender())
                        .birthYear(pair.person1.getBirthDate().getYear())
                        .deathYear(pair.person1.getDeathDate() != null ? pair.person1.getDeathDate().getYear() : null)
                        .build())
                .person2Info(PersonInfoDTO.builder()
                        .id(pair.person2.getId())
                        .name(pair.person2.getFirstName() + " " + pair.person2.getLastName())
                        .gender(pair.person2.getGender())
                        .birthYear(pair.person2.getBirthDate().getYear())
                        .deathYear(pair.person2.getDeathDate() != null ? pair.person2.getDeathDate().getYear() : null)
                        .build())
                .options(options)
                .correctAnswer(correctAnswer)
                .difficulty(difficulty)
                .timeLimit(difficulty.getTimeLimit())
                .build();
    }

    /**
     * Akıllı seçenekler üretir
     */
    private List<String> generateSmartOptions(TurkishFamilyRelationType correctRelation, Person targetPerson, Difficulty difficulty) {
        Set<String> options = new HashSet<>();
        options.add(correctRelation.getTurkishName());

        Gender targetGender = targetPerson.getGender();
        FamilyGeneration correctGeneration = correctRelation.getGeneration();
        
        // Cinsiyet uyumlu yanıltıcı seçenekler
        List<TurkishFamilyRelationType> sameGenderTypes = Arrays.stream(TurkishFamilyRelationType.values())
                .filter(type -> type.getRequiredGender() == targetGender)
                .filter(type -> type != correctRelation)
                .collect(Collectors.toList());

        // Zorluk seviyesine göre seçenek stratejisi
        switch (difficulty) {
            case EASY -> addEasyDistractors(options, sameGenderTypes, correctGeneration);
            case MEDIUM -> addMediumDistractors(options, sameGenderTypes, correctGeneration);
            case HARD -> addHardDistractors(options, sameGenderTypes, correctGeneration);
        }

        // Eksik seçenekleri tamamla
        while (options.size() < difficulty.getOptionCount()) {
            TurkishFamilyRelationType randomType = sameGenderTypes.get(random.nextInt(sameGenderTypes.size()));
            options.add(randomType.getTurkishName());
        }

        List<String> result = new ArrayList<>(options);
        Collections.shuffle(result);
        return result;
    }

    private void addEasyDistractors(Set<String> options, List<TurkishFamilyRelationType> sameGenderTypes, FamilyGeneration correctGeneration) {
        // Kolay seviye: Aynı generasyondan seçenekler ekle
        sameGenderTypes.stream()
                .filter(type -> type.getGeneration() == correctGeneration)
                .limit(2)
                .forEach(type -> options.add(type.getTurkishName()));
    }

    private void addMediumDistractors(Set<String> options, List<TurkishFamilyRelationType> sameGenderTypes, FamilyGeneration correctGeneration) {
        // Orta seviye: Karışık generasyonlardan seçenekler
        sameGenderTypes.stream()
                .limit(3)
                .forEach(type -> options.add(type.getTurkishName()));
    }

    private void addHardDistractors(Set<String> options, List<TurkishFamilyRelationType> sameGenderTypes, FamilyGeneration correctGeneration) {
        // Zor seviye: Benzer ilişki türlerinden seçenekler
        sameGenderTypes.stream()
                .filter(type -> Math.abs(type.getGeneration().getGenerationDifference() - 
                                       correctGeneration.getGenerationDifference()) <= 1)
                .limit(4)
                .forEach(type -> options.add(type.getTurkishName()));
    }

    private int calculateAge(Person person) {
        LocalDate referenceDate = person.getDeathDate() != null ? person.getDeathDate() : LocalDate.now();
        return Period.between(person.getBirthDate(), referenceDate).getYears();
    }

    private String createQuestionId(Person p1, Person p2) {
        return String.format("enhanced_%d_%d_%d", p1.getId(), p2.getId(), System.currentTimeMillis() % 10000);
    }

    private boolean isQuestionValid(GameQuestionDTO question) {
        return question != null &&
               question.getQuestionText() != null && !question.getQuestionText().trim().isEmpty() &&
               question.getCorrectAnswer() != null && !question.getCorrectAnswer().trim().isEmpty() &&
               question.getOptions() != null && question.getOptions().size() >= 2 &&
               question.getOptions().contains(question.getCorrectAnswer());
    }

    private static class PersonPair {
        final Person person1;
        final Person person2;

        PersonPair(Person person1, Person person2) {
            this.person1 = person1;
            this.person2 = person2;
        }
    }
} 