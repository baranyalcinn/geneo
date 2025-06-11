package by.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async configuration for parallel relationship processing
 * Enables concurrent pathfinding and relationship calculations
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    /**
     * Task executor for relationship calculations
     * Optimized for CPU-intensive family tree operations
     */
    @Bean("relationshipTaskExecutor")
    public Executor relationshipTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // CPU-optimized thread pool
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 2);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("RelationshipCalc-");
        executor.setKeepAliveSeconds(60);
        
        // Rejection policy for overload protection
        executor.setRejectedExecutionHandler((r, executor1) -> {
            System.err.println("Relationship calculation task rejected: " + r.toString());
            // Fallback: run in calling thread
            r.run();
        });
        
        executor.initialize();
        return executor;
    }
    
    /**
     * Task executor for graph operations
     * Optimized for graph preprocessing and cache warming
     */
    @Bean("graphTaskExecutor")
    public Executor graphTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Graph operations can be more memory intensive
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("GraphOps-");
        executor.setKeepAliveSeconds(120);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * Task executor for game operations
     * Optimized for game question generation and analysis
     */
    @Bean("gameTaskExecutor")
    public Executor gameTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("GameOps-");
        executor.setKeepAliveSeconds(60);
        
        executor.initialize();
        return executor;
    }
} 