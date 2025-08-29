package com.dataprofiler.service.impl;

import com.dataprofiler.config.CacheConfig;
import com.dataprofiler.dto.ConnectionTestResult;
import com.dataprofiler.dto.response.DataSourceInfoDto;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.profiler.IDatabaseProfiler;
import com.dataprofiler.repository.DataSourceConfigRepository;
import com.dataprofiler.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DataSourceService interface
 * Provides CRUD operations and connection testing for data source configurations
 */
@Service
@Transactional
public class DataSourceServiceImpl implements DataSourceService {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceServiceImpl.class);

    // Connection timeout in seconds
    private static final int CONNECTION_TIMEOUT = 5;

    // Database driver mappings
    private static final Map<DataSourceConfig.DataSourceType, String> DRIVER_MAPPINGS = new HashMap<>();
    private static final Map<DataSourceConfig.DataSourceType, String> URL_TEMPLATES = new HashMap<>();
    private static final Map<DataSourceConfig.DataSourceType, String> TEST_QUERIES = new HashMap<>();

    static {
        // Initialize driver mappings
        DRIVER_MAPPINGS.put(DataSourceConfig.DataSourceType.MYSQL, "com.mysql.cj.jdbc.Driver");
        DRIVER_MAPPINGS.put(DataSourceConfig.DataSourceType.POSTGRESQL, "org.postgresql.Driver");
        DRIVER_MAPPINGS.put(DataSourceConfig.DataSourceType.SQLSERVER, "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DRIVER_MAPPINGS.put(DataSourceConfig.DataSourceType.ORACLE, "oracle.jdbc.driver.OracleDriver");
        DRIVER_MAPPINGS.put(DataSourceConfig.DataSourceType.SQLITE, "org.sqlite.JDBC");

        // Initialize URL templates
        URL_TEMPLATES.put(DataSourceConfig.DataSourceType.MYSQL, "jdbc:mysql://{host}:{port}/{database}?useSSL=false&serverTimezone=UTC");
        URL_TEMPLATES.put(DataSourceConfig.DataSourceType.POSTGRESQL, "jdbc:postgresql://{host}:{port}/{database}");
        URL_TEMPLATES.put(DataSourceConfig.DataSourceType.SQLSERVER, "jdbc:sqlserver://{host}:{port};databaseName={database};encrypt=false;trustServerCertificate=true");
        URL_TEMPLATES.put(DataSourceConfig.DataSourceType.ORACLE, "jdbc:oracle:thin:@{host}:{port}:{database}");
        URL_TEMPLATES.put(DataSourceConfig.DataSourceType.SQLITE, "jdbc:sqlite:{database}");

        // Initialize test queries
        TEST_QUERIES.put(DataSourceConfig.DataSourceType.MYSQL, "SELECT 1");
        TEST_QUERIES.put(DataSourceConfig.DataSourceType.POSTGRESQL, "SELECT 1");
        TEST_QUERIES.put(DataSourceConfig.DataSourceType.SQLSERVER, "SELECT 1");
        TEST_QUERIES.put(DataSourceConfig.DataSourceType.ORACLE, "SELECT 1 FROM DUAL");
        TEST_QUERIES.put(DataSourceConfig.DataSourceType.SQLITE, "PRAGMA quick_check");
    }

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private List<IDatabaseProfiler> profilers;

    @Override
    public DataSourceConfig createDataSource(DataSourceConfig dataSourceConfig) {
        logger.info("Creating new data source: {}", dataSourceConfig.getName());

        // Check if name already exists
        if (existsByName(dataSourceConfig.getName())) {
            throw new IllegalArgumentException("Data source name already exists: " + dataSourceConfig.getName());
        }

        // Generate unique source ID if not provided
        if (dataSourceConfig.getSourceId() == null || dataSourceConfig.getSourceId().isEmpty()) {
            dataSourceConfig.setSourceId(generateSourceId(dataSourceConfig.getName()));
        }

        return dataSourceConfigRepository.save(dataSourceConfig);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSourceConfig> getAllDataSources() {
        logger.debug("Retrieving all data sources");
        return dataSourceConfigRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DataSourceConfig> getDataSourcesByType(DataSourceConfig.DataSourceType type) {
        logger.debug("Retrieving data sources by type: {}", type);
        return dataSourceConfigRepository.findByType(type.name());
    }

    @Override
    @Transactional(readOnly = true)
    public DataSourceConfig getDataSourceBySourceId(String sourceId) {
        logger.debug("Retrieving data source by source ID: {}", sourceId);
        DataSourceConfig dataSource = dataSourceConfigRepository.findBySourceId(sourceId);
        if (dataSource == null) {
            throw new IllegalArgumentException("Data source not found with source ID: " + sourceId);
        }
        return dataSource;
    }

    @Override
    public DataSourceConfig updateDataSource(String sourceId, DataSourceConfig updatedConfig) {
        logger.info("Updating data source with source ID: {}", sourceId);

        DataSourceConfig existingConfig = getDataSourceBySourceId(sourceId);

        // Check if new name conflicts with existing names (excluding current record)
        if (!existingConfig.getName().equals(updatedConfig.getName()) &&
                existsByName(updatedConfig.getName())) {
            throw new IllegalArgumentException("Data source name already exists: " + updatedConfig.getName());
        }

        // Update fields
        existingConfig.setName(updatedConfig.getName());
        existingConfig.setType(updatedConfig.getType());
        existingConfig.setProperties(updatedConfig.getProperties());

        return dataSourceConfigRepository.save(existingConfig);
    }

    @Override
//    @Transactional
    public void deleteDataSource(Long id) {
        logger.info("Deleting data source with source ID: {}", id);
        dataSourceConfigRepository.deleteById(id);
    }


    @Override
    public boolean testConnection(DataSourceConfig dataSourceConfig) {
        ConnectionTestResult result = testConnectionDetailed(dataSourceConfig);
        return result.isSuccess();
    }

    /**
     * Test connection with detailed result information
     *
     * @param dataSourceConfig the data source configuration to test
     * @return detailed connection test result
     */
    public ConnectionTestResult testConnectionDetailed(DataSourceConfig dataSourceConfig) {
        logger.info("Testing connection for data source: {}", dataSourceConfig.getName());

        long startTime = System.currentTimeMillis();

        try {
            if (dataSourceConfig.getType() == DataSourceConfig.DataSourceType.FILE) {
                // For file data sources, just check if file properties are valid
                boolean isValid = dataSourceConfig.getProperties() != null &&
                        dataSourceConfig.getProperties().containsKey("originalFileName");
                long duration = System.currentTimeMillis() - startTime;

                if (isValid) {
                    return new ConnectionTestResult(true, "File data source configuration is valid", duration);
                } else {
                    return new ConnectionTestResult(false, "Invalid file data source configuration", duration);
                }
            }

            String jdbcUrl = buildJdbcUrl(dataSourceConfig);
            String username = dataSourceConfig.getUsername();
            String password = dataSourceConfig.getPassword();

            // Set connection timeout
            DriverManager.setLoginTimeout(CONNECTION_TIMEOUT);

            // Load driver
            String driverClassName = DRIVER_MAPPINGS.get(dataSourceConfig.getType());
            if (driverClassName != null) {
                Class.forName(driverClassName);
            }

            // Test connection with additional properties for SQL Server
            Connection connection;
            if (dataSourceConfig.getType() == DataSourceConfig.DataSourceType.SQLSERVER) {
                Properties props = new Properties();
                props.setProperty("user", username);
                props.setProperty("password", password);
                props.setProperty("encrypt", "false");
                props.setProperty("trustServerCertificate", "true");
                connection = DriverManager.getConnection(jdbcUrl, props);
            } else {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            }
            
            try (Connection conn = connection) {
                // Execute test query to verify connection is working
                String testQuery = TEST_QUERIES.get(dataSourceConfig.getType());
                if (testQuery != null) {
                    try (PreparedStatement stmt = conn.prepareStatement(testQuery);
                         ResultSet rs = stmt.executeQuery()) {
                        // Connection successful if we can execute the query
                        long duration = System.currentTimeMillis() - startTime;
                        logger.info("Connection test successful for data source: {}", dataSourceConfig.getName());
                        return new ConnectionTestResult(true, "Connection successful!", duration);
                    }
                }

                long duration = System.currentTimeMillis() - startTime;
                logger.info("Connection test successful for data source: {}", dataSourceConfig.getName());
                return new ConnectionTestResult(true, "Connection successful!", duration);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String message = "Database driver not found: " + e.getMessage();
            logger.warn("Connection test failed for data source: {} - {}",
                    dataSourceConfig.getName(), message);
            return new ConnectionTestResult(false, message, duration);
        }
    }

    /**
     * Check if a data source name already exists
     */
    private boolean existsByName(String name) {
        List<DataSourceConfig> existing = dataSourceConfigRepository.findByNameContainingIgnoreCase(name);
        return existing.stream().anyMatch(ds -> ds.getName().equalsIgnoreCase(name));
    }

    /**
     * Generate a unique source ID based on name
     */
    private String generateSourceId(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "_") + "_" + System.currentTimeMillis();
    }

    /**
     * Build JDBC URL from data source configuration
     */
    private String buildJdbcUrl(DataSourceConfig config) {
        String template = URL_TEMPLATES.get(config.getType());
        if (template == null) {
            throw new IllegalArgumentException("Unsupported data source type: " + config.getType());
        }

        String url = template;
        if (config.getHost() != null) {
            url = url.replace("{host}", config.getHost());
        }
        if (config.getPort() != null) {
            url = url.replace("{port}", config.getPort().toString());
        }
        if (config.getDatabaseName() != null) {
            url = url.replace("{database}", config.getDatabaseName());
        }

        return url;
    }

    @Override
    @Cacheable(value = CacheConfig.DATASOURCE_SCHEMAS_CACHE, key = "#sourceId")
    public List<String> getSchemas(String sourceId) {
        logger.info("Getting schemas for data source: {}", sourceId);
        DataSourceConfig config = getDataSourceBySourceId(sourceId);

        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = config.getPassword();

        try (Connection connection = createConnection(config, jdbcUrl, username, password)) {
            List<String> schemas = new java.util.ArrayList<>();
            java.sql.DatabaseMetaData metaData = connection.getMetaData();
            try (java.sql.ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    schemas.add(rs.getString("TABLE_SCHEM"));
                }
            }
            return schemas;
        } catch (java.sql.SQLException e) {
            logger.error("Error getting schemas for data source: {}", sourceId, e);
            throw new RuntimeException("Failed to retrieve schemas", e);
        }
    }

    @Override
    @Cacheable(value = CacheConfig.DATASOURCE_TABLES_CACHE, key = "#sourceId + '_' + #schema")
    public List<String> getTables(String sourceId, String schema) {
        logger.info("Getting tables for data source: {}, schema: {}", sourceId, schema);
        DataSourceConfig config = getDataSourceBySourceId(sourceId);

        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = config.getPassword();

        try (Connection connection = createConnection(config, jdbcUrl, username, password)) {
            List<String> tables = new java.util.ArrayList<>();
            java.sql.DatabaseMetaData metaData = connection.getMetaData();
            try (java.sql.ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            return tables;
        } catch (java.sql.SQLException e) {
            logger.error("Error getting tables for data source: {}, schema: {}", sourceId, schema, e);
            throw new RuntimeException("Failed to retrieve tables", e);
        }
    }

    @Override
    @Cacheable(value = CacheConfig.DATASOURCE_INFO_CACHE, key = "#sourceId")
    public DataSourceInfoDto getDatasourceInfo(String sourceId) {
        logger.info("Getting complete data source info for: {}", sourceId);
        DataSourceConfig config = getDataSourceBySourceId(sourceId);

        Map<String, List<String>> schemasWithTables = new LinkedHashMap<>();

        try {
            // Get appropriate profiler for the data source type
            IDatabaseProfiler profiler = getProfiler(config.getType().name());

            List<String> schemas = profiler.getSchemas(config);
            for (String schema : schemas) {
                List<String> tables = profiler.getTables(config, schema);
                schemasWithTables.put(schema, tables);
            }

            // Use profiler to get database metadata
//            Map<String, List<String>> schemasWithTables = profiler.getDatabaseMetadata(config);
            
            DataSourceInfoDto result = new DataSourceInfoDto(
                config.getSourceId(),
                config.getName(),
                config.getType().name(),
                schemasWithTables
            );
            
            logger.info("Retrieved {} schemas with {} total tables for data source: {} using profiler", 
                result.getSchemaCount(), result.getTotalTableCount(), sourceId);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error getting data source info for: {} using profiler", sourceId, e);
            
            // Fallback to original JDBC method if profiler fails
            logger.warn("Falling back to generic JDBC method for data source: {}", sourceId);
            return getDatasourceInfoFallback(config);
        }
    }

    @Override
    @CacheEvict(value = {
        CacheConfig.DATASOURCE_INFO_CACHE,
        CacheConfig.DATASOURCE_SCHEMAS_CACHE,
        CacheConfig.DATASOURCE_TABLES_CACHE
    }, key = "#sourceId")
    public void refreshDatasourceInfoCache(String sourceId) {
        logger.info("Refreshing cache for data source: {}", sourceId);
        // Cache eviction is handled by the annotation
        // The next call to getDatasourceInfo will populate the cache with fresh data
    }

    /**
     * Get appropriate profiler for data source type
     * 
     * @param dataSourceType The data source type
     * @return IDatabaseProfiler instance
     * @throws UnsupportedOperationException if no profiler found
     */
    private IDatabaseProfiler getProfiler(String dataSourceType) {
        return profilers.stream()
                .filter(p -> p.supports(dataSourceType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Unsupported data source type: " + dataSourceType));
    }

    /**
     * Fallback method using generic JDBC metadata retrieval
     * Used when profiler-based method fails
     * 
     * @param config DataSource configuration
     * @return DataSourceInfoDto with metadata
     */
    private DataSourceInfoDto getDatasourceInfoFallback(DataSourceConfig config) {
        logger.info("Using fallback JDBC method for data source: {}", config.getSourceId());
        
        String jdbcUrl = buildJdbcUrl(config);
        String username = config.getUsername();
        String password = config.getPassword();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            Map<String, List<String>> schemasWithTables = new LinkedHashMap<>();
            java.sql.DatabaseMetaData metaData = connection.getMetaData();
            
            // Get all schemas first
            List<String> schemas = new ArrayList<>();
            try (java.sql.ResultSet rs = metaData.getSchemas()) {
                while (rs.next()) {
                    schemas.add(rs.getString("TABLE_SCHEM"));
                }
            }
            
            // For each schema, get its tables
            for (String schema : schemas) {
                List<String> tables = new ArrayList<>();
                try (java.sql.ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE"})) {
                    while (rs.next()) {
                        tables.add(rs.getString("TABLE_NAME"));
                    }
                }
                schemasWithTables.put(schema, tables);
            }
            
            DataSourceInfoDto result = new DataSourceInfoDto(
                config.getSourceId(),
                config.getName(),
                config.getType().name(),
                schemasWithTables
            );
            
            logger.info("Retrieved {} schemas with {} total tables for data source: {} using fallback method", 
                result.getSchemaCount(), result.getTotalTableCount(), config.getSourceId());
            
            return result;
            
        } catch (java.sql.SQLException e) {
            logger.error("Error getting data source info for: {} using fallback method", config.getSourceId(), e);
            throw new RuntimeException("Failed to retrieve data source information using both profiler and fallback methods", e);
        }
    }
    
    /**
     * Create database connection with proper SSL configuration for SQL Server
     */
    private Connection createConnection(DataSourceConfig config, String jdbcUrl, String username, String password) throws java.sql.SQLException {
        if (config.getType() == DataSourceConfig.DataSourceType.SQLSERVER) {
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("encrypt", "false");
            props.setProperty("trustServerCertificate", "true");
            return DriverManager.getConnection(jdbcUrl, props);
        } else {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
    }
}