package by.backend.controller;

import by.backend.exception.ResourceNotFoundException;
import by.backend.model.dto.FamilyTreeDTO;
import by.backend.model.dto.PersonDTO;
import by.backend.model.entity.FamilyTree;
import by.backend.model.entity.Person;
import by.backend.service.familytree.FamilyTreeService;
import by.backend.mapper.FamilyTreeMapper;
import by.backend.mapper.PersonMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/family-trees")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
public class FamilyTreeController {
    
    private final FamilyTreeService familyTreeService;
    private final PersonMapper personMapper;
    private final FamilyTreeMapper familyTreeMapper;

    @GetMapping
    public ResponseEntity<List<FamilyTreeDTO>> getAllFamilyTrees() {
        List<FamilyTree> trees = familyTreeService.getAllFamilyTrees();
        List<FamilyTreeDTO> dtos = trees.stream()
            .map(familyTreeMapper::toDTO)
            .toList();
        
        log.debug("Bulunan aile ağaçları sayısı: {}", dtos.size());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<FamilyTreeDTO> createFamilyTree(@RequestParam String name) {
        FamilyTree tree = familyTreeService.createFamilyTree(name);
        FamilyTreeDTO treeDTO = familyTreeMapper.toDTO(tree);
        return new ResponseEntity<>(treeDTO, HttpStatus.CREATED);
    }

    @PostMapping("/{treeId}/members/{personId}")
    @Transactional
    public ResponseEntity<Void> addMember(@PathVariable Long treeId, @PathVariable Long personId) {
        familyTreeService.addMember(treeId, personId);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @DeleteMapping("/{treeId}/members/{personId}")
    @Transactional
    public ResponseEntity<Void> removeMember(@PathVariable Long treeId, @PathVariable Long personId) {
        familyTreeService.removeMember(treeId, personId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{treeId}/members")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PersonDTO>> getTreeMembers(@PathVariable Long treeId) {
        FamilyTree tree = familyTreeService.getFamilyTree(treeId);
        
        if (tree == null) {
            throw new ResourceNotFoundException("Aile ağacı", treeId);
        }
        
        Set<Person> memberSet = tree.getMembers();
        if (memberSet == null || memberSet.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        
        List<Person> members = new ArrayList<>(memberSet);
        List<PersonDTO> memberDTOs = personMapper.toDTOList(members);
        
        return ResponseEntity.ok(List.copyOf(memberDTOs));
    }

    @GetMapping("/by-person/{personId}")
    public ResponseEntity<List<FamilyTreeDTO>> findByPerson(@PathVariable Long personId) {
        List<FamilyTree> trees = familyTreeService.findByPerson(personId);
        List<FamilyTreeDTO> dtos = trees.stream()
            .map(familyTreeMapper::toDTO)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/by-lastname/{lastName}")
    public ResponseEntity<List<FamilyTreeDTO>> findByLastName(@PathVariable String lastName) {
        List<FamilyTree> trees = familyTreeService.findByLastName(lastName);
        List<FamilyTreeDTO> dtos = trees.stream()
            .map(familyTreeMapper::toDTO)
            .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<FamilyTreeDTO> getFamilyTree(@PathVariable Long id) {
        FamilyTree tree = familyTreeService.getFamilyTree(id);
        
        if (tree == null) {
            throw new ResourceNotFoundException("Aile ağacı", id);
        }
        
        log.debug("Bulunan aile ağacı: {}", tree);
        return ResponseEntity.ok(familyTreeMapper.toDTO(tree));
    }
} 