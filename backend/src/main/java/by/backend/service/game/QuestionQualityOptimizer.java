package by.backend.service.game;

import by.backend.model.dto.GameQuestionDTO;
import by.backend.model.entity.Person;
import by.backend.model.enums.Difficulty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Soru kalitesi optimizasyonu ve akıllı soru filtreleme
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionQualityOptimizer {
    
    /**
     * İlişki çeşitliliğini artır
     */
    public List<GameQuestionDTO> optimizeQuestionDiversity(List<GameQuestionDTO> questions, 
                                                          Set<String> recentQuestionTypes) {
        // Son sorularda çıkan tipleri deprioritize et
        return questions.stream()
                .sorted((q1, q2) -> {
                    String type1 = extractRelationshipType(q1);
                    String type2 = extractRelationshipType(q2);
                    
                    boolean q1Recent = recentQuestionTypes.contains(type1);
                    boolean q2Recent = recentQuestionTypes.contains(type2);
                    
                    if (q1Recent && !q2Recent) return 1;
                    if (!q1Recent && q2Recent) return -1;
                    return 0;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Yaş dengesi optimizasyonu
     */
    public double calculateAgeBalanceScore(Person p1, Person p2) {
        if (p1.getBirthDate() == null || p2.getBirthDate() == null) {
            return 0.5; // Neutral score for missing data
        }
        
        int age1 = java.time.LocalDate.now().getYear() - p1.getBirthDate().getYear();
        int age2 = java.time.LocalDate.now().getYear() - p2.getBirthDate().getYear();
        
        int ageDiff = Math.abs(age1 - age2);
        
        // Optimal yaş farkları (biyolojik gerçeklik)
        if (ageDiff <= 5) return 0.9; // Kardeş/kuzen
        if (ageDiff <= 30 && ageDiff >= 15) return 1.0; // Anne-baba/çocuk
        if (ageDiff <= 60 && ageDiff >= 40) return 0.8; // Büyükanne-torun
        
        return Math.max(0.1, 1.0 - (ageDiff / 100.0));
    }
    
    /**
     * Coğrafi/kültürel uyumluluk
     */
    public double calculateCulturalRelevance(Person p1, Person p2) {
        // İsim analizine dayalı kültürel yakınlık
        String surname1 = p1.getLastName() != null ? p1.getLastName().toLowerCase() : "";
        String surname2 = p2.getLastName() != null ? p2.getLastName().toLowerCase() : "";
        
        // Aynı soyisim bonus
        if (surname1.equals(surname2) && !surname1.isEmpty()) {
            return 1.0;
        }
        
        // Benzer kökenli isimler (Türkçe isimlerde)
        if (isTurkishName(p1.getFirstName()) && isTurkishName(p2.getFirstName())) {
            return 0.8;
        }
        
        return 0.6; // Default cultural relevance
    }
    
    private boolean isTurkishName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        // Basit Türkçe isim kontrolü
        return lower.contains("ç") || lower.contains("ğ") || lower.contains("ı") || 
               lower.contains("ö") || lower.contains("ş") || lower.contains("ü") ||
               Arrays.asList("ahmet", "mehmet", "ali", "fatma", "ayşe", "zeynep", 
                           "mustafa", "ibrahim", "osman", "hasan", "huseyin").contains(lower);
    }
    
    private String extractRelationshipType(GameQuestionDTO question) {
        // Basit relationship type extraction
        String id = question.getId().toLowerCase();
        if (id.contains("parent") || id.contains("child")) return "parent_child";
        if (id.contains("sibling")) return "sibling";
        if (id.contains("spouse")) return "spouse";
        if (id.contains("uncle") || id.contains("aunt")) return "aunt_uncle";
        if (id.contains("cousin")) return "cousin";
        return "complex";
    }
} 