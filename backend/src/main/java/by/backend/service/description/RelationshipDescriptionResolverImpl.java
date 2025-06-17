package by.backend.service.description;

import by.backend.config.RelationshipProperties;
import by.backend.mapper.PersonMapper;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.Gender;
import by.backend.model.enums.RelationshipStatus;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.RelationshipRepository;
import by.backend.service.pathfinding.RelationshipPathFinder;
import by.backend.service.cache.RelationshipCache;
import by.backend.service.graph.FamilyGraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipDescriptionResolverImpl implements RelationshipDescriptionResolver {

    private final RelationshipRepository relationshipRepository;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;
    private final RelationshipPathFinder relationshipPathFinder;
    private final RelationshipProperties relationshipProperties;
    private final RelationshipCache relationshipCache;
    private final FamilyGraphService familyGraphService;

    private static final String GENDER_MALE = "MALE";
    private static final String GENDER_FEMALE = "FEMALE";
    private static final String DISTANT_RELATIVE_KEY = "relationship.distant_relative";
    private static final String PARENT_LITERAL = "parent";
    private static final String CHILD_LITERAL = "child";
    private static final String SIBLING_LITERAL = "sibling";
    private static final String RELATIONSHIP_PREFIX = "relationship.";
    private static final String RELATIONSHIP_ITSELF = "relationship.itself";

    @Override
    public RelationshipDescriptionResult resolveDescription(Person person1, Person person2, Locale locale) {
        if (person1.getId().equals(person2.getId())) {
            return createSelfRelationshipResult(person1, locale);
        }
        return relationshipCache.findRelationship(person1.getId(), person2.getId(),
            (p1Id, p2Id) -> computeRelationshipInternal(person1, person2, locale));
    }

    private RelationshipDescriptionResult createSelfRelationshipResult(Person person, Locale locale) {
        PersonSummaryDTO personSummary = personMapper.toSummaryDTO(person);
        return RelationshipDescriptionResult.builder()
            .localizedDescription(getMessage(RELATIONSHIP_ITSELF, locale))
            .messageKey(RELATIONSHIP_ITSELF)
            .acceptableMessageKeys(List.of(RELATIONSHIP_ITSELF))
            .status(RelationshipStatus.FOUND)
            .person1(personSummary)
            .person2(personSummary)
            .confidenceScore(1.0)
            .complexityLevel(0)
            .pathLength(0)
            .build();
    }

    private RelationshipDescriptionResult computeRelationshipInternal(Person person1, Person person2, Locale locale) {
        PersonSummaryDTO person1Summary = personMapper.toSummaryDTO(person1);
        PersonSummaryDTO person2Summary = personMapper.toSummaryDTO(person2);

        if (!familyGraphService.areConnected(person1.getId(), person2.getId())) {
            return createNotFoundResult(person1Summary, person2Summary, locale);
        }

        // 1. Doğrudan ilişkiyi kontrol et (en hızlı)
        Optional<RelationshipDescriptionResult> directResult = findDirectRelationshipOptimized(person1, person2, locale);
        if (directResult.isPresent()) {
            return directResult.get();
        }

        // 2. Dolaylı ilişki yolunu bul
        return findIndirectRelationship(person1, person2, locale);
    }

    private RelationshipDescriptionResult findIndirectRelationship(Person person1, Person person2, Locale locale) {
        int maxDepth = relationshipProperties.getDefaultPathDisplayMaxDepth();
        // Sadece person1 -> person2 yönündeki yolu ara
        List<Relationship> directedPath = relationshipPathFinder.findDirectedPath(person1, person2, maxDepth);

        if (directedPath.isEmpty()) {
            return createNotFoundResult(personMapper.toSummaryDTO(person1), personMapper.toSummaryDTO(person2), locale);
        }

        return resolvePathToDescription(directedPath, person1, person2, locale);
    }
    
    private RelationshipDescriptionResult resolvePathToDescription(List<Relationship> path, Person startPerson, Person endPerson, Locale locale) {
        List<RelationshipStepDTO> pathDTO = relationshipPathFinder.convertPathToDTO(path, startPerson, endPerson, locale);
        String messageKey = determineMessageKeyFromPath(path, startPerson, endPerson);
        String localizedDescription = getMessage(messageKey, locale, endPerson.getFirstName(), startPerson.getFirstName());
        
        // Cinsiyete özel anahtarları da kabul edilebilir olarak ekle
        List<String> acceptableKeys = new ArrayList<>();
        acceptableKeys.add(messageKey);
        // Örnek: "relationship.grandparent" ise "relationship.grandmother" ve "relationship.grandfather" ekle
        if (messageKey.endsWith(PARENT_LITERAL)) {
            acceptableKeys.add(messageKey.replace(PARENT_LITERAL, "mother"));
            acceptableKeys.add(messageKey.replace(PARENT_LITERAL, "father"));
        }
        if(messageKey.endsWith(CHILD_LITERAL)) {
            acceptableKeys.add(messageKey.replace(CHILD_LITERAL, "son"));
            acceptableKeys.add(messageKey.replace(CHILD_LITERAL, "daughter"));
        }
         if(messageKey.endsWith(SIBLING_LITERAL)) {
            acceptableKeys.add(messageKey.replace(SIBLING_LITERAL, "brother"));
            acceptableKeys.add(messageKey.replace(SIBLING_LITERAL, "sister"));
        }

        return RelationshipDescriptionResult.builder()
                .localizedDescription(localizedDescription)
                .messageKey(messageKey)
                .acceptableMessageKeys(acceptableKeys)
                .status(RelationshipStatus.FOUND)
                .person1(personMapper.toSummaryDTO(startPerson))
                .person2(personMapper.toSummaryDTO(endPerson))
                .path(pathDTO)
                .pathLength(path.size())
                .confidenceScore(calculateConfidence(path.size()))
                .complexityLevel(calculateComplexity(path.size()))
                .build();
    }
    
    private String determineMessageKeyFromPath(List<Relationship> path, Person startPerson, Person endPerson) {
        if (path.isEmpty()) return "relationship.not_found";
        if (path.size() == 1) return handleDirectRelationship(path.get(0), startPerson);
        if (path.size() == 2) return handleTwoStepRelationship(path, startPerson, endPerson);
        if (path.size() == 3) return handleThreeStepRelationship(path, startPerson, endPerson);
        if (path.size() >= 4) return handleComplexRelationship(path, startPerson, endPerson);
        
        log.warn("Path logic not implemented for path size {}. Falling back to distant relative.", path.size());
        return DISTANT_RELATIVE_KEY;
    }

    private String handleDirectRelationship(Relationship rel, Person startPerson) {
        boolean isForward = rel.getPerson1().getId().equals(startPerson.getId());
        String direction = isForward ? "direct" : "reverse";
        return RELATIONSHIP_PREFIX + direction + "." + rel.getType().name().toLowerCase(Locale.ROOT);
    }

    private String handleTwoStepRelationship(List<Relationship> path, Person startPerson, Person endPerson) {
        Relationship firstStep = path.get(0);
        Relationship lastStep = path.get(1);
        
        // Büyükanne/Büyükbaba ilişkileri
        if (firstStep.getType() == RelationshipType.PARENT_CHILD && lastStep.getType() == RelationshipType.PARENT_CHILD) {
            return handleGrandRelationship(firstStep, lastStep, startPerson, endPerson);
        }
        
        // Amca/Teyze/Dayı/Hala ilişkileri
        if (firstStep.getType() == RelationshipType.PARENT_CHILD && lastStep.getType() == RelationshipType.SIBLING) {
            return handleUncleAuntRelationship(firstStep, startPerson, endPerson);
        }
        
        // Yeğen ilişkileri (Kardeş -> Çocuk)
        if (firstStep.getType() == RelationshipType.SIBLING && lastStep.getType() == RelationshipType.PARENT_CHILD) {
            return handleNephewNieceRelationship(startPerson, endPerson);
        }
        
        // Kardeş ilişkileri (Ebeveyn -> Çocuk -> Ebeveyn -> Çocuk)
        if (firstStep.getType() == RelationshipType.PARENT_CHILD && lastStep.getType() == RelationshipType.PARENT_CHILD) {
            // Ortak ebeveyn üzerinden kardeş ilişkisi
            Person middlePerson = getMiddlePerson(firstStep, lastStep, startPerson);
            if (middlePerson != null) {
                return getSiblingKey(endPerson.getGender().name());
            }
        }
        
        // Kayın ilişkileri
        if (firstStep.getType() == RelationshipType.SPOUSE && lastStep.getType() == RelationshipType.PARENT_CHILD) {
            return getInLawParentKey(endPerson.getGender().name());
        }
        
        if (firstStep.getType() == RelationshipType.PARENT_CHILD && lastStep.getType() == RelationshipType.SPOUSE) {
            return getInLawChildKey(endPerson.getGender().name());
        }
        
        return DISTANT_RELATIVE_KEY;
    }

    private String handleThreeStepRelationship(List<Relationship> path, Person startPerson, Person endPerson) {
        // 3 adımlı ilişkiler: kuzenler, büyük amcalar/teyzeler, kayın akrabaları
        Relationship firstStep = path.get(0);
        Relationship secondStep = path.get(1);
        Relationship thirdStep = path.get(2);
        
        // Kuzen ilişkilerini kontrol et - SPESIFIK
        if (isCousinRelationshipPath(path, startPerson)) {
            return getSpecificCousinKey(path, startPerson, endPerson);
        }
        
        // Büyük amca/teyze ilişkileri - babanın/annenin amcası/teyzesi
        if (isGrandUncleAuntPath(path, startPerson)) {
            return getSpecificGrandUncleAuntKey(path, startPerson, endPerson);
        }
        
        // Kayın akraba ilişkilerini kontrol et - SPESIFIK
        if (isInLawRelationshipPath(path, startPerson)) {
            return getSpecificInLawKey(path, startPerson, endPerson);
        }
        
        // Büyük ebeveyn ilişkileri - büyükbabanın babası/annesi
        if (isGreatGrandparentPath(path, startPerson)) {
            return getGreatGrandparentKey(endPerson.getGender().name());
        }
        
        return "relationship.complex.third_degree";
    }
    
    private String handleComplexRelationship(List<Relationship> path, Person startPerson, Person endPerson) {
        // 4+ adımlı karmaşık ilişkiler
        
        // Çok uzak kuzen ilişkileri
        if (isDistantCousinPath(path)) {
            return "relationship.distant.cousin_removed";
        }
        
        // Karmaşık kayın ilişkileri (bacanak/elti gibi)
        if (isVeryComplexInLawPath(path, startPerson, endPerson)) {
            return getVeryComplexInLawKey(path, startPerson, endPerson);
        }
        
        // Üvey aile ilişkileri
        if (isStepFamilyPath(path)) {
            return getStepFamilyKey(endPerson.getGender().name());
        }
        
        return "relationship.complex.distant";
    }
    
    private boolean isCousinRelationshipPath(List<Relationship> path, Person startPerson) {
        // Parent -> Sibling -> Child -> Child paterni
        return path.size() >= 3 &&
               path.get(0).getType() == RelationshipType.PARENT_CHILD &&
               path.get(1).getType() == RelationshipType.SIBLING &&
               path.get(2).getType() == RelationshipType.PARENT_CHILD;
    }
    
    private boolean isInLawRelationshipPath(List<Relationship> path, Person startPerson) {
        // Spouse -> ... paterni
        return path.stream().anyMatch(rel -> rel.getType() == RelationshipType.SPOUSE);
    }
    
    private boolean isGrandUncleAuntPath(List<Relationship> path, Person startPerson) {
        // Parent -> Parent -> Sibling paterni (büyükbaba/büyükanne -> kardeşi)
        return path.size() >= 3 &&
               path.get(0).getType() == RelationshipType.PARENT_CHILD &&
               path.get(1).getType() == RelationshipType.PARENT_CHILD &&
               path.get(2).getType() == RelationshipType.SIBLING;
    }
    
    private boolean isDistantCousinPath(List<Relationship> path) {
        // Uzun kuzen yolları
        return path.size() >= 4 && path.stream().anyMatch(rel -> rel.getType() == RelationshipType.SIBLING);
    }
    
    private boolean isVeryComplexInLawPath(List<Relationship> path, Person startPerson, Person endPerson) {
        // Eş -> Kardeş -> Eş paterni (bacanak/elti)
        long spouseCount = path.stream().filter(rel -> rel.getType() == RelationshipType.SPOUSE).count();
        long siblingCount = path.stream().filter(rel -> rel.getType() == RelationshipType.SIBLING).count();
        return spouseCount >= 2 && siblingCount >= 1;
    }
    
    private boolean isStepFamilyPath(List<Relationship> path) {
        // Üvey aile yolları - gelecekte implement edilecek
        return false;
    }
    
    private String getCousinKey(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.cousin.male";
        if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.cousin.female";
        return "relationship.cousin";
    }
    
    private String getInLawKey(List<Relationship> path, Person endPerson) {
        String gender = endPerson.getGender().name();
        
        // Detaylı kayın ilişkisi analizi
        if (path.size() == 3) {
            if (GENDER_MALE.equalsIgnoreCase(gender)) {
                return "relationship.inlaw.brother";
            } else {
                return "relationship.inlaw.sister_of_wife";
            }
        }
        
        return "relationship.inlaw.general";
    }
    
    private String getGrandUncleAuntKey(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.grand_uncle";
        if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.grand_aunt";
        return "relationship.grand_uncle_aunt";
    }
    
    private String getVeryComplexInLawKey(List<Relationship> path, Person startPerson, Person endPerson) {
        String gender = endPerson.getGender().name();
        
        // Bacanak/Elti analizi
        if (GENDER_MALE.equalsIgnoreCase(gender)) {
            return "relationship.bacanak";
        } else {
            return "relationship.elti";
        }
    }
    
    private String getStepFamilyKey(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.step.male";
        if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.step.female";
        return "relationship.step.general";
    }

    private String handleGrandRelationship(Relationship firstStep, Relationship lastStep, Person startPerson, Person endPerson) {
        long p1Id = startPerson.getId();
        
        // P1 -> Parent -> Grandparent (P2)
        if (firstStep.getPerson2().getId().equals(p1Id) && lastStep.getPerson2().getId().equals(firstStep.getPerson1().getId())) {
            return getGrandparentKey(endPerson.getGender().name());
        }
        
        // P1 -> Child -> Grandchild (P2)
        if (firstStep.getPerson1().getId().equals(p1Id) && lastStep.getPerson1().getId().equals(firstStep.getPerson2().getId())) {
            return getGrandchildKey(endPerson.getGender().name());
        }
        
        return DISTANT_RELATIVE_KEY;
    }

    private String handleUncleAuntRelationship(Relationship firstStep, Person startPerson, Person endPerson) {
        if (firstStep.getPerson2().getId().equals(startPerson.getId())) {
            return getUncleAuntKey(endPerson.getGender().name());
        }
        return DISTANT_RELATIVE_KEY;
    }

    private String getGrandparentKey(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.grandfather";
        if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.grandmother";
        return "relationship.grandparent";
    }

    private String getGrandchildKey(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.grandson";
        if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.granddaughter";
        return "relationship.grandchild";
    }

    private String getUncleAuntKey(String gender) {
        if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.uncle";
        if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.aunt";
        return DISTANT_RELATIVE_KEY;
    }

    private String handleNephewNieceRelationship(Person startPerson, Person endPerson) {
        if (endPerson.getGender() == Gender.MALE) {
            return "relationship.nephew";
        } else {
            return "relationship.niece";
        }
    }

    private String getSiblingKey(String gender) {
        if (GENDER_MALE.equals(gender)) {
            return "relationship.sibling.brother";
        } else {
            return "relationship.sibling.sister";
        }
    }

    private String getInLawParentKey(String gender) {
        if (GENDER_MALE.equals(gender)) {
            return "relationship.inlaw.father";
        } else {
            return "relationship.inlaw.mother";
        }
    }

    private String getInLawChildKey(String gender) {
        if (GENDER_MALE.equals(gender)) {
            return "relationship.inlaw.son";
        } else {
            return "relationship.inlaw.daughter";
        }
    }

    private Person getMiddlePerson(Relationship firstStep, Relationship lastStep, Person startPerson) {
        // İki relationship arasındaki ortak kişiyi bul
        if (firstStep.getPerson1().getId().equals(startPerson.getId())) {
            // startPerson -> middlePerson -> endPerson
            if (firstStep.getPerson2().getId().equals(lastStep.getPerson1().getId())) {
                return firstStep.getPerson2();
            }
            if (firstStep.getPerson2().getId().equals(lastStep.getPerson2().getId())) {
                return firstStep.getPerson2();
            }
        } else if (firstStep.getPerson2().getId().equals(startPerson.getId())) {
            // startPerson -> middlePerson -> endPerson
            if (firstStep.getPerson1().getId().equals(lastStep.getPerson1().getId())) {
                return firstStep.getPerson1();
            }
            if (firstStep.getPerson1().getId().equals(lastStep.getPerson2().getId())) {
                return firstStep.getPerson1();
            }
        }
        return null;
    }

    private Optional<RelationshipDescriptionResult> findDirectRelationshipOptimized(Person person1, Person person2, Locale locale) {
        List<Relationship> allDirectRelationships = relationshipRepository.findDirectRelationshipsBidirectional(
                person1.getId(), person2.getId());

        if (allDirectRelationships.isEmpty()) {
            return Optional.empty();
        }

        // Genellikle tek bir aktif ilişki olmalı, ilkini alıyoruz.
        Relationship relationship = allDirectRelationships.get(0);

        List<RelationshipStepDTO> pathDTO = relationshipPathFinder.convertPathToDTO(List.of(relationship), person1, person2, locale);

        // Person1'in Person2'ye olan ilişkisini belirle
        String messageKey = determineRelationshipFromPerson1ToPerson2(relationship, person1, person2);
        String localizedDescription = relationshipPathFinder.formatDirectRelationship(person1, person2, relationship.getType(), locale);
        
        List<String> acceptableKeys = new ArrayList<>();
        acceptableKeys.add(messageKey);
        
        // Add gender-neutral fallback
        if (messageKey.contains("father") || messageKey.contains("mother")) acceptableKeys.add(RELATIONSHIP_PREFIX + PARENT_LITERAL);
        if (messageKey.contains("son") || messageKey.contains("daughter")) acceptableKeys.add(RELATIONSHIP_PREFIX + CHILD_LITERAL);
        if (messageKey.contains("brother") || messageKey.contains("sister")) acceptableKeys.add(RELATIONSHIP_PREFIX + SIBLING_LITERAL);

        return Optional.of(RelationshipDescriptionResult.builder()
                .localizedDescription(localizedDescription)
                .messageKey(messageKey)
                .acceptableMessageKeys(List.copyOf(new HashSet<>(acceptableKeys)))
                .status(RelationshipStatus.FOUND)
                .person1(personMapper.toSummaryDTO(person1))
                .person2(personMapper.toSummaryDTO(person2))
                .directTypeIfApplicable(relationship.getType())
                .path(pathDTO)
                .pathLength(1)
                .confidenceScore(1.0)
                .complexityLevel(1)
                .build());
    }

    /**
     * Person1'in Person2'ye olan ilişkisini belirler
     * Örnek: "Mehmet Demir, Zehra Öztürk'nin nesidir?" -> Mehmet'in Zehra'ya olan ilişkisi
     */
    private String determineRelationshipFromPerson1ToPerson2(Relationship relationship, Person person1, Person person2) {
        RelationshipType type = relationship.getType();
        boolean person1IsFirst = relationship.getPerson1().getId().equals(person1.getId());

        switch (type) {
            case PARENT_CHILD:
                return resolveParentChild(person1IsFirst, person1);
            case SIBLING:
                return resolveSibling(person1);
            case SPOUSE:
                return "relationship.spouse";
            case MATERNAL_UNCLE:
            case MATERNAL_AUNT:
            case PATERNAL_UNCLE:
            case PATERNAL_AUNT:
                return resolveUncleAunt(person1IsFirst, person1, type);
            default:
                log.warn("Unhandled relationship type: {}", type);
                return RELATIONSHIP_PREFIX + "unknown";
        }
    }

    private String resolveParentChild(boolean person1IsParent, Person person1) {
        if (person1IsParent) {
            return person1.getGender() == Gender.FEMALE ? "relationship.mother" : "relationship.father";
        } else {
            return person1.getGender() == Gender.FEMALE ? "relationship.daughter" : "relationship.son";
        }
    }

    private String resolveSibling(Person person1) {
        return person1.getGender() == Gender.FEMALE ? "relationship.sister" : "relationship.brother";
    }

    private String resolveUncleAunt(boolean person1IsUncleAunt, Person person1, RelationshipType type) {
        if (person1IsUncleAunt) {
            return RELATIONSHIP_PREFIX + type.name().toLowerCase(Locale.ROOT);
        } else {
            return person1.getGender() == Gender.FEMALE ? "relationship.niece" : "relationship.nephew";
        }
    }

    private RelationshipDescriptionResult createNotFoundResult(PersonSummaryDTO p1, PersonSummaryDTO p2, Locale locale) {
         return RelationshipDescriptionResult.builder()
                .localizedDescription(getMessage("relationship.not_found", locale))
                .messageKey("relationship.not_found")
                .status(RelationshipStatus.NOT_FOUND)
                .person1(p1)
                .person2(p2)
                .confidenceScore(0.0)
                .complexityLevel(5) // Max complexity for not found
                .build();
    }
    
    private double calculateConfidence(int pathLength) {
        return Math.max(0.1, 1.0 - (pathLength - 1) * 0.2);
    }

    private int calculateComplexity(int pathLength) {
        if (pathLength <= 1) return 1;
        if (pathLength <= 2) return 2;
        if (pathLength <= 3) return 3;
        if (pathLength <= 4) return 4;
        return 5;
    }

    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException _) {
            log.warn("Message not found for code '{}' in locale '{}', returning code as fallback", code, locale);
            return code;
        }
    }
    
    // ========== SPESİFİK İLİŞKİ TANIMLAMA METOTLARı ==========
    
    private String getSpecificCousinKey(List<Relationship> path, Person startPerson, Person endPerson) {
        // Path: startPerson -> parent -> grandparent -> sibling -> endPerson
        // Bu yolda hangi taraftan geldiğini kontrol edelim
        
        if (path.size() < 3) return getCousinKey(endPerson.getGender().name());
        
        Relationship firstStep = path.get(0);
        
        // İlk adımda hangi ebeveyne gittiğini kontrol et
        Person parent = (firstStep.getPerson1().getId().equals(startPerson.getId())) 
                       ? firstStep.getPerson2() : firstStep.getPerson1();
        
        // Ebeveynin cinsiyetine göre hangi taraf olduğunu belirle
        boolean isPaternal = parent.getGender() == Gender.MALE;
        
        // Amca/dayı/hala/teyze'nin çocuğu olduğunu kontrol et
        if (isPaternal) {
            // Baba tarafı - amca/hala çocuğu
            if (endPerson.getGender() == Gender.MALE) {
                return "relationship.cousin.paternal_uncle_son"; // Amca Oğlu
            } else {
                return "relationship.cousin.paternal_uncle_daughter"; // Amca Kızı
            }
        } else {
            // Anne tarafı - dayı/teyze çocuğu
            if (endPerson.getGender() == Gender.MALE) {
                return "relationship.cousin.maternal_uncle_son"; // Dayı Oğlu
            } else {
                return "relationship.cousin.maternal_uncle_daughter"; // Dayı Kızı
            }
        }
    }
    
    private String getSpecificGrandUncleAuntKey(List<Relationship> path, Person startPerson, Person endPerson) {
        // Büyük amca/dayı/hala/teyze - babanın/annenin amcası/dayısı/halası/teyzesi
        
        if (path.size() < 1) return getGrandUncleAuntKey(endPerson.getGender().name());
        
        Relationship firstStep = path.get(0);
        Person parent = (firstStep.getPerson1().getId().equals(startPerson.getId())) 
                       ? firstStep.getPerson2() : firstStep.getPerson1();
        
        boolean isPaternal = parent.getGender() == Gender.MALE;
        
        if (isPaternal) {
            // Baba tarafı büyük amca/hala
            if (endPerson.getGender() == Gender.MALE) {
                return "relationship.grand_uncle.paternal"; // Büyük Amcası
            } else {
                return "relationship.grand_aunt.paternal"; // Büyük Halası
            }
        } else {
            // Anne tarafı büyük dayı/teyze
            if (endPerson.getGender() == Gender.MALE) {
                return "relationship.grand_uncle.maternal"; // Büyük Dayısı
            } else {
                return "relationship.grand_aunt.maternal"; // Büyük Teyzesi
            }
        }
    }
    
    private String getSpecificInLawKey(List<Relationship> path, Person startPerson, Person endPerson) {
        // Karmaşık kayın ilişkileri için spesifik tanımlama
        
        if (path.size() < 2) return getInLawKey(path, endPerson);
        
        // Eş -> Kardeş -> ? pattern'ini kontrol et
        if (path.get(0).getType() == RelationshipType.SPOUSE && 
            path.get(1).getType() == RelationshipType.SIBLING) {
            
            if (endPerson.getGender() == Gender.MALE) {
                return "relationship.complex_inlaw.brother_wife_brother"; // Kayınbiraderinin Erkek Kardeşi
            } else {
                return "relationship.complex_inlaw.brother_wife_sister"; // Kayınbiraderinin Kız Kardeşi
            }
        }
        
        // Kardeş -> Eş -> ? pattern'ini kontrol et
        if (path.get(0).getType() == RelationshipType.SIBLING && 
            path.get(1).getType() == RelationshipType.SPOUSE) {
            
            if (endPerson.getGender() == Gender.MALE) {
                return "relationship.complex_inlaw.sister_husband_brother"; // Kayınkardeşinin Erkek Kardeşi
            } else {
                return "relationship.complex_inlaw.sister_husband_sister"; // Kayınkardeşinin Kız Kardeşi
            }
        }
        
        // Fallback - genel kayın akraba
        return getInLawKey(path, endPerson);
    }
    
    private boolean isGreatGrandparentPath(List<Relationship> path, Person startPerson) {
        // Büyük büyük ebeveyn kontrolü - 3 adım parent-child
        return path.size() == 3 &&
               path.stream().allMatch(rel -> rel.getType() == RelationshipType.PARENT_CHILD);
    }
    
    private String getGreatGrandparentKey(String gender) {
        if (GENDER_MALE.equals(gender)) {
            return "relationship.great_grandparent.male"; // Büyük Dedesi
        } else {
            return "relationship.great_grandparent.female"; // Büyük Ninesi
        }
    }

    /**
     * İki kişi arasındaki daha karmaşık, çok adımlı ilişkileri çözer.
     * Örnek: Elti, Bacanak, Kayınbirader, Dünür vb.
     */
    private RelationshipDescriptionResult resolveComplexRelationship(Person person1, Person person2, List<Relationship> path, Locale locale) {
        if (path == null || path.isEmpty() || path.size() < 2) {
            return null; // Yetersiz yol, bu metot çözemez
        }

        List<Person> personPath = getPersonPath(person1, path);
        if (personPath.get(personPath.size() - 1).getId().longValue() != person2.getId().longValue()) {
            return null; // Yol hedef kişiye ulaşmıyor
        }

        String messageKey = "relationship.distant"; // Default

        // 2 adımlı karmaşık ilişkiler (p1 -> A -> p2)
        if (path.size() == 2) {
            Person p_mid = personPath.get(1);
            Relationship r1 = path.get(0);
            Relationship r2 = path.get(1);

            // Kayınpeder/Kayınvalide: p1 -> eş(p_mid) -> ebeveyn(p2)
            if (r1.getType() == RelationshipType.SPOUSE && getOtherPerson(r1, person1).getId().equals(p_mid.getId()) &&
                r2.getType() == RelationshipType.PARENT_CHILD && r2.getPerson1().getId().equals(person2.getId()) &&
                r2.getPerson2().getId().equals(p_mid.getId())) {
                messageKey = person2.getGender() == Gender.FEMALE ? "relationship.inlaw.mother_in_law" : "relationship.inlaw.father_in_law";
            }
            // Kayınbirader/Görümce/Baldız: p1 -> eş(p_mid) -> kardeş(p2)
            else if (r1.getType() == RelationshipType.SPOUSE && getOtherPerson(r1, person1).getId().equals(p_mid.getId()) &&
                     r2.getType() == RelationshipType.SIBLING && getOtherPerson(r2, p_mid).getId().equals(person2.getId())) {
                if (person1.getGender() == Gender.FEMALE) { // p1 kadınsa, kocasının kardeşleri
                    messageKey = person2.getGender() == Gender.FEMALE ? "relationship.inlaw.gorumce" : "relationship.inlaw.kayinbirader";
                } else { // p1 erkekse, karısının kardeşleri
                    messageKey = person2.getGender() == Gender.FEMALE ? "relationship.inlaw.baldiz" : "relationship.inlaw.kayinbirader";
                }
            }
        }

        // 3 adımlı karmaşık ilişkiler (p1 -> A -> B -> p2)
        if (path.size() == 3) {
            Relationship r1 = path.get(0), r2 = path.get(1), r3 = path.get(2);

            // Elti/Bacanak Pattern: p1 -> eş -> kardeş -> eş -> p2
            if (r1.getType() == RelationshipType.SPOUSE && r2.getType() == RelationshipType.SIBLING && r3.getType() == RelationshipType.SPOUSE) {
                // Elti: İki erkek kardeşin eşleri. p1(F) ve p2(F)
                if (person1.getGender() == Gender.FEMALE && person2.getGender() == Gender.FEMALE) {
                    messageKey = "relationship.inlaw.elti";
                }
                // Bacanak: İki kız kardeşin eşleri. p1(M) ve p2(M)
                else if (person1.getGender() == Gender.MALE && person2.getGender() == Gender.MALE) {
                    messageKey = "relationship.inlaw.bacanak";
                }
            }
            // Dünür Pattern: p1 -> çocuk -> eş -> ebeveyn -> p2
            else if (r1.getType() == RelationshipType.PARENT_CHILD && r1.getPerson1().getId().equals(person1.getId()) &&
                     r2.getType() == RelationshipType.SPOUSE &&
                     r3.getType() == RelationshipType.PARENT_CHILD && r3.getPerson1().getId().equals(person2.getId())) {
                messageKey = "relationship.inlaw.dunur";
            }
        }

        if (messageKey.equals("relationship.distant")) {
            return null; // Çözümlenmiş bir karmaşık ilişki bulunamadı, null döndürerek ana metodun devam etmesini sağla
        }

        return RelationshipDescriptionResult.builder()
                .status(RelationshipStatus.FOUND)
                .messageKey(messageKey)
                .path(relationshipPathFinder.convertPathToDTO(path, person1, person2, locale))
                .build();
    }

    /**
     * Bir ilişki nesnesinden ve bir başlangıç kişisinden diğer kişiyi alır.
     */
    private Person getOtherPerson(Relationship relationship, Person person) {
        return relationship.getPerson1().getId().equals(person.getId()) ? relationship.getPerson2() : relationship.getPerson1();
    }

    /**
     * Verilen başlangıç kişisi ve ilişki listesinden tam kişi yolunu oluşturur.
     */
    private List<Person> getPersonPath(Person start, List<Relationship> relationships) {
        List<Person> personPath = new ArrayList<>();
        personPath.add(start);
        Person current = start;

        for (Relationship rel : relationships) {
            Person next = getOtherPerson(rel, current);
            personPath.add(next);
            current = next;
        }
        return personPath;
    }

    private RelationshipDescriptionResult createDistantRelationshipResult(Person person1, Person person2, List<Relationship> path, Locale locale) {
        String messageKey = "relationship.distant";
        // ... existing code ...
        return null; // Placeholder return, actual implementation needed
    }
}
