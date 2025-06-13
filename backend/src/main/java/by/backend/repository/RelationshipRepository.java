package by.backend.repository;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {
    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2")
    List<Relationship> findAllWithPersons();

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.person1 = :person AND r.type = :type AND r.isActive = true")
    List<Relationship> findByPerson1AndTypeAndIsActiveTrue(@Param("person") Person person, @Param("type") RelationshipType type);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.person2 = :person AND r.type = :type AND r.isActive = true")
    List<Relationship> findByPerson2AndTypeAndIsActiveTrue(@Param("person") Person person, @Param("type") RelationshipType type);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE (r.person1 = :person1 OR r.person2 = :person2) AND r.isActive = true")
    List<Relationship> findByPerson1OrPerson2AndIsActiveTrue(@Param("person1") Person person1, @Param("person2") Person person2);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.person1 = :person AND r.isActive = true")
    List<Relationship> findByPerson1AndIsActiveTrue(@Param("person") Person person);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.person1 = :person1 AND r.person2 = :person2 AND r.isActive = true")
    List<Relationship> findByPerson1AndPerson2AndIsActiveTrue(@Param("person1") Person person1, @Param("person2") Person person2);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.person1 = :person1 AND r.person2 = :person2 AND r.type = :type AND r.isActive = true")
    List<Relationship> findByPerson1AndPerson2AndTypeAndIsActiveTrue(@Param("person1") Person person1, @Param("person2") Person person2, @Param("type") RelationshipType type);
    
    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE " +
           "((r.person1 = :person1 AND r.person2 = :person2) OR " +
           "(r.person1 = :person2 AND r.person2 = :person1)) AND " +
           "r.type = :type AND r.isActive = true")
    Optional<Relationship> findActiveRelationship(@Param("person1") Person person1, @Param("person2") Person person2, @Param("type") RelationshipType type);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE " +
           "(r.person1 = :person OR r.person2 = :person) AND " +
           "r.type = :type AND r.isActive = true")
    List<Relationship> findActiveRelationships(@Param("person") Person person, @Param("type") RelationshipType type);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Relationship r WHERE (r.person1.id = :personId OR r.person2.id = :personId) AND r.type = :type AND r.isActive = true")
    boolean existsByPersonAndTypeAndIsActiveTrue(@Param("personId") Long personId, @Param("type") RelationshipType type);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.type = :type AND r.isActive = true")
    List<Relationship> findAllByTypeAndIsActiveTrue(@Param("type") RelationshipType type);

    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.person2 = :person AND r.isActive = true")
    List<Relationship> findByPerson2AndIsActiveTrue(@Param("person") Person person);
    
    // PathFinderService için eksik metodları ekle
    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE r.isActive = true")
    List<Relationship> findAllByIsActiveTrue();
    
    @Query("SELECT DISTINCT r FROM Relationship r LEFT JOIN FETCH r.person1 LEFT JOIN FETCH r.person2 WHERE (r.person1.id = :personId OR r.person2.id = :personId) AND r.isActive = true")
    List<Relationship> findAllActiveRelationshipsForPerson(@Param("personId") Long personId);
    
    // ===== OPTIMIZED BATCH QUERIES - Performance Enhancement =====
    
    /**
     * Batch query for multiple persons - solves N+1 problem
     * O(1) query instead of O(N) queries for N persons
     */
    @Query("SELECT DISTINCT r FROM Relationship r " +
           "LEFT JOIN FETCH r.person1 " +
           "LEFT JOIN FETCH r.person2 " +
           "WHERE (r.person1.id IN :personIds OR r.person2.id IN :personIds) " +
           "AND r.isActive = true")
    List<Relationship> findAllByPersonIdsWithPersons(@Param("personIds") List<Long> personIds);
    
    /**
     * Optimized query for specific relationship types with batch processing
     */
    @Query("SELECT DISTINCT r FROM Relationship r " +
           "LEFT JOIN FETCH r.person1 " +
           "LEFT JOIN FETCH r.person2 " +
           "WHERE r.type IN :types AND r.isActive = true")
    List<Relationship> findAllByTypesWithPersons(@Param("types") List<RelationshipType> types);
    
    /**
     * Pre-computed distance queries for performance
     */
    @Query(value = """
        WITH RECURSIVE relationship_paths AS (
            -- Base case: direct relationships (distance 1)
            SELECT r.person1_id, r.person2_id, r.type, 1 as distance, ARRAY[r.id] as path
            FROM relationships r 
            WHERE r.is_active = true 
            AND (r.person1_id = :startPersonId OR r.person2_id = :startPersonId)
            
            UNION ALL
            
            -- Recursive case: extend paths
            SELECT 
                rp.person1_id, 
                CASE 
                    WHEN r.person1_id = (CASE WHEN rp.person1_id = :startPersonId THEN rp.person2_id ELSE rp.person1_id END) 
                    THEN r.person2_id 
                    ELSE r.person1_id 
                END,
                r.type,
                rp.distance + 1,
                rp.path || r.id
            FROM relationship_paths rp
            JOIN relationships r ON (
                r.person1_id = (CASE WHEN rp.person1_id = :startPersonId THEN rp.person2_id ELSE rp.person1_id END) OR
                r.person2_id = (CASE WHEN rp.person1_id = :startPersonId THEN rp.person2_id ELSE rp.person1_id END)
            )
            WHERE rp.distance < :maxDistance 
            AND r.is_active = true
            AND NOT (r.id = ANY(rp.path)) -- Prevent cycles
        )
        SELECT DISTINCT person2_id, distance, path
        FROM relationship_paths 
        WHERE person2_id = :endPersonId
        ORDER BY distance
        LIMIT 1
        """, nativeQuery = true)
    List<Object[]> findShortestPathOptimized(@Param("startPersonId") Long startPersonId, 
                                           @Param("endPersonId") Long endPersonId,
                                           @Param("maxDistance") Integer maxDistance);
    
    /**
     * Cached ancestor lookup for family tree navigation
     */
    @Query(value = """
        WITH RECURSIVE ancestors AS (
            -- Base case: parents
            SELECT 
                r.person2_id as descendant_id, 
                r.person1_id as ancestor_id, 
                1 as generation_diff,
                p.first_name || ' ' || p.last_name as ancestor_name
            FROM relationships r
            JOIN persons p ON p.id = r.person1_id
            WHERE r.type = 'PARENT_CHILD' AND r.is_active = true 
            AND r.person2_id = :personId
            
            UNION ALL
            
            -- Recursive case: grandparents and beyond
            SELECT 
                a.descendant_id,
                r.person1_id,
                a.generation_diff + 1,
                p.first_name || ' ' || p.last_name
            FROM ancestors a
            JOIN relationships r ON a.ancestor_id = r.person2_id
            JOIN persons p ON p.id = r.person1_id
            WHERE r.type = 'PARENT_CHILD' AND r.is_active = true 
            AND a.generation_diff < :maxGenerations
        )
        SELECT ancestor_id, generation_diff, ancestor_name
        FROM ancestors
        ORDER BY generation_diff, ancestor_id
        """, nativeQuery = true)
    List<Object[]> findAncestorsWithGenerations(@Param("personId") Long personId,
                                               @Param("maxGenerations") Integer maxGenerations);
    
    /**
     * Optimized descendant lookup for family tree navigation
     */
    @Query(value = """
        WITH RECURSIVE descendants AS (
            -- Base case: children
            SELECT 
                r.person1_id as ancestor_id, 
                r.person2_id as descendant_id, 
                1 as generation_diff,
                p.first_name || ' ' || p.last_name as descendant_name
            FROM relationships r
            JOIN persons p ON p.id = r.person2_id
            WHERE r.type = 'PARENT_CHILD' AND r.is_active = true 
            AND r.person1_id = :personId
            
            UNION ALL
            
            -- Recursive case: grandchildren and beyond
            SELECT 
                d.ancestor_id,
                r.person2_id,
                d.generation_diff + 1,
                p.first_name || ' ' || p.last_name
            FROM descendants d
            JOIN relationships r ON d.descendant_id = r.person1_id
            JOIN persons p ON p.id = r.person2_id
            WHERE r.type = 'PARENT_CHILD' AND r.is_active = true 
            AND d.generation_diff < :maxGenerations
        )
        SELECT descendant_id, generation_diff, descendant_name
        FROM descendants
        ORDER BY generation_diff, descendant_id
        """, nativeQuery = true)
    List<Object[]> findDescendantsWithGenerations(@Param("personId") Long personId,
                                                  @Param("maxGenerations") Integer maxGenerations);

    /**
     * Optimized bidirectional query for direct relationship checking
     * Performance: O(1) instead of O(2) queries
     */
    @Query("SELECT DISTINCT r FROM Relationship r " +
           "LEFT JOIN FETCH r.person1 " +
           "LEFT JOIN FETCH r.person2 " +
           "WHERE ((r.person1.id = :person1Id AND r.person2.id = :person2Id) OR " +
           "       (r.person1.id = :person2Id AND r.person2.id = :person1Id)) " +
           "AND r.isActive = true")
    List<Relationship> findDirectRelationshipsBidirectional(@Param("person1Id") Long person1Id, @Param("person2Id") Long person2Id);

    /**
     * Batch query for family network analysis - prevents N+1 problem
     */
    @Query("SELECT DISTINCT r FROM Relationship r " +
           "LEFT JOIN FETCH r.person1 " +
           "LEFT JOIN FETCH r.person2 " +
           "WHERE (r.person1.id IN :familyMemberIds OR r.person2.id IN :familyMemberIds) " +
           "AND r.type IN :relevantTypes " +
           "AND r.isActive = true")
    List<Relationship> findFamilyNetworkRelationships(@Param("familyMemberIds") List<Long> familyMemberIds, 
                                                      @Param("relevantTypes") List<RelationshipType> relevantTypes);

    /**
     * Optimized query for complex in-law relationship chains
     */
    @Query("SELECT DISTINCT r FROM Relationship r " +
           "LEFT JOIN FETCH r.person1 " +
           "LEFT JOIN FETCH r.person2 " +
           "WHERE (r.person1.id = :spouseId OR r.person2.id = :spouseId) " +
           "AND r.type = 'SIBLING' " +
           "AND r.isActive = true")
    List<Relationship> findSpouseSiblings(@Param("spouseId") Long spouseId);

    /**
     * High-performance query for ancestry chains with depth limitation
     */
    @Query(value = "WITH RECURSIVE ancestry_chain AS (" +
           "  SELECT r.person1_id, r.person2_id, 1 as depth " +
           "  FROM relationships r " +
           "  WHERE r.person2_id = :personId AND r.type = 'PARENT_CHILD' AND r.is_active = true " +
           "  UNION ALL " +
           "  SELECT r.person1_id, r.person2_id, ac.depth + 1 " +
           "  FROM relationships r " +
           "  JOIN ancestry_chain ac ON r.person2_id = ac.person1_id " +
           "  WHERE r.type = 'PARENT_CHILD' AND r.is_active = true AND ac.depth < :maxDepth" +
           ") " +
           "SELECT DISTINCT r.* FROM relationships r " +
           "JOIN ancestry_chain ac ON (r.person1_id = ac.person1_id OR r.person2_id = ac.person1_id) " +
           "WHERE r.is_active = true",
           nativeQuery = true)
    List<Relationship> findAncestryChain(@Param("personId") Long personId, @Param("maxDepth") int maxDepth);

    /**
     * Optimized descendant query with depth control
     */
    @Query(value = "WITH RECURSIVE descendant_chain AS (" +
           "  SELECT r.person1_id, r.person2_id, 1 as depth " +
           "  FROM relationships r " +
           "  WHERE r.person1_id = :personId AND r.type = 'PARENT_CHILD' AND r.is_active = true " +
           "  UNION ALL " +
           "  SELECT r.person1_id, r.person2_id, dc.depth + 1 " +
           "  FROM relationships r " +
           "  JOIN descendant_chain dc ON r.person1_id = dc.person2_id " +
           "  WHERE r.type = 'PARENT_CHILD' AND r.is_active = true AND dc.depth < :maxDepth" +
           ") " +
           "SELECT DISTINCT r.* FROM relationships r " +
           "JOIN descendant_chain dc ON (r.person1_id = dc.person2_id OR r.person2_id = dc.person2_id) " +
           "WHERE r.is_active = true",
           nativeQuery = true)
    List<Relationship> findDescendantChain(@Param("personId") Long personId, @Param("maxDepth") int maxDepth);

    /**
     * Smart query for cousin relationship detection
     */
    @Query("SELECT DISTINCT r FROM Relationship r " +
           "LEFT JOIN FETCH r.person1 " +
           "LEFT JOIN FETCH r.person2 " +
           "WHERE r.person1.id IN (" +
           "  SELECT DISTINCT parent_rel.person1.id " +
           "  FROM Relationship parent_rel " +
           "  WHERE parent_rel.person2.id IN (" +
           "    SELECT DISTINCT grandparent_rel.person1.id " +
           "    FROM Relationship grandparent_rel " +
           "    WHERE grandparent_rel.person2.id = :targetPersonId " +
           "    AND grandparent_rel.type = 'PARENT_CHILD' " +
           "    AND grandparent_rel.isActive = true" +
           "  ) " +
           "  AND parent_rel.type = 'PARENT_CHILD' " +
           "  AND parent_rel.isActive = true" +
           ") " +
           "AND r.person2.id = :queryPersonId " +
           "AND r.type = 'PARENT_CHILD' " +
           "AND r.isActive = true")
    List<Relationship> findPotentialCousinRelationships(@Param("queryPersonId") Long queryPersonId, 
                                                        @Param("targetPersonId") Long targetPersonId);

    /**
     * Performance-optimized query for relationship counting and statistics
     */
    @Query("SELECT r.type, COUNT(r) FROM Relationship r " +
           "WHERE (r.person1.id = :personId OR r.person2.id = :personId) " +
           "AND r.isActive = true " +
           "GROUP BY r.type")
    List<Object[]> getRelationshipStatistics(@Param("personId") Long personId);

    /**
     * Batch existence check for multiple relationship pairs
     */
    @Query("SELECT CONCAT(r.person1.id, ':', r.person2.id, ':', r.type) " +
           "FROM Relationship r " +
           "WHERE ((r.person1.id IN :person1Ids AND r.person2.id IN :person2Ids) OR " +
           "       (r.person1.id IN :person2Ids AND r.person2.id IN :person1Ids)) " +
           "AND r.isActive = true")
    List<String> batchCheckRelationshipExistence(@Param("person1Ids") List<Long> person1Ids, 
                                                 @Param("person2Ids") List<Long> person2Ids);

    List<Relationship> findByPerson1AndType(Person person, RelationshipType type);
    List<Relationship> findByPerson2AndType(Person person, RelationshipType type);

    // Optimized basic relationship queries
    @Cacheable(value = "relationshipCache", key = "'person_' + #personId")
    @Query("SELECT r FROM Relationship r WHERE (r.person1.id = :personId OR r.person2.id = :personId) AND r.isActive = true")
    List<Relationship> findActiveByPersonId(@Param("personId") Long personId);

    @Cacheable(value = "relationshipCache", key = "'persons_' + #person1Id + '_' + #person2Id")
    @Query("SELECT r FROM Relationship r WHERE " +
           "((r.person1.id = :person1Id AND r.person2.id = :person2Id) OR " +
           " (r.person1.id = :person2Id AND r.person2.id = :person1Id)) " +
           "AND r.isActive = true")
    Optional<Relationship> findActiveByPersonIds(@Param("person1Id") Long person1Id, @Param("person2Id") Long person2Id);

    @Cacheable(value = "relationshipCache", key = "'type_' + #relationshipType")
    @Query("SELECT r FROM Relationship r WHERE r.type = :type AND r.isActive = true")
    List<Relationship> findByTypeAndActive(@Param("type") RelationshipType relationshipType);

    // Optimized path-finding queries for family relationship calculation
    @Cacheable(value = "relationshipPaths", key = "'path_' + #startPersonId + '_' + #maxDepth")
    @Query("SELECT r FROM Relationship r WHERE " +
           "(r.person1.id = :startPersonId OR r.person2.id = :startPersonId) " +
           "AND r.isActive = true " +
           "AND r.relationshipStrength >= :minStrength " +
           "ORDER BY r.relationshipStrength DESC")
    List<Relationship> findRelationshipPathsFrom(@Param("startPersonId") Long startPersonId,
                                                @Param("minStrength") Integer minStrength);

    // Game-specific optimized queries
    @Query("SELECT r FROM Relationship r WHERE " +
           "r.isActive = true " +
           "AND r.type IN :allowedTypes " +
           "AND r.relationshipStrength >= :minStrength " +
           "ORDER BY RANDOM()")
    List<Relationship> findRandomRelationshipsForGame(@Param("allowedTypes") List<RelationshipType> allowedTypes,
                                                     @Param("minStrength") Integer minStrength);

    @Query(value = "SELECT r.* FROM relationships r " +
                   "INNER JOIN persons p1 ON r.person1_id = p1.id " +
                   "INNER JOIN persons p2 ON r.person2_id = p2.id " +
                   "WHERE r.is_active = true " +
                   "AND p1.is_active = true " +
                   "AND p2.is_active = true " +
                   "AND p1.relationship_count >= :minConnections " +
                   "AND p2.relationship_count >= :minConnections " +
                   "AND r.relationship_strength >= :minStrength " +
                   "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Relationship> findGameCandidateRelationships(@Param("minConnections") Integer minConnections,
                                                     @Param("minStrength") Integer minStrength,
                                                     @Param("limit") Integer limit);

    // Complex family relationship queries
    @Query("SELECT r FROM Relationship r WHERE " +
           "r.isActive = true " +
           "AND ((r.person1.id = :personId AND r.type = :forwardType) OR " +
           "     (r.person2.id = :personId AND r.type = :reverseType))")
    List<Relationship> findDirectionalRelationships(@Param("personId") Long personId,
                                                   @Param("forwardType") RelationshipType forwardType,
                                                   @Param("reverseType") RelationshipType reverseType);

    @Query("SELECT DISTINCT r FROM Relationship r " +
           "WHERE r.isActive = true " +
           "AND ((r.person1.id IN :personIds) OR (r.person2.id IN :personIds)) " +
           "AND r.type IN :relationshipTypes")
    List<Relationship> findRelationshipsInFamily(@Param("personIds") List<Long> personIds,
                                                @Param("relationshipTypes") List<RelationshipType> relationshipTypes);

    // Statistics and analytics queries
    @Query("SELECT COUNT(r) FROM Relationship r WHERE (r.person1.id = :personId OR r.person2.id = :personId) AND r.isActive = true")
    long countActiveByPersonId(@Param("personId") Long personId);

    @Query("SELECT r.type, COUNT(r) FROM Relationship r WHERE r.isActive = true GROUP BY r.type ORDER BY COUNT(r) DESC")
    List<Object[]> getRelationshipTypeStatistics();

    @Query("SELECT AVG(r.relationshipStrength) FROM Relationship r WHERE r.isActive = true AND r.type = :type")
    Double getAverageStrengthByType(@Param("type") RelationshipType type);

    // Performance monitoring queries
    @Query("SELECT r FROM Relationship r WHERE r.updatedAt < :cutoffDate AND r.isActive = true")
    List<Relationship> findStaleRelationships(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query(value = "SELECT r.type, COUNT(*) as count, AVG(r.relationship_strength) as avg_strength " +
                   "FROM relationships r " +
                   "WHERE r.is_active = true " +
                   "GROUP BY r.type " +
                   "ORDER BY count DESC", nativeQuery = true)
    List<Object[]> getDetailedRelationshipStatistics();

    // Batch operations for performance
    @Modifying
    @Transactional
    @Query("UPDATE Relationship r SET r.isActive = false, r.endDate = CURRENT_DATE WHERE r.id = :relationshipId")
    void softDeleteRelationship(@Param("relationshipId") Long relationshipId);

    @Modifying
    @Transactional
    @Query("UPDATE Relationship r SET r.relationshipStrength = :strength WHERE r.id = :relationshipId")
    void updateRelationshipStrength(@Param("relationshipId") Long relationshipId, @Param("strength") Integer strength);

    @Modifying
    @Transactional
    @Query("UPDATE Relationship r SET r.updatedAt = CURRENT_TIMESTAMP WHERE r.id IN :relationshipIds")
    void touchRelationships(@Param("relationshipIds") List<Long> relationshipIds);

    // Existence and validation queries
    @Query("SELECT COUNT(r) > 0 FROM Relationship r WHERE " +
           "((r.person1.id = :person1Id AND r.person2.id = :person2Id) OR " +
           " (r.person1.id = :person2Id AND r.person2.id = :person1Id)) " +
           "AND r.type = :type AND r.isActive = true")
    boolean existsActiveRelationship(@Param("person1Id") Long person1Id,
                                   @Param("person2Id") Long person2Id,
                                   @Param("type") RelationshipType type);

    @Query("SELECT COUNT(r) FROM Relationship r WHERE " +
           "(r.person1.id = :personId OR r.person2.id = :personId) " +
           "AND r.type = :type AND r.isActive = true")
    long countRelationshipsByType(@Param("personId") Long personId, @Param("type") RelationshipType type);

    // Age-compatible relationship queries
    @Query("SELECT r FROM Relationship r " +
           "INNER JOIN r.person1 p1 " +
           "INNER JOIN r.person2 p2 " +
           "WHERE r.isActive = true " +
           "AND ABS(p1.birthYear - p2.birthYear) <= :maxAgeDifference " +
           "AND r.type = :relationshipType")
    List<Relationship> findAgeCompatibleRelationships(@Param("relationshipType") RelationshipType relationshipType,
                                                     @Param("maxAgeDifference") Integer maxAgeDifference);

    // Turkish family relationship specific queries
    @Query("SELECT r FROM Relationship r WHERE " +
           "r.isActive = true " +
           "AND r.type IN ('PARENT_CHILD', 'SIBLING', 'SPOUSE') " +
           "AND ((r.person1.id = :personId) OR (r.person2.id = :personId))")
    List<Relationship> findDirectFamilyRelationships(@Param("personId") Long personId);

    @Query("SELECT r FROM Relationship r WHERE " +
           "r.isActive = true " +
           "AND r.type IN ('UNCLE_AUNT_NEPHEW_NIECE', 'COUSIN') " +
           "AND ((r.person1.id = :personId) OR (r.person2.id = :personId))")
    List<Relationship> findExtendedFamilyRelationships(@Param("personId") Long personId);

    // Network analysis queries for complex relationship detection
    @Query(value = "WITH RECURSIVE family_network AS (" +
                   "    SELECT person1_id as person_id, person2_id as connected_to, 1 as depth " +
                   "    FROM relationships " +
                   "    WHERE person1_id = :startPersonId AND is_active = true " +
                   "    UNION ALL " +
                   "    SELECT person2_id as person_id, person1_id as connected_to, 1 as depth " +
                   "    FROM relationships " +
                   "    WHERE person2_id = :startPersonId AND is_active = true " +
                   "    UNION ALL " +
                   "    SELECT r.person1_id, r.person2_id, fn.depth + 1 " +
                   "    FROM relationships r " +
                   "    INNER JOIN family_network fn ON r.person1_id = fn.connected_to " +
                   "    WHERE fn.depth < :maxDepth AND r.is_active = true " +
                   "    UNION ALL " +
                   "    SELECT r.person2_id, r.person1_id, fn.depth + 1 " +
                   "    FROM relationships r " +
                   "    INNER JOIN family_network fn ON r.person2_id = fn.connected_to " +
                   "    WHERE fn.depth < :maxDepth AND r.is_active = true " +
                   ") " +
                   "SELECT DISTINCT connected_to, MIN(depth) as min_depth " +
                   "FROM family_network " +
                   "WHERE connected_to = :targetPersonId " +
                   "GROUP BY connected_to", nativeQuery = true)
    List<Object[]> findShortestPath(@Param("startPersonId") Long startPersonId,
                                  @Param("targetPersonId") Long targetPersonId,
                                  @Param("maxDepth") Integer maxDepth);

    // Cleanup and maintenance queries
    @Modifying
    @Transactional
    @Query("DELETE FROM Relationship r WHERE r.isActive = false AND r.endDate < :cutoffDate")
    void purgeOldInactiveRelationships(@Param("cutoffDate") LocalDateTime cutoffDate);
} 