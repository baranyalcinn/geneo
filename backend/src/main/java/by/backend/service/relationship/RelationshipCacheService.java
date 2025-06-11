package by.backend.service.relationship;

import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.dto.RelationshipStepDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * İlişki bulma performansını artırmak için özel caching mantığı
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipCacheService {
    
    private final CacheManager cacheManager;
    
    // Sıcak cache - sıkça kullanılan kısa mesafe ilişkiler için
    private final Map<String, CachedRelationship> hotCache = new ConcurrentHashMap<>();
    
    // İlişki gücü cache - karmaşık hesaplamalar için
    private final Map<String, RelationshipStrength> strengthCache = new ConcurrentHashMap<>();
    
    // Mesafe matrisi cache - kısa yollar için
    private final Map<String, Integer> distanceCache = new ConcurrentHashMap<>();
    
    private static final int HOT_CACHE_MAX_SIZE = 10000;
    private static final int HOT_CACHE_TTL_MINUTES = 30;
    
    /**
     * İki kişi arasındaki en kısa yolu cache'den getir veya hesapla
     */
    @Cacheable(value = "shortestPaths", key = "#person1Id + '-' + #person2Id")
    public Optional<List<RelationshipStepDTO>> getShortestPath(Long person1Id, Long person2Id) {
        String key = createCacheKey(person1Id, person2Id);
        
        // Önce sıcak cache'ye bak
        CachedRelationship cached = hotCache.get(key);
        if (cached != null && !cached.isExpired()) {
            log.debug("Sıcak cache'den ilişki bulundu: {}", key);
            return Optional.of(cached.getPath());
        }
        
        return Optional.empty();
    }
    
    /**
     * İlişki gücünü cache'le - oyun zorluk seviyesi belirleme için
     */
    @Cacheable(value = "relationshipStrength", key = "#person1Id + '-' + #person2Id")
    public RelationshipStrength calculateRelationshipStrength(Long person1Id, Long person2Id, 
            List<RelationshipStepDTO> path) {
        String key = createCacheKey(person1Id, person2Id);
        
        RelationshipStrength existing = strengthCache.get(key);
        if (existing != null) {
            return existing;
        }
        
        // İlişki gücü hesaplama - yol uzunluğu, ilişki türleri, yaş farkları vs.
        RelationshipStrength strength = new RelationshipStrength();
        strength.setPathLength(path.size());
        strength.setDirectness(calculateDirectness(path));
        strength.setBloodRelation(isBloodRelation(path));
        strength.setComplexityScore(calculateComplexityScore(path));
        strength.setCalculatedAt(LocalDateTime.now());
        
        strengthCache.put(key, strength);
        return strength;
    }
    
    /**
     * Sıcak cache'e yeni ilişki ekle
     */
    public void cacheHotRelationship(Long person1Id, Long person2Id, List<RelationshipStepDTO> path, 
            RelationshipDescriptionResult result) {
        String key = createCacheKey(person1Id, person2Id);
        
        if (hotCache.size() >= HOT_CACHE_MAX_SIZE) {
            // LRU eviction - en eski kullanılanları temizle
            cleanupOldEntries();
        }
        
        CachedRelationship cached = new CachedRelationship();
        cached.setPath(path);
        cached.setResult(result);
        cached.setCachedAt(LocalDateTime.now());
        cached.setLastAccessed(LocalDateTime.now());
        
        hotCache.put(key, cached);
        log.debug("Sıcak cache'e ilişki eklendi: {}", key);
    }
    
    /**
     * Mesafe matrisi güncelle
     */
    public void updateDistanceMatrix(Long person1Id, Long person2Id, int distance) {
        String key = createCacheKey(person1Id, person2Id);
        distanceCache.put(key, distance);
        
        // Tersini de cache'le (simetrik ilişki) - metod zaten içsel olarak sıralama yapıyor
        String reverseKey = createCacheKey(person1Id, person2Id);
        distanceCache.put(reverseKey, distance);
    }
    
    /**
     * Mesafe matrisi sorgula
     */
    public Optional<Integer> getDistance(Long person1Id, Long person2Id) {
        String key = createCacheKey(person1Id, person2Id);
        Integer distance = distanceCache.get(key);
        return Optional.ofNullable(distance);
    }
    
    /**
     * Önceden hesaplanmış yolları temizle (veri güncellemelerinde)
     */
    @CacheEvict(value = {"shortestPaths", "relationshipStrength", "hotCache"}, allEntries = true)
    public void invalidateRelationshipCache(Long personId) {
        // Bu kişiyle ilgili tüm cache'leri temizle
        hotCache.entrySet().removeIf(entry -> 
            entry.getKey().contains(personId.toString()));
        distanceCache.entrySet().removeIf(entry -> 
            entry.getKey().contains(personId.toString()));
        strengthCache.entrySet().removeIf(entry -> 
            entry.getKey().contains(personId.toString()));
        
        log.info("Kişi {} için tüm ilişki cache'leri temizlendi", personId);
    }
    
    /**
     * Periyodik cache temizliği - her 30 dakikada
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void cleanupExpiredCache() {
        long startTime = System.currentTimeMillis();
        int removed = 0;
        
        // Süresi dolan hot cache'leri temizle
        Iterator<Map.Entry<String, CachedRelationship>> hotIterator = hotCache.entrySet().iterator();
        while (hotIterator.hasNext()) {
            Map.Entry<String, CachedRelationship> entry = hotIterator.next();
            if (entry.getValue().isExpired()) {
                hotIterator.remove();
                removed++;
            }
        }
        
        // Eski strength cache'leri temizle (1 saatlik TTL)
        Iterator<Map.Entry<String, RelationshipStrength>> strengthIterator = strengthCache.entrySet().iterator();
        while (strengthIterator.hasNext()) {
            Map.Entry<String, RelationshipStrength> entry = strengthIterator.next();
            if (entry.getValue().getCalculatedAt().isBefore(LocalDateTime.now().minusHours(1))) {
                strengthIterator.remove();
                removed++;
            }
        }
        
        if (removed > 0) {
            log.info("Cache temizliği tamamlandı: {} entry silindi, süre: {}ms", 
                    removed, System.currentTimeMillis() - startTime);
        }
    }
    
    /**
     * Cache istatistikleri
     */
    public CacheStatistics getCacheStatistics() {
        CacheStatistics stats = new CacheStatistics();
        stats.setHotCacheSize(hotCache.size());
        stats.setStrengthCacheSize(strengthCache.size());
        stats.setDistanceCacheSize(distanceCache.size());
        
        // Hit/Miss oranları hesapla
        Cache shortestPathsCache = cacheManager.getCache("shortestPaths");
        if (shortestPathsCache != null) {
            // Cache provider'a göre istatistik alma yöntemi değişebilir
            stats.setShortestPathsCacheSize(estimateCacheSize(shortestPathsCache));
        }
        
        return stats;
    }
    
    // Yardımcı metodlar
    private String createCacheKey(Long person1Id, Long person2Id) {
        // Deterministik key oluştur (sıra önemli değil)
        return person1Id.compareTo(person2Id) < 0 ? 
                person1Id + "-" + person2Id : 
                person2Id + "-" + person1Id;
    }
    
    private void cleanupOldEntries() {
        // En az kullanılan %20'sini temizle
        int toRemove = (int) (hotCache.size() * 0.2);
        
        hotCache.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                        Comparator.comparing(CachedRelationship::getLastAccessed)))
                .limit(toRemove)
                .map(Map.Entry::getKey)
                .forEach(hotCache::remove);
    }
    
    private double calculateDirectness(List<RelationshipStepDTO> path) {
        if (path.isEmpty()) return 0.0;
        
        // Doğrudan ilişki türlerinin yoğunluğu
        long directRelations = path.stream()
                .mapToLong(step -> isDirectRelation(step.getRelationshipTypeName()) ? 1 : 0)
                .sum();
        
        return (double) directRelations / path.size();
    }
    
    private boolean isBloodRelation(List<RelationshipStepDTO> path) {
        // Kan bağı kontrolü - evlilik hariç diğer ilişkiler
        return path.stream()
                .noneMatch(step -> "SPOUSE".equals(step.getRelationshipTypeName()));
    }
    
    private double calculateComplexityScore(List<RelationshipStepDTO> path) {
        // Karmaşıklık skoru: yol uzunluğu + ilişki türü çeşitliliği + yaş faktörü
        double baseScore = path.size();
        
        // İlişki türü çeşitliliği
        Set<String> uniqueTypes = path.stream()
                .map(RelationshipStepDTO::getRelationshipTypeName)
                .collect(Collectors.toSet());
        double diversityBonus = uniqueTypes.size() * 0.5;
        
        // Yaş farkı faktörü (varsa)
        double ageFactor = calculateAgeFactor(path);
        
        return baseScore + diversityBonus + ageFactor;
    }
    
    private double calculateAgeFactor(List<RelationshipStepDTO> path) {
        // Yaş farklılıklarına göre karmaşıklık artışı
        return path.stream()
                .filter(step -> step.getPersonBirthYear() != null)
                .mapToDouble(step -> {
                    int age = LocalDateTime.now().getYear() - step.getPersonBirthYear();
                    // Yaşlı/genç bonus hesaplama
                    if (age > 80) {
                        return 0.3;
                    } else if (age < 20) {
                        return 0.2;
                    } else {
                        return 0.1;
                    }
                })
                .sum();
    }
    
    private boolean isDirectRelation(String relationType) {
        return Arrays.asList("PARENT_CHILD", "SPOUSE", "SIBLING").contains(relationType);
    }
    
    private int estimateCacheSize(Cache cache) {
        // Cache implementasyonuna göre boyut tahmini
        try {
            return cache.getNativeCache().toString().split(",").length;
        } catch (Exception _) {
            return -1; // Bilinmeyen
        }
    }
    
    // İç sınıflar
    public static class CachedRelationship {
        private List<RelationshipStepDTO> path;
        private RelationshipDescriptionResult result;
        private LocalDateTime cachedAt;
        private LocalDateTime lastAccessed;
        
        public boolean isExpired() {
            return cachedAt.isBefore(LocalDateTime.now().minusMinutes(HOT_CACHE_TTL_MINUTES));
        }
        
        // Getters and setters
        public List<RelationshipStepDTO> getPath() { return path; }
        public void setPath(List<RelationshipStepDTO> path) { this.path = path; }
        public RelationshipDescriptionResult getResult() { return result; }
        public void setResult(RelationshipDescriptionResult result) { this.result = result; }
        public LocalDateTime getCachedAt() { return cachedAt; }
        public void setCachedAt(LocalDateTime cachedAt) { this.cachedAt = cachedAt; }
        public LocalDateTime getLastAccessed() { return lastAccessed; }
        public void setLastAccessed(LocalDateTime lastAccessed) { this.lastAccessed = lastAccessed; }
    }
    
    public static class RelationshipStrength {
        private int pathLength;
        private double directness;
        private boolean bloodRelation;
        private double complexityScore;
        private LocalDateTime calculatedAt;
        
        // Getters and setters
        public int getPathLength() { return pathLength; }
        public void setPathLength(int pathLength) { this.pathLength = pathLength; }
        public double getDirectness() { return directness; }
        public void setDirectness(double directness) { this.directness = directness; }
        public boolean isBloodRelation() { return bloodRelation; }
        public void setBloodRelation(boolean bloodRelation) { this.bloodRelation = bloodRelation; }
        public double getComplexityScore() { return complexityScore; }
        public void setComplexityScore(double complexityScore) { this.complexityScore = complexityScore; }
        public LocalDateTime getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
    }
    
    public static class CacheStatistics {
        private int hotCacheSize;
        private int strengthCacheSize;
        private int distanceCacheSize;
        private int shortestPathsCacheSize;
        
        // Getters and setters
        public int getHotCacheSize() { return hotCacheSize; }
        public void setHotCacheSize(int hotCacheSize) { this.hotCacheSize = hotCacheSize; }
        public int getStrengthCacheSize() { return strengthCacheSize; }
        public void setStrengthCacheSize(int strengthCacheSize) { this.strengthCacheSize = strengthCacheSize; }
        public int getDistanceCacheSize() { return distanceCacheSize; }
        public void setDistanceCacheSize(int distanceCacheSize) { this.distanceCacheSize = distanceCacheSize; }
        public int getShortestPathsCacheSize() { return shortestPathsCacheSize; }
        public void setShortestPathsCacheSize(int shortestPathsCacheSize) { this.shortestPathsCacheSize = shortestPathsCacheSize; }
    }
} 