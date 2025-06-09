package by.backend.service.description;

import by.backend.config.RelationshipProperties;
import by.backend.mapper.PersonMapper;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
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
import java.util.stream.Collectors;

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

    private static final String GENDER_MALE = "ERKEK";
    private static final String GENDER_FEMALE = "KADIN";

    @Override
    public RelationshipDescriptionResult resolveDescription(Person person1, Person person2, Locale locale) {
        // Early exit for same person
        if (person1.getId().equals(person2.getId())) {
            PersonSummaryDTO person1Summary = personMapper.toSummaryDTO(person1);
            return RelationshipDescriptionResult.builder()
                .localizedDescription(getMessage("relationship.itself", locale))
                .messageKey("relationship.itself")
                .acceptableMessageKeys(List.of("relationship.itself"))
                .status(RelationshipStatus.FOUND)
                .person1(person1Summary)
                .person2(person1Summary)
                .build();
        }

        // Use cache for O(1) lookup after first computation
        return relationshipCache.findRelationship(person1.getId(), person2.getId(), 
            (p1Id, p2Id) -> computeRelationshipInternal(person1, person2, locale));
    }
    
    /**
     * Internal computation method - only called on cache miss
     */
    private RelationshipDescriptionResult computeRelationshipInternal(Person person1, Person person2, Locale locale) {
        PersonSummaryDTO person1Summary = personMapper.toSummaryDTO(person1);
        PersonSummaryDTO person2Summary = personMapper.toSummaryDTO(person2);

        // Quick graph-based checks first
        if (!familyGraphService.areSameFamily(person1.getId(), person2.getId())) {
            // Not in same family cluster - early exit
            return RelationshipDescriptionResult.builder()
                .localizedDescription(getMessage("relationship.not_found", locale))
                .messageKey("relationship.not_found")
                .acceptableMessageKeys(List.of("relationship.not_found"))
                .status(RelationshipStatus.NOT_FOUND)
                .person1(person1Summary)
                .person2(person2Summary)
                .build();
        }

        // Check graph-based direct connection first - O(1) lookup
        if (familyGraphService.areConnected(person1.getId(), person2.getId())) {
            // 1. İlk olarak doğrudan ilişki (P1->P2) arama
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

            // 2. Ters ilişki (P2->P1) arama
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
        }

        // 3. Özel akrabalık ilişkilerini ara (amca, dayı, hala, teyze, vb.)
        Optional<RelationshipDescriptionResult> specialRelationshipOpt = findSpecialRelationship(person1, person2, locale);
        if (specialRelationshipOpt.isPresent()) {
            return specialRelationshipOpt.get();
        }

        // 4. Kan bağı var mı kontrol et
        boolean isBloodRelated = isBloodRelated(person1, person2);
        if (isBloodRelated) {
            return RelationshipDescriptionResult.builder()
                .localizedDescription(getMessage("relationship.distant_relative", locale))
                .messageKey("relationship.distant_relative")
                .acceptableMessageKeys(List.of("relationship.distant_relative"))
                .status(RelationshipStatus.FOUND)
                .person1(person1Summary)
                .person2(person2Summary)
                .build();
        }

        // 5. İlişki bulunamadı
        return RelationshipDescriptionResult.builder()
            .localizedDescription(getMessage("relationship.not_found", locale))
            .messageKey("relationship.not_found")
            .acceptableMessageKeys(List.of("relationship.not_found"))
            .status(RelationshipStatus.NOT_FOUND)
            .person1(person1Summary)
            .person2(person2Summary)
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
                .or(() -> tryResolveInLawChildSpouseRelationship(person1, person2, person1Children, locale))
                .or(() -> tryResolveBackanakEltiRelationship(person1, person2, person1Spouse, locale));
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
                            .localizedDescription(getMessage(key, locale))
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
                            .localizedDescription(getMessage("relationship.grandchild", locale))
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
                    String key;
                    boolean isMaternal = (parent.getGender() != null && GENDER_FEMALE.equals(parent.getGender().name().toUpperCase()));
                    
                    if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) {
                            key = isMaternal ? "relationship.maternal_uncle" : "relationship.paternal_uncle";
                        } else if (GENDER_FEMALE.equals(person2GenderName)) {
                            key = isMaternal ? "relationship.maternal_aunt" : "relationship.paternal_aunt";
                        } else {
                            key = "relationship.aunt_uncle";
                        }
                    } else {
                        key = "relationship.aunt_uncle";
                    }
                    
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale))
                            .messageKey(key)
                            .acceptableMessageKeys(List.of(key, "relationship.aunt_uncle"))
                            .status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1))
                            .person2(personMapper.toSummaryDTO(person2))
                            .build());
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
                            .localizedDescription(getMessage(key, locale))
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
                                .localizedDescription(getMessage("relationship.cousin", locale))
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
                        .localizedDescription(getMessage(key, locale))
                        .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.inlaw.parent")).status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
            }
        }
        return Optional.empty();
    }

    /**
     * Eşin kardeşleri ile ilişkiyi çözümler (Türk aile yapısına uygun)
     * Örnek: Baran'ın eşi Gaye, Gaye'nin kardeşi Meltem -> Baran için Meltem "baldızı"
     */
    private Optional<RelationshipDescriptionResult> tryResolveInLawSiblingRelationship(Person person1, Person person2, Person person1Spouse, Locale locale) {
        List<Person> spouseSiblings = getSiblingsForResolver(person1Spouse);
        for (Person spouseSibling : spouseSiblings) {
            if (spouseSibling.getId().equals(person2.getId())) {
                String key = determineSpouseSiblingRelationship(person1, person2, person1Spouse);
                
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale))
                        .messageKey(key)
                        .acceptableMessageKeys(List.of(key))
                        .status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1))
                        .person2(personMapper.toSummaryDTO(person2))
                        .build());
            }
        }
        
        // Ters yönü de kontrol et: Person2'nin kardeşi Person1 ise
        return tryResolveSiblingSpouseRelationship(person1, person2, locale);
    }
    
    /**
     * Eşin kardeşi için doğru terimi belirler
     */
    private String determineSpouseSiblingRelationship(Person person1, Person person2, Person person1Spouse) {
        if (person1.getGender() == null || person2.getGender() == null || person1Spouse.getGender() == null) {
            return "relationship.inlaw.brother"; // Fallback
        }
        
        String person1Gender = person1.getGender().name().toUpperCase();
        String person2Gender = person2.getGender().name().toUpperCase();
        String spouseGender = person1Spouse.getGender().name().toUpperCase();
        
        // Person2 erkekse -> Kayınbirader
        if (GENDER_MALE.equals(person2Gender)) {
            return "relationship.inlaw.brother";
        }
        
        // Person2 kadınsa -> Baldız veya Görümce
        if (GENDER_FEMALE.equals(person2Gender)) {
            if (GENDER_MALE.equals(person1Gender) && GENDER_FEMALE.equals(spouseGender)) {
                // Erkek (Person1) - Kadın (Eş) -> Eşin kız kardeşi = Baldız
                return "relationship.inlaw.sister_of_wife";
            } else if (GENDER_FEMALE.equals(person1Gender) && GENDER_MALE.equals(spouseGender)) {
                // Kadın (Person1) - Erkek (Eş) -> Eşin kız kardeşi = Görümce
                return "relationship.inlaw.sister_of_husband";
            }
        }
        
        return "relationship.inlaw.brother"; // Fallback
    }
    
    /**
     * Kardeşin eşi ilişkisini çözümler (Enişte/Yenge)
     * Örnek: Meltem'in kardeşi Gaye, Gaye'nin eşi Baran -> Meltem için Baran "enişte"
     */
    private Optional<RelationshipDescriptionResult> tryResolveSiblingSpouseRelationship(Person person1, Person person2, Locale locale) {
        List<Person> person1Siblings = getSiblingsForResolver(person1);
        for (Person sibling : person1Siblings) {
            Person siblingSpouse = getSpouseForResolver(sibling);
            if (siblingSpouse != null && siblingSpouse.getId().equals(person2.getId())) {
                String key = determineSiblingSpouseRelationship(person1, person2, sibling);
                
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale))
                        .messageKey(key)
                        .acceptableMessageKeys(List.of(key))
                        .status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1))
                        .person2(personMapper.toSummaryDTO(person2))
                        .build());
            }
        }
        return Optional.empty();
    }
    
    /**
     * Kardeşin eşi için doğru terimi belirler
     */
    private String determineSiblingSpouseRelationship(Person person1, Person person2, Person sibling) {
        if (person2.getGender() == null) {
            return "relationship.sibling_spouse.male"; // Fallback
        }
        
        String person2Gender = person2.getGender().name().toUpperCase();
        
        if (GENDER_MALE.equals(person2Gender)) {
            return "relationship.sibling_spouse.male"; // Enişte
        } else if (GENDER_FEMALE.equals(person2Gender)) {
            return "relationship.sibling_spouse.female"; // Yenge  
        }
        
        return "relationship.sibling_spouse.male"; // Fallback
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
                        .localizedDescription(getMessage(key, locale))
                        .messageKey(key).acceptableMessageKeys(List.of(key, "relationship.inlaw.child")).status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1)).person2(personMapper.toSummaryDTO(person2)).build());
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveBackanakEltiRelationship(Person person1, Person person2, Person person1Spouse, Locale locale) {
        if (person1Spouse == null) return Optional.empty();
        
        // Bacanak/Elti: Person1'in eşinin kardeşinin eşi
        List<Person> spouseSiblings = getSiblingsForResolver(person1Spouse);
        for (Person spouseSibling : spouseSiblings) {
            Person spouseSiblingSpouse = getSpouseForResolver(spouseSibling);
            if (spouseSiblingSpouse != null && spouseSiblingSpouse.getId().equals(person2.getId())) {
                String key;
                if (person2.getGender() != null && person1.getGender() != null) {
                    String person2GenderName = person2.getGender().name().toUpperCase();
                    String person1GenderName = person1.getGender().name().toUpperCase();
                    
                    if (GENDER_MALE.equals(person1GenderName) && GENDER_MALE.equals(person2GenderName)) {
                        key = "relationship.spouse_sibling_spouse.bacanak"; // Bacanak (erkek-erkek)
                    } else if (GENDER_FEMALE.equals(person1GenderName) && GENDER_FEMALE.equals(person2GenderName)) {
                        key = "relationship.spouse_sibling_spouse.elti"; // Elti (kadın-kadın)
                    } else {
                        key = "relationship.spouse_sibling_spouse.bacanak"; // Karışık cinsiyet fallback
                    }
                } else {
                    key = "relationship.spouse_sibling_spouse";
                }
                
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale))
                        .messageKey(key)
                        .acceptableMessageKeys(List.of(key))
                        .status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1))
                        .person2(personMapper.toSummaryDTO(person2))
                        .build());
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
            // Artık basit format kullanıyoruz - parametreler görmezden geliniyor  
            return messageSource.getMessage(code, null, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message key not found in RelationshipDescriptionResolver: {} for locale {}", code, locale);
            return "[[DESC_MSG_NOT_FOUND: " + code + "]]";
        }
    }
} 