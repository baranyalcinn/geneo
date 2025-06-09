package by.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * High-performance cache configuration for family tree operations
 * Optimizes relationship calculations from O(V!) to O(1) after preprocessing
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Primary cache manager for relationship operations
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // High-performance cache configuration
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(50_000)                    // Large cache for family data
            .expireAfterWrite(Duration.ofMinutes(10)) // Reasonable TTL
            .expireAfterAccess(Duration.ofMinutes(5)) // LRU eviction
            .recordStats()                          // Enable monitoring
            .removalListener((key, value, cause) -> {
                // Optional: Log cache evictions for monitoring
                if (cause.wasEvicted()) {
                    System.out.println("Cache eviction: " + key + " - " + cause);
                }
            }));
        
        return cacheManager;
    }
    
    /**
     * Specialized cache for relationship paths
     */
    @Bean("pathCacheManager")
    public CacheManager pathCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(20_000)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats());
        
        return cacheManager;
    }
    
    /**
     * Long-term cache for family graph structure
     */
    @Bean("graphCacheManager")
    public CacheManager graphCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(100_000)                   // Large graph cache
            .expireAfterWrite(Duration.ofMinutes(15)) // Longer TTL for graph
            .recordStats());
        
        return cacheManager;
    }
    
    /**
     * Game-specific cache for questions and analysis
     */
    @Bean("gameCacheManager")
    public CacheManager gameCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
            .maximumSize(5_000)
            .expireAfterWrite(Duration.ofMinutes(30)) // Game session duration
            .recordStats());
        
        return cacheManager;
    }
} 