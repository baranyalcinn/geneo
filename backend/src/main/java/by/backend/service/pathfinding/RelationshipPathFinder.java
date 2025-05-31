package by.backend.service.pathfinding;

import by.backend.model.dto.RelationshipStepDTO;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;

import java.util.List;
import java.util.Locale;

public interface RelationshipPathFinder {
    List<List<Relationship>> findPaths(Person startPerson, Person endPerson, int maxDepth);
    List<RelationshipStepDTO> convertPathToDTO(List<Relationship> path, Person startPerson, Person endPerson, Locale locale);
    String formatDirectRelationship(Person person1, Person person2, RelationshipType type, Locale locale);
    String formatReverseRelationship(Person person1, Person person2, RelationshipType type, Locale locale);
} 