package by.backend.controller;

import by.backend.service.family.FamilyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/family")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @GetMapping("/relationship")
    public ResponseEntity<?> getRelationship(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id) {
        try {
            // İki kişi arasındaki ilişkiyi ve ortak atayı bul
            Map<String, Object> relationshipData = familyService.findRelationshipBetween(Long.valueOf(person1Id), Long.valueOf(person2Id));
            return ResponseEntity.ok(relationshipData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("İlişki bilgisi alınamadı: " + e.getMessage());
        }
    }
} 