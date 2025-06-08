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
} 