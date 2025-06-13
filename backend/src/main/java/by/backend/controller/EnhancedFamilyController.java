package by.backend.controller;

import by.backend.model.enums.TurkishFamilyRelationType;
import by.backend.service.relationship.EnhancedRelationshipService;
import by.backend.service.game.EnhancedQuestionGenerationService;
import by.backend.service.validation.FamilyRelationshipValidator;
import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.enums.Difficulty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Gelişmiş aile ilişki sistemi REST API'si
 */
@RestController
@RequestMapping("/api/enhanced-family")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}, allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class EnhancedFamilyController {

    private final EnhancedRelationshipService enhancedRelationshipService;
    private final EnhancedQuestionGenerationService enhancedQuestionService;
    private final FamilyRelationshipValidator familyValidator;

    /**
     * Türkçe aile ilişki türlerini listeler
     */
    @GetMapping("/turkish-relation-types")
    public ResponseEntity<Map<String, Object>> getTurkishRelationTypes() {
        try {
            Map<String, Object> response = new HashMap<>();
            
            // Kategorilere göre grupla
            Map<String, List<Map<String, Object>>> categorizedTypes = new HashMap<>();
            
            for (TurkishFamilyRelationType type : TurkishFamilyRelationType.values()) {
                String category = getCategoryName(type);
                
                Map<String, Object> typeInfo = Map.of(
                    "name", type.name(),
                    "turkishName", type.getTurkishName(),
                    "requiredGender", type.getRequiredGender().name(),
                    "generation", type.getGeneration().name(),
                    "generationDescription", type.getGeneration().getDescription(),
                    "isDirectFamily", type.isDirectFamily(),
                    "side", type.getSide().name(),
                    "sideDescription", type.getSide().getDescription()
                );
                
                categorizedTypes.computeIfAbsent(category, k -> new ArrayList<>()).add(typeInfo);
            }
            
            response.put("categories", categorizedTypes);
            response.put("totalTypes", TurkishFamilyRelationType.values().length);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Türkçe ilişki türleri alınırken hata oluştu", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "İlişki türleri alınamadı: " + e.getMessage()));
        }
    }

    /**
     * Türkçe aile ilişkisi oluşturur
     */
    @PostMapping("/relationships")
    public ResponseEntity<Map<String, Object>> createTurkishRelationship(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id,
            @RequestParam String relationshipType) {
        try {
            TurkishFamilyRelationType turkishType = TurkishFamilyRelationType.valueOf(relationshipType);
            
            var relationship = enhancedRelationshipService.createTurkishRelationship(
                    person1Id, person2Id, turkishType);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "message", "İlişki başarıyla oluşturuldu",
                "relationshipId", relationship.getId(),
                "turkishRelationType", turkishType.getTurkishName()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("İlişki oluşturma hatası: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        } catch (Exception e) {
            log.error("İlişki oluşturulurken beklenmeyen hata", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "İç sunucu hatası: " + e.getMessage()));
        }
    }

    /**
     * Kişinin aile üyelerini kategorilere göre getirir
     */
    @GetMapping("/family-members/{personId}")
    public ResponseEntity<Map<String, Object>> getFamilyMembers(@PathVariable Long personId) {
        try {
            var familyByCategory = enhancedRelationshipService.getFamilyMembersByCategory(personId);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "personId", personId,
                "familyMembers", familyByCategory,
                "totalCategories", familyByCategory.size(),
                "totalMembers", familyByCategory.values().stream().mapToInt(List::size).sum()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Aile üyeleri alınırken hata oluştu: personId={}", personId, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Aile üyeleri alınamadı: " + e.getMessage()));
        }
    }

    /**
     * İki kişi arasındaki aile ilişkisini analiz eder
     */
    @GetMapping("/analyze-relationship")
    public ResponseEntity<Map<String, Object>> analyzeRelationship(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id) {
        try {
            var analysis = enhancedRelationshipService.analyzeTurkishRelationship(person1Id, person2Id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("person1Id", person1Id);
            response.put("person2Id", person2Id);
            response.put("relationshipExists", analysis.isRelationshipExists());
            response.put("relationshipDescription", analysis.getRelationshipDescription());
            response.put("isDirectRelationship", analysis.isDirectRelationship());
            response.put("generationDifference", analysis.getGenerationDifference());
            response.put("ageCompatible", analysis.isAgeCompatible());
            
            if (analysis.getTurkishRelationType() != null) {
                response.put("turkishRelationType", Map.of(
                    "name", analysis.getTurkishRelationType().name(),
                    "turkishName", analysis.getTurkishRelationType().getTurkishName(),
                    "generation", analysis.getTurkishRelationType().getGeneration().getDescription()
                ));
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("İlişki analizi yapılırken hata oluştu: person1Id={}, person2Id={}", person1Id, person2Id, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "İlişki analizi yapılamadı: " + e.getMessage()));
        }
    }

    /**
     * Yaş uyumluluğunu kontrol eder
     */
    @GetMapping("/age-compatibility")
    public ResponseEntity<Map<String, Object>> checkAgeCompatibility(
            @RequestParam Long person1Id,
            @RequestParam Long person2Id,
            @RequestParam String relationshipType) {
        try {
            TurkishFamilyRelationType turkishType = TurkishFamilyRelationType.valueOf(relationshipType);
            
            var report = enhancedRelationshipService.analyzeAgeCompatibility(
                    person1Id, person2Id, turkishType);
            
            Map<String, Object> response = Map.of(
                "success", true,
                "compatible", report.isCompatible(),
                "person1Age", report.getPerson1Age(),
                "person2Age", report.getPerson2Age(),
                "ageDifference", report.getAgeDifference(),
                "recommendedMinAge", report.getRecommendedMinAge(),
                "recommendedMaxAge", report.getRecommendedMaxAge(),
                "validationMessage", report.getValidationMessage(),
                "suggestions", report.getSuggestions()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Yaş uyumluluğu kontrolü yapılırken hata oluştu", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Gelişmiş soru üretir
     */
    @GetMapping("/enhanced-question")
    public ResponseEntity<Map<String, Object>> generateEnhancedQuestion(
            @RequestParam(defaultValue = "MEDIUM") String difficulty,
            @RequestParam(defaultValue = "tr") String lang) {
        try {
            Difficulty diff = Difficulty.valueOf(difficulty.toUpperCase());
            Locale locale = new Locale(lang);
            
            GameQuestionDTO question = enhancedQuestionService.generateEnhancedQuestion(
                    diff, new HashSet<>(), locale);
            
            if (question == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "error", "Soru üretilemedi"
                ));
            }
            
            Map<String, Object> response = Map.of(
                "success", true,
                "question", question,
                "message", "Gelişmiş soru başarıyla üretildi"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Gelişmiş soru üretilirken hata oluştu: difficulty={}", difficulty, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Soru üretilemedi: " + e.getMessage()));
        }
    }

    /**
     * Sistem durumunu kontrol eder
     */
    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        try {
            Map<String, Object> status = Map.of(
                "enhancedSystem", "active",
                "turkishRelationTypes", TurkishFamilyRelationType.values().length,
                "supportedDifficulties", Difficulty.values().length,
                "timestamp", System.currentTimeMillis()
            );
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("Sistem durumu alınırken hata oluştu", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Sistem durumu alınamadı"));
        }
    }

    // Helper methods
    private String getCategoryName(TurkishFamilyRelationType type) {
        return switch (type.getGeneration()) {
            case PARENT -> "Ebeveynler";
            case CHILD -> "Çocuklar";
            case SAME -> type.isDirectFamily() ? "Kardeşler ve Eş" : "Aynı Kuşak";
            case GRANDPARENT -> "Büyükanne/Büyükbaba";
            case GRANDCHILD -> "Torunlar";
        };
    }
} 