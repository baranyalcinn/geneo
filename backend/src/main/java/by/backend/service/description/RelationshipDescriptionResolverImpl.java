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
import by.backend.repository.PersonRepository; // Direkt PersonSummaryDTO için değil, Person yüklemek için
import by.backend.repository.RelationshipRepository;
import by.backend.service.pathfinding.RelationshipPathFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipDescriptionResolverImpl implements RelationshipDescriptionResolver {

    private final PersonRepository personRepository; // Kişi ID'lerinden Person yüklemek için eklendi
    private final RelationshipRepository relationshipRepository;
    private final PersonMapper personMapper;
    private final MessageSource messageSource;
    private final RelationshipPathFinder relationshipPathFinder;
    private final RelationshipProperties relationshipProperties; // findIndirectRelationship içindeki getRelationshipPath maxDepth için

    private static final String GENDER_MALE = "ERKEK";
    private static final String GENDER_FEMALE = "KADIN";
    private static final String GENDER_UNKNOWN = "BILINMEYEN";

    @Override
    public RelationshipDescriptionResult resolveDescription(Person person1, Person person2, Locale locale) {
        // person1 ve person2 zaten yüklü geldiği için tekrar yüklemeye gerek yok.
        PersonSummaryDTO person1Summary = personMapper.toSummaryDTO(person1);
        PersonSummaryDTO person2Summary = personMapper.toSummaryDTO(person2);

        try {
            if (person1.getId().equals(person2.getId())) {
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(getMessage("relationship.self", locale))
                    .messageKey("relationship.self")
                    .acceptableMessageKeys(List.of("relationship.self"))
                    .status(RelationshipStatus.SELF_REFERENCE)
                    .person1(person1Summary)
                    .person2(person2Summary)
                    .build();
            }

            List<Relationship> directRelationshipsP1P2 = relationshipRepository.findByPerson1AndPerson2AndIsActiveTrue(person1, person2);
            if (!directRelationshipsP1P2.isEmpty()) {
                Relationship relationship = directRelationshipsP1P2.get(0);
                String description = relationshipPathFinder.formatDirectRelationship(person1, person2, relationship.getType(), locale);
                String key = "relationship.direct." + relationship.getType().name().toLowerCase();
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(description)
                    .messageKey(key)
                    .acceptableMessageKeys(List.of(key))
                    .status(RelationshipStatus.FOUND)
                    .person1(person1Summary)
                    .person2(person2Summary)
                    .directTypeIfApplicable(relationship.getType())
                    .build();
            }

            List<Relationship> directRelationshipsP2P1 = relationshipRepository.findByPerson1AndPerson2AndIsActiveTrue(person2, person1);
            if (!directRelationshipsP2P1.isEmpty()) {
                Relationship relationship = directRelationshipsP2P1.get(0);
                String description = relationshipPathFinder.formatReverseRelationship(person1, person2, relationship.getType(), locale);
                String key = "relationship.reverse." + relationship.getType().name().toLowerCase();
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(description)
                    .messageKey(key)
                    .acceptableMessageKeys(List.of(key))
                    .status(RelationshipStatus.FOUND)
                    .person1(person1Summary)
                    .person2(person2Summary)
                    .directTypeIfApplicable(relationship.getType())
                    .build();
            }

            Optional<RelationshipDescriptionResult> specialRelationshipOpt = findSpecialRelationship(person1, person2, locale);
            if (specialRelationshipOpt.isPresent()) {
                return specialRelationshipOpt.get(); // person1Summary ve person2Summary zaten special metotlar tarafından ekleniyor
            }

            RelationshipDescriptionResult indirectRelationshipResult = findIndirectRelationship(person1, person2, locale);
            if (indirectRelationshipResult.getStatus() == RelationshipStatus.FOUND) {
                return indirectRelationshipResult; // person1Summary ve person2Summary zaten indirect metot tarafından ekleniyor
            }

            boolean isBloodRelated = isBloodRelated(person1, person2);
            if (isBloodRelated) {
                String distantKey = "relationship.distant_blood_relative";
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(getMessage(distantKey, locale, person1.getFirstName(), person2.getFirstName()))
                    .messageKey(distantKey)
                    .acceptableMessageKeys(List.of(distantKey))
                    .status(RelationshipStatus.FOUND)
                    .person1(person1Summary)
                    .person2(person2Summary)
                    .build();
            }

            String notFoundKey = "relationship.not_found";
            return RelationshipDescriptionResult.builder()
                .localizedDescription(getMessage(notFoundKey, locale))
                .messageKey(notFoundKey)
                .acceptableMessageKeys(List.of(notFoundKey))
                .status(RelationshipStatus.NOT_FOUND)
                .person1(person1Summary)
                .person2(person2Summary)
                .build();
        } catch (Exception e) {
            log.error("İlişki tarifi (Resolver) bulunurken bir hata oluştu ({}, {} için)", person1.getId(), person2.getId(), e);
            return RelationshipDescriptionResult.builder()
                .localizedDescription("Bilinmeyen ilişki (hata oluştu)")
                .messageKey("relationship.error")
                .acceptableMessageKeys(List.of("relationship.error"))
                .status(RelationshipStatus.ERROR)
                .person1(person1Summary)
                .person2(person2Summary)
                .build();
        }
    }

    private RelationshipDescriptionResult findIndirectRelationship(Person person1, Person person2, Locale locale) {
        List<RelationshipStepDTO> path = relationshipPathFinder.findPaths(person1, person2, relationshipProperties.getDefaultPathDisplayMaxDepth())
            .stream()
            .min(Comparator.comparingInt(List::size))
            .map(shortestPath -> relationshipPathFinder.convertPathToDTO(shortestPath, person1, person2, locale))
            .orElse(Collections.emptyList());

        if (!path.isEmpty()) {
            String description;
            if (path.size() == 1) {
                 description = path.get(0).getRelationshipToNextPerson();
            } else {
                 description = getMessage("relationship.indirect.path_found", locale, person1.getFirstName(), person2.getFirstName());
            }
            return RelationshipDescriptionResult.builder()
                .localizedDescription(description)
                .messageKey("relationship.indirect.found")
                .status(RelationshipStatus.FOUND)
                .person1(personMapper.toSummaryDTO(person1))
                .person2(personMapper.toSummaryDTO(person2))
                .path(path)
                .build();
        }
        return RelationshipDescriptionResult.builder()
            .localizedDescription(getMessage("relationship.not_found.indirect", locale))
            .messageKey("relationship.not_found.indirect")
            .status(RelationshipStatus.NOT_FOUND)
            .person1(personMapper.toSummaryDTO(person1))
            .person2(personMapper.toSummaryDTO(person2))
            .build();
    }

    private Optional<RelationshipDescriptionResult> findSpecialRelationship(Person person1, Person person2, Locale locale) {
        List<Person> person1Parents = getParentsForResolver(person1);
        List<Person> person1Children = getChildrenForResolver(person1);
        Person person1Spouse = getSpouseForResolver(person1);

        return tryResolveGrandparentRelationship(person1, person2, person1Parents, locale)
                .or(() -> tryResolveGrandchildRelationship(person1, person2, person1Children, locale))
                .or(() -> tryResolveAuntUncleRelationship(person1, person2, person1Parents, locale))
                .or(() -> {
                    List<Person> person1Siblings = getSiblingsForResolver(person1);
                    return tryResolveNephewNieceRelationship(person1, person2, person1Siblings, locale);
                })
                .or(() -> tryResolveCousinRelationship(person1, person2, person1Parents, locale))
                .or(() -> {
                    if (person1Spouse != null) {
                        return tryResolveInLawParentRelationship(person1, person2, person1Spouse, locale)
                                .or(() -> tryResolveInLawSiblingRelationship(person1, person2, person1Spouse, locale));
                    }
                    return Optional.empty();
                })
                .or(() -> tryResolveInLawChildSpouseRelationship(person1, person2, person1Children, locale));
    }

    // --- Special relationship helper methods (tryResolve...) --- 
    // Bu metotlar RelationshipServiceImpl'den kopyalanacak ve personMapper.toSummaryDTO() kullanacaklar
    private Optional<RelationshipDescriptionResult> tryResolveGrandparentRelationship(Person person1, Person person2, List<Person> person1Parents, Locale locale) {
        for (Person parent : person1Parents) {
            List<Person> grandparents = getParentsForResolver(parent); // Use local resolver version
            for (Person grandparent : grandparents) {
                if (grandparent.getId().equals(person2.getId())) {
                    String key = "relationship.grandparent";
                    if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) key = "relationship.grandfather";
                        else if (GENDER_FEMALE.equals(person2GenderName)) key = "relationship.grandmother";
                    }
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                            .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.grandparent")).status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveGrandchildRelationship(Person person1, Person person2, List<Person> person1Children, Locale locale) {
        for (Person child : person1Children) {
            List<Person> grandchildren = getChildrenForResolver(child);
            for (Person grandchild : grandchildren) {
                if (grandchild.getId().equals(person2.getId())) {
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage("relationship.grandchild", locale, person1.getFirstName(), person2.getFirstName()))
                            .messageKey("relationship.grandchild").acceptableMessageKeys(List.of("relationship.grandchild")).status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveAuntUncleRelationship(Person person1, Person person2, List<Person> person1Parents, Locale locale) {
        for (Person parent : person1Parents) {
            List<Person> parentSiblings = getSiblingsForResolver(parent);
            for (Person parentSibling : parentSiblings) {
                if (parentSibling.getId().equals(person2.getId())) {
                    String key = "relationship.aunt_uncle";
                    boolean isMaternal = (parent.getGender() != null && GENDER_FEMALE.equals(parent.getGender().name().toUpperCase()));
                    if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) key = isMaternal ? "relationship.maternal_uncle" : "relationship.paternal_uncle";
                        else if (GENDER_FEMALE.equals(person2GenderName)) key = isMaternal ? "relationship.maternal_aunt" : "relationship.paternal_aunt";
                    }
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                            .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.aunt_uncle")).status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
                }
            }
        }
        return Optional.empty();
    }

     private Optional<RelationshipDescriptionResult> tryResolveNephewNieceRelationship(Person person1, Person person2, List<Person> person1Siblings, Locale locale) {
        for (Person sibling : person1Siblings) {
            List<Person> nephewsNieces = getChildrenForResolver(sibling);
            for (Person nephewNiece : nephewsNieces) {
                if (nephewNiece.getId().equals(person2.getId())) {
                    String key = "relationship.nephew_niece";
                     if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) key = "relationship.nephew";
                        else if (GENDER_FEMALE.equals(person2GenderName)) key = "relationship.niece";
                    }
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person1.getFirstName(), person2.getFirstName()))
                            .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.nephew_niece")).status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveCousinRelationship(Person person1, Person person2, List<Person> person1Parents, Locale locale) {
        for (Person parent : person1Parents) {
            List<Person> parentSiblings = getSiblingsForResolver(parent);
            for (Person parentSibling : parentSiblings) {
                List<Person> cousins = getChildrenForResolver(parentSibling);
                for (Person cousin : cousins) {
                    if (cousin.getId().equals(person2.getId())) {
                         return Optional.of(RelationshipDescriptionResult.builder()
                                .localizedDescription(getMessage("relationship.cousin", locale, person1.getFirstName(), person2.getFirstName()))
                                .messageKey("relationship.cousin").acceptableMessageKeys(List.of("relationship.cousin")).status(RelationshipStatus.FOUND)
                                .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveInLawParentRelationship(Person person1, Person person2, Person person1Spouse, Locale locale) {
        List<Person> spouseParents = getParentsForResolver(person1Spouse);
        for (Person spouseParent : spouseParents) {
            if (spouseParent.getId().equals(person2.getId())) {
                String key = "relationship.inlaw.parent";
                if (person2.getGender() != null) {
                    String person2GenderName = person2.getGender().name().toUpperCase();
                    if (GENDER_MALE.equals(person2GenderName)) key = "relationship.inlaw.father";
                    else if (GENDER_FEMALE.equals(person2GenderName)) key = "relationship.inlaw.mother";
                }
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                        .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.inlaw.parent")).status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveInLawSiblingRelationship(Person person1, Person person2, Person person1Spouse, Locale locale) {
        List<Person> spouseSiblings = getSiblingsForResolver(person1Spouse);
        for (Person spouseSibling : spouseSiblings) {
            if (spouseSibling.getId().equals(person2.getId())) {
                String key = "relationship.inlaw.sibling";
                if (person2.getGender() != null && person1Spouse.getGender() != null) {
                     String person2GenderName = person2.getGender().name().toUpperCase();
                     String spouseGenderName = person1Spouse.getGender().name().toUpperCase();
                    if (GENDER_MALE.equals(person2GenderName)) key = "relationship.inlaw.brother";
                    else if (GENDER_FEMALE.equals(person2GenderName)) {
                        if (GENDER_MALE.equals(spouseGenderName)) key = "relationship.inlaw.sister_of_husband";
                        else key = "relationship.inlaw.sister_of_wife";
                    }
                }
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                        .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.inlaw.sibling")).status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
            }
        }
        return Optional.empty();
    }
    
    private Optional<RelationshipDescriptionResult> tryResolveInLawChildSpouseRelationship(Person person1, Person person2, List<Person> person1Children, Locale locale) {
        for (Person child : person1Children) {
            Person childSpouse = getSpouseForResolver(child);
            if (childSpouse != null && childSpouse.getId().equals(person2.getId())) {
                String key = "relationship.inlaw.child";
                if (person2.getGender() != null) {
                    String person2GenderName = person2.getGender().name().toUpperCase();
                    if (GENDER_MALE.equals(person2GenderName)) key = "relationship.inlaw.son";
                    else if (GENDER_FEMALE.equals(person2GenderName)) key = "relationship.inlaw.daughter";
                }
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale, person1.getFirstName(), person2.getFirstName()))
                        .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.inlaw.child")).status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
            }
        }
        return Optional.empty();
    }

    // --- Helper methods for fetching relatives (local to resolver, no caching here by default) ---
    private List<Person> getParentsForResolver(Person person) {
        return relationshipRepository.findByPerson2AndTypeAndIsActiveTrue(person, RelationshipType.PARENT_CHILD)
                .stream().map(Relationship::getPerson1).collect(Collectors.toList());
    }

    private List<Person> getChildrenForResolver(Person person) {
        return relationshipRepository.findByPerson1AndTypeAndIsActiveTrue(person, RelationshipType.PARENT_CHILD)
                .stream().map(Relationship::getPerson2).collect(Collectors.toList());
    }

    private List<Person> getSiblingsForResolver(Person person) {
        // Siblings are found if they share at least one parent.
        // This is a more complex logic if SIBLING type is not directly stored.
        // Assuming SIBLING type is stored or can be derived. For now, using direct SIBLING type query.
        // If SIBLING type is not directly stored, this needs to find common parents then children of those parents (excluding self).
        return relationshipRepository.findActiveRelationships(person, RelationshipType.SIBLING)
                .stream()
                .map(rel -> rel.getPerson1().getId().equals(person.getId()) ? rel.getPerson2() : rel.getPerson1())
                .collect(Collectors.toList());
    }

    private Person getSpouseForResolver(Person person) {
        return relationshipRepository.findActiveRelationships(person, RelationshipType.SPOUSE)
                .stream()
                .map(rel -> rel.getPerson1().getId().equals(person.getId()) ? rel.getPerson2() : rel.getPerson1())
                .findFirst().orElse(null);
    }

    private Set<Person> getAncestorsForResolver(Person person) {
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.add(person);
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            List<Person> parentRelationships = getParentsForResolver(current); // uses local getParents
            for (Person parent : parentRelationships) { // Changed from Relationship rel to Person parent
                if (!visited.contains(parent.getId())) {
                    ancestors.add(parent);
                    queue.add(parent);
                    visited.add(parent.getId());
                }
            }
        }
        return ancestors;
    }

    private boolean isBloodRelated(Person person1, Person person2) {
        if (person1.getId().equals(person2.getId())) return true;
        Set<Person> ancestors1 = getAncestorsForResolver(person1);
        Set<Person> ancestors2 = getAncestorsForResolver(person2);
        if (ancestors1.contains(person2) || ancestors2.contains(person1)) return true;
        Set<Person> commonAncestors = new HashSet<>(ancestors1);
        commonAncestors.retainAll(ancestors2);
        return !commonAncestors.isEmpty();
    }

    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message key not found in RelationshipDescriptionResolver: {} for locale {}", code, locale);
            String fallback = code;
            if (args != null && args.length > 0) {
                fallback += " (" + Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            }
            return "[[DESC_MSG_NOT_FOUND: " + fallback + "]] ";
        }
    }
} 