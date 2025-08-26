package com.dataprofiler.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration for data source information
 * Enables caching for schema and table metadata to improve performance
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String DATASOURCE_INFO_CACHE = "datasourceInfo";
    public static final String DATASOURCE_SCHEMAS_CACHE = "datasourceSchemas";
    public static final String DATASOURCE_TABLES_CACHE = "datasourceTables";

    /**
     * Configure cache manager with predefined cache names
     * Using ConcurrentMapCacheManager for simplicity
     * In production, consider using Redis or other distributed cache solutions
     */
    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            DATASOURCE_INFO_CACHE,
            DATASOURCE_SCHEMAS_CACHE,
            DATASOURCE_TABLES_CACHE
        );
        
        // Allow dynamic cache creation
        cacheManager.setAllowNullValues(false);
        
        return cacheManager;
    }
}