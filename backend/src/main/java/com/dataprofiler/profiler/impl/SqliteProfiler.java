package com.dataprofiler.profiler.impl;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.profiler.IDatabaseProfiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * SQLite database profiler implementation
 * Implements adaptive profiling strategy for SQLite databases
 * Used for both direct SQLite connections and file-based data sources converted to SQLite
 */
@Component
public class SqliteProfiler implements IDatabaseProfiler {

    private static final Logger logger = LoggerFactory.getLogger(SqliteProfiler.class);
    private static final long LARGE_TABLE_THRESHOLD = 1000000; // 1M rows
    private static final int SAMPLE_SIZE = 1000;

    @Override
    public RawProfileDataDto profile(DataSourceConfig dataSource, ProfilingTaskRequest.DataSourceScope scope) throws Exception {
        logger.info("Starting SQLite profiling for data source: {}", dataSource.getSourceId());

        RawProfileDataDto rawData = new RawProfileDataDto(
            dataSource.getSourceId(), 
            DataSourceConfig.DataSourceType.SQLITE
        );

        try (Connection connection = createConnection(dataSource)) {
            rawData.setDatabaseName(getDatabaseName(dataSource));
            
            List<RawProfileDataDto.TableData> tables = new ArrayList<>();
            
            // Get tables to profile based on scope
            List<String> tablesToProfile = getTablesList(connection, scope);
            
            for (String tableName : tablesToProfile) {
                try {
                    RawProfileDataDto.TableData tableData = profileTable(connection, tableName);
                    if (tableData != null) {
                        tables.add(tableData);
                    }
                } catch (Exception e) {
                    logger.warn("Failed to profile table: {}", tableName, e);
                }
            }
            
            rawData.setTables(tables);
            
            // Add database metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("sqlite_version", getDatabaseVersion(connection));
            metadata.put("total_tables_profiled", tables.size());
            rawData.setMetadata(metadata);
            
        } catch (Exception e) {
            logger.error("Failed to profile SQLite data source: {}", dataSource.getSourceId(), e);
            throw e;
        }

        logger.info("Completed SQLite profiling for data source: {}, profiled {} tables", 
                   dataSource.getSourceId(), rawData.getTables().size());
        return rawData;
    }

    @Override
    public boolean testConnection(DataSourceConfig dataSource) {
        try (Connection connection = createConnection(dataSource)) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            logger.warn("SQLite connection test failed for data source: {}", dataSource.getSourceId(), e);
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return "SQLITE";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return "SQLITE".equalsIgnoreCase(dataSourceType) || "FILE".equalsIgnoreCase(dataSourceType);
    }

    /**
     * Create database connection
     */
    private Connection createConnection(DataSourceConfig dataSource) throws SQLException {
        String url = buildConnectionUrl(dataSource);
        Properties props = new Properties();
        
        // SQLite doesn't require username/password for file-based databases
        if (dataSource.getUsername() != null && !dataSource.getUsername().isEmpty()) {
            props.setProperty("user", dataSource.getUsername());
        }
        if (dataSource.getPassword() != null && !dataSource.getPassword().isEmpty()) {
            props.setProperty("password", dataSource.getPassword());
        }
        
        return DriverManager.getConnection(url, props);
    }

    /**
     * Build connection URL for SQLite
     */
    private String buildConnectionUrl(DataSourceConfig dataSource) {
        if (dataSource.getConnectionUrl() != null && !dataSource.getConnectionUrl().isEmpty()) {
            return dataSource.getConnectionUrl();
        }
        
        // For FILE type data sources, use the core SQLite database
        if (DataSourceConfig.DataSourceType.FILE.equals(dataSource.getType())) {
            return "jdbc:sqlite:data/core.db";
        }
        
        // For direct SQLite connections
        if (dataSource.getDatabaseName() != null && !dataSource.getDatabaseName().isEmpty()) {
            return "jdbc:sqlite:" + dataSource.getDatabaseName();
        }
        
        throw new IllegalArgumentException("Invalid SQLite configuration: missing database path");
    }

    /**
     * Get database name for display purposes
     */
    private String getDatabaseName(DataSourceConfig dataSource) {
        if (DataSourceConfig.DataSourceType.FILE.equals(dataSource.getType())) {
            return "core.db";
        }
        return dataSource.getDatabaseName() != null ? dataSource.getDatabaseName() : "sqlite.db";
    }

