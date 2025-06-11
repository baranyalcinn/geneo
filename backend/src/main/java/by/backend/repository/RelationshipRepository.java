package by.backend.repository;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
} 