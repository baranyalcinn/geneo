package by.backend.service.description;

import by.backend.config.RelationshipProperties;
import by.backend.mapper.PersonMapper;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
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
        
        if (firstStep.getType() == RelationshipType.PARENT_CHILD && lastStep.getType() == RelationshipType.PARENT_CHILD) {
            return handleGrandRelationship(firstStep, lastStep, startPerson, endPerson);
        }
        
        if (firstStep.getType() == RelationshipType.PARENT_CHILD && lastStep.getType() == RelationshipType.SIBLING) {
            return handleUncleAuntRelationship(firstStep, startPerson, endPerson);
        }
        
        return DISTANT_RELATIVE_KEY;
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


    private Optional<RelationshipDescriptionResult> findDirectRelationshipOptimized(Person person1, Person person2, Locale locale) {
        List<Relationship> allDirectRelationships = relationshipRepository.findDirectRelationshipsBidirectional(
                person1.getId(), person2.getId());

        if (allDirectRelationships.isEmpty()) {
            return Optional.empty();
        }

        // Genellikle tek bir aktif ilişki olmalı, ilkini alıyoruz.
        Relationship relationship = allDirectRelationships.get(0);

        List<RelationshipStepDTO> pathDTO = relationshipPathFinder.convertPathToDTO(List.of(relationship), person1, person2, locale);

        boolean isForward = relationship.getPerson1().getId().equals(person1.getId());
        String messageKey, localizedDescription;

        if (isForward) {
            messageKey = getForwardRelationshipKey(relationship.getType(), person2.getGender().name());
            localizedDescription = relationshipPathFinder.formatDirectRelationship(person1, person2, relationship.getType(), locale);
        } else {
            messageKey = getReverseRelationshipKey(relationship.getType(), person2.getGender().name());
            localizedDescription = relationshipPathFinder.formatReverseRelationship(person1, person2, relationship.getType(), locale);
        }
        
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

    private String getForwardRelationshipKey(RelationshipType type, String gender) {
        switch (type) {
            case PARENT_CHILD:
                if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.son";
                if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.daughter";
                return RELATIONSHIP_PREFIX + CHILD_LITERAL;
            case SIBLING:
                 if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.brother";
                if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.sister";
                return RELATIONSHIP_PREFIX + SIBLING_LITERAL;
            case SPOUSE:
                return "relationship.spouse";
            default:
                return RELATIONSHIP_PREFIX + "direct." + type.name().toLowerCase(Locale.ROOT);
        }
    }
    
    private String getReverseRelationshipKey(RelationshipType type, String gender) {
        if (type == RelationshipType.PARENT_CHILD) {
            if (GENDER_MALE.equalsIgnoreCase(gender)) return "relationship.father";
            if (GENDER_FEMALE.equalsIgnoreCase(gender)) return "relationship.mother";
            return RELATIONSHIP_PREFIX + PARENT_LITERAL;
        }
        // Sibling and spouse are symmetric
        return getForwardRelationshipKey(type, gender);
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
}
