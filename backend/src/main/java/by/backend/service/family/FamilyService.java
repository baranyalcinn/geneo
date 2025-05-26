package by.backend.service.family;

import java.util.Map;

public interface FamilyService {
    Map<String, Object> findRelationshipBetween(Long person1Id, Long person2Id);
} 