package by.backend.service.relationship;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.Gender;
import by.backend.model.enums.RelationshipType;
import by.backend.model.enums.TurkishFamilyRelationType;
import by.backend.model.enums.TurkishFamilyRelationType.FamilyGeneration;
import by.backend.model.enums.TurkishFamilyRelationType.RelationshipSide;
import by.backend.repository.PersonRepository;
import by.backend.repository.RelationshipRepository;
import by.backend.service.validation.FamilyRelationshipValidator;
import by.backend.service.validation.FamilyRelationshipValidator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gelişmiş aile ilişki yönetim servisi
 * Türkçe aile yapısı ve yaş/cinsiyet doğrulaması ile çalışır
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EnhancedRelationshipService {

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final FamilyRelationshipValidator familyValidator;

    /**
     * Türkçe aile ilişki sistemi ile ilişki oluşturur
     */
    @Transactional
    public Relationship createTurkishRelationship(Long person1Id, Long person2Id, 
                                                TurkishFamilyRelationType turkishRelationType) {
        
        Person person1 = getPersonById(person1Id);
        Person person2 = getPersonById(person2Id);

        // Doğrulama
        ValidationResult validation = familyValidator.validateRelationship(person1, person2, turkishRelationType);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("İlişki doğrulama hatası: " + validation.getMessage());
        }

        // Mevcut ilişki kontrolü
        if (hasExistingRelationship(person1, person2, turkishRelationType)) {
            throw new IllegalArgumentException("Bu kişiler arasında zaten benzer bir ilişki mevcut");
        }

        // Standard RelationshipType'a dönüştür
        RelationshipType standardType = mapToStandardType(turkishRelationType);
        
        // İlişki oluştur
        Relationship relationship = Relationship.builder()
                .person1(person1)
                .person2(person2)
                .type(standardType)
                .startDate(LocalDate.now())
                .isActive(true)
                .build();

        Relationship savedRelationship = relationshipRepository.save(relationship);
        
        // Karşılıklı ilişki oluştur (gerekirse)
        createReciprocalRelationship(person1, person2, turkishRelationType, standardType);

        log.info("Türkçe aile ilişkisi oluşturuldu: {} -> {} ({})", 
                person1.getFirstName(), person2.getFirstName(), turkishRelationType.getTurkishName());

        return savedRelationship;
    }

    /**
     * Kişinin aile üyelerini Türkçe kategorilere göre getirir
     */
    @Transactional(readOnly = true)
    public Map<String, List<FamilyMember>> getFamilyMembersByCategory(Long personId) {
        Person person = getPersonById(personId);
        Map<String, List<FamilyMember>> familyByCategory = new HashMap<>();

        // Tüm ilişkileri al
        List<Relationship> allRelationships = relationshipRepository
                .findAllActiveRelationshipsForPerson(personId);

        // Kategorilere ayır
        familyByCategory.put("Ebeveynler", getParents(person, allRelationships));
        familyByCategory.put("Çocuklar", getChildren(person, allRelationships));
        familyByCategory.put("Kardeşler", getSiblings(person, allRelationships));
        familyByCategory.put("Büyükanne/Büyükbaba", getGrandparents(person, allRelationships));
        familyByCategory.put("Torunlar", getGrandchildren(person, allRelationships));
        familyByCategory.put("Amca/Dayı/Hala/Teyze", getAuntsUncles(person, allRelationships));
        familyByCategory.put("Yeğenler", getNephewsNieces(person, allRelationships));
        familyByCategory.put("Kuzenler", getCousins(person, allRelationships));
        familyByCategory.put("Eş", getSpouse(person, allRelationships));
        familyByCategory.put("Kayın İlişkileri", getInLaws(person, allRelationships));

        return familyByCategory;
    }

    /**
     * İki kişi arasındaki aile ilişkisini Türkçe olarak analiz eder
     */
    @Transactional(readOnly = true)
    public TurkishRelationshipAnalysis analyzeTurkishRelationship(Long person1Id, Long person2Id) {
        Person person1 = getPersonById(person1Id);
        Person person2 = getPersonById(person2Id);

        if (person1.equals(person2)) {
            return TurkishRelationshipAnalysis.builder()
                    .relationshipExists(false)
                    .relationshipDescription("Aynı kişi")
                    .build();
        }

        // Doğrudan ilişki kontrolü
        Optional<Relationship> directRelationship = findDirectRelationship(person1, person2);
        if (directRelationship.isPresent()) {
            TurkishFamilyRelationType turkishType = mapToTurkishType(
                    directRelationship.get(), person1, person2);
            
            return TurkishRelationshipAnalysis.builder()
                    .relationshipExists(true)
                    .turkishRelationType(turkishType)
                    .relationshipDescription(turkishType != null ? turkishType.getTurkishName() : "Belirsiz")
                    .isDirectRelationship(true)
                    .generationDifference(turkishType != null ? turkishType.getGeneration().getGenerationDifference() : 0)
                    .ageCompatible(isAgeCompatible(person1, person2, turkishType))
                    .build();
        }

        // Dolaylı ilişki analizi (kuzen, kayın vb.)
        return analyzeIndirectRelationship(person1, person2);
    }

    /**
     * Yaş uyumluluğu analizi
     */
    public AgeCompatibilityReport analyzeAgeCompatibility(Long person1Id, Long person2Id, 
                                                        TurkishFamilyRelationType proposedRelationType) {
        Person person1 = getPersonById(person1Id);
        Person person2 = getPersonById(person2Id);

        ValidationResult validation = familyValidator.validateRelationship(person1, person2, proposedRelationType);
        
        int age1 = calculateAge(person1);
        int age2 = calculateAge(person2);
        int ageDifference = Math.abs(age1 - age2);

        return AgeCompatibilityReport.builder()
                .compatible(validation.isValid())
                .person1Age(age1)
                .person2Age(age2)
                .ageDifference(ageDifference)
                .recommendedMinAge(getRecommendedMinAge(proposedRelationType))
                .recommendedMaxAge(getRecommendedMaxAge(proposedRelationType))
                .validationMessage(validation.getMessage())
                .suggestions(generateAgeSuggestions(person1, person2, proposedRelationType))
                .build();
    }

    // Private helper methods

    private Person getPersonById(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kişi bulunamadı: " + id));
    }

    private boolean hasExistingRelationship(Person person1, Person person2, TurkishFamilyRelationType turkishType) {
        RelationshipType standardType = mapToStandardType(turkishType);
        return relationshipRepository
                .findActiveRelationship(person1, person2, standardType)
                .isPresent();
    }

    private RelationshipType mapToStandardType(TurkishFamilyRelationType turkishType) {
        return switch (turkishType) {
            case ANNE, BABA -> RelationshipType.PARENT_CHILD;
            case KIZ_COCUK, ERKEK_COCUK -> RelationshipType.PARENT_CHILD;
            case KIZ_KARDES, ERKEK_KARDES -> RelationshipType.SIBLING;
            case ES_KADIN, ES_ERKEK -> RelationshipType.SPOUSE;
            case DAYI -> RelationshipType.MATERNAL_UNCLE;
            case TEYZE -> RelationshipType.MATERNAL_AUNT;
            case AMCA -> RelationshipType.PATERNAL_UNCLE;
            case HALA -> RelationshipType.PATERNAL_AUNT;
            case BUYUKANNE, BUYUKBABA, NENE, DEDE -> RelationshipType.GRANDPARENT_GRANDCHILD;
            case KIZ_TORUN, ERKEK_TORUN -> RelationshipType.GRANDPARENT_GRANDCHILD;
            case YEGEN_KIZ, YEGEN_ERKEK -> RelationshipType.UNCLE_AUNT_NEPHEW_NIECE;
            case KUZEN_KIZ, KUZEN_ERKEK -> RelationshipType.COUSIN;
            case GELIN, DAMAT -> RelationshipType.CHILD_IN_LAW;
            case KAYNANA, KAYNATA -> RelationshipType.PARENT_IN_LAW;
            case KAYIN_BIRADER, BALDIZ, GORUMCE, ENISTE -> RelationshipType.SIBLING_IN_LAW;
        };
    }

    private void createReciprocalRelationship(Person person1, Person person2, 
                                            TurkishFamilyRelationType turkishType,
                                            RelationshipType standardType) {
        // Karşılıklı ilişki gerekiyorsa oluştur
        if (needsReciprocalRelationship(turkishType)) {
            RelationshipType reciprocalType = getReciprocalType(standardType, turkishType);
            
            if (reciprocalType != null) {
                Relationship reciprocal = Relationship.builder()
                        .person1(person2)
                        .person2(person1)
                        .type(reciprocalType)
                        .startDate(LocalDate.now())
                        .isActive(true)
                        .build();
                
                relationshipRepository.save(reciprocal);
            }
        }
    }

    private boolean needsReciprocalRelationship(TurkishFamilyRelationType turkishType) {
        return turkishType.getGeneration() != FamilyGeneration.SAME;
    }

    private RelationshipType getReciprocalType(RelationshipType originalType, TurkishFamilyRelationType turkishType) {
        return switch (originalType) {
            case PARENT_CHILD -> originalType; // Aynı tip ama ters yönde
            case GRANDPARENT_GRANDCHILD -> originalType;
            case UNCLE_AUNT_NEPHEW_NIECE -> originalType;
            default -> null;
        };
    }

    private Optional<Relationship> findDirectRelationship(Person person1, Person person2) {
        return relationshipRepository.findDirectRelationshipsBidirectional(person1.getId(), person2.getId())
                .stream().findFirst();
    }

    private TurkishFamilyRelationType mapToTurkishType(Relationship relationship, Person person1, Person person2) {
        RelationshipType type = relationship.getType();
        Gender targetGender = person2.getGender();
        
        return familyValidator.mapToTurkishType(type, targetGender).orElse(null);
    }

    private boolean isAgeCompatible(Person person1, Person person2, TurkishFamilyRelationType turkishType) {
        if (turkishType == null) return false;
        
        ValidationResult validation = familyValidator.validateRelationship(person1, person2, turkishType);
        return validation.isValid();
    }

    private TurkishRelationshipAnalysis analyzeIndirectRelationship(Person person1, Person person2) {
        // Dolaylı ilişki analizi (gelecekte genişletilebilir)
        return TurkishRelationshipAnalysis.builder()
                .relationshipExists(false)
                .relationshipDescription("Akrabalık tespit edilemedi")
                .isDirectRelationship(false)
                .build();
    }

    private int calculateAge(Person person) {
        LocalDate referenceDate = person.getDeathDate() != null ? person.getDeathDate() : LocalDate.now();
        return Period.between(person.getBirthDate(), referenceDate).getYears();
    }

    private int getRecommendedMinAge(TurkishFamilyRelationType relationType) {
        return switch (relationType.getGeneration()) {
            case PARENT -> 16;
            case CHILD -> 0;
            case SAME -> 0;
            case GRANDPARENT -> 35;
            case GRANDCHILD -> 0;
        };
    }

    private int getRecommendedMaxAge(TurkishFamilyRelationType relationType) {
        return switch (relationType.getGeneration()) {
            case PARENT -> 65;
            case CHILD -> 50;
            case SAME -> 120;
            case GRANDPARENT -> 100;
            case GRANDCHILD -> 80;
        };
    }

    private List<String> generateAgeSuggestions(Person person1, Person person2, TurkishFamilyRelationType relationType) {
        List<String> suggestions = new ArrayList<>();
        int age1 = calculateAge(person1);
        int age2 = calculateAge(person2);
        
        if (relationType.getGeneration() == FamilyGeneration.PARENT && age2 >= age1) {
            suggestions.add("Ebeveyn yaşça büyük olmalıdır");
        }
        
        if (relationType.getGeneration() == FamilyGeneration.CHILD && age2 <= age1) {
            suggestions.add("Çocuk yaşça küçük olmalıdır");
        }
        
        return suggestions;
    }

    // Category-specific helper methods (simplified implementations)
    private List<FamilyMember> getParents(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD && r.getPerson2().equals(person))
                .map(r -> createFamilyMember(r.getPerson1(), "Ebeveyn"))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getChildren(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_CHILD && r.getPerson1().equals(person))
                .map(r -> createFamilyMember(r.getPerson2(), "Çocuk"))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getSiblings(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.SIBLING)
                .map(r -> r.getPerson1().equals(person) ? r.getPerson2() : r.getPerson1())
                .map(p -> createFamilyMember(p, "Kardeş"))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getGrandparents(Person person, List<Relationship> relationships) {
        // Simplified implementation
        return new ArrayList<>();
    }

    private List<FamilyMember> getGrandchildren(Person person, List<Relationship> relationships) {
        // Simplified implementation
        return new ArrayList<>();
    }

    private List<FamilyMember> getAuntsUncles(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.MATERNAL_UNCLE || 
                           r.getType() == RelationshipType.MATERNAL_AUNT ||
                           r.getType() == RelationshipType.PATERNAL_UNCLE ||
                           r.getType() == RelationshipType.PATERNAL_AUNT)
                .map(r -> createFamilyMember(r.getPerson1(), getTurkishRelationName(r.getType(), r.getPerson1())))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getNephewsNieces(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.UNCLE_AUNT_NEPHEW_NIECE && r.getPerson1().equals(person))
                .map(r -> createFamilyMember(r.getPerson2(), "Yeğen"))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getCousins(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.COUSIN)
                .map(r -> r.getPerson1().equals(person) ? r.getPerson2() : r.getPerson1())
                .map(p -> createFamilyMember(p, "Kuzen"))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getSpouse(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.SPOUSE)
                .map(r -> r.getPerson1().equals(person) ? r.getPerson2() : r.getPerson1())
                .map(p -> createFamilyMember(p, "Eş"))
                .collect(Collectors.toList());
    }

    private List<FamilyMember> getInLaws(Person person, List<Relationship> relationships) {
        return relationships.stream()
                .filter(r -> r.getType() == RelationshipType.PARENT_IN_LAW || 
                           r.getType() == RelationshipType.CHILD_IN_LAW ||
                           r.getType() == RelationshipType.SIBLING_IN_LAW)
                .map(r -> createFamilyMember(r.getPerson1(), getTurkishRelationName(r.getType(), r.getPerson1())))
                .collect(Collectors.toList());
    }

    private FamilyMember createFamilyMember(Person person, String relationshipName) {
        return FamilyMember.builder()
                .personId(person.getId())
                .name(person.getFirstName() + " " + person.getLastName())
                .gender(person.getGender())
                .age(calculateAge(person))
                .relationshipName(relationshipName)
                .build();
    }

    private String getTurkishRelationName(RelationshipType type, Person person) {
        Gender gender = person.getGender();
        return switch (type) {
            case MATERNAL_UNCLE -> "Dayı";
            case MATERNAL_AUNT -> "Teyze";
            case PATERNAL_UNCLE -> "Amca";
            case PATERNAL_AUNT -> "Hala";
            case PARENT_IN_LAW -> gender == Gender.KADIN ? "Kaynana" : "Kaynata";
            case CHILD_IN_LAW -> gender == Gender.KADIN ? "Gelin" : "Damat";
            case SIBLING_IN_LAW -> gender == Gender.KADIN ? "Baldız/Görümce" : "Kayınbirader/Enişte";
            default -> "Belirsiz";
        };
    }

    // Data classes
    public static class TurkishRelationshipAnalysis {
        private boolean relationshipExists;
        private TurkishFamilyRelationType turkishRelationType;
        private String relationshipDescription;
        private boolean isDirectRelationship;
        private int generationDifference;
        private boolean ageCompatible;

        public static TurkishRelationshipAnalysisBuilder builder() {
            return new TurkishRelationshipAnalysisBuilder();
        }

        // Getters
        public boolean isRelationshipExists() { return relationshipExists; }
        public TurkishFamilyRelationType getTurkishRelationType() { return turkishRelationType; }
        public String getRelationshipDescription() { return relationshipDescription; }
        public boolean isDirectRelationship() { return isDirectRelationship; }
        public int getGenerationDifference() { return generationDifference; }
        public boolean isAgeCompatible() { return ageCompatible; }

        // Builder
        public static class TurkishRelationshipAnalysisBuilder {
            private boolean relationshipExists;
            private TurkishFamilyRelationType turkishRelationType;
            private String relationshipDescription;
            private boolean isDirectRelationship;
            private int generationDifference;
            private boolean ageCompatible;

            public TurkishRelationshipAnalysisBuilder relationshipExists(boolean relationshipExists) {
                this.relationshipExists = relationshipExists;
                return this;
            }

            public TurkishRelationshipAnalysisBuilder turkishRelationType(TurkishFamilyRelationType turkishRelationType) {
                this.turkishRelationType = turkishRelationType;
                return this;
            }

            public TurkishRelationshipAnalysisBuilder relationshipDescription(String relationshipDescription) {
                this.relationshipDescription = relationshipDescription;
                return this;
            }

            public TurkishRelationshipAnalysisBuilder isDirectRelationship(boolean isDirectRelationship) {
                this.isDirectRelationship = isDirectRelationship;
                return this;
            }

            public TurkishRelationshipAnalysisBuilder generationDifference(int generationDifference) {
                this.generationDifference = generationDifference;
                return this;
            }

            public TurkishRelationshipAnalysisBuilder ageCompatible(boolean ageCompatible) {
                this.ageCompatible = ageCompatible;
                return this;
            }

            public TurkishRelationshipAnalysis build() {
                TurkishRelationshipAnalysis analysis = new TurkishRelationshipAnalysis();
                analysis.relationshipExists = this.relationshipExists;
                analysis.turkishRelationType = this.turkishRelationType;
                analysis.relationshipDescription = this.relationshipDescription;
                analysis.isDirectRelationship = this.isDirectRelationship;
                analysis.generationDifference = this.generationDifference;
                analysis.ageCompatible = this.ageCompatible;
                return analysis;
            }
        }
    }

    public static class AgeCompatibilityReport {
        private boolean compatible;
        private int person1Age;
        private int person2Age;
        private int ageDifference;
        private int recommendedMinAge;
        private int recommendedMaxAge;
        private String validationMessage;
        private List<String> suggestions;

        public static AgeCompatibilityReportBuilder builder() {
            return new AgeCompatibilityReportBuilder();
        }

        // Getters
        public boolean isCompatible() { return compatible; }
        public int getPerson1Age() { return person1Age; }
        public int getPerson2Age() { return person2Age; }
        public int getAgeDifference() { return ageDifference; }
        public int getRecommendedMinAge() { return recommendedMinAge; }
        public int getRecommendedMaxAge() { return recommendedMaxAge; }
        public String getValidationMessage() { return validationMessage; }
        public List<String> getSuggestions() { return suggestions; }

        // Builder
        public static class AgeCompatibilityReportBuilder {
            private boolean compatible;
            private int person1Age;
            private int person2Age;
            private int ageDifference;
            private int recommendedMinAge;
            private int recommendedMaxAge;
            private String validationMessage;
            private List<String> suggestions;

            public AgeCompatibilityReportBuilder compatible(boolean compatible) {
                this.compatible = compatible;
                return this;
            }

            public AgeCompatibilityReportBuilder person1Age(int person1Age) {
                this.person1Age = person1Age;
                return this;
            }

            public AgeCompatibilityReportBuilder person2Age(int person2Age) {
                this.person2Age = person2Age;
                return this;
            }

            public AgeCompatibilityReportBuilder ageDifference(int ageDifference) {
                this.ageDifference = ageDifference;
                return this;
            }

            public AgeCompatibilityReportBuilder recommendedMinAge(int recommendedMinAge) {
                this.recommendedMinAge = recommendedMinAge;
                return this;
            }

            public AgeCompatibilityReportBuilder recommendedMaxAge(int recommendedMaxAge) {
                this.recommendedMaxAge = recommendedMaxAge;
                return this;
            }

            public AgeCompatibilityReportBuilder validationMessage(String validationMessage) {
                this.validationMessage = validationMessage;
                return this;
            }

            public AgeCompatibilityReportBuilder suggestions(List<String> suggestions) {
                this.suggestions = suggestions;
                return this;
            }

            public AgeCompatibilityReport build() {
                AgeCompatibilityReport report = new AgeCompatibilityReport();
                report.compatible = this.compatible;
                report.person1Age = this.person1Age;
                report.person2Age = this.person2Age;
                report.ageDifference = this.ageDifference;
                report.recommendedMinAge = this.recommendedMinAge;
                report.recommendedMaxAge = this.recommendedMaxAge;
                report.validationMessage = this.validationMessage;
                report.suggestions = this.suggestions;
                return report;
            }
        }
    }

    public static class FamilyMember {
        private Long personId;
        private String name;
        private Gender gender;
        private int age;
        private String relationshipName;

        public static FamilyMemberBuilder builder() {
            return new FamilyMemberBuilder();
        }

        // Getters
        public Long getPersonId() { return personId; }
        public String getName() { return name; }
        public Gender getGender() { return gender; }
        public int getAge() { return age; }
        public String getRelationshipName() { return relationshipName; }

        // Builder
        public static class FamilyMemberBuilder {
            private Long personId;
            private String name;
            private Gender gender;
            private int age;
            private String relationshipName;

            public FamilyMemberBuilder personId(Long personId) {
                this.personId = personId;
                return this;
            }

            public FamilyMemberBuilder name(String name) {
                this.name = name;
                return this;
            }

            public FamilyMemberBuilder gender(Gender gender) {
                this.gender = gender;
                return this;
            }

            public FamilyMemberBuilder age(int age) {
                this.age = age;
                return this;
            }

            public FamilyMemberBuilder relationshipName(String relationshipName) {
                this.relationshipName = relationshipName;
                return this;
            }

            public FamilyMember build() {
                FamilyMember member = new FamilyMember();
                member.personId = this.personId;
                member.name = this.name;
                member.gender = this.gender;
                member.age = this.age;
                member.relationshipName = this.relationshipName;
                return member;
            }
        }
    }
} 