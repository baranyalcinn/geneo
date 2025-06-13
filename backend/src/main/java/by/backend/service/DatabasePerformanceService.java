package by.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatabasePerformanceService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final CacheManager cacheManager;
    
    // Performance metrics storage
    private final Map<String, Long> queryExecutionTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> queryExecutionCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheHitCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheMissCounts = new ConcurrentHashMap<>();

    /**
     * Database performance monitoring scheduled every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void monitorDatabasePerformance() {
        try {
            log.info("=== Database Performance Monitoring ===");
            
            // Check connection pool status
            monitorConnectionPool();
            
            // Check database size and growth
            monitorDatabaseSize();
            
            // Check slow queries
            monitorSlowQueries();
            
            // Check cache performance
            monitorCachePerformance();
            
            // Check index usage
            monitorIndexUsage();
            
            log.info("=== Database Performance Monitoring Complete ===");
            
        } catch (Exception e) {
            log.error("Database performance monitoring failed", e);
        }
    }

    /**
     * Connection pool monitoring
     */
    private void monitorConnectionPool() {
        try {
            // HikariCP specific monitoring
            String sql = "SELECT " +
                        "setting AS max_connections " +
                        "FROM pg_settings " +
                        "WHERE name = 'max_connections'";
            
            Integer maxConnections = jdbcTemplate.queryForObject(sql, Integer.class);
            
            String activeSql = "SELECT count(*) FROM pg_stat_activity WHERE state = 'active'";
            Integer activeConnections = jdbcTemplate.queryForObject(activeSql, Integer.class);
            
            log.info("Connection Pool Status: Active={}, Max={}, Usage={}%", 
                    activeConnections, maxConnections, 
                    (activeConnections * 100.0 / maxConnections));
            
            if (activeConnections != null && maxConnections != null && 
                activeConnections > (maxConnections * 0.8)) {
                log.warn("High connection pool usage detected: {}%", 
                        (activeConnections * 100.0 / maxConnections));
            }
            
        } catch (Exception e) {
            log.error("Connection pool monitoring failed", e);
        }
    }

    /**
     * Database size monitoring
     */
    private void monitorDatabaseSize() {
        try {
            String sql = "SELECT " +
                        "pg_size_pretty(pg_database_size(current_database())) as db_size, " +
                        "pg_database_size(current_database()) as db_size_bytes";
            
            Map<String, Object> result = jdbcTemplate.queryForMap(sql);
            String dbSize = (String) result.get("db_size");
            Long dbSizeBytes = (Long) result.get("db_size_bytes");
            
            log.info("Database Size: {} ({} bytes)", dbSize, dbSizeBytes);
            
            // Check table sizes
            String tableSizeSql = "SELECT " +
                                 "schemaname, tablename, " +
                                 "pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size, " +
                                 "pg_total_relation_size(schemaname||'.'||tablename) as size_bytes " +
                                 "FROM pg_tables " +
                                 "WHERE schemaname = 'public' " +
                                 "ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC " +
                                 "LIMIT 5";
            
            List<Map<String, Object>> tableSizes = jdbcTemplate.queryForList(tableSizeSql);
            log.info("Top 5 largest tables:");
            for (Map<String, Object> table : tableSizes) {
                log.info("  {}: {}", table.get("tablename"), table.get("size"));
            }
            
        } catch (Exception e) {
            log.error("Database size monitoring failed", e);
        }
    }

    /**
     * Slow query monitoring
     */
    private void monitorSlowQueries() {
        try {
            String sql = "SELECT " +
                        "query, " +
                        "calls, " +
                        "total_time, " +
                        "mean_time, " +
                        "max_time " +
                        "FROM pg_stat_statements " +
                        "WHERE mean_time > 100 " + // Queries slower than 100ms
                        "ORDER BY mean_time DESC " +
                        "LIMIT 10";
            
            try {
                List<Map<String, Object>> slowQueries = jdbcTemplate.queryForList(sql);
                if (!slowQueries.isEmpty()) {
                    log.warn("Slow queries detected (>100ms average):");
                    for (Map<String, Object> query : slowQueries) {
                        log.warn("  Query: {} | Calls: {} | Avg: {}ms | Max: {}ms", 
                               truncateQuery((String) query.get("query")), 
                               query.get("calls"), 
                               Math.round((Double) query.get("mean_time")), 
                               Math.round((Double) query.get("max_time")));
                    }
                }
            } catch (Exception e) {
                log.debug("pg_stat_statements not available for slow query monitoring");
            }
            
        } catch (Exception e) {
            log.error("Slow query monitoring failed", e);
        }
    }

    /**
     * Cache performance monitoring
     */
    private void monitorCachePerformance() {
        try {
            Collection<String> cacheNames = cacheManager.getCacheNames();
            log.info("Cache Performance Analysis:");
            
            for (String cacheName : cacheNames) {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    // Basic cache info
                    log.info("  Cache '{}': Available", cacheName);
                    
                    // Try to get statistics if available (Caffeine cache)
                    try {
                        Object nativeCache = cache.getNativeCache();
                        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache) {
                            com.github.benmanes.caffeine.cache.stats.CacheStats stats = 
                                ((com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache).stats();
                            
                            log.info("    Hit Rate: {:.2f}%, Hits: {}, Misses: {}, Evictions: {}", 
                                   stats.hitRate() * 100, 
                                   stats.hitCount(), 
                                   stats.missCount(), 
                                   stats.evictionCount());
                        }
                    } catch (Exception e) {
                        log.debug("Cache statistics not available for {}", cacheName);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Cache performance monitoring failed", e);
        }
    }

    /**
     * Index usage monitoring
     */
    private void monitorIndexUsage() {
        try {
            String sql = "SELECT " +
                        "schemaname, " +
                        "relname as tablename, " +
                        "indexrelname as indexname, " +
                        "idx_tup_read, " +
                        "idx_tup_fetch " +
                        "FROM pg_stat_user_indexes " +
                        "WHERE schemaname = 'public' " +
                        "ORDER BY idx_tup_read DESC " +
                        "LIMIT 10";
            
            List<Map<String, Object>> indexStats = jdbcTemplate.queryForList(sql);
            if (!indexStats.isEmpty()) {
                log.info("Top 10 most used indexes:");
                for (Map<String, Object> index : indexStats) {
                    log.info("  {}.{}: {} reads, {} fetches", 
                           index.get("tablename"), 
                           index.get("indexname"), 
                           index.get("idx_tup_read"), 
                           index.get("idx_tup_fetch"));
                }
            }
            
            // Check for unused indexes
            String unusedIndexSql = "SELECT " +
                                   "schemaname, " +
                                   "relname as tablename, " +
                                   "indexrelname as indexname " +
                                   "FROM pg_stat_user_indexes " +
                                   "WHERE schemaname = 'public' " +
                                   "AND idx_tup_read = 0 " +
                                   "AND idx_tup_fetch = 0";
            
            List<Map<String, Object>> unusedIndexes = jdbcTemplate.queryForList(unusedIndexSql);
            if (!unusedIndexes.isEmpty()) {
                log.warn("Unused indexes detected (consider dropping):");
                for (Map<String, Object> index : unusedIndexes) {
                    log.warn("  {}.{}", index.get("tablename"), index.get("indexname"));
                }
            }
            
        } catch (Exception e) {
            log.error("Index usage monitoring failed", e);
        }
    }

    /**
     * Get comprehensive performance report
     */
    public Map<String, Object> getPerformanceReport() {
        Map<String, Object> report = new HashMap<>();
        
        try {
            // Database info
            report.put("timestamp", LocalDateTime.now());
            report.put("connectionPool", getConnectionPoolInfo());
            report.put("databaseSize", getDatabaseSizeInfo());
            report.put("tableStats", getTableStatsInfo());
            report.put("indexStats", getIndexStatsInfo());
            report.put("cacheStats", getCacheStatsInfo());
            
        } catch (Exception e) {
            log.error("Failed to generate performance report", e);
            report.put("error", e.getMessage());
        }
        
        return report;
    }

    private Map<String, Object> getConnectionPoolInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            Connection connection = dataSource.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            
            info.put("databaseProduct", metaData.getDatabaseProductName());
            info.put("databaseVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            
            connection.close();
        } catch (SQLException e) {
            log.error("Failed to get connection pool info", e);
        }
        return info;
    }

    private Map<String, Object> getDatabaseSizeInfo() {
        Map<String, Object> info = new HashMap<>();
        try {
            String sql = "SELECT pg_database_size(current_database()) as size_bytes";
            Long sizeBytes = jdbcTemplate.queryForObject(sql, Long.class);
            info.put("sizeBytes", sizeBytes);
            info.put("sizeFormatted", formatBytes(sizeBytes));
        } catch (Exception e) {
            log.error("Failed to get database size info", e);
        }
        return info;
    }

    private List<Map<String, Object>> getTableStatsInfo() {
        try {
            String sql = "SELECT " +
                        "schemaname, tablename, " +
                        "n_tup_ins as inserts, " +
                        "n_tup_upd as updates, " +
                        "n_tup_del as deletes, " +
                        "n_live_tup as live_tuples, " +
                        "n_dead_tup as dead_tuples " +
                        "FROM pg_stat_user_tables " +
                        "WHERE schemaname = 'public' " +
                        "ORDER BY n_live_tup DESC";
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Failed to get table stats info", e);
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> getIndexStatsInfo() {
        try {
            String sql = "SELECT " +
                        "schemaname, relname as tablename, indexrelname as indexname, " +
                        "idx_tup_read, idx_tup_fetch " +
                        "FROM pg_stat_user_indexes " +
                        "WHERE schemaname = 'public' " +
                        "ORDER BY idx_tup_read DESC " +
                        "LIMIT 20";
            
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            log.error("Failed to get index stats info", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> getCacheStatsInfo() {
        Map<String, Object> stats = new HashMap<>();
        Collection<String> cacheNames = cacheManager.getCacheNames();
        
        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Map<String, Object> cacheInfo = new HashMap<>();
                cacheInfo.put("name", cacheName);
                cacheInfo.put("type", cache.getClass().getSimpleName());
                stats.put(cacheName, cacheInfo);
            }
        }
        
        return stats;
    }

    // Utility methods
    private String truncateQuery(String query) {
        if (query == null) return "null";
        return query.length() > 100 ? query.substring(0, 100) + "..." : query;
    }

    private String formatBytes(Long bytes) {
        if (bytes == null) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }
} 