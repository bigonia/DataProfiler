package com.dataprofiler.service;

import com.dataprofiler.dto.response.DataSourceInfoDto;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.service.impl.DataSourceServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing data source configurations
 * Handles CRUD operations, validation logic and connection testing for data sources
 * This is the unified data source management service as per architecture documentation
 */
public interface DataSourceService {

    /**
     * Create a new data source configuration
     *
     * @param dataSourceConfig the data source configuration to create
     * @return the created data source configuration
     * @throws IllegalArgumentException if data source name already exists
     */
    DataSourceConfig createDataSource(DataSourceConfig dataSourceConfig);

    /**
     * Get all active data sources
     *
     * @return list of active data source configurations
     */
    List<DataSourceConfig> getAllDataSources();

    /**
     * Get data sources by type
     *
     * @param type the data source type
     * @return list of data sources of the specified type
     */
    List<DataSourceConfig> getDataSourcesByType(DataSourceConfig.DataSourceType type);

    /**
     * Get data source by source ID
     *
     * @param sourceId the data source unique identifier
     * @return the data source configuration
     * @throws IllegalArgumentException if data source not found
     */
    DataSourceConfig getDataSourceBySourceId(String sourceId);

    /**
     * Update data source configuration
     *
     * @param sourceId the data source unique identifier to update
     * @param updatedConfig the updated configuration
     * @return the updated data source configuration
     * @throws IllegalArgumentException if data source not found or name conflict
     */
    DataSourceConfig updateDataSource(String sourceId, DataSourceConfig updatedConfig);

    /**
     * Delete a data source
     *
     * @param sourceId the data source unique identifier to delete
     */
    void deleteDataSource(Long sourceId);

    /**
     * Test connection to a data source
     *
     * @param dataSourceConfig the data source configuration to test
     * @return true if connection is successful, false otherwise
     */
    boolean testConnection(DataSourceConfig dataSourceConfig);

    /**
     * Get all schemas for a given data source.
     *
     * @param sourceId the data source unique identifier
     * @return list of schema names
     * @throws IllegalArgumentException if data source not found
     */
    List<String> getSchemas(String sourceId);

    /**
     * Get all tables for a given data source and schema.
     *
     * @param sourceId the data source unique identifier
     * @param schema   the schema name
     * @return list of table names
     * @throws IllegalArgumentException if data source not found
     */
    List<String> getTables(String sourceId, String schema);

    /**
     * Get complete data source information including all schemas and their tables.
     * This method combines schema and table retrieval for better performance.
     *
     * @param sourceId the data source unique identifier
     * @return data source information with schemas and tables
     * @throws IllegalArgumentException if data source not found
     */
    DataSourceInfoDto getDatasourceInfo(String sourceId);

    /**
     * Refresh cached data source information.
     * This method should be called after connection tests or when data structure changes.
     *
     * @param sourceId the data source unique identifier
     */
    void refreshDatasourceInfoCache(String sourceId);

}