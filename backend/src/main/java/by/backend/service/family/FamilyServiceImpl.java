package by.backend.service.family;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.PersonRepository;
import by.backend.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {
    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findRelationshipBetween(Long person1Id, Long person2Id) {
        Person person1 = personRepository.findById(person1Id)
            .orElseThrow(() -> new RuntimeException("Kişi bulunamadı: ID " + person1Id));
        Person person2 = personRepository.findById(person2Id)
            .orElseThrow(() -> new RuntimeException("Kişi bulunamadı: ID " + person2Id));

        Map<String, Object> result = new HashMap<>();
        Person commonAncestor = findCommonAncestor(person1, person2);
        
        addCommonAncestorToResult(result, commonAncestor);
        Set<Person> relatedPeople = buildRelatedPeopleSet(person1, person2, commonAncestor);
        
        List<Map<String, Object>> peopleResultList = buildPeopleResultList(relatedPeople);
        List<Map<String, Object>> relationshipsResultList = buildRelationshipsResultList(relatedPeople);
        
        result.put("people", peopleResultList.stream().distinct().toList());
        result.put("relationships", relationshipsResultList.stream().distinct().toList());
        
        return result;
    }

    private void addCommonAncestorToResult(Map<String, Object> result, Person commonAncestor) {
        if (commonAncestor == null) return;
        
        Map<String, Object> ancestorMap = createPersonMap(commonAncestor);
        result.put("commonAncestor", ancestorMap);
    }

    private Set<Person> buildRelatedPeopleSet(Person person1, Person person2, Person commonAncestor) {
        Set<Person> relatedPeople = new HashSet<>();
        relatedPeople.add(person1);
        relatedPeople.add(person2);
        
        if (commonAncestor != null) {
            relatedPeople.add(commonAncestor);
            List<Person> pathToPerson1 = findPathToAncestor(person1, commonAncestor);
            List<Person> pathToPerson2 = findPathToAncestor(person2, commonAncestor);
            relatedPeople.addAll(pathToPerson1);
            relatedPeople.addAll(pathToPerson2);
        }
        
        return relatedPeople;
    }

    private List<Map<String, Object>> buildPeopleResultList(Set<Person> relatedPeople) {
        List<Map<String, Object>> peopleResultList = new ArrayList<>();
        
        for (Person person : relatedPeople) {
            Map<String, Object> personMap = createPersonMap(person);
            addParentInfo(personMap, person);
            addSpouseInfo(personMap, person);
            peopleResultList.add(personMap);
        }
        
        return peopleResultList;
    }

    private Map<String, Object> createPersonMap(Person person) {
        Map<String, Object> personMap = new HashMap<>();
        personMap.put("id", person.getId());
        personMap.put("firstName", person.getFirstName());
        personMap.put("lastName", person.getLastName());
        personMap.put("gender", person.getGender() != null ? person.getGender().name() : "BİLİNMEYEN");
        return personMap;
    }

    private void addParentInfo(Map<String, Object> personMap, Person person) {
        List<Person> parents = relationshipRepository
            .findByPerson2AndTypeAndIsActiveTrue(person, RelationshipType.PARENT_CHILD)
            .stream()
            .map(Relationship::getPerson1)
            .toList();

        parents.forEach(parent -> {
            if (parent.getGender() != null) {
                if (parent.getGender().name().equals("ERKEK")) {
                    personMap.put("fatherId", parent.getId());
                } else {
                    personMap.put("motherId", parent.getId());
                }
            }
        });
    }

    private void addSpouseInfo(Map<String, Object> personMap, Person person) {
        relationshipRepository
            .findActiveRelationship(person, person, RelationshipType.SPOUSE)
            .ifPresent(rel -> {
                Person spouse = rel.getPerson1().getId().equals(person.getId()) ? 
                    rel.getPerson2() : rel.getPerson1();
                personMap.put("spouseId", spouse.getId());
            });
    }

    private List<Map<String, Object>> buildRelationshipsResultList(Set<Person> relatedPeople) {
        List<Map<String, Object>> relationshipsResultList = new ArrayList<>();
        
        for (Person person : relatedPeople) {
            List<Relationship> personRelationships = relationshipRepository
                .findByPerson1OrPerson2AndIsActiveTrue(person, person);
            
            addValidRelationships(relationshipsResultList, personRelationships, relatedPeople);
        }
        
        return relationshipsResultList;
    }

    private void addValidRelationships(List<Map<String, Object>> relationshipsResultList, 
                                     List<Relationship> personRelationships, 
                                     Set<Person> relatedPeople) {
        for (Relationship rel : personRelationships) {
            if (relatedPeople.contains(rel.getPerson1()) && relatedPeople.contains(rel.getPerson2())) {
                Map<String, Object> relationshipMap = createRelationshipMap(rel);
                relationshipsResultList.add(relationshipMap);
            }
        }
    }

    private Map<String, Object> createRelationshipMap(Relationship rel) {
        Map<String, Object> relationshipMap = new HashMap<>();
        relationshipMap.put("person1Id", rel.getPerson1().getId());
        relationshipMap.put("person2Id", rel.getPerson2().getId());
        relationshipMap.put("type", rel.getType());
        relationshipMap.put("startDate", rel.getStartDate());
        if (rel.getEndDate() != null) {
            relationshipMap.put("endDate", rel.getEndDate());
        }
        return relationshipMap;
    }

    private Person findCommonAncestor(Person person1, Person person2) {
        Set<Person> ancestorsP1 = findAncestors(person1);
        Set<Person> ancestorsP2 = findAncestors(person2);
        
        ancestorsP1.retainAll(ancestorsP2);
        if (ancestorsP1.isEmpty()) return null;

        // Find the "lowest" common ancestor (most recent)
        // This can be complex. For now, returning any common ancestor is fine.
        // A simple heuristic: the one with the latest birth date among common ancestors, or just the first one.
        return ancestorsP1.stream().max(Comparator.comparing(Person::getBirthDate, Comparator.nullsFirst(LocalDate::compareTo))).orElse(null);
    }

    private Set<Person> findAncestors(Person person) {
        Set<Person> ancestors = new HashSet<>();
        Queue<Person> queue = new LinkedList<>();
        queue.offer(person);
        Set<Long> visitedIds = new HashSet<>();

        while (!queue.isEmpty()) {
            Person current = queue.poll();
            if (visitedIds.contains(current.getId())) {
                continue;
            }
            visitedIds.add(current.getId());
            if (!current.equals(person)) { // Don't add the person themselves to their ancestor list
                ancestors.add(current);
            }

            relationshipRepository
                .findByPerson2AndTypeAndIsActiveTrue(current, RelationshipType.PARENT_CHILD)
                .stream()
                .map(Relationship::getPerson1)
                .forEach(queue::offer);
        }
        return ancestors;
    }

    private List<Person> findPathToAncestor(Person person, Person ancestor) {
        if (ancestor == null || person.equals(ancestor)) {
            return Collections.emptyList();
        }
        
        Map<Person, Person> parentMap = buildParentMapThroughBFS(person, ancestor);
        return reconstructPath(person, ancestor, parentMap);
    }
    
    private Map<Person, Person> buildParentMapThroughBFS(Person startPerson, Person targetAncestor) {
        Map<Person, Person> parentMap = new HashMap<>();
        Queue<Person> queue = new LinkedList<>();
        Set<Person> visited = new HashSet<>();
        
        queue.offer(startPerson);
        visited.add(startPerson);
        
        while (!queue.isEmpty()) {
            Person current = queue.poll();
            
            if (current.equals(targetAncestor)) {
                break;
            }
            
            processParentsForBFS(current, parentMap, queue, visited);
        }
        
        return parentMap;
    }
    
    private void processParentsForBFS(Person current, Map<Person, Person> parentMap, 
                                     Queue<Person> queue, Set<Person> visited) {
        List<Person> parents = relationshipRepository
            .findByPerson2AndTypeAndIsActiveTrue(current, RelationshipType.PARENT_CHILD)
            .stream()
            .map(Relationship::getPerson1)
            .toList();
            
        for (Person parent : parents) {
            if (!visited.contains(parent)) {
                visited.add(parent);
                parentMap.put(current, parent);
                queue.offer(parent);
            }
        }
    }
    
    private List<Person> reconstructPath(Person startPerson, Person ancestor, Map<Person, Person> parentMap) {
        List<Person> resultPath = new ArrayList<>();
        Person current = startPerson;
        
        while (parentMap.containsKey(current)) {
            Person parentOfCurrent = parentMap.get(current);
            if (parentOfCurrent.equals(ancestor)) {
                break;
            }
            resultPath.add(parentOfCurrent);
            current = parentOfCurrent;
        }
        
        return resultPath;
    }
} 