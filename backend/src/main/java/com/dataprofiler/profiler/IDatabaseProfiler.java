package com.dataprofiler.profiler;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.entity.DataSourceConfig;

/**
 * Unified interface for database profilers
 * Each database type (MySQL, PostgreSQL, SQLite, etc.) should implement this interface
 */
public interface IDatabaseProfiler {

    /**
     * Profile a data source and return raw profiling data
     * This method implements the adaptive profiling strategy internally
     * 
     * @param dataSourceConfig The data source configuration
     * @param scope The profiling scope (schemas, tables, etc.)
     * @return Raw profiling data containing metadata and basic metrics
     * @throws Exception if profiling fails
     */
    RawProfileDataDto profile(DataSourceConfig dataSourceConfig, ProfilingTaskRequest.DataSourceScope scope) throws Exception;

    /**
     * Test connection to the data source
     * 
     * @param dataSourceConfig The data source configuration
     * @return true if connection is successful, false otherwise
     */
    boolean testConnection(DataSourceConfig dataSourceConfig);

    /**
     * Get supported data source type
     * 
     * @return The data source type this profiler supports
     */
    String getSupportedType();

    /**
     * Check if this profiler supports the given data source type
     * 
     * @param dataSourceType The data source type to check
     * @return true if supported, false otherwise
     */
    boolean supports(String dataSourceType);

}