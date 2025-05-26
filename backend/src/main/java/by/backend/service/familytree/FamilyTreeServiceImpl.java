package by.backend.service.familytree;

import by.backend.exception.ResourceNotFoundException;
import by.backend.model.entity.FamilyTree;
import by.backend.model.entity.Person;
import by.backend.repository.FamilyTreeRepository;
import by.backend.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyTreeServiceImpl implements FamilyTreeService {

    private final FamilyTreeRepository familyTreeRepository;
    private final PersonRepository personRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FamilyTree> getAllFamilyTrees() {
        return familyTreeRepository.findAll();
    }

    @Override
    @Transactional
    public FamilyTree createFamilyTree(String name) {
        FamilyTree familyTree = new FamilyTree(name);
        return familyTreeRepository.save(familyTree);
    }

    @Override
    @Transactional
    public void addMember(Long treeId, Long personId) {
        FamilyTree familyTree = getFamilyTreeById(treeId);
        Person person = getPersonById(personId);
        
        familyTree.addMember(person);
        familyTreeRepository.save(familyTree);
    }

    @Override
    @Transactional
    public void removeMember(Long treeId, Long personId) {
        FamilyTree familyTree = getFamilyTreeById(treeId);
        Person person = getPersonById(personId);
        
        familyTree.removeMember(person);
        familyTreeRepository.save(familyTree);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyTree> findByPerson(Long personId) {
        Person person = getPersonById(personId);
        return familyTreeRepository.findByMembersContaining(person);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FamilyTree> findByLastName(String lastName) {
        return familyTreeRepository.findByLastName(lastName);
    }

    @Override
    @Transactional(readOnly = true)
    public FamilyTree getFamilyTree(Long id) {
        return familyTreeRepository.findById(id).orElse(null);
    }
    
    private FamilyTree getFamilyTreeById(Long id) {
        return familyTreeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Aile ağacı", id));
    }
    
    private Person getPersonById(Long id) {
        return personRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Kişi", id));
    }
} 