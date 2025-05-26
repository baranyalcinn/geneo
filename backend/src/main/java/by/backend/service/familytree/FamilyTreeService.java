package by.backend.service.familytree;

import by.backend.model.entity.FamilyTree;

import java.util.List;

public interface FamilyTreeService {
    List<FamilyTree> getAllFamilyTrees();
    FamilyTree createFamilyTree(String name);
    void addMember(Long treeId, Long personId);
    void removeMember(Long treeId, Long personId);
    List<FamilyTree> findByPerson(Long personId);
    List<FamilyTree> findByLastName(String lastName);
    FamilyTree getFamilyTree(Long id);
} 