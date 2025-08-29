package com.dataprofiler.profiler.impl;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.profiler.IDatabaseProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.*;
import java.util.*;

/**
 * MySQL database profiler implementation
 * Implements adaptive profiling strategy for MySQL databases
 */
@Component
public class MySqlProfiler implements IDatabaseProfiler {

    private static final Logger logger = LoggerFactory.getLogger(MySqlProfiler.class);
    private static final long LARGE_TABLE_THRESHOLD = 1000000; // 1M rows
    private static final int SAMPLE_SIZE = 1000;

    @Override
    public RawProfileDataDto profile(DataSourceConfig dataSource, ProfilingTaskRequest.DataSourceScope scope) throws Exception {
        logger.info("Starting MySQL profiling for data source: {}", dataSource.getSourceId());

        RawProfileDataDto rawData = new RawProfileDataDto(
                dataSource.getSourceId(),
                DataSourceConfig.DataSourceType.MYSQL
        );

        try (Connection connection = createConnection(dataSource)) {
            rawData.setDatabaseName(connection.getCatalog());

            List<RawProfileDataDto.TableData> tables = new ArrayList<>();

            // Get tables to profile based on scope
//            List<String> tablesToProfile = getTablesList(connection, scope);
            Map<String, List<String>> tablesList = getTablesList(connection, scope);

            for (Map.Entry<String, List<String>> stringListEntry : tablesList.entrySet()) {
                String schemaName = stringListEntry.getKey();
                // Use USE statement to switch database in MySQL
                try (Statement useStmt = connection.createStatement()) {
                    useStmt.execute("USE `" + schemaName + "`");
                }
                for (String tableName : stringListEntry.getValue()) {
                    try {
                        RawProfileDataDto.TableData tableData = profileTable(connection, tableName, schemaName);
                        if (tableData != null) {
                            tables.add(tableData);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to profile table: {} in schema: {}", tableName, schemaName, e);
                    }
                }
            }
            rawData.setTables(tables);

            // Add database metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("mysql_version", getDatabaseVersion(connection));
            metadata.put("total_tables_profiled", tables.size());
            rawData.setMetadata(metadata);

        } catch (Exception e) {
            logger.error("Failed to profile MySQL data source: {}", dataSource.getSourceId(), e);
            throw e;
        }

        logger.info("Completed MySQL profiling for data source: {}, profiled {} tables",
                dataSource.getSourceId(), rawData.getTables().size());
        return rawData;
    }

    @Override
    public boolean testConnection(DataSourceConfig dataSource) {
        try (Connection connection = createConnection(dataSource)) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            logger.warn("MySQL connection test failed for data source: {}", dataSource.getSourceId(), e);
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return "MYSQL";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return "MYSQL".equalsIgnoreCase(dataSourceType);
    }

    @Override
    public Map<String, List<String>> getDatabaseMetadata(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting database metadata for MySQL data source: {}", dataSourceConfig.getSourceId());

        Map<String, List<String>> schemasWithTables = new LinkedHashMap<>();

        try (Connection connection = createConnection(dataSourceConfig)) {
            // For MySQL, we typically work with the current database as the schema
            String currentDatabase = connection.getCatalog();
            if (currentDatabase == null || currentDatabase.isEmpty()) {
                currentDatabase = dataSourceConfig.getDatabaseName();
            }

            List<String> tables = getTablesForDatabase(connection, currentDatabase);
            schemasWithTables.put(currentDatabase, tables);

            logger.info("Retrieved {} tables for MySQL database: {}", tables.size(), currentDatabase);
            return schemasWithTables;

        } catch (SQLException e) {
            logger.error("Error getting database metadata for MySQL data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve MySQL database metadata", e);
        }
    }

    @Override
    public List<String> getSchemas(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting schemas for MySQL data source: {}", dataSourceConfig.getSourceId());

        List<String> schemas = new ArrayList<>();

        try (Connection connection = createConnection(dataSourceConfig)) {
            // For MySQL, schemas are equivalent to databases
            String sql = "SHOW DATABASES";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String database = rs.getString(1);
                    
                    // If databaseName is configured, only return that specific database
                    if (StringUtils.hasText(dataSourceConfig.getDatabaseName())) {
                        if (dataSourceConfig.getDatabaseName().equals(database)) {
                            schemas.add(database);
                        }
                    } else {
                        // If no specific database configured, return all non-system databases
                        if (!isSystemDatabase(database)) {
                            schemas.add(database);
                        }
                    }
                }
            }

            logger.info("Retrieved {} schemas for MySQL data source: {}", schemas.size(), dataSourceConfig.getSourceId());
            return schemas;

        } catch (SQLException e) {
            logger.error("Error getting schemas for MySQL data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve MySQL schemas", e);
        }
    }

    @Override
    public List<String> getTables(DataSourceConfig dataSourceConfig, String schema) throws Exception {
        logger.info("Getting tables for MySQL schema: {} in data source: {}", schema, dataSourceConfig.getSourceId());

        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> tables = getTablesForDatabase(connection, schema);

            logger.info("Retrieved {} tables for MySQL schema: {}", tables.size(), schema);
            return tables;

        } catch (SQLException e) {
            logger.error("Error getting tables for MySQL schema: {} in data source: {}", schema, dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve MySQL tables for schema: " + schema, e);
        }
    }

    /**
     * Create database connection
     */
    private Connection createConnection(DataSourceConfig dataSource) throws SQLException {
        String url = buildConnectionUrl(dataSource);
        Properties props = new Properties();
        props.setProperty("user", dataSource.getUsername());
        props.setProperty("password", dataSource.getPassword());
        props.setProperty("useSSL", "false");
        props.setProperty("allowPublicKeyRetrieval", "true");

        return DriverManager.getConnection(url, props);
    }

    /**
     * Build connection URL
     */
    private String buildConnectionUrl(DataSourceConfig dataSource) {
        if (dataSource.getConnectionUrl() != null && !dataSource.getConnectionUrl().isEmpty()) {
            return dataSource.getConnectionUrl();
        }

        return String.format("jdbc:mysql://%s:%d/%s",
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDatabaseName());
    }

    /**
     * Get list of tables to profile based on scope
     */
    private Map<String, List<String>> getTablesList(Connection connection, ProfilingTaskRequest.DataSourceScope scope) throws SQLException {
        HashMap<String, List<String>> map = new HashMap<>();
        if (scope == null || scope.getSchemas() == null || scope.getSchemas().isEmpty()) {
            // Profile all tables in the current database
            String sql = "SELECT table_name, table_schema FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String schemaName = rs.getString("table_schema");
                    if (!map.containsKey(schemaName)) {
                        map.put(schemaName, new ArrayList<>());
                    }
                    map.get(schemaName).add(tableName);
                }
            }
        } else {
            // Profile specific tables from scope
            for (Map.Entry<String, List<String>> entry : scope.getSchemas().entrySet()) {
                String schema = entry.getKey();
                List<String> tablesToInclude = entry.getValue();

                if (tablesToInclude.isEmpty()) {
                    // Include all tables from this schema
                    String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'";
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setString(1, schema);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
//                                tables.add(rs.getString("table_name"));
                                String tableName = rs.getString("table_name");
                                if (!map.containsKey(schema)) {
                                    map.put(schema, new ArrayList<>());
                                }
                                map.get(schema).add(tableName);
                            }
                        }
                    }
                } else {
                    // Include specific tables
//                    tables.addAll(tablesToInclude);
                    map.put(schema, tablesToInclude);
                }
            }
        }

        return map;
    }

    /**
     * Profile a single table using adaptive strategy
     */
    private RawProfileDataDto.TableData profileTable(Connection connection, String tableName, String schemaName) throws SQLException {
        logger.debug("Profiling schema: {} table: {}", schemaName, tableName);

        RawProfileDataDto.TableData tableData = new RawProfileDataDto.TableData(tableName, schemaName);

        // Get table metadata
        getTableMetadata(connection, tableData, schemaName);

        // Get row count (adaptive: exact vs approximate)
        long rowCount = getRowCount(connection, tableName, schemaName);
        tableData.setRowCount(rowCount);

        // Determine if we should use sampling for large tables
        boolean useSampling = rowCount > LARGE_TABLE_THRESHOLD;
        tableData.setUseSample(useSampling);

        // Get column information and profile each column
        List<RawProfileDataDto.ColumnData> columns = profileColumns(connection, tableName, schemaName, useSampling);
        tableData.setColumns(columns);

        // Get index information
        List<RawProfileDataDto.IndexData> indexes = getIndexes(connection, tableName, schemaName);
        tableData.setIndexes(indexes);

        return tableData;
    }

    /**
     * Get table metadata
     */
    private void getTableMetadata(Connection connection, RawProfileDataDto.TableData tableData, String schemaName) throws SQLException {
        String sql = "SELECT table_type, engine, table_comment FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableData.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tableData.setTableType(rs.getString("table_type"));

                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("engine", rs.getString("engine"));
                    metadata.put("comment", rs.getString("table_comment"));
                    tableData.setTableMetadata(metadata);
                }
            }
        }
    }

    /**
     * Get row count using adaptive strategy
     */
    private long getRowCount(Connection connection, String tableName, String schemaName) throws SQLException {
        // First try to get approximate count from information_schema (fast)
        String approxSql = "SELECT table_rows FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";

        try (PreparedStatement stmt = connection.prepareStatement(approxSql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long approxCount = rs.getLong("table_rows");

                    // If approximate count is reasonable, use exact count for small tables
                    if (approxCount < LARGE_TABLE_THRESHOLD) {
                        return getExactRowCount(connection, tableName, schemaName);
                    } else {
                        logger.debug("Using approximate row count for large table {}.{}: {}", schemaName, tableName, approxCount);
                        return approxCount;
                    }
                }
            }
        }

        // Fallback to exact count
        return getExactRowCount(connection, tableName, schemaName);
    }

    /**
     * Get exact row count
     */
    private long getExactRowCount(Connection connection, String tableName, String schemaName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `" + schemaName + "`.`" + tableName + "`";

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }

        return 0;
    }

    /**
     * Profile all columns in a table
     */
    private List<RawProfileDataDto.ColumnData> profileColumns(Connection connection, String tableName, String schemaName, boolean useSampling) throws SQLException {
        List<RawProfileDataDto.ColumnData> columns = new ArrayList<>();

        // Get column metadata
        DatabaseMetaData metaData = connection.getMetaData();
        // For MySQL, catalog is the database name (schema), and schema parameter should be null
        try (ResultSet rs = metaData.getColumns(schemaName, null, tableName, null)) {
            while (rs.next()) {
                RawProfileDataDto.ColumnData columnData = new RawProfileDataDto.ColumnData();
                columnData.setColumnName(rs.getString("COLUMN_NAME"));
                columnData.setDataType(rs.getString("TYPE_NAME"));
                columnData.setNativeType(rs.getString("TYPE_NAME"));
                columnData.setColumnSize(rs.getInt("COLUMN_SIZE"));
                columnData.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                columnData.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                columnData.setDefaultValue(rs.getString("COLUMN_DEF"));

                // Profile column data
                profileColumnData(connection, tableName, schemaName, columnData, useSampling);

                columns.add(columnData);
            }
        }

        return columns;
    }

    /**
     * Profile individual column data
     */
    private void profileColumnData(Connection connection, String tableName, String schemaName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        String columnName = columnData.getColumnName();

        // Build profiling query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("COUNT(*) as total_count, ");
        sql.append("COUNT(").append(columnName).append(") as non_null_count, ");
        sql.append("COUNT(DISTINCT ").append(columnName).append(") as unique_count");

        // Add min/max for numeric and date types
        if (isNumericType(columnData.getDataType()) || isDateType(columnData.getDataType())) {
            sql.append(", MIN(").append(columnName).append(") as min_value");
            sql.append(", MAX(").append(columnName).append(") as max_value");
        }

        // Add length statistics for string types
        if (isStringType(columnData.getDataType())) {
            sql.append(", AVG(LENGTH(").append(columnName).append(")) as avg_length");
            sql.append(", MAX(LENGTH(").append(columnName).append(")) as max_length");
            sql.append(", MIN(LENGTH(").append(columnName).append(")) as min_length");
        }

        sql.append(" FROM ").append("`").append(schemaName).append("`").append(".").append(tableName);

        // Add sampling for large tables
        if (useSampling) {
            sql.append(" ORDER BY RAND() LIMIT ").append(SAMPLE_SIZE);
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString());
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                long totalCount = rs.getLong("total_count");
                long nonNullCount = rs.getLong("non_null_count");

                columnData.setTotalCount(totalCount);
                columnData.setNullCount(totalCount - nonNullCount);
                columnData.setUniqueCount(rs.getLong("unique_count"));

                // Set min/max values if available
                try {
                    columnData.setMinValue(rs.getObject("min_value"));
                    columnData.setMaxValue(rs.getObject("max_value"));
                } catch (SQLException e) {
                    // Column doesn't exist in result set, ignore
                }

                // Set length statistics if available
                try {
                    columnData.setAvgLength(rs.getDouble("avg_length"));
                    columnData.setMaxLength(rs.getLong("max_length"));
                    columnData.setMinLength(rs.getLong("min_length"));
                } catch (SQLException e) {
                    // Column doesn't exist in result set, ignore
                }
            }
        } catch (SQLException e) {
            logger.warn("Failed to profile column: {} in table: {} in schema: {} ,sql: {} ", columnName, tableName, schemaName, sql, e);
        }

        // Get sample values
        getSampleValues(connection, tableName, schemaName, columnData, useSampling);
    }

    /**
     * Get sample values for a column
     */
    private void getSampleValues(Connection connection, String tableName, String schemaName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        String sql = "SELECT DISTINCT " + columnData.getColumnName() + " FROM `" + schemaName + "`." + tableName +
                " WHERE " + columnData.getColumnName() + " IS NOT NULL";

        if (useSampling) {
            sql += " ORDER BY RAND() LIMIT 10";
        } else {
            sql += " LIMIT 10";
        }

        List<Object> sampleValues = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                sampleValues.add(rs.getObject(1));
            }
        }

        columnData.setSampleValues(sampleValues);
    }

    /**
     * Get index information for a table
     */
    private List<RawProfileDataDto.IndexData> getIndexes(Connection connection, String tableName, String schemaName) throws SQLException {
        List<RawProfileDataDto.IndexData> indexes = new ArrayList<>();

        String sql = "SHOW INDEX FROM `" + schemaName + "`." + tableName;

        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            Map<String, RawProfileDataDto.IndexData> indexMap = new HashMap<>();

            while (rs.next()) {
                String indexName = rs.getString("Key_name");

                RawProfileDataDto.IndexData indexData = indexMap.get(indexName);
                if (indexData == null) {
                    indexData = new RawProfileDataDto.IndexData(indexName, rs.getString("Index_type"));
                    indexData.setIsUnique(rs.getInt("Non_unique") == 0);
                    indexData.setIsPrimary("PRIMARY".equals(indexName));
                    indexData.setColumnNames(new ArrayList<>());
                    indexMap.put(indexName, indexData);
                }

                indexData.getColumnNames().add(rs.getString("Column_name"));
            }

            indexes.addAll(indexMap.values());
        }

        return indexes;
    }

    /**
     * Get database version
     */
    private String getDatabaseVersion(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT VERSION()");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "Unknown";
    }

    /**
     * Check if data type is numeric
     */
    private boolean isNumericType(String dataType) {
        return dataType != null && (
                dataType.toUpperCase().contains("INT") ||
                        dataType.toUpperCase().contains("DECIMAL") ||
                        dataType.toUpperCase().contains("FLOAT") ||
                        dataType.toUpperCase().contains("DOUBLE") ||
                        dataType.toUpperCase().contains("NUMERIC")
        );
    }

    /**
     * Check if data type is date/time
     */
    private boolean isDateType(String dataType) {
        return dataType != null && (
                dataType.toUpperCase().contains("DATE") ||
                        dataType.toUpperCase().contains("TIME") ||
                        dataType.toUpperCase().contains("TIMESTAMP")
        );
    }

    /**
     * Check if data type is string
     */
    private boolean isStringType(String dataType) {
        return dataType != null && (
                dataType.toUpperCase().contains("CHAR") ||
                        dataType.toUpperCase().contains("TEXT") ||
                        dataType.toUpperCase().contains("BLOB")
        );
    }

    /**
     * Get tables for a specific database
     *
     * @param connection Database connection
     * @param database   Database name
     * @return List of table names
     * @throws SQLException if query fails
     */
    private List<String> getTablesForDatabase(Connection connection, String database) throws SQLException {
        List<String> tables = new ArrayList<>();

        String sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, database);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        }

        return tables;
    }

    /**
     * Check if a database is a system database that should be filtered out
     *
     * @param database Database name
     * @return true if it's a system database
     */
    private boolean isSystemDatabase(String database) {
        return "information_schema".equalsIgnoreCase(database) ||
                "performance_schema".equalsIgnoreCase(database) ||
                "mysql".equalsIgnoreCase(database) ||
                "sys".equalsIgnoreCase(database);
    }
}