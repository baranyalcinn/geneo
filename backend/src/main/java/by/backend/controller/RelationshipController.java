package by.backend.controller;

import by.backend.model.dto.RelationshipDTO;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.enums.RelationshipType;
import by.backend.service.person.PersonService;
import by.backend.service.relationship.RelationshipService;
import by.backend.mapper.RelationshipMapper;
import by.backend.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import by.backend.model.dto.PersonSummaryDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relationships")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class RelationshipController {
    
    private final RelationshipService relationshipService;
    private final PersonService personService;
    private final RelationshipMapper relationshipMapper;
    private final PersonMapper personMapper;

    @GetMapping
    public ResponseEntity<List<RelationshipDTO>> getAllRelationships() {
        List<Relationship> relationships = relationshipService.getAllRelationships();
        return ResponseEntity.ok(relationshipMapper.toDTOList(relationships));
    }

    @PostMapping
    public ResponseEntity<RelationshipDTO> createRelationship(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id,
            @RequestParam RelationshipType type) {
        Person person1 = personService.getPersonEntity(person1Id);
        Person person2 = personService.getPersonEntity(person2Id);
        Relationship relationship = relationshipService.createRelationship(person1, person2, type);
        return ResponseEntity.ok(relationshipMapper.toDTO(relationship));
    }

    @DeleteMapping("/{relationshipId}")
    public ResponseEntity<Void> endRelationship(@PathVariable Long relationshipId) {
        relationshipService.endRelationship(relationshipId);
        return ResponseEntity.ok(null);
    }

    @GetMapping("/relatives")
    public ResponseEntity<List<by.backend.model.dto.PersonSummaryDTO>> findRelatives(
            @RequestParam Long personId,
            @RequestParam RelationshipType type) {
        Person person = personService.getPersonEntity(personId);
        List<Person> relatives = relationshipService.findRelatives(person, type);
        return ResponseEntity.ok(personMapper.toSummaryDTOList(relatives));
    }

    @GetMapping("/description")
    public ResponseEntity<Map<String, String>> findRelationshipDescription(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id) {
        PersonSummaryDTO person1Summary = personService.getPersonSummary(person1Id);
        PersonSummaryDTO person2Summary = personService.getPersonSummary(person2Id);
        RelationshipDescriptionResult result = relationshipService.findRelationshipDescription(person1Summary, person2Summary);
        return ResponseEntity.ok(Map.of("relationship", result.getLocalizedDescription()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<RelationshipDTO>> findAllActiveRelationships(@RequestParam Long personId) {
        Person person = personService.getPersonEntity(personId);
        List<Relationship> relationships = relationshipService.findAllActiveRelationships(person);
        return ResponseEntity.ok(relationshipMapper.toDTOList(relationships));
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> hasActiveRelationship(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id,
            @RequestParam RelationshipType type) {
        Person person1 = personService.getPersonEntity(person1Id);
        Person person2 = personService.getPersonEntity(person2Id);
        return ResponseEntity.ok(relationshipService.hasActiveRelationship(person1, person2, type));
    }

    @GetMapping("/path")
    public ResponseEntity<List<by.backend.model.dto.RelationshipStepDTO>> getRelationshipPath(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id) {
        Person person1 = personService.getPersonEntity(person1Id);
        Person person2 = personService.getPersonEntity(person2Id);
        List<by.backend.model.dto.RelationshipStepDTO> path = relationshipService.getRelationshipPath(person1, person2);
        if (path.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(path);
    }
} 