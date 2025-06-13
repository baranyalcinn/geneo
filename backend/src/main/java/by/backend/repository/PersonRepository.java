package by.backend.repository;

import by.backend.model.entity.Person;
import by.backend.model.enums.Gender;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    // Optimized basic queries with indexes
    @Cacheable(value = "personCache", key = "#firstName + '_' + #lastName")
    List<Person> findByFirstNameAndLastName(String firstName, String lastName);

    @Cacheable(value = "personCache", key = "'fullName_' + #fullName")
    @Query("SELECT p FROM Person p WHERE p.fullName = :fullName AND p.isActive = true")
    List<Person> findByFullName(@Param("fullName") String fullName);

    @Cacheable(value = "personCache", key = "'gender_' + #gender")
    List<Person> findByGenderAndIsActiveTrue(Gender gender);

    @Cacheable(value = "personCache", key = "'birthYear_' + #birthYear")
    @Query("SELECT p FROM Person p WHERE p.birthYear = :birthYear AND p.isActive = true")
    List<Person> findByBirthYear(@Param("birthYear") Integer birthYear);

    // Optimized search queries
    @Query("SELECT p FROM Person p WHERE " +
           "(LOWER(p.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "AND p.isActive = true ORDER BY p.lastName, p.firstName")
    Page<Person> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    @Query("SELECT p FROM Person p WHERE p.birthYear BETWEEN :startYear AND :endYear AND p.isActive = true")
    List<Person> findByBirthYearRange(@Param("startYear") Integer startYear, @Param("endYear") Integer endYear);

    // Game-optimized queries
    @Cacheable(value = "gameQuestionCache", key = "'game_persons_' + #gender + '_' + #minBirthYear + '_' + #maxBirthYear")
    @Query("SELECT p FROM Person p WHERE p.gender = :gender " +
           "AND p.birthYear BETWEEN :minBirthYear AND :maxBirthYear " +
           "AND p.isActive = true " +
           "AND p.relationshipCount > 0 " +
           "ORDER BY RANDOM()")
    List<Person> findRandomPersonsForGame(@Param("gender") Gender gender,
                                         @Param("minBirthYear") Integer minBirthYear,
                                         @Param("maxBirthYear") Integer maxBirthYear);

    @Query(value = "SELECT p.* FROM persons p " +
                   "WHERE p.is_active = true " +
                   "AND p.relationship_count >= :minRelationships " +
                   "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
    List<Person> findRandomPersonsWithRelationships(@Param("minRelationships") Integer minRelationships,
                                                   @Param("limit") Integer limit);

    // Relationship-aware queries with EntityGraph for performance
    @EntityGraph(value = "Person.withRelationships", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT DISTINCT p FROM Person p " +
           "LEFT JOIN p.relationships1 r1 " +
           "LEFT JOIN p.relationships2 r2 " +
           "WHERE (r1.isActive = true OR r2.isActive = true) " +
           "AND p.isActive = true")
    List<Person> findPersonsWithActiveRelationships();

    @EntityGraph(value = "Person.minimal", type = EntityGraph.EntityGraphType.LOAD)
    @Query("SELECT p FROM Person p WHERE p.isActive = true ORDER BY p.lastName, p.firstName")
    Page<Person> findAllActivePersonsMinimal(Pageable pageable);

    // Count queries for performance monitoring
    @Query("SELECT COUNT(p) FROM Person p WHERE p.isActive = true")
    long countActivePersons();

    @Query("SELECT COUNT(p) FROM Person p WHERE p.gender = :gender AND p.isActive = true")
    long countByGenderAndActive(@Param("gender") Gender gender);

    @Query("SELECT COUNT(p) FROM Person p WHERE p.relationshipCount > 0 AND p.isActive = true")
    long countPersonsWithRelationships();

    // Statistics queries
    @Query("SELECT p.gender, COUNT(p) FROM Person p WHERE p.isActive = true GROUP BY p.gender")
    List<Object[]> getGenderStatistics();

    @Query("SELECT p.birthYear, COUNT(p) FROM Person p WHERE p.isActive = true AND p.birthYear IS NOT NULL " +
           "GROUP BY p.birthYear ORDER BY p.birthYear")
    List<Object[]> getBirthYearStatistics();

    // Batch operations for performance
    @Modifying
    @Transactional
    @Query("UPDATE Person p SET p.relationshipCount = " +
           "(SELECT COUNT(r) FROM Relationship r WHERE (r.person1 = p OR r.person2 = p) AND r.isActive = true) " +
           "WHERE p.id = :personId")
    void updateRelationshipCount(@Param("personId") Long personId);

    @Modifying
    @Transactional
    @Query("UPDATE Person p SET p.lastAccessed = :accessDate WHERE p.id = :personId")
    void updateLastAccessed(@Param("personId") Long personId, @Param("accessDate") LocalDate accessDate);

    @Modifying
    @Transactional
    @Query("UPDATE Person p SET p.isActive = false WHERE p.id = :personId")
    void softDeletePerson(@Param("personId") Long personId);

    // Age-based queries for game logic
    @Query("SELECT p FROM Person p WHERE " +
           "p.isActive = true AND " +
           "(:currentYear - p.birthYear) BETWEEN :minAge AND :maxAge " +
           "ORDER BY p.birthYear DESC")
    List<Person> findByAgeRange(@Param("minAge") Integer minAge, 
                               @Param("maxAge") Integer maxAge,
                               @Param("currentYear") Integer currentYear);

    // Family tree optimization queries
    @Query("SELECT DISTINCT p FROM Person p " +
           "JOIN p.familyTrees ft " +
           "WHERE ft.id = :familyTreeId AND p.isActive = true")
    List<Person> findByFamilyTreeId(@Param("familyTreeId") Long familyTreeId);

    // Performance monitoring queries
    @Query("SELECT p FROM Person p WHERE p.lastAccessed < :cutoffDate AND p.isActive = true")
    List<Person> findInactivePersons(@Param("cutoffDate") LocalDate cutoffDate);

    @Query(value = "SELECT p.id, p.full_name, p.relationship_count " +
                   "FROM persons p " +
                   "WHERE p.is_active = true " +
                   "ORDER BY p.relationship_count DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findMostConnectedPersons(@Param("limit") Integer limit);

    // Custom queries for specific business logic
    @Query("SELECT p FROM Person p WHERE " +
           "p.isActive = true AND " +
           "p.deathDate IS NULL AND " +
           "(:currentYear - p.birthYear) BETWEEN 18 AND 80")
    List<Person> findActiveAdults(@Param("currentYear") Integer currentYear);

    @Query("SELECT p FROM Person p WHERE " +
           "p.isActive = true AND " +
           "p.gender = :gender AND " +
           "p.deathDate IS NULL AND " +
           "p.relationshipCount >= :minRelationships " +
           "ORDER BY p.relationshipCount DESC")
    List<Person> findViableGamePersons(@Param("gender") Gender gender, 
                                     @Param("minRelationships") Integer minRelationships);

    // Existence checks for performance
    boolean existsByFullNameAndIsActiveTrue(String fullName);
    
    boolean existsByFirstNameAndLastNameAndIsActiveTrue(String firstName, String lastName);
} 