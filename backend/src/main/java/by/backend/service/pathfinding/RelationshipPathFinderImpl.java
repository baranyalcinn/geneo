package by.backend.service.pathfinding;

import by.backend.config.RelationshipProperties;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import by.backend.repository.RelationshipRepository;
import by.backend.service.FamilyGraphService;
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
public class RelationshipPathFinderImpl implements RelationshipPathFinder {

    private final RelationshipRepository relationshipRepository;
    private final MessageSource messageSource;
    private final RelationshipProperties relationshipProperties;
    private final FamilyGraphService familyGraphService;

    private static final String GENDER_MALE = "ERKEK";
    private static final String GENDER_FEMALE = "KADIN";
    private static final String GENDER_UNKNOWN = "BILINMEYEN";

    private record PathExpansionState(List<Relationship> relationsInPath, Person currentEndPerson, Set<Long> personsInThisPathIds) {}

    @Override
    public List<List<Relationship>> findPaths(Person startPerson, Person endPerson, int maxDepth) {
        List<List<Relationship>> allPathsFound = new ArrayList<>();
        Queue<PathExpansionState> queue = new LinkedList<>();

        Set<Long> initialPersonsInPath = new HashSet<>();
        initialPersonsInPath.add(startPerson.getId());
        queue.add(new PathExpansionState(new ArrayList<>(), startPerson, initialPersonsInPath));

        while (!queue.isEmpty()) {
            PathExpansionState currentState = queue.poll();
            
            if (processPathState(currentState, endPerson, maxDepth, allPathsFound, queue)) {
                break; // Single break for termination condition
            }
        }
        return allPathsFound;
    }
    
    private boolean processPathState(PathExpansionState currentState, Person endPerson, int maxDepth, 
                                   List<List<Relationship>> allPathsFound, Queue<PathExpansionState> queue) {
        List<Relationship> currentRelations = currentState.relationsInPath();
        Person currentLastPerson = currentState.currentEndPerson();

        if (currentLastPerson.getId().equals(endPerson.getId())) {
            allPathsFound.add(new ArrayList<>(currentRelations));
            if (allPathsFound.size() >= relationshipProperties.getMaxBfsPathsToCollect()) {
                log.debug("PathFinder: {} ile {} arasında {} farklı yol bulundu (limit {}), arama sonlandırıldı.",
                        endPerson.getFirstName(), endPerson.getFirstName(), allPathsFound.size(), relationshipProperties.getMaxBfsPathsToCollect());
                return true; // Signal to terminate main loop
            }
            return false; // Continue processing
        }

        if (currentRelations.size() >= maxDepth) {
            return false; // Skip this path
        }

        expandPathState(currentState, queue);
        return false; // Continue processing
    }
    
    private void expandPathState(PathExpansionState currentState, Queue<PathExpansionState> queue) {
        Person currentLastPerson = currentState.currentEndPerson();
        List<Relationship> currentRelations = currentState.relationsInPath();
        Set<Long> personsCurrentlyInThisPath = currentState.personsInThisPathIds();
        
        Set<FamilyGraphService.PersonEdge> neighborEdges = familyGraphService.getNeighbors(currentLastPerson.getId());
        List<Relationship> relationshipsToExplore = neighborEdges.stream()
                .map(FamilyGraphService.PersonEdge::relationship)
                .collect(Collectors.toList());

        relationshipsToExplore.sort((r1, r2) -> {
            boolean r1IsDirect = r1.getType() == RelationshipType.PARENT_CHILD ||
                                 r1.getType() == RelationshipType.SPOUSE ||
                                 r1.getType() == RelationshipType.SIBLING;
            boolean r2IsDirect = r2.getType() == RelationshipType.PARENT_CHILD ||
                                 r2.getType() == RelationshipType.SPOUSE ||
                                 r2.getType() == RelationshipType.SIBLING;
            if (r1IsDirect && !r2IsDirect) return -1;
            if (!r1IsDirect && r2IsDirect) return 1;
            return 0;
        });

        for (Relationship relationship : relationshipsToExplore) {
            Person neighbor = relationship.getPerson1().getId().equals(currentLastPerson.getId()) ?
                relationship.getPerson2() : relationship.getPerson1();

            if (!personsCurrentlyInThisPath.contains(neighbor.getId())) {
                List<Relationship> newRelationsForPath = new ArrayList<>(currentRelations);
                newRelationsForPath.add(relationship);
                Set<Long> newPersonsInThisPath = new HashSet<>(personsCurrentlyInThisPath);
                newPersonsInThisPath.add(neighbor.getId());
                queue.add(new PathExpansionState(newRelationsForPath, neighbor, newPersonsInThisPath));
            }
        }
    }

