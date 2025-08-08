package com.dataprofiler.service;

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
    void deleteDataSource(String sourceId);

    /**
     * Test connection to a data source
     *
     * @param dataSourceConfig the data source configuration to test
     * @return true if connection is successful, false otherwise
     */
    boolean testConnection(DataSourceConfig dataSourceConfig);

}