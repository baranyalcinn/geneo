package by.backend.service.validation;

import by.backend.model.entity.Person;
import by.backend.model.enums.Gender;
import by.backend.model.enums.RelationshipType;
import by.backend.model.enums.TurkishFamilyRelationType;
import by.backend.model.enums.TurkishFamilyRelationType.FamilyGeneration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;

/**
 * Aile ilişkilerinin yaş, cinsiyet ve mantık uyumluluğunu kontrol eden servis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FamilyRelationshipValidator {

    // Yaş sabitleri
    private static final int MIN_PARENT_AGE = 16;
    private static final int MAX_PARENT_AGE = 65;
    private static final int MIN_MARRIAGE_AGE = 16;
    private static final int MAX_AGE_DIFFERENCE_SPOUSE = 30;
    private static final int MIN_AGE_DIFFERENCE_PARENT_CHILD = 16;
    private static final int MAX_AGE_DIFFERENCE_PARENT_CHILD = 50;
    private static final int MAX_AGE_DIFFERENCE_SIBLING = 25;
    private static final int MIN_AGE_DIFFERENCE_GRANDPARENT = 35;
    private static final int MAX_AGE_DIFFERENCE_GRANDPARENT = 80;

    /**
     * İki kişi arasındaki ilişkinin geçerliliğini kontrol eder
     */
    public ValidationResult validateRelationship(Person person1, Person person2, 
                                               TurkishFamilyRelationType relationType) {
        try {
            // Temel kontroller
            if (person1 == null || person2 == null) {
                return ValidationResult.invalid("Kişi bilgileri eksik");
            }

            if (person1.equals(person2)) {
                return ValidationResult.invalid("Kişi kendisiyle ilişki kuramaz");
            }

            // Cinsiyet kontrolü
            ValidationResult genderResult = validateGender(person2, relationType);
            if (!genderResult.isValid()) {
                return genderResult;
            }

            // Yaş kontrolü
            ValidationResult ageResult = validateAgeCompatibility(person1, person2, relationType);
            if (!ageResult.isValid()) {
                return ageResult;
            }

            // Generasyon kontrolü
            ValidationResult generationResult = validateGeneration(person1, person2, relationType);
            if (!generationResult.isValid()) {
                return generationResult;
            }

            return ValidationResult.valid("İlişki geçerli");

        } catch (Exception e) {
            log.error("İlişki doğrulama hatası: person1={}, person2={}, relation={}", 
                     person1.getId(), person2.getId(), relationType, e);
            return ValidationResult.invalid("Doğrulama işlemi sırasında hata oluştu");
        }
    }

    /**
     * Cinsiyet uyumluluğunu kontrol eder
     */
    private ValidationResult validateGender(Person person, TurkishFamilyRelationType relationType) {
        Gender requiredGender = relationType.getRequiredGender();
        Gender personGender = person.getGender();

        if (requiredGender != personGender) {
            return ValidationResult.invalid(
                String.format("%s ilişkisi için %s cinsiyet gerekli, %s verildi", 
                             relationType.getTurkishName(), 
                             requiredGender.getLabel(), 
                             personGender.getLabel())
            );
        }

        return ValidationResult.valid("Cinsiyet uyumlu");
    }

    /**
     * Yaş uyumluluğunu kontrol eder
     */
    private ValidationResult validateAgeCompatibility(Person person1, Person person2, 
                                                    TurkishFamilyRelationType relationType) {
        int age1 = calculateAge(person1);
        int age2 = calculateAge(person2);
        int ageDifference = Math.abs(age1 - age2);

        switch (relationType.getGeneration()) {
            case PARENT:
                return validateParentChildAge(person1, person2, relationType, age1, age2);
            case CHILD:
                return validateParentChildAge(person2, person1, relationType, age2, age1);
            case SAME:
                return validateSameGenerationAge(person1, person2, relationType, ageDifference);
            case GRANDPARENT:
                return validateGrandparentAge(person1, person2, age1, age2);
            case GRANDCHILD:
                return validateGrandparentAge(person2, person1, age2, age1);
            default:
                return ValidationResult.valid("Yaş kontrolü atlandı");
        }
    }

    /**
     * Ebeveyn-çocuk yaş uyumluluğunu kontrol eder
     */
    private ValidationResult validateParentChildAge(Person parent, Person child, 
                                                  TurkishFamilyRelationType relationType,
                                                  int parentAge, int childAge) {
        int ageDifference = parentAge - childAge;

        if (ageDifference < MIN_AGE_DIFFERENCE_PARENT_CHILD) {
            return ValidationResult.invalid(
                String.format("Ebeveyn-çocuk arasında en az %d yaş farkı olmalı (Mevcut: %d)", 
                             MIN_AGE_DIFFERENCE_PARENT_CHILD, ageDifference)
            );
        }

        if (ageDifference > MAX_AGE_DIFFERENCE_PARENT_CHILD) {
            return ValidationResult.invalid(
                String.format("Ebeveyn-çocuk arasında en fazla %d yaş farkı olmalı (Mevcut: %d)", 
                             MAX_AGE_DIFFERENCE_PARENT_CHILD, ageDifference)
            );
        }

        if (parentAge < MIN_PARENT_AGE) {
            return ValidationResult.invalid(
                String.format("Ebeveyn en az %d yaşında olmalı (Mevcut: %d)", 
                             MIN_PARENT_AGE, parentAge)
            );
        }

        return ValidationResult.valid("Ebeveyn-çocuk yaş uyumluluğu geçerli");
    }

    /**
     * Aynı generasyon yaş uyumluluğunu kontrol eder
     */
    private ValidationResult validateSameGenerationAge(Person person1, Person person2, 
                                                     TurkishFamilyRelationType relationType,
                                                     int ageDifference) {
        // Eş kontrolü
        if (relationType == TurkishFamilyRelationType.ES_ERKEK || 
            relationType == TurkishFamilyRelationType.ES_KADIN) {
            
            if (ageDifference > MAX_AGE_DIFFERENCE_SPOUSE) {
                return ValidationResult.invalid(
                    String.format("Eşler arasında en fazla %d yaş farkı olmalı (Mevcut: %d)", 
                                 MAX_AGE_DIFFERENCE_SPOUSE, ageDifference)
                );
            }

            int minAge = Math.min(calculateAge(person1), calculateAge(person2));
            if (minAge < MIN_MARRIAGE_AGE) {
                return ValidationResult.invalid(
                    String.format("Evlilik için minimum yaş %d (En genç: %d)", 
                                 MIN_MARRIAGE_AGE, minAge)
                );
            }
        }

        // Kardeş kontrolü
        if (relationType == TurkishFamilyRelationType.KIZ_KARDES || 
            relationType == TurkishFamilyRelationType.ERKEK_KARDES) {
            
            if (ageDifference > MAX_AGE_DIFFERENCE_SIBLING) {
                return ValidationResult.invalid(
                    String.format("Kardeşler arasında en fazla %d yaş farkı olmalı (Mevcut: %d)", 
                                 MAX_AGE_DIFFERENCE_SIBLING, ageDifference)
                );
            }
        }

        return ValidationResult.valid("Aynı generasyon yaş uyumluluğu geçerli");
    }

    /**
     * Büyükanne/büyükbaba yaş uyumluluğunu kontrol eder
     */
    private ValidationResult validateGrandparentAge(Person grandparent, Person grandchild, 
                                                   int grandparentAge, int grandchildAge) {
        int ageDifference = grandparentAge - grandchildAge;

        if (ageDifference < MIN_AGE_DIFFERENCE_GRANDPARENT) {
            return ValidationResult.invalid(
                String.format("Büyükanne/baba-torun arasında en az %d yaş farkı olmalı (Mevcut: %d)", 
                             MIN_AGE_DIFFERENCE_GRANDPARENT, ageDifference)
            );
        }

        if (ageDifference > MAX_AGE_DIFFERENCE_GRANDPARENT) {
            return ValidationResult.invalid(
                String.format("Büyükanne/baba-torun arasında en fazla %d yaş farkı olmalı (Mevcut: %d)", 
                             MAX_AGE_DIFFERENCE_GRANDPARENT, ageDifference)
            );
        }

        return ValidationResult.valid("Büyükanne/baba-torun yaş uyumluluğu geçerli");
    }

    /**
     * Generasyon uyumluluğunu kontrol eder
     */
    private ValidationResult validateGeneration(Person person1, Person person2, 
                                              TurkishFamilyRelationType relationType) {
        // Generasyon kontrolü için ek mantık eklenebilir
        // Örneğin: aynı generasyonda olanların birbirine ebeveyn olamayacağı
        return ValidationResult.valid("Generasyon uyumluluğu geçerli");
    }

    /**
     * Kişinin yaşını hesaplar
     */
    private int calculateAge(Person person) {
        LocalDate birthDate = person.getBirthDate();
        LocalDate referenceDate = person.getDeathDate() != null ? 
                                 person.getDeathDate() : LocalDate.now();
        
        return Period.between(birthDate, referenceDate).getYears();
    }

    /**
     * RelationshipType'dan TurkishFamilyRelationType'a dönüştürür
     */
    public Optional<TurkishFamilyRelationType> mapToTurkishType(RelationshipType relationshipType, 
                                                               Gender targetGender) {
        return switch (relationshipType) {
            case PARENT_CHILD -> targetGender == Gender.KADIN ? 
                               Optional.of(TurkishFamilyRelationType.ANNE) :
                               Optional.of(TurkishFamilyRelationType.BABA);
            case SIBLING -> targetGender == Gender.KADIN ?
                          Optional.of(TurkishFamilyRelationType.KIZ_KARDES) :
                          Optional.of(TurkishFamilyRelationType.ERKEK_KARDES);
            case SPOUSE -> targetGender == Gender.KADIN ?
                         Optional.of(TurkishFamilyRelationType.ES_KADIN) :
                         Optional.of(TurkishFamilyRelationType.ES_ERKEK);
            case MATERNAL_UNCLE -> Optional.of(TurkishFamilyRelationType.DAYI);
            case MATERNAL_AUNT -> Optional.of(TurkishFamilyRelationType.TEYZE);
            case PATERNAL_UNCLE -> Optional.of(TurkishFamilyRelationType.AMCA);
            case PATERNAL_AUNT -> Optional.of(TurkishFamilyRelationType.HALA);
            default -> Optional.empty();
        };
    }

    /**
     * Doğrulama sonucu sınıfı
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult valid(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("ValidationResult{valid=%s, message='%s'}", valid, message);
        }
    }
} 