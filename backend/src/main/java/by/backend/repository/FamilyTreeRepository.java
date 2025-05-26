package by.backend.repository;

import by.backend.model.entity.FamilyTree;
import by.backend.model.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, Long> {
    List<FamilyTree> findByMembersContaining(Person person);
    
    @Query("SELECT ft FROM FamilyTree ft JOIN ft.members m WHERE m.lastName = :lastName")
    List<FamilyTree> findByLastName(String lastName);
} 