    /**
     * Get list of tables to profile based on scope
     */
    private List<String> getTablesList(Connection connection, ProfilingTaskRequest.DataSourceScope scope) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        if (scope == null || scope.getSchemas() == null || scope.getSchemas().isEmpty()) {
            // Profile all tables in the database
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
            }
        } else {
            // Profile specific tables from scope
            // SQLite doesn't have schemas, so we treat schema names as table prefixes or ignore them
            for (Map.Entry<String, List<String>> entry : scope.getSchemas().entrySet()) {
                List<String> tablesToInclude = entry.getValue();
                
                if (tablesToInclude.isEmpty()) {
                    // Include all tables
                    String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'";
                    try (PreparedStatement stmt = connection.prepareStatement(sql);
                         ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            tables.add(rs.getString("name"));
                        }
                    }
                } else {
                    // Include specific tables
                    tables.addAll(tablesToInclude);
                }
            }
        }
        
        return tables;
    }

    /**
     * Profile a single table using adaptive strategy
     */
    private RawProfileDataDto.TableData profileTable(Connection connection, String tableName) throws SQLException {
        logger.debug("Profiling table: {}", tableName);
        
        RawProfileDataDto.TableData tableData = new RawProfileDataDto.TableData(tableName, "main");
        
        // Get table metadata
        getTableMetadata(connection, tableData);
        
        // Get row count (adaptive: exact vs approximate)
        long rowCount = getRowCount(connection, tableName);
        tableData.setRowCount(rowCount);
        
        // Determine if we should use sampling for large tables
        boolean useSampling = rowCount > LARGE_TABLE_THRESHOLD;
        
        // Get column information and profile each column
        List<RawProfileDataDto.ColumnData> columns = profileColumns(connection, tableName, useSampling);
        tableData.setColumns(columns);
        
        // Get index information
        List<RawProfileDataDto.IndexData> indexes = getIndexes(connection, tableName);
        tableData.setIndexes(indexes);
        
        return tableData;
    }

    /**
     * Get table metadata
     */
    private void getTableMetadata(Connection connection, RawProfileDataDto.TableData tableData) throws SQLException {
        String sql = "SELECT sql FROM sqlite_master WHERE type='table' AND name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, tableData.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String createSql = rs.getString("sql");
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("create_sql", createSql);
                    metadata.put("table_type", "BASE TABLE");
                    tableData.setTableMetadata(metadata);
                }
            }
        }
    }

    /**
     * Get row count for a table
     */
    private long getRowCount(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) as row_count FROM " + escapeTableName(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("row_count");
            }
        }
        return 0;
    }

    /**
     * Profile columns in a table
     */
    private List<RawProfileDataDto.ColumnData> profileColumns(Connection connection, String tableName, boolean useSampling) throws SQLException {
        List<RawProfileDataDto.ColumnData> columns = new ArrayList<>();
        
        // Get column metadata
        String pragmaSql = "PRAGMA table_info(" + escapeTableName(tableName) + ")";
        
        try (PreparedStatement stmt = connection.prepareStatement(pragmaSql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String columnName = rs.getString("name");
                String dataType = rs.getString("type");
                boolean notNull = rs.getBoolean("notnull");
                boolean isPrimaryKey = rs.getBoolean("pk");
                
                RawProfileDataDto.ColumnData columnData = new RawProfileDataDto.ColumnData(
                    columnName, dataType
                );
                
                // Set column properties
                columnData.setNullable(!notNull);
                columnData.setIsPrimaryKey(isPrimaryKey);
                
                // Set column metadata
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("is_primary_key", isPrimaryKey);
                metadata.put("not_null", notNull);
                columnData.setColumnMetadata(metadata);
                
                // Profile column data
                profileColumnData(connection, tableName, columnData, useSampling);
                
                columns.add(columnData);
            }
        }
        
        return columns;
    }

    /**
     * Profile data for a specific column
     */
    private void profileColumnData(Connection connection, String tableName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        String columnName = columnData.getColumnName();
        String escapedTableName = escapeTableName(tableName);
        String escapedColumnName = escapeColumnName(columnName);
        
        Map<String, Object> profile = new HashMap<>();
        
        // Basic statistics
        String basicStatsSql = String.format(
            "SELECT COUNT(*) as total_count, COUNT(%s) as non_null_count, COUNT(DISTINCT %s) as distinct_count FROM %s",
            escapedColumnName, escapedColumnName, escapedTableName
        );
        
        if (useSampling) {
            basicStatsSql += " ORDER BY RANDOM() LIMIT " + SAMPLE_SIZE;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(basicStatsSql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                profile.put("total_count", rs.getLong("total_count"));
                profile.put("non_null_count", rs.getLong("non_null_count"));
                profile.put("distinct_count", rs.getLong("distinct_count"));
                profile.put("null_count", rs.getLong("total_count") - rs.getLong("non_null_count"));
            }
        }
        
        // Data type specific profiling
        String dataType = columnData.getDataType().toLowerCase();
        if (dataType.contains("int") || dataType.contains("real") || dataType.contains("numeric")) {
            profileNumericColumn(connection, escapedTableName, escapedColumnName, profile, useSampling);
        } else if (dataType.contains("text") || dataType.contains("char") || dataType.contains("varchar")) {
            profileTextColumn(connection, escapedTableName, escapedColumnName, profile, useSampling);
        }
        
        // Set profiling metrics to column data
        if (profile.containsKey("total_count")) {
            columnData.setTotalCount((Long) profile.get("total_count"));
        }
        if (profile.containsKey("null_count")) {
            columnData.setNullCount((Long) profile.get("null_count"));
        }
        if (profile.containsKey("distinct_count")) {
            columnData.setUniqueCount((Long) profile.get("distinct_count"));
        }
        if (profile.containsKey("min_value")) {
            columnData.setMinValue(profile.get("min_value"));
        }
        if (profile.containsKey("max_value")) {
            columnData.setMaxValue(profile.get("max_value"));
        }
        if (profile.containsKey("avg_length")) {
            columnData.setAvgLength((Double) profile.get("avg_length"));
        }
        if (profile.containsKey("max_length")) {
            columnData.setMaxLength((Long) profile.get("max_length"));
        }
        if (profile.containsKey("min_length")) {
            columnData.setMinLength((Long) profile.get("min_length"));
        }
    }

    /**
     * Profile numeric column
     */
    private void profileNumericColumn(Connection connection, String tableName, String columnName, Map<String, Object> profile, boolean useSampling) throws SQLException {
        String sql = String.format(
            "SELECT MIN(%s) as min_val, MAX(%s) as max_val, AVG(%s) as avg_val FROM %s WHERE %s IS NOT NULL",
            columnName, columnName, columnName, tableName, columnName
        );
        
        if (useSampling) {
            sql += " ORDER BY RANDOM() LIMIT " + SAMPLE_SIZE;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                profile.put("min_value", rs.getObject("min_val"));
                profile.put("max_value", rs.getObject("max_val"));
                profile.put("avg_value", rs.getObject("avg_val"));
            }
        }
    }

    /**
     * Profile text column
     */
    private void profileTextColumn(Connection connection, String tableName, String columnName, Map<String, Object> profile, boolean useSampling) throws SQLException {
        String sql = String.format(
            "SELECT MIN(LENGTH(%s)) as min_length, MAX(LENGTH(%s)) as max_length, AVG(LENGTH(%s)) as avg_length FROM %s WHERE %s IS NOT NULL",
            columnName, columnName, columnName, tableName, columnName
        );
        
        if (useSampling) {
            sql += " ORDER BY RANDOM() LIMIT " + SAMPLE_SIZE;
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                profile.put("min_length", rs.getInt("min_length"));
                profile.put("max_length", rs.getInt("max_length"));
                profile.put("avg_length", rs.getDouble("avg_length"));
            }
        }
    }

    /**
     * Get index information for a table
     */
    private List<RawProfileDataDto.IndexData> getIndexes(Connection connection, String tableName) throws SQLException {
        List<RawProfileDataDto.IndexData> indexes = new ArrayList<>();
        
        String sql = "PRAGMA index_list(" + escapeTableName(tableName) + ")";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                String indexName = rs.getString("name");
                boolean unique = rs.getBoolean("unique");
                
                // Get index columns
                List<String> columns = getIndexColumns(connection, indexName);
                
                RawProfileDataDto.IndexData indexData = new RawProfileDataDto.IndexData(
                    indexName, unique ? "UNIQUE" : "NON_UNIQUE"
                );
                indexData.setIsUnique(unique);
                indexData.setIsPrimary(false); // SQLite doesn't have explicit primary key indexes in PRAGMA index_list
                indexData.setColumnNames(columns);
                
                indexes.add(indexData);
            }
        }
        
        return indexes;
    }

    /**
     * Get columns for a specific index
     */
    private List<String> getIndexColumns(Connection connection, String indexName) throws SQLException {
        List<String> columns = new ArrayList<>();
        
        String sql = "PRAGMA index_info(" + escapeTableName(indexName) + ")";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        
        return columns;
    }

    /**
     * Get database version
     */
    private String getDatabaseVersion(Connection connection) throws SQLException {
        String sql = "SELECT sqlite_version() as version";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("version");
            }
        }
        return "Unknown";
    }

    /**
     * Escape table name for SQL queries
     */
    private String escapeTableName(String tableName) {
        return "\"" + tableName.replace("\"", "\"\"") + "\"";
    }

    /**
     * Escape column name for SQL queries
     */
    private String escapeColumnName(String columnName) {
        return "\"" + columnName.replace("\"", "\"\"") + "\"";
    }
}