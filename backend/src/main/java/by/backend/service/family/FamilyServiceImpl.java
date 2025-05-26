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
import java.util.stream.Collectors;

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
        List<Map<String, Object>> relationshipsResultList = new ArrayList<>(); // Renamed to avoid conflict
        List<Map<String, Object>> peopleResultList = new ArrayList<>(); // Renamed to avoid conflict

        // Ortak atayı bul
        Person commonAncestor = findCommonAncestor(person1, person2);
        if (commonAncestor != null) {
            Map<String, Object> ancestorMap = new HashMap<>();
            ancestorMap.put("id", commonAncestor.getId());
            ancestorMap.put("firstName", commonAncestor.getFirstName());
            ancestorMap.put("lastName", commonAncestor.getLastName());
            ancestorMap.put("gender", commonAncestor.getGender() != null ? commonAncestor.getGender().name() : "BİLİNMEYEN");
            result.put("commonAncestor", ancestorMap);
        }

        // İlişkili kişileri ekle
        Set<Person> relatedPeople = new HashSet<>();
        relatedPeople.add(person1);
        relatedPeople.add(person2);
        if (commonAncestor != null) {
            relatedPeople.add(commonAncestor);
        }

        // Ara kişileri bul ve ekle
        if (commonAncestor != null) { // Only find paths if common ancestor exists
            List<Person> pathToPerson1 = findPathToAncestor(person1, commonAncestor);
            List<Person> pathToPerson2 = findPathToAncestor(person2, commonAncestor);
            relatedPeople.addAll(pathToPerson1);
            relatedPeople.addAll(pathToPerson2);
        }

        // Kişileri listeye ekle
        for (Person person : relatedPeople) {
            Map<String, Object> personMap = new HashMap<>();
            personMap.put("id", person.getId());
            personMap.put("firstName", person.getFirstName());
            personMap.put("lastName", person.getLastName());
            personMap.put("gender", person.getGender() != null ? person.getGender().name() : "BİLİNMEYEN");

            // Ebeveynleri bul
            List<Person> parents = relationshipRepository
                .findByPerson2AndTypeAndIsActiveTrue(person, RelationshipType.PARENT_CHILD)
                .stream()
                .map(Relationship::getPerson1)
                .collect(Collectors.toList());

            parents.forEach(parent -> {
                if (parent.getGender() != null) { // Null check for gender
                    if (parent.getGender().name().equals("ERKEK")) {
                        personMap.put("fatherId", parent.getId());
                    } else {
                        personMap.put("motherId", parent.getId());
                    }
                }
            });

            // Eşi bul
            relationshipRepository
                .findActiveRelationship(person, person, RelationshipType.SPOUSE) // findActiveRelationship metodu RelationshipRepository'de tanımlı mı?
                .ifPresent(rel -> {
                    Person spouse = rel.getPerson1().getId().equals(person.getId()) ? 
                        rel.getPerson2() : rel.getPerson1();
                    personMap.put("spouseId", spouse.getId());
                });

            peopleResultList.add(personMap);

            // İlişkileri ekle (sadece bu kişiyle ilgili olanları ve diğer relatedPeople içindekilerle olanları)
            List<Relationship> personRelationships = relationshipRepository
                .findByPerson1OrPerson2AndIsActiveTrue(person, person);
            
            for (Relationship rel : personRelationships) {
                 // Ensure both parties of the relationship are in our relatedPeople set to avoid too much data
                if (relatedPeople.contains(rel.getPerson1()) && relatedPeople.contains(rel.getPerson2())) {
                    Map<String, Object> relationshipMap = new HashMap<>();
                    relationshipMap.put("person1Id", rel.getPerson1().getId());
                    relationshipMap.put("person2Id", rel.getPerson2().getId());
                    relationshipMap.put("type", rel.getType());
                    relationshipMap.put("startDate", rel.getStartDate());
                    if (rel.getEndDate() != null) {
                        relationshipMap.put("endDate", rel.getEndDate());
                    }
                    relationshipsResultList.add(relationshipMap);
                }
            }
        }

        result.put("people", peopleResultList.stream().distinct().collect(Collectors.toList())); // Remove duplicates just in case
        result.put("relationships", relationshipsResultList.stream().distinct().collect(Collectors.toList())); // Remove duplicates
        return result;
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
        if (ancestor == null || person.equals(ancestor)) return Collections.emptyList();

        List<Person> path = new ArrayList<>();
        Queue<List<Person>> queue = new LinkedList<>();
        Set<Person> visitedInPathSearch = new HashSet<>();

        List<Person> initialPath = new ArrayList<>();
        initialPath.add(person);
        queue.offer(initialPath);
        visitedInPathSearch.add(person);

        while (!queue.isEmpty()) {
            List<Person> currentPath = queue.poll();
            Person lastPersonInPath = currentPath.get(currentPath.size() - 1);

            if (lastPersonInPath.equals(ancestor)) {
                path.addAll(currentPath);
                path.remove(path.size()-1); // Remove ancestor itself from path to person
                Collections.reverse(path); // Path from ancestor to person (excluding ancestor)
                return path.subList(0, path.size() > 0 ? path.size()-1:0); // Return path from person to ancestor (excluding person and ancestor)
                                                                       // Let's re-think what this path should represent. 
                                                                       // If it's nodes between person and ancestor, this logic needs care.
                                                                       // For now, it returns nodes from person up to (but not including) ancestor.
            }
            
            // Find parents of the last person in the path
            relationshipRepository
                .findByPerson2AndTypeAndIsActiveTrue(lastPersonInPath, RelationshipType.PARENT_CHILD)
                .stream()
                .map(Relationship::getPerson1) // These are the parents
                .filter(parent -> !visitedInPathSearch.contains(parent))
                .forEach(parent -> {
                    visitedInPathSearch.add(parent);
                    List<Person> newPath = new ArrayList<>(currentPath);
                    newPath.add(parent);
                    queue.offer(newPath);
                });
        }
        // If used for visualization, path should be from person to ancestor. Current logic seems to build it that way then reverses.
        // For relatedPeople set, order doesn't matter. Current logic adds person to ancestor, which is fine.
        // The method is `findPathToAncestor`, so it should be person -> parent -> ... -> ancestor. 
        // The current path construction adds person, then their parents, etc. So the list is in order from person upwards.
        // If lastPersonInPath is ancestor, currentPath is [person, p1, p2, ..., ancestor]. We need [p1, p2, ...]
        // If `path.remove(path.size()-1)` removes ancestor, and `path.remove(0)` removes person, then reverse. This is confusing.

        // Simpler BFS path logic returning [person, parent_of_person, ..., grandparent_of_ancestor, ancestor]
        // Then caller can decide what to do with it. Let's stick to finding *a* path.
        // The current logic for `relatedPeople.addAll(pathToPerson1);` is fine if it adds intermediate nodes.

        // Returning the currentPath (excluding the starting person, up to ancestor) if found in loop
        // The BFS will find shortest path in terms of generations. The current BFS returns the path from person UP TO ancestor.
        // Let's refine the return: a list of people on the path from `person` to `ancestor`, excluding `person` and `ancestor` themselves.
        // If Person P, path A, B, C, Ancestor A. Path should be A, B, C. 
        // The BFS queue stores paths starting with `person`. If a path reaches `ancestor`, like [person, node1, node2, ancestor], 
        // we should return [node1, node2].
        // The original path.add(current) was for a DFS like approach. BFS is better for shortest path.

        // Re-simplifying findPathToAncestor to return just the intermediate nodes.
        // This BFS finds one path. We need to reconstruct it. 
        // A common way is to store parent pointers during BFS from ancestor down to person, or person up to ancestor.
        Map<Person, Person> parentMap = new HashMap<>(); // Child -> Parent map for path reconstruction
        Queue<Person> bfsQueue = new LinkedList<>();
        Set<Person> visitedForBfs = new HashSet<>();

        bfsQueue.offer(person);
        visitedForBfs.add(person);

        Person foundAncestorInBfs = null;

        while(!bfsQueue.isEmpty()){
            Person current = bfsQueue.poll();
            if(current.equals(ancestor)){
                foundAncestorInBfs = current;
                break;
            }
            List<Person> parents = relationshipRepository
                .findByPerson2AndTypeAndIsActiveTrue(current, RelationshipType.PARENT_CHILD)
                .stream().map(Relationship::getPerson1).collect(Collectors.toList());
            for(Person p : parents){
                if(!visitedForBfs.contains(p)){
                    visitedForBfs.add(p);
                    parentMap.put(current, p); // current's parent is p
                    bfsQueue.offer(p);
                }
            }
        }

        List<Person> resultPath = new ArrayList<>();
        if(foundAncestorInBfs != null){
            Person curr = person;
            while(parentMap.containsKey(curr)){
                 Person parentOfCurr = parentMap.get(curr);
                 if(parentOfCurr.equals(ancestor)) break; // Stop before adding ancestor
                 resultPath.add(parentOfCurr);
                 curr = parentOfCurr;
            }            
        }
        // resultPath is now from parent_of_person up to child_of_ancestor. This is what we want for intermediate nodes.
        return resultPath; 
    }
} 