    @Override
    public List<Relationship> findDirectedPath(Person startPerson, Person endPerson, int maxDepth) {
        List<List<Relationship>> allPathsFound = new ArrayList<>();
        Queue<PathExpansionState> queue = new LinkedList<>();

        Set<Long> initialPersonsInPath = new HashSet<>();
        initialPersonsInPath.add(startPerson.getId());
        queue.add(new PathExpansionState(new ArrayList<>(), startPerson, initialPersonsInPath));

        while (!queue.isEmpty()) {
            PathExpansionState currentState = queue.poll();
            
            if (processDirectedPathState(currentState, endPerson, maxDepth, allPathsFound, queue)) {
                break; 
            }
        }
        
        return allPathsFound.stream()
            .min(Comparator.comparingInt(List::size))
            .orElse(Collections.emptyList());
    }

    private boolean processDirectedPathState(PathExpansionState currentState, Person endPerson, int maxDepth, 
                                           List<List<Relationship>> allPathsFound, Queue<PathExpansionState> queue) {
        List<Relationship> currentRelations = currentState.relationsInPath();
        Person currentLastPerson = currentState.currentEndPerson();

        if (currentLastPerson.getId().equals(endPerson.getId())) {
            allPathsFound.add(new ArrayList<>(currentRelations));
            // For directed path, we usually want the first and shortest, so we can stop.
            return true; 
        }

        if (currentRelations.size() >= maxDepth) {
            return false; // Skip this path
        }

        expandDirectedPathState(currentState, queue);
        return false; // Continue processing
    }
    
    private void expandDirectedPathState(PathExpansionState currentState, Queue<PathExpansionState> queue) {
        Person currentLastPerson = currentState.currentEndPerson();
        List<Relationship> currentRelations = currentState.relationsInPath();
        Set<Long> personsCurrentlyInThisPath = currentState.personsInThisPathIds();
        
        Set<FamilyGraphService.PersonEdge> neighborEdges = familyGraphService.getNeighbors(currentLastPerson.getId());
        List<Relationship> relationshipsToExplore = neighborEdges.stream()
            .map(FamilyGraphService.PersonEdge::relationship)
            .filter(rel -> rel.getPerson1().getId().equals(currentLastPerson.getId()))
            .collect(Collectors.toList());

        for (Relationship relationship : relationshipsToExplore) {
            Person neighbor = relationship.getPerson2(); // İleri yönlü olduğu için komşu her zaman person2'dir

            if (!personsCurrentlyInThisPath.contains(neighbor.getId())) {
                List<Relationship> newRelationsForPath = new ArrayList<>(currentRelations);
                newRelationsForPath.add(relationship);
                Set<Long> newPersonsInThisPath = new HashSet<>(personsCurrentlyInThisPath);
                newPersonsInThisPath.add(neighbor.getId());
                queue.add(new PathExpansionState(newRelationsForPath, neighbor, newPersonsInThisPath));
            }
        }
    }

    @Override
    public List<RelationshipStepDTO> convertPathToDTO(List<Relationship> path, Person startPerson, Person endPerson, Locale locale) {
        List<RelationshipStepDTO> dtos = new ArrayList<>();
        Person currentPerson = startPerson;

        for (int i = 0; i < path.size(); i++) {
            Relationship rel = path.get(i);
            Person nextPersonInRel = rel.getPerson1().getId().equals(currentPerson.getId()) ? rel.getPerson2() : rel.getPerson1();
            String description;
            boolean isCurrentPersonThePerson1InRel = rel.getPerson1().getId().equals(currentPerson.getId());

            if (isCurrentPersonThePerson1InRel) {
                description = formatDirectRelationship(currentPerson, nextPersonInRel, rel.getType(), locale);
            } else {
                description = formatReverseRelationship(currentPerson, nextPersonInRel, rel.getType(), locale);
            }

            dtos.add(new RelationshipStepDTO(
                currentPerson.getId(),
                currentPerson.getFirstName() + " " + currentPerson.getLastName(),
                currentPerson.getGender() != null ? currentPerson.getGender().name() : null,
                currentPerson.getBirthDate() != null ? currentPerson.getBirthDate().getYear() : null,
                description,
                currentPerson.getId().equals(startPerson.getId()),
                false, // ara kişiler targetPerson olarak işaretlenmez
                nextPersonInRel.getId(),
                nextPersonInRel.getFirstName() + " " + nextPersonInRel.getLastName(),
                rel.getType().name(),
                rel.getStartDate(),
                rel.getEndDate()
            ));
            currentPerson = nextPersonInRel;
        }
        
        // Son kişiyi (endPerson) DTO listesine ekle
        if (!path.isEmpty()) {
            dtos.add(new RelationshipStepDTO(
                endPerson.getId(),
                endPerson.getFirstName() + " " + endPerson.getLastName(),
                endPerson.getGender() != null ? endPerson.getGender().name() : null,
                endPerson.getBirthDate() != null ? endPerson.getBirthDate().getYear() : null,
                null, // Son kişi için relationship description yok
                false, // Son kişi sourcePerson değil
                true,  // Son kişi targetPerson
                null,  // Son kişi için nextPersonId yok
                null,  // Son kişi için nextPersonName yok
                null,  // Son kişi için relationshipType yok
                null,  // Son kişi için startDate yok
                null   // Son kişi için endDate yok
            ));
        }
        
        return dtos;
    }

