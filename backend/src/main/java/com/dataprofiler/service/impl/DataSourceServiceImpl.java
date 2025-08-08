package com.dataprofiler.service.impl;

import com.dataprofiler.dto.ConnectionTestResult;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.repository.DataSourceConfigRepository;
import com.dataprofiler.service.DataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        URL_TEMPLATES.put(DataSourceConfig.DataSourceType.SQLSERVER, "jdbc:sqlserver://{host}:{port};databaseName={database}");
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
    @Transactional
    public void deleteDataSource(String sourceId) {
        logger.info("Deleting data source with source ID: {}", sourceId);
        if (!dataSourceConfigRepository.existsBySourceId(sourceId)) {
            throw new IllegalArgumentException("Data source not found with source ID: " + sourceId);
        }

        dataSourceConfigRepository.deleteBySourceId(sourceId);
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

            // Test connection
            try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
                // Execute test query to verify connection is working
                String testQuery = TEST_QUERIES.get(dataSourceConfig.getType());
                if (testQuery != null) {
                    try (PreparedStatement stmt = connection.prepareStatement(testQuery);
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

}