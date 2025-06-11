package by.backend.service.relationship;

import by.backend.config.RelationshipProperties;
import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.repository.PersonRepository;
import by.backend.repository.RelationshipRepository;
import by.backend.mapper.PersonMapper;
import by.backend.service.description.RelationshipDescriptionResolver;
import by.backend.service.pathfinding.RelationshipPathFinder;
import by.backend.service.validation.RelationshipValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RelationshipServiceImpl implements RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final PersonRepository personRepository;
    private final MessageSource messageSource;
    private final PersonMapper personMapper;
    private final RelationshipProperties relationshipProperties;
    private final RelationshipValidator relationshipValidator;
    private final RelationshipPathFinder relationshipPathFinder;
    private final RelationshipDescriptionResolver relationshipDescriptionResolver;
    private final RelationshipService self;
    
    public RelationshipServiceImpl(RelationshipRepository relationshipRepository,
                                 PersonRepository personRepository,
                                 MessageSource messageSource,
                                 PersonMapper personMapper,
                                 RelationshipProperties relationshipProperties,
                                 RelationshipValidator relationshipValidator,
                                 RelationshipPathFinder relationshipPathFinder,
                                 RelationshipDescriptionResolver relationshipDescriptionResolver,
                                 @Lazy RelationshipService self) {
        this.relationshipRepository = relationshipRepository;
        this.personRepository = personRepository;
        this.messageSource = messageSource;
        this.personMapper = personMapper;
        this.relationshipProperties = relationshipProperties;
        this.relationshipValidator = relationshipValidator;
        this.relationshipPathFinder = relationshipPathFinder;
        this.relationshipDescriptionResolver = relationshipDescriptionResolver;
        this.self = self;
    }
    
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationships", key = "'all'")
    public List<Relationship> getAllRelationships() {
        return relationshipRepository.findAllWithPersons();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"relationships", "activeRelationships", "relationshipStatus", "relationshipPaths"}, allEntries = true)
    public Relationship createRelationship(Person person1, Person person2, by.backend.model.enums.RelationshipType type) {
        Locale locale = LocaleContextHolder.getLocale();
        relationshipValidator.validateRelationship(person1, person2, type, locale);
        
        if (self.hasActiveRelationship(person1, person2, type)) {
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
    public CompletableFuture<List<PersonSummaryDTO>> findRelativesAsync(Person person, by.backend.model.enums.RelationshipType type) {
        return CompletableFuture.supplyAsync(() -> 
            personMapper.toSummaryDTOList(self.findRelatives(person, type))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Person> findRelatives(Person person, by.backend.model.enums.RelationshipType type) {
        List<Relationship> relationships = relationshipRepository.findByPerson1AndTypeAndIsActiveTrue(person, type);
        return relationships.stream()
                .map(Relationship::getPerson2)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationships", key = "#person1Summary.id() + '-' + #person2Summary.id()", condition = "#person1Summary != null && #person2Summary != null")
    public RelationshipDescriptionResult findRelationshipDescription(PersonSummaryDTO person1Summary, PersonSummaryDTO person2Summary) {
        Person person1 = personRepository.findById(person1Summary.id())
            .orElseThrow(() -> new IllegalArgumentException("Kişi 1 bulunamadı: " + person1Summary.id()));
        Person person2 = personRepository.findById(person2Summary.id())
            .orElseThrow(() -> new IllegalArgumentException("Kişi 2 bulunamadı: " + person2Summary.id()));
        
        Locale locale = LocaleContextHolder.getLocale();
        return relationshipDescriptionResolver.resolveDescription(person1, person2, locale);
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
    @Cacheable(value = "relationshipStatus", key = "#person1.id + '-' + #person2.id + '-' + #type", condition = "#person1 != null && #person2 != null")
    public boolean hasActiveRelationship(Person person1, Person person2, by.backend.model.enums.RelationshipType type) {
        return relationshipRepository.findActiveRelationship(person1, person2, type).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "relationshipPaths", key = "#person1.id + '-' + #person2.id", condition = "#person1 != null && #person2 != null")
    public List<RelationshipStepDTO> getRelationshipPath(Person person1, Person person2) {
        if (person1 == null || person2 == null) {
            log.warn("getRelationshipPath called with null parameters: person1={}, person2={}", person1, person2);
            return Collections.emptyList();
        }
        
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
        Set<Person> ancestors1 = self.getAllAncestors(person1);
        Set<Person> ancestors2 = self.getAllAncestors(person2);

        ancestors1.retainAll(ancestors2);
        return personMapper.toSummaryDTOList(new ArrayList<>(ancestors1));
    }

    @Override
    @Cacheable(value = "ancestors", key = "#person.id")
    public Set<Person> getAllAncestors(Person person) {
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.add(person);
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            List<Relationship> parentRelationships = relationshipRepository.findByPerson2AndTypeAndIsActiveTrue(current, by.backend.model.enums.RelationshipType.PARENT_CHILD);
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
        Set<Person> descendants1 = self.getAllDescendants(person1);
        Set<Person> descendants2 = self.getAllDescendants(person2);

        descendants1.retainAll(descendants2);
        return personMapper.toSummaryDTOList(new ArrayList<>(descendants1));
    }

    @Override
    @Cacheable(value = "descendants", key = "#person.id")
    public Set<Person> getAllDescendants(Person person) {
        Set<Person> descendants = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.add(person);
        Set<Long> visited = new HashSet<>();
        visited.add(person.getId());

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            List<Relationship> childRelationships = relationshipRepository.findByPerson1AndTypeAndIsActiveTrue(current, by.backend.model.enums.RelationshipType.PARENT_CHILD);
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

            processRelationships(currentPerson, currentDistance, degree, relatives, queue, visited);
        }
        return personMapper.toSummaryDTOList(new ArrayList<>(relatives));
    }
    
    private void processRelationships(Person currentPerson, int currentDistance, int degree, 
                                    Set<Person> relatives, Queue<PersonDistance> queue, Set<Long> visited) {
                    List<Relationship> relationships = self.findAllActiveRelationships(currentPerson);
        
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
        Set<Person> ancestors1 = self.getAllAncestors(person1);
        Set<Person> ancestors2 = self.getAllAncestors(person2);
        if (ancestors1.contains(person2) || ancestors2.contains(person1)) return true;
        Set<Person> commonAncestors = new HashSet<>(ancestors1);
        commonAncestors.retainAll(ancestors2);
        return !commonAncestors.isEmpty();
    }
    
    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException _) {
            log.warn("Message key not found in RelationshipService: {} for locale {}", code, locale);
            String fallback = code;
            if (args != null && args.length > 0) {
                fallback += " (" + Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            }
            return "[[" + fallback + "]]";
        }
    }
} 