    /**
     * Finds all active relationships for a person. 
     * This is a local version for PathFinder to avoid cyclic dependency or to have specific logic if needed.
     * @deprecated Bu metot, performans optimizasyonu sonrası FamilyGraphService ile değiştirilmiştir.
     */
    @Deprecated
    private List<Relationship> findAllActiveRelationshipsForPathFinder(Person person) {
        List<Relationship> relationshipsAsP1 = relationshipRepository.findByPerson1AndIsActiveTrue(person);
        List<Relationship> relationshipsAsP2 = relationshipRepository.findByPerson2AndIsActiveTrue(person);
        
        Set<Relationship> allRelationships = new HashSet<>(relationshipsAsP1);
        allRelationships.addAll(relationshipsAsP2);
        
        return new ArrayList<>(allRelationships);
    }

    @Override
    public String formatDirectRelationship(Person person1, Person person2, RelationshipType type, Locale locale) {
        String p1Gender = person1.getGender() != null ? person1.getGender().name().toUpperCase() : GENDER_UNKNOWN;
        String p2Gender = person2.getGender() != null ? person2.getGender().name().toUpperCase() : GENDER_UNKNOWN;

        switch (type) {
            case PARENT_CHILD:
                return getMessage("relationship.parent_child.parent", locale, person1.getFirstName(), person2.getFirstName());
            case SPOUSE:
                return getMessage("relationship.spouse.is_spouse_of", locale, person1.getFirstName(), person2.getFirstName());
            case SIBLING:
                return getMessage(getDetailedSiblingMessageKey(p1Gender, p2Gender), locale, person1.getFirstName(), person2.getFirstName());
            default:
                return getMessage("relationship.unknown", locale, person1.getFirstName(), person2.getFirstName(), type.name());
        }
    }

    @Override
    public String formatReverseRelationship(Person person1, Person person2, RelationshipType type, Locale locale) {
        String p1Gender = person1.getGender() != null ? person1.getGender().name().toUpperCase() : GENDER_UNKNOWN;
        String p2Gender = person2.getGender() != null ? person2.getGender().name().toUpperCase() : GENDER_UNKNOWN;

        switch (type) {
            case PARENT_CHILD:
                return getMessage("relationship.parent_child.child", locale, person1.getFirstName(), person2.getFirstName());
            case SPOUSE:
                return getMessage("relationship.spouse.is_spouse_of", locale, person1.getFirstName(), person2.getFirstName());
            case SIBLING:
                return getMessage(getDetailedSiblingMessageKey(p1Gender, p2Gender), locale, person1.getFirstName(), person2.getFirstName());
            default:
                return getMessage("relationship.unknown.reverse", locale, person1.getFirstName(), person2.getFirstName(), type.name());
        }
    }
    
    private String getDetailedSiblingMessageKey(String person1Gender, String person2Gender) {
        if (GENDER_MALE.equals(person1Gender) && GENDER_MALE.equals(person2Gender)) return "relationship.sibling.is_brother_of";
        if (GENDER_FEMALE.equals(person1Gender) && GENDER_FEMALE.equals(person2Gender)) return "relationship.sibling.is_sister_of";
        if (GENDER_MALE.equals(person1Gender) && GENDER_FEMALE.equals(person2Gender)) return "relationship.sibling.is_brother_of_sister";
        if (GENDER_FEMALE.equals(person1Gender) && GENDER_MALE.equals(person2Gender)) return "relationship.sibling.is_sister_of_brother";
        return "relationship.sibling.is_sibling_of";
    }

    private String getMessage(String code, Locale locale, Object... args) {
        try {
            return messageSource.getMessage(code, args, locale);
        } catch (NoSuchMessageException _) {
            log.warn("Message key not found in RelationshipPathFinder: {} for locale {}", code, locale);
            String fallback = code;
            if (args != null && args.length > 0) {
                fallback += " (" + Arrays.stream(args).map(String::valueOf).collect(Collectors.joining(", ")) + ")";
            }
            return "[[PATH_MSG_NOT_FOUND: " + fallback + "]] ";
        }
    }

    /**
     * Finds only forward-facing relationships for a person for directed pathfinding.
     * @deprecated Bu metot, performans optimizasyonu sonrası FamilyGraphService ile değiştirilmiştir.
     */
    @Deprecated
    private List<Relationship> findForwardRelationshipsForPathFinder(Person person) {
        return relationshipRepository.findByPerson1AndIsActiveTrue(person);
    }
} 