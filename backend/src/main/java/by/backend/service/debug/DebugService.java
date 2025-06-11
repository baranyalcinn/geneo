package by.backend.service.debug;

import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.enums.Difficulty;
import by.backend.model.enums.RelationshipStatus;
import by.backend.repository.PersonRepository;
import by.backend.repository.RelationshipRepository;
import by.backend.service.relationship.RelationshipService;
import by.backend.mapper.PersonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class DebugService {

    private final PersonRepository personRepository;
    private final RelationshipRepository relationshipRepository;
    private final RelationshipService relationshipService;
    private final PersonMapper personMapper;

    @Transactional(readOnly = true)
    public void debugDatabaseState() {
        log.info("=== DEBUG: Veritabanı Durumu ===");
        
        try {
            // 1. Kişi sayısı
            List<Person> allPersons = personRepository.findAll();
            log.info("Toplam kişi sayısı: {}", allPersons.size());
            
            if (allPersons.isEmpty()) {
                log.error("VERİTABANINDA HİÇ KİŞİ YOK! Bu soru üretme sorunun ana nedeni.");
                return;
            }
            
            // İlk 5 kişiyi listele
            log.info("İlk 5 kişi:");
            allPersons.stream().limit(5).forEach(p -> 
                log.info("  - ID: {}, Ad: {} {}, Doğum: {}, Ölüm: {}", 
                    p.getId(), p.getFirstName(), p.getLastName(), p.getBirthDate(), p.getDeathDate())
            );
            
            // 2. İlişki sayısı
            List<Relationship> allRelationships = relationshipRepository.findAll();
            log.info("Toplam ilişki sayısı: {}", allRelationships.size());
            
            if (allRelationships.isEmpty()) {
                log.error("VERİTABANINDA HİÇ İLİŞKİ YOK! Bu da soru üretme sorunun nedeni olabilir.");
                return;
            }
            
            // İlişki türlerine göre dağılım
            allRelationships.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    r -> r.getType(), 
                    java.util.stream.Collectors.counting()))
                .forEach((type, count) -> log.info("  {} türünde {} ilişki", type, count));
            
            // 3. Test soru üretimi
            log.info("=== DEBUG: Test Soru Üretimi ===");
            debugQuestionGeneration(allPersons);
            
        } catch (Exception e) {
            log.error("Debug sırasında hata: {}", e.getMessage(), e);
        }
    }
    
    private void debugQuestionGeneration(List<Person> persons) {
        Random random = new Random();
        int attempts = 10;
        int foundRelationships = 0;
        int validQuestions = 0;
        
        for (int i = 0; i < attempts; i++) {
            Person p1 = persons.get(random.nextInt(persons.size()));
            Person p2 = persons.get(random.nextInt(persons.size()));
            
            if (p1.getId().equals(p2.getId())) {
                continue;
            }
            
            log.info("Test #{}: {} (ID:{}) -> {} (ID:{})", 
                i+1, p1.getFirstName(), p1.getId(), p2.getFirstName(), p2.getId());
            
            try {
                RelationshipDescriptionResult result = relationshipService.findRelationshipDescription(
                    personMapper.toSummaryDTO(p1), personMapper.toSummaryDTO(p2));
                
                if (result != null) {
                    log.info("  Sonuç: Status={}, MessageKey={}, DirectType={}", 
                        result.getStatus(), result.getMessageKey(), result.getDirectTypeIfApplicable());
                    
                    if (result.getStatus() == RelationshipStatus.FOUND) {
                        foundRelationships++;
                        log.info("  ✓ Geçerli ilişki bulundu!");
                        
                        if (isSimpleRelationship(result.getMessageKey())) {
                            validQuestions++;
                            log.info("  ✓✓ Soru olarak kullanılabilir!");
                        } else {
                            log.info("  ⚠ Karmaşık ilişki, soru için uygun olmayabilir.");
                        }
                    } else {
                        log.info("  ✗ İlişki bulunamadı");
                    }
                } else {
                    log.info("  ✗ Null sonuç");
                }
            } catch (Exception e) {
                log.error("  ✗ Hata: {}", e.getMessage());
            }
        }
        
        log.info("=== DEBUG SONUÇ ===");
        log.info("Test edilen çift sayısı: {}", attempts);
        log.info("Bulunan ilişki sayısı: {}", foundRelationships);
        log.info("Geçerli soru sayısı: {}", validQuestions);
        
        if (foundRelationships == 0) {
            log.error("HİÇ İLİŞKİ BULUNAMADI! RelationshipService veya FamilyGraphService sorunu olabilir.");
        } else if (validQuestions == 0) {
            log.error("İLİŞKİLER VAR AMA SORU OLARAK UYGUN DEĞİL! Zorluk filtreleri çok katı olabilir.");
        } else {
            log.info("Sistemde {} adet geçerli ilişki var, soru üretme sorunu başka bir yerde.", validQuestions);
        }
    }
    
    private boolean isSimpleRelationship(String messageKey) {
        if (messageKey == null) return false;
        String lower = messageKey.toLowerCase();
        return lower.contains("parent") || lower.contains("child") || 
               lower.contains("sibling") || lower.contains("spouse") ||
               lower.contains("grandparent") || lower.contains("grandchild");
    }
} 