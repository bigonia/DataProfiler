package com.dataprofiler.profiler;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.entity.DataSourceConfig;

import java.util.List;
import java.util.Map;

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

    /**
     * Get database metadata including schemas and tables
     * This is a lightweight operation focused on metadata retrieval only
     * 
     * @param dataSourceConfig The data source configuration
     * @return Map of schema names to their table lists
     * @throws Exception if metadata retrieval fails
     */
    Map<String, List<String>> getDatabaseMetadata(DataSourceConfig dataSourceConfig) throws Exception;

    /**
     * Get list of schemas for the data source
     * 
     * @param dataSourceConfig The data source configuration
     * @return List of schema names
     * @throws Exception if schema retrieval fails
     */
    List<String> getSchemas(DataSourceConfig dataSourceConfig) throws Exception;

    /**
     * Get list of tables for a specific schema
     * 
     * @param dataSourceConfig The data source configuration
     * @param schema The schema name
     * @return List of table names in the schema
     * @throws Exception if table retrieval fails
     */
    List<String> getTables(DataSourceConfig dataSourceConfig, String schema) throws Exception;

}