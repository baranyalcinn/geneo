package by.backend.controller;

import by.backend.service.DatabasePerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/database-performance")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Database Performance", description = "Database performance monitoring ve optimization endpoints")
@WebEndpoint(id = "database-performance")
public class DatabasePerformanceController {

    private final DatabasePerformanceService databasePerformanceService;
    private final CacheManager cacheManager;

    @GetMapping("/report")
    @Operation(summary = "Comprehensive database performance report alır", 
              description = "Database performance, cache statistics ve system metrics'lerini döndürür")
    @ReadOperation
    public ResponseEntity<Map<String, Object>> getPerformanceReport() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> report = databasePerformanceService.getPerformanceReport();
            
            long executionTime = System.currentTimeMillis() - startTime;
            report.put("reportGenerationTime", executionTime + "ms");
            
            log.info("Database performance report generated successfully in {}ms", executionTime);
            
            return ResponseEntity.ok(report);
            
        } catch (Exception e) {
            log.error("Database performance report generation failed", e);
            Map<String, Object> errorReport = new HashMap<>();
            errorReport.put("error", "Report generation failed: " + e.getMessage());
            errorReport.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.internalServerError().body(errorReport);
        }
    }

    @GetMapping("/cache-stats")
    @Operation(summary = "Cache performance statistics alır",
              description = "Tüm cache'lerin hit/miss oranları ve performance metrics'lerini döndürür")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            Map<String, Object> cacheStats = new HashMap<>();
            
            cacheManager.getCacheNames().forEach(cacheName -> {
                try {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        Map<String, Object> stats = new HashMap<>();
                        stats.put("name", cacheName);
                        stats.put("type", cache.getClass().getSimpleName());
                        
                        // Caffeine cache statistics
                        Object nativeCache = cache.getNativeCache();
                        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                            var caffeineStats = ((com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache).stats();
                            stats.put("hitCount", caffeineStats.hitCount());
                            stats.put("missCount", caffeineStats.missCount());
                            stats.put("hitRate", String.format("%.2f%%", caffeineStats.hitRate() * 100));
                            stats.put("evictionCount", caffeineStats.evictionCount());
                            stats.put("loadTime", caffeineStats.averageLoadPenalty() / 1_000_000.0 + "ms");
                        }
                        
                        cacheStats.put(cacheName, stats);
                    }
                } catch (Exception e) {
                    log.warn("Cache statistics alınamadı for {}: {}", cacheName, e.getMessage());
                }
            });
            
            return ResponseEntity.ok(cacheStats);
            
        } catch (Exception e) {
            log.error("Cache statistics alınamadı", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Cache statistics alınamadı: " + e.getMessage()));
        }
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Tüm cache'leri temizler",
              description = "Performance optimization için tüm cache'leri temizler")
    public ResponseEntity<Map<String, Object>> clearAllCaches() {
        try {
            long startTime = System.currentTimeMillis();
            int clearedCaches = 0;
            
            for (String cacheName : cacheManager.getCacheNames()) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    clearedCaches++;
                    log.info("Cache temizlendi: {}", cacheName);
                }
            }
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Tüm cache'ler başarıyla temizlendi");
            response.put("clearedCacheCount", clearedCaches);
            response.put("executionTime", executionTime + "ms");
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("{} cache temizlendi in {}ms", clearedCaches, executionTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Cache temizleme işlemi başarısız", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Cache temizleme başarısız: " + e.getMessage()));
        }
    }

    @PostMapping("/cache/clear/{cacheName}")
    @Operation(summary = "Belirli bir cache'i temizler",
              description = "Sadece belirtilen cache'i temizler")
    public ResponseEntity<Map<String, Object>> clearSpecificCache(@PathVariable String cacheName) {
        try {
            var cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cache bulunamadı: " + cacheName));
            }
            
            long startTime = System.currentTimeMillis();
            cache.clear();
            long executionTime = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Cache başarıyla temizlendi");
            response.put("cacheName", cacheName);
            response.put("executionTime", executionTime + "ms");
            response.put("timestamp", System.currentTimeMillis());
            
            log.info("Cache temizlendi: {} in {}ms", cacheName, executionTime);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Cache temizleme başarısız for {}", cacheName, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Cache temizleme başarısız: " + e.getMessage()));
        }
    }

    @GetMapping("/health-check")
    @Operation(summary = "Database health check yapar",
              description = "Database bağlantısı ve temel performance metrics'leri kontrol eder")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> healthReport = new HashMap<>();
            healthReport.put("status", "UP");
            healthReport.put("timestamp", System.currentTimeMillis());
            
            // Test database connectivity
            Map<String, Object> dbHealth = new HashMap<>();
            try {
                Map<String, Object> performanceData = databasePerformanceService.getPerformanceReport();
                dbHealth.put("status", "UP");
                dbHealth.put("connectionPool", performanceData.get("connectionPool"));
                
            } catch (Exception e) {
                dbHealth.put("status", "DOWN");
                dbHealth.put("error", e.getMessage());
                healthReport.put("status", "DEGRADED");
            }
            
            healthReport.put("database", dbHealth);
            
            // Cache health
            Map<String, Object> cacheHealth = new HashMap<>();
            try {
                cacheHealth.put("status", "UP");
                cacheHealth.put("availableCaches", cacheManager.getCacheNames().size());
            } catch (Exception e) {
                cacheHealth.put("status", "DOWN");
                cacheHealth.put("error", e.getMessage());
            }
            
            healthReport.put("cache", cacheHealth);
            
            long executionTime = System.currentTimeMillis() - startTime;
            healthReport.put("checkDuration", executionTime + "ms");
            
            return ResponseEntity.ok(healthReport);
            
        } catch (Exception e) {
            log.error("Health check başarısız", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "DOWN");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/metrics")
    @Operation(summary = "Database performance metrics alır",
              description = "Real-time database performance metrics ve statistics döndürür")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // JVM metrics
            Runtime runtime = Runtime.getRuntime();
            Map<String, Object> jvmMetrics = new HashMap<>();
            jvmMetrics.put("totalMemory", formatBytes(runtime.totalMemory()));
            jvmMetrics.put("freeMemory", formatBytes(runtime.freeMemory()));
            jvmMetrics.put("usedMemory", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
            jvmMetrics.put("maxMemory", formatBytes(runtime.maxMemory()));
            jvmMetrics.put("memoryUsagePercentage", 
                String.format("%.2f%%", 
                    ((runtime.totalMemory() - runtime.freeMemory()) * 100.0) / runtime.maxMemory()));
            
            metrics.put("jvm", jvmMetrics);
            metrics.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(metrics);
            
        } catch (Exception e) {
            log.error("Metrics alınamadı", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Metrics alınamadı: " + e.getMessage()));
        }
    }

    @PostMapping("/optimize")
    @Operation(summary = "Database optimization işlemi başlatır",
              description = "Cache temizleme ve performance tuning işlemlerini çalıştırır")
    public ResponseEntity<Map<String, Object>> optimizeDatabase() {
        try {
            long startTime = System.currentTimeMillis();
            
            Map<String, Object> optimizationResults = new HashMap<>();
            
            // Clear all caches
            int clearedCaches = 0;
            for (String cacheName : cacheManager.getCacheNames()) {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                    clearedCaches++;
                }
            }
            
            // Force garbage collection
            System.gc();
            
            // Run performance analysis
            databasePerformanceService.monitorDatabasePerformance();
            
            long executionTime = System.currentTimeMillis() - startTime;
            
            optimizationResults.put("message", "Database optimization tamamlandı");
            optimizationResults.put("clearedCaches", clearedCaches);
            optimizationResults.put("garbageCollectionTriggered", true);
            optimizationResults.put("performanceAnalysisCompleted", true);
            optimizationResults.put("executionTime", executionTime + "ms");
            optimizationResults.put("timestamp", System.currentTimeMillis());
            
            log.info("Database optimization tamamlandı in {}ms", executionTime);
            
            return ResponseEntity.ok(optimizationResults);
            
        } catch (Exception e) {
            log.error("Database optimization başarısız", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Database optimization başarısız: " + e.getMessage()));
        }
    }

    // Utility method
    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
} 