package by.backend.service.relationship;

import by.backend.model.dto.PersonSummaryDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RelationshipService {
    List<Relationship> getAllRelationships();
    Relationship createRelationship(Person person1, Person person2, RelationshipType type);
    void endRelationship(Long relationshipId);
    List<Person> findRelatives(Person person, RelationshipType type);
    CompletableFuture<List<PersonSummaryDTO>> findRelativesAsync(Person person, RelationshipType type);
    RelationshipDescriptionResult findRelationshipDescription(PersonSummaryDTO person1Summary, PersonSummaryDTO person2Summary);
    List<Relationship> findAllActiveRelationships(Person person);
    boolean hasActiveRelationship(Person person1, Person person2, RelationshipType type);
    List<RelationshipStepDTO> getRelationshipPath(Person person1, Person person2);
    List<PersonSummaryDTO> findCommonAncestors(Person person1, Person person2);
    List<PersonSummaryDTO> findCommonDescendants(Person person1, Person person2);
    List<PersonSummaryDTO> findRelativesByDegree(Person person, int degree);
    boolean isBloodRelated(Person person1, Person person2);
} 