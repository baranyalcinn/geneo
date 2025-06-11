package by.backend.service.cache;

import by.backend.model.dto.RelationshipDescriptionResult;
import by.backend.model.entity.Person;
import by.backend.model.entity.Relationship;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * High-performance cache for relationship calculations
 * Implements memoization pattern for O(1) lookup after first calculation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RelationshipCache {
    
    // Main relationship result cache
    private final Cache<String, RelationshipDescriptionResult> resultCache = 
        Caffeine.newBuilder()
            .maximumSize(50_000)
            .expireAfterWrite(Duration.ofMinutes(10))
            .recordStats()
            .build();
    
    // Path cache for complex relationships
    private final Cache<String, List<Relationship>> pathCache = 
        Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();
    
    // Ancestor cache for blood relation checks
    private final Cache<Long, List<Person>> ancestorCache = 
        Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build();
    
    // In-progress computation tracking to prevent duplicate work
    private final ConcurrentMap<String, CompletableFuture<RelationshipDescriptionResult>> 
        inProgressComputations = new ConcurrentHashMap<>();

    /**
     * Get relationship with caching - O(1) lookup after first computation
     */
    public RelationshipDescriptionResult findRelationship(Long person1Id, Long person2Id,
                                                         RelationshipComputer computer) {
        String cacheKey = createRelationshipKey(person1Id, person2Id);
        
        // Try cache first
        RelationshipDescriptionResult cached = resultCache.getIfPresent(cacheKey);
        if (cached != null) {
            log.debug("Cache HIT for relationship: {} -> {}", person1Id, person2Id);
            return cached;
        }
        
        // Check if computation is already in progress
        CompletableFuture<RelationshipDescriptionResult> inProgress = 
            inProgressComputations.get(cacheKey);
        if (inProgress != null) {
            try {
                return inProgress.get();
            } catch (Exception e) {
                log.warn("Error waiting for in-progress computation: {}", e.getMessage());
                inProgressComputations.remove(cacheKey);
            }
        }
        
        // Start new computation
        CompletableFuture<RelationshipDescriptionResult> future = CompletableFuture.supplyAsync(() -> {
            try {
                RelationshipDescriptionResult result = computer.compute(person1Id, person2Id);
                resultCache.put(cacheKey, result);
                log.debug("Cache MISS - computed and cached: {} -> {}", person1Id, person2Id);
                return result;
            } finally {
                inProgressComputations.remove(cacheKey);
            }
        });
        
        inProgressComputations.put(cacheKey, future);
        
        try {
            return future.get();
        } catch (Exception e) {
            log.error("Error computing relationship: {}", e.getMessage());
            inProgressComputations.remove(cacheKey);
            throw new RuntimeException("Failed to compute relationship", e);
        }
    }
    
    /**
     * Cache relationship path
     */
    public List<Relationship> findPath(Long person1Id, Long person2Id, PathComputer computer) {
        String pathKey = createPathKey(person1Id, person2Id);
        
        return pathCache.get(pathKey, key -> {
            log.debug("Computing path: {} -> {}", person1Id, person2Id);
            return computer.computePath(person1Id, person2Id);
        });
    }
    
    /**
     * Cache ancestors for blood relation checks
     */
    public List<Person> getAncestors(Long personId, AncestorComputer computer) {
        return ancestorCache.get(personId, id -> {
            log.debug("Computing ancestors for person: {}", personId);
            return computer.computeAncestors(id);
        });
    }
    
    /**
     * Invalidate cache entries for a person (when relationships change)
     */
    public void invalidatePerson(Long personId) {
        // Remove all cache entries involving this person
        resultCache.asMap().entrySet().removeIf(entry -> 
            entry.getKey().contains(personId.toString()));
        pathCache.asMap().entrySet().removeIf(entry -> 
            entry.getKey().contains(personId.toString()));
        ancestorCache.invalidate(personId);
        
        log.info("Invalidated cache for person: {}", personId);
    }
    
    /**
     * Invalidate all caches (when major data changes occur)
     */
    public void invalidateAll() {
        resultCache.invalidateAll();
        pathCache.invalidateAll();
        ancestorCache.invalidateAll();
        inProgressComputations.clear();
        
        log.info("Invalidated all relationship caches");
    }
    
    // Helper methods
    private String createRelationshipKey(Long p1Id, Long p2Id) {
        // Always put smaller ID first for consistent caching
        long small = Math.min(p1Id, p2Id);
        long large = Math.max(p1Id, p2Id);
        return "rel:" + small + ":" + large;
    }
    
    private String createPathKey(Long p1Id, Long p2Id) {
        // Path direction matters, so don't swap IDs
        return "path:" + p1Id + ":" + p2Id;
    }
    
    // Functional interfaces for computations
    @FunctionalInterface
    public interface RelationshipComputer {
        RelationshipDescriptionResult compute(Long person1Id, Long person2Id);
    }
    
    @FunctionalInterface
    public interface PathComputer {
        List<Relationship> computePath(Long person1Id, Long person2Id);
    }
    
    @FunctionalInterface
    public interface AncestorComputer {
        List<Person> computeAncestors(Long personId);
    }
} 