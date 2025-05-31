package by.backend.service.relationship;

import by.backend.config.RelationshipProperties;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipStatus;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.PersonRepository;
import by.backend.repository.RelationshipRepository;
import by.backend.mapper.PersonMapper;
import by.backend.service.validation.RelationshipValidator;
import by.backend.service.pathfinding.RelationshipPathFinder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipServiceImpl implements RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final PersonRepository personRepository;
    private final MessageSource messageSource;
    private final PersonMapper personMapper;
    private final RelationshipProperties relationshipProperties;
    private final RelationshipValidator relationshipValidator;
    private final RelationshipPathFinder relationshipPathFinder;
    
    private static final String GENDER_MALE = "ERKEK";
    private static final String GENDER_FEMALE = "KADIN";
    private static final String GENDER_UNKNOWN = "BILINMEYEN";

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationships", key = "'all'")
    public List<Relationship> getAllRelationships() {
        return relationshipRepository.findAllWithPersons();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"relationships", "activeRelationships", "relationshipStatus", "relationshipPaths"}, allEntries = true)
    public Relationship createRelationship(Person person1, Person person2, RelationshipType type) {
        Locale locale = LocaleContextHolder.getLocale();
        relationshipValidator.validateRelationship(person1, person2, type, locale);
        
        if (hasActiveRelationship(person1, person2, type)) {
            throw new IllegalStateException(getMessage("relationship.error.already_exists", locale));
        }

        Relationship relationship = Relationship.builder()
            .person1(person1)
            .person2(person2)
            .type(type)
            .build();
        return relationshipRepository.save(relationship);
    }
    
    @Override
    @Transactional
    @CacheEvict(value = {"relationships", "activeRelationships", "relationshipStatus", "relationshipPaths"}, allEntries = true)
    public void endRelationship(Long relationshipId) {
        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("İlişki bulunamadı"));
        relationship.setEndDate(LocalDate.now());
        relationship.setActive(false);
        relationshipRepository.save(relationship);
    }

    @Override
    @Transactional(readOnly = true)
    @Async
    public CompletableFuture<List<PersonSummaryDTO>> findRelativesAsync(Person person, RelationshipType type) {
        return CompletableFuture.completedFuture(
            personMapper.toSummaryDTOList(findRelatives(person, type))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> findRelatives(Person person, RelationshipType type) {
        List<Relationship> relationships = relationshipRepository.findByPerson1AndTypeAndIsActiveTrue(person, type);
        return relationships.stream()
                .map(Relationship::getPerson2)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationships", key = "#person1Summary.id() + '-' + #person2Summary.id()")
    public RelationshipDescriptionResult findRelationshipDescription(PersonSummaryDTO person1Summary, PersonSummaryDTO person2Summary) {
        Person person1 = personRepository.findById(person1Summary.id())
            .orElseThrow(() -> new IllegalArgumentException("Kişi 1 bulunamadı: " + person1Summary.id()));
        Person person2 = personRepository.findById(person2Summary.id())
            .orElseThrow(() -> new IllegalArgumentException("Kişi 2 bulunamadı: " + person2Summary.id()));

        try {
            Locale locale = LocaleContextHolder.getLocale(); 

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
                String description = formatDirectRelationship(person1, person2, relationship.getType(), locale);
                String key = "relationship.direct." + relationship.getType().name().toLowerCase();
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(description)
                    .messageKey(key)
                    .acceptableMessageKeys(List.of(key))
                    .status(RelationshipStatus.FOUND)
                    .person1(personMapper.toSummaryDTO(person1))
                    .person2(personMapper.toSummaryDTO(person2))
                    .directTypeIfApplicable(relationship.getType())
                    .build();
            }

            List<Relationship> directRelationshipsP2P1 = relationshipRepository.findByPerson1AndPerson2AndIsActiveTrue(person2, person1);
            if (!directRelationshipsP2P1.isEmpty()) {
                Relationship relationship = directRelationshipsP2P1.get(0);
                String description = formatReverseRelationship(person1, person2, relationship.getType(), locale);
                String key = "relationship.reverse." + relationship.getType().name().toLowerCase();
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(description)
                    .messageKey(key)
                    .acceptableMessageKeys(List.of(key))
                    .status(RelationshipStatus.FOUND)
                    .person1(personMapper.toSummaryDTO(person1))
                    .person2(personMapper.toSummaryDTO(person2))
                    .directTypeIfApplicable(relationship.getType())
                    .build();
            }

            Optional<RelationshipDescriptionResult> specialRelationshipOpt = findSpecialRelationship(person1, person2, locale);
            if (specialRelationshipOpt.isPresent()) {
                return specialRelationshipOpt.get();
            }

            RelationshipDescriptionResult indirectRelationshipResult = findIndirectRelationship(person1, person2, locale);
            if (indirectRelationshipResult.getStatus() == RelationshipStatus.FOUND) {
                return indirectRelationshipResult;
            }

            boolean isBloodRelated = isBloodRelated(person1, person2);
            if (isBloodRelated) {
                String distantKey = "relationship.distant_blood_relative";
                return RelationshipDescriptionResult.builder()
                    .localizedDescription(getMessage(distantKey, locale, person1.getFirstName(), person2.getFirstName()))
                    .messageKey(distantKey)
                    .acceptableMessageKeys(List.of(distantKey))
                    .status(RelationshipStatus.FOUND)
                    .person1(personMapper.toSummaryDTO(person1))
                    .person2(personMapper.toSummaryDTO(person2))
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
            log.error("İlişki tarifi bulunurken bir hata oluştu ({}, {} için)", person1Summary.id(), person2Summary.id(), e);
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

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "activeRelationships", key = "#person.id")
    public List<Relationship> findAllActiveRelationships(Person person) {
        List<Relationship> relationshipsAsP1 = relationshipRepository.findByPerson1AndIsActiveTrue(person);
        List<Relationship> relationshipsAsP2 = relationshipRepository.findByPerson2AndIsActiveTrue(person);
        
        Set<Relationship> allRelationships = new HashSet<>(relationshipsAsP1);
        allRelationships.addAll(relationshipsAsP2);
        
        return new ArrayList<>(allRelationships);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationshipStatus", key = "#person1.id + '-' + #person2.id + '-' + #type")
    public boolean hasActiveRelationship(Person person1, Person person2, RelationshipType type) {
        return relationshipRepository.findActiveRelationship(person1, person2, type).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationshipPaths", key = "#person1.id + '-' + #person2.id")
    public List<RelationshipStepDTO> getRelationshipPath(Person person1, Person person2) {
        if (person1.getId().equals(person2.getId())) {
            return Collections.emptyList();
        }

        List<List<Relationship>> allPaths = relationshipPathFinder.findPaths(person1, person2, relationshipProperties.getDefaultPathDisplayMaxDepth());

        if (allPaths.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Relationship> shortestPath = allPaths.stream()
            .min(Comparator.comparingInt(List::size))
            .orElse(Collections.emptyList());

        Locale locale = LocaleContextHolder.getLocale();
        return relationshipPathFinder.convertPathToDTO(shortestPath, person1, person2, locale);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> findCommonAncestors(Person person1, Person person2) {
        Set<Person> ancestors1 = getAllAncestors(person1);
        Set<Person> ancestors2 = getAllAncestors(person2);

        ancestors1.retainAll(ancestors2);
        return personMapper.toSummaryDTOList(new ArrayList<>(ancestors1));
    }

    @Cacheable(value = "ancestors", key = "#person.id")
    private Set<Person> getAllAncestors(Person person) {
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.add(person);
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            List<Relationship> parentRelationships = relationshipRepository.findByPerson2AndTypeAndIsActiveTrue(current, RelationshipType.PARENT_CHILD);
            for (Relationship rel : parentRelationships) {
                Person parent = rel.getPerson1();
                if (!visited.contains(parent.getId())) {
                    ancestors.add(parent);
                    queue.add(parent);
                    visited.add(parent.getId());
                }
            }
        }
        return ancestors;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> findCommonDescendants(Person person1, Person person2) {
        Set<Person> descendants1 = getAllDescendants(person1);
        Set<Person> descendants2 = getAllDescendants(person2);

        descendants1.retainAll(descendants2);
        return personMapper.toSummaryDTOList(new ArrayList<>(descendants1));
    }

    @Cacheable(value = "descendants", key = "#person.id")
    private Set<Person> getAllDescendants(Person person) {
        Set<Person> descendants = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.add(person);
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            List<Relationship> childRelationships = relationshipRepository.findByPerson1AndTypeAndIsActiveTrue(current, RelationshipType.PARENT_CHILD);
            for (Relationship rel : childRelationships) {
                Person child = rel.getPerson2();
                if (!visited.contains(child.getId())) {
                    descendants.add(child);
                    queue.add(child);
                    visited.add(child.getId());
                }
            }
        }
        return descendants;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> findRelativesByDegree(Person person, int degree) {
        if (degree <= 0) return Collections.emptyList();

        Set<Person> relatives = new HashSet<>();
        Queue<PersonDistance> queue = new LinkedList<>();
        queue.add(new PersonDistance(person, 0));
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());

        while(!queue.isEmpty()) {
            PersonDistance currentPD = queue.poll();
            Person currentPerson = currentPD.getPerson();
            int currentDistance = currentPD.getDistance();

            if (currentDistance >= degree) continue;

            List<Relationship> relationships = findAllActiveRelationships(currentPerson);

            for (Relationship rel : relationships) {
                Person relatedPerson = rel.getPerson1().getId().equals(currentPerson.getId()) ? rel.getPerson2() : rel.getPerson1();
                if (!visited.contains(relatedPerson.getId())) {
                    visited.add(relatedPerson.getId());
                    relatives.add(relatedPerson);
                    if (currentDistance + 1 < degree) {
                        queue.add(new PersonDistance(relatedPerson, currentDistance + 1));
                    }
                }
            }
        }
        return personMapper.toSummaryDTOList(new ArrayList<>(relatives));
    }
    
    private static class PersonDistance {
        private final Person person;
        private final int distance;
        public PersonDistance(Person person, int distance) { this.person = person; this.distance = distance; }
        public Person getPerson() { return person; }
        public int getDistance() { return distance; }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBloodRelated(Person person1, Person person2) {
        if (person1.getId().equals(person2.getId())) return true;

        Set<Person> ancestors1 = getAllAncestors(person1);
        Set<Person> ancestors2 = getAllAncestors(person2);

        if (ancestors1.contains(person2) || ancestors2.contains(person1)) return true;

        Set<Person> commonAncestors = new HashSet<>(ancestors1);
        commonAncestors.retainAll(ancestors2);

        return !commonAncestors.isEmpty();
    }
    
    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException e) {
            log.warn("Message key not found: {} for locale {}", code, locale);
            String fallback = code;
            if (args != null && args.length > 0) {
                fallback += " (" + Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            }
            return "[[" + fallback + "]]";
        }
    }

    private String formatDirectRelationship(Person person1, Person person2, RelationshipType type, Locale locale) {
        return relationshipPathFinder.formatDirectRelationship(person1, person2, type, locale);
    }

    private String formatReverseRelationship(Person person1, Person person2, RelationshipType type, Locale locale) {
        return relationshipPathFinder.formatReverseRelationship(person1, person2, type, locale);
    }

    private RelationshipDescriptionResult findIndirectRelationship(Person person1, Person person2, Locale locale) {
        List<RelationshipStepDTO> path = getRelationshipPath(person1, person2);
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
            .localizedDescription(getMessage("relationship.not_found", locale))
            .messageKey("relationship.not_found")
            .status(RelationshipStatus.NOT_FOUND)
            .person1(personMapper.toSummaryDTO(person1))
            .person2(personMapper.toSummaryDTO(person2))
            .build();
    }

    /**
     * Özel ilişki türlerini belirler (örn. büyükanne/büyükbaba, amca/dayı, teyze/hala, kuzen vs.)
     */
    private Optional<RelationshipDescriptionResult> findSpecialRelationship(Person person1, Person person2, Locale locale) {
        List<Person> person1Parents = getParents(person1);
        List<Person> person1Children = getChildren(person1);
        Person person1Spouse = getSpouse(person1);

        return tryResolveGrandparentRelationship(person1, person2, person1Parents, locale)
                .or(() -> tryResolveGrandchildRelationship(person1, person2, person1Children, locale))
                .or(() -> tryResolveAuntUncleRelationship(person1, person2, person1Parents, locale))
                .or(() -> {
                    List<Person> person1Siblings = getSiblings(person1);
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

    private Optional<RelationshipDescriptionResult> tryResolveGrandparentRelationship(Person person1, Person person2, List<Person> person1Parents, Locale locale) {
        for (Person parent : person1Parents) {
            List<Person> grandparents = getParents(parent);
            for (Person grandparent : grandparents) {
                if (grandparent.getId().equals(person2.getId())) {
                    String key = "relationship.grandparent";
                    if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) {
                            key = "relationship.grandfather";
                        } else if (GENDER_FEMALE.equals(person2GenderName)) {
                            key = "relationship.grandmother";
                        }
                    }
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                            .messageKey(key)
                            .acceptableMessageKeys(List.of(key, "relationship.grandparent"))
                            .status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1))
                            .person2(personMapper.toSummaryDTO(person2))
                            .build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveGrandchildRelationship(Person person1, Person person2, List<Person> person1Children, Locale locale) {
        for (Person child : person1Children) {
            List<Person> grandchildren = getChildren(child);
            for (Person grandchild : grandchildren) {
                if (grandchild.getId().equals(person2.getId())) {
                    String key = "relationship.grandchild";
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person1.getFirstName(), person2.getFirstName()))
                            .messageKey(key)
                            .acceptableMessageKeys(List.of(key))
                            .status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1))
                            .person2(personMapper.toSummaryDTO(person2))
                            .build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveAuntUncleRelationship(Person person1, Person person2, List<Person> person1Parents, Locale locale) {
        for (Person parent : person1Parents) {
            List<Person> parentSiblings = getSiblings(parent);
            for (Person parentSibling : parentSiblings) {
                if (parentSibling.getId().equals(person2.getId())) {
                    String key = "relationship.aunt_uncle";
                    boolean isMaternal = false;
                    if (parent.getGender() != null && GENDER_FEMALE.equals(parent.getGender().name().toUpperCase())) {
                        isMaternal = true; 
                    }
                    
                    if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) {
                            key = isMaternal ? "relationship.maternal_uncle" : "relationship.paternal_uncle";
                        } else if (GENDER_FEMALE.equals(person2GenderName)) {
                            key = isMaternal ? "relationship.maternal_aunt" : "relationship.paternal_aunt";
                        }
                    }
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
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
            List<Person> nephewsNieces = getChildren(sibling);
            for (Person nephewNiece : nephewsNieces) {
                if (nephewNiece.getId().equals(person2.getId())) {
                    String key = "relationship.nephew_niece";
                    if (person2.getGender() != null) {
                        String person2GenderName = person2.getGender().name().toUpperCase();
                        if (GENDER_MALE.equals(person2GenderName)) {
                            key = "relationship.nephew";
                        } else if (GENDER_FEMALE.equals(person2GenderName)) {
                            key = "relationship.niece";
                        }
                    }
                    return Optional.of(RelationshipDescriptionResult.builder()
                            .localizedDescription(getMessage(key, locale, person1.getFirstName(), person2.getFirstName()))
                            .messageKey(key)
                            .acceptableMessageKeys(List.of(key, "relationship.nephew_niece"))
                            .status(RelationshipStatus.FOUND)
                            .person1(personMapper.toSummaryDTO(person1))
                            .person2(personMapper.toSummaryDTO(person2))
                            .build());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveCousinRelationship(Person person1, Person person2, List<Person> person1Parents, Locale locale) {
        for (Person parent : person1Parents) {
            List<Person> parentSiblings = getSiblings(parent);
            for (Person parentSibling : parentSiblings) {
                List<Person> cousins = getChildren(parentSibling);
                for (Person cousin : cousins) {
                    if (cousin.getId().equals(person2.getId())) {
                        return Optional.of(RelationshipDescriptionResult.builder()
                                .localizedDescription(getMessage("relationship.cousin", locale, person1.getFirstName(), person2.getFirstName()))
                                .messageKey("relationship.cousin")
                                .acceptableMessageKeys(List.of("relationship.cousin"))
                                .status(RelationshipStatus.FOUND)
                                .person1(personMapper.toSummaryDTO(person1))
                                .person2(personMapper.toSummaryDTO(person2))
                                .build());
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveInLawParentRelationship(Person person1, Person person2, Person person1Spouse, Locale locale) {
        List<Person> spouseParents = getParents(person1Spouse);
        for (Person spouseParent : spouseParents) {
            if (spouseParent.getId().equals(person2.getId())) {
                String key = "relationship.inlaw.parent";
                if (person2.getGender() != null) {
                    String person2GenderName = person2.getGender().name().toUpperCase();
                    if (GENDER_MALE.equals(person2GenderName)) {
                        key = "relationship.inlaw.father";
                    } else if (GENDER_FEMALE.equals(person2GenderName)) {
                        key = "relationship.inlaw.mother";
                    }
                }
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                        .messageKey(key)
                        .acceptableMessageKeys(List.of(key, "relationship.inlaw.parent"))
                        .status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1))
                        .person2(personMapper.toSummaryDTO(person2))
                        .build());
            }
        }
        return Optional.empty();
    }

    private Optional<RelationshipDescriptionResult> tryResolveInLawSiblingRelationship(Person person1, Person person2, Person person1Spouse, Locale locale) {
        List<Person> spouseSiblings = getSiblings(person1Spouse);
        for (Person spouseSibling : spouseSiblings) {
            if (spouseSibling.getId().equals(person2.getId())) {
                String key = "relationship.inlaw.sibling";
                if (person2.getGender() != null && person1.getGender() != null) { // person1.getGender() was not checked before, but spouse.getGender() was. Assuming person1Spouse.getGender()
                     String person2GenderName = person2.getGender().name().toUpperCase();
                     String spouseGenderName = person1Spouse.getGender() != null ? person1Spouse.getGender().name().toUpperCase() : GENDER_UNKNOWN;

                    if (GENDER_MALE.equals(person2GenderName)) {
                        key = "relationship.inlaw.brother";
                    } else if (GENDER_FEMALE.equals(person2GenderName)) {
                        if (GENDER_MALE.equals(spouseGenderName)) {
                            key = "relationship.inlaw.sister_of_husband"; // Görümce
                        } else { // spouse is female or unknown
                            key = "relationship.inlaw.sister_of_wife"; // Baldız
                        }
                    }
                }
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale, person2.getFirstName(), person1.getFirstName()))
                        .messageKey(key)
                        .acceptableMessageKeys(List.of(key, "relationship.inlaw.sibling"))
                        .status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1))
                        .person2(personMapper.toSummaryDTO(person2))
                        .build());
            }
        }
        return Optional.empty();
    }
    
    private Optional<RelationshipDescriptionResult> tryResolveInLawChildSpouseRelationship(Person person1, Person person2, List<Person> person1Children, Locale locale) {
        for (Person child : person1Children) {
            Person childSpouse = getSpouse(child);
            if (childSpouse != null && childSpouse.getId().equals(person2.getId())) {
                String key = "relationship.inlaw.child";
                if (person2.getGender() != null) {
                    String person2GenderName = person2.getGender().name().toUpperCase();
                    if (GENDER_MALE.equals(person2GenderName)) {
                        key = "relationship.inlaw.son";
                    } else if (GENDER_FEMALE.equals(person2GenderName)) {
                        key = "relationship.inlaw.daughter";
                    }
                }
                return Optional.of(RelationshipDescriptionResult.builder()
                        .localizedDescription(getMessage(key, locale, person1.getFirstName(), person2.getFirstName()))
                        .messageKey(key)
                        .acceptableMessageKeys(List.of(key, "relationship.inlaw.child"))
                        .status(RelationshipStatus.FOUND)
                        .person1(personMapper.toSummaryDTO(person1))
                        .person2(personMapper.toSummaryDTO(person2))
                        .build());
            }
        }
        return Optional.empty();
    }
    
    private List<Person> getParents(Person person) {
        return relationshipRepository.findByPerson2AndTypeAndIsActiveTrue(person, RelationshipType.PARENT_CHILD)
                .stream()
                .map(Relationship::getPerson1)
                .collect(Collectors.toList());
    }

    private List<Person> getChildren(Person person) {
        return relationshipRepository.findByPerson1AndTypeAndIsActiveTrue(person, RelationshipType.PARENT_CHILD)
                .stream()
                .map(Relationship::getPerson2)
                .collect(Collectors.toList());
    }

    private List<Person> getSiblings(Person person) {
        return relationshipRepository.findActiveRelationships(person, RelationshipType.SIBLING)
                .stream()
                .map(rel -> rel.getPerson1().getId().equals(person.getId()) ? rel.getPerson2() : rel.getPerson1())
                .collect(Collectors.toList());
    }

    private Person getSpouse(Person person) {
        return relationshipRepository.findActiveRelationships(person, RelationshipType.SPOUSE)
                .stream()
                .map(rel -> rel.getPerson1().getId().equals(person.getId()) ? rel.getPerson2() : rel.getPerson1())
                .findFirst()
                .orElse(null);
    }
} 