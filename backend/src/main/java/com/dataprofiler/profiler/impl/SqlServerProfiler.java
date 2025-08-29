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
import java.util.LinkedHashMap;

/**
 * SQL Server database profiler implementation
 * Implements adaptive profiling strategy for SQL Server databases
 */
@Component
public class SqlServerProfiler implements IDatabaseProfiler {

    private static final Logger logger = LoggerFactory.getLogger(SqlServerProfiler.class);
    private static final long LARGE_TABLE_THRESHOLD = 1000000; // 1M rows
    private static final int SAMPLE_SIZE = 1000;

    @Override
    public RawProfileDataDto profile(DataSourceConfig dataSource, ProfilingTaskRequest.DataSourceScope scope) throws Exception {
        logger.info("Starting SQL Server profiling for data source: {}", dataSource.getSourceId());

        RawProfileDataDto rawData = new RawProfileDataDto(
            dataSource.getSourceId(), 
            DataSourceConfig.DataSourceType.SQLSERVER
        );

        try (Connection connection = createConnection(dataSource)) {
            rawData.setDatabaseName(connection.getCatalog());
            
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
            metadata.put("sqlserver_version", getDatabaseVersion(connection));
            metadata.put("total_tables_profiled", tables.size());
            rawData.setMetadata(metadata);
            
        } catch (Exception e) {
            logger.error("Failed to profile SQL Server data source: {}", dataSource.getSourceId(), e);
            throw e;
        }

        logger.info("Completed SQL Server profiling for data source: {}, profiled {} tables", 
                   dataSource.getSourceId(), rawData.getTables().size());
        return rawData;
    }

    @Override
    public boolean testConnection(DataSourceConfig dataSource) {
        try (Connection connection = createConnection(dataSource)) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            logger.warn("SQL Server connection test failed for data source: {}", dataSource.getSourceId(), e);
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return "SQLSERVER";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return "SQLSERVER".equalsIgnoreCase(dataSourceType);
    }

    @Override
    public Map<String, List<String>> getDatabaseMetadata(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting database metadata for SQL Server data source: {}", dataSourceConfig.getSourceId());
        
        Map<String, List<String>> schemasWithTables = new LinkedHashMap<>();
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            // Get all schemas first
            List<String> schemas = getSchemasInternal(connection, dataSourceConfig.getDatabaseName());
            
            // For each schema, get its tables
            for (String schema : schemas) {
                List<String> tables = getTablesForSchema(connection, schema);
                schemasWithTables.put(schema, tables);
            }
            
            logger.info("Retrieved {} schemas with total tables for SQL Server data source: {}", 
                schemas.size(), dataSourceConfig.getSourceId());
            return schemasWithTables;
            
        } catch (SQLException e) {
            logger.error("Error getting database metadata for SQL Server data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve SQL Server database metadata", e);
        }
    }

    @Override
    public List<String> getSchemas(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting schemas for SQL Server data source: {}", dataSourceConfig.getSourceId());
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> schemas = getSchemasInternal(connection, dataSourceConfig.getDatabaseName());
            
            logger.info("Retrieved {} schemas for SQL Server data source: {}", schemas.size(), dataSourceConfig.getSourceId());
            return schemas;
            
        } catch (SQLException e) {
            logger.error("Error getting schemas for SQL Server data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve SQL Server schemas", e);
        }
    }

    @Override
    public List<String> getTables(DataSourceConfig dataSourceConfig, String schema) throws Exception {
        logger.info("Getting tables for SQL Server schema: {} in data source: {}", schema, dataSourceConfig.getSourceId());
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> tables = getTablesForSchema(connection, schema);
            
            logger.info("Retrieved {} tables for SQL Server schema: {}", tables.size(), schema);
            return tables;
            
        } catch (SQLException e) {
            logger.error("Error getting tables for SQL Server schema: {} in data source: {}", schema, dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve SQL Server tables for schema: " + schema, e);
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
        props.setProperty("encrypt", "false");
        props.setProperty("trustServerCertificate", "true");
        
        return DriverManager.getConnection(url, props);
    }

    /**
     * Build connection URL
     */
    private String buildConnectionUrl(DataSourceConfig dataSource) {
        if (dataSource.getConnectionUrl() != null && !dataSource.getConnectionUrl().isEmpty()) {
            return dataSource.getConnectionUrl();
        }
        
        return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", 
                           dataSource.getHost(), 
                           dataSource.getPort(), 
                           dataSource.getDatabaseName());
    }

    /**
     * Get all schemas from the database
     * 
     * @param connection Database connection
     * @param databaseName Specific database name to filter (optional)
     * @return List of schema names
     * @throws SQLException if query fails
     */
    private List<String> getSchemasInternal(Connection connection, String databaseName) throws SQLException {
        List<String> schemas = new ArrayList<>();
        
        String sql = "SELECT name FROM sys.schemas " +
                    "WHERE name NOT IN ('sys', 'information_schema', 'guest', 'INFORMATION_SCHEMA', " +
                    "'db_owner', 'db_accessadmin', 'db_securityadmin', 'db_ddladmin', 'db_backupoperator', " +
                    "'db_datareader', 'db_datawriter', 'db_denydatareader', 'db_denydatawriter') " +
                    "ORDER BY name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                schemas.add(rs.getString("name"));
            }
        }
        if(StringUtils.hasText(databaseName) && schemas.contains(databaseName)){
            return List.of(databaseName);
        }
        return schemas;
    }

    /**
     * Get tables for a specific schema
     * 
     * @param connection Database connection
     * @param schema Schema name
     * @return List of table names
     * @throws SQLException if query fails
     */
    private List<String> getTablesForSchema(Connection connection, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        String sql = "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = ? AND table_type = 'BASE TABLE' " +
                    "ORDER BY table_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        }
        
        return tables;
    }

    /**
     * Get list of tables to profile based on scope
     */
    private List<String> getTablesList(Connection connection, ProfilingTaskRequest.DataSourceScope scope) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        if (scope == null || scope.getSchemas() == null || scope.getSchemas().isEmpty()) {
            // Profile all tables in the database
            String sql = "SELECT table_name FROM information_schema.tables WHERE table_catalog = DB_NAME() AND table_type = 'BASE TABLE'";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        } else {
            // Profile specific tables from scope
            for (Map.Entry<String, List<String>> entry : scope.getSchemas().entrySet()) {
                String schema = entry.getKey();
                List<String> tablesToInclude = entry.getValue();
                
                if (tablesToInclude.isEmpty()) {
                    // Include all tables from this schema
                    String sql = "SELECT table_name FROM information_schema.tables WHERE table_catalog = DB_NAME() AND table_schema = ? AND table_type = 'BASE TABLE'";
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setString(1, schema);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                tables.add(rs.getString("table_name"));
                            }
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
        
        RawProfileDataDto.TableData tableData = new RawProfileDataDto.TableData(tableName, connection.getCatalog());
        
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
        String sql = "SELECT table_type FROM information_schema.tables WHERE table_catalog = DB_NAME() AND table_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, tableData.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tableData.setTableType(rs.getString("table_type"));
                    
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("table_catalog", connection.getCatalog());
                    tableData.setTableMetadata(metadata);
                }
            }
        }
    }

    /**
     * Get row count using adaptive strategy
     */
    private long getRowCount(Connection connection, String tableName) throws SQLException {
        // First try to get approximate count from sys.dm_db_partition_stats (fast)
        String approxSql = "SELECT SUM(row_count) as row_count FROM sys.dm_db_partition_stats ps " +
                          "INNER JOIN sys.objects o ON ps.object_id = o.object_id " +
                          "WHERE o.name = ? AND ps.index_id IN (0,1)";
        
        try (PreparedStatement stmt = connection.prepareStatement(approxSql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long approxCount = rs.getLong("row_count");
                    
                    // If approximate count is reasonable, use exact count for small tables
                    if (approxCount < LARGE_TABLE_THRESHOLD) {
                        return getExactRowCount(connection, tableName);
                    } else {
                        logger.debug("Using approximate row count for large table {}: {}", tableName, approxCount);
                        return approxCount;
                    }
                }
            }
        }
        
        // Fallback to exact count
        return getExactRowCount(connection, tableName);
    }

    /**
     * Get exact row count
     */
    private long getExactRowCount(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM [" + tableName + "]";
        
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
    private List<RawProfileDataDto.ColumnData> profileColumns(Connection connection, String tableName, boolean useSampling) throws SQLException {
        List<RawProfileDataDto.ColumnData> columns = new ArrayList<>();
        
        // Get column metadata
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, null)) {
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
                profileColumnData(connection, tableName, columnData, useSampling);
                
                columns.add(columnData);
            }
        }
        
        return columns;
    }

    /**
     * Profile individual column data
     */
    private void profileColumnData(Connection connection, String tableName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        String columnName = columnData.getColumnName();
        
        // Build profiling query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("COUNT(*) as total_count, ");
        // For LOB types (TEXT, NTEXT, IMAGE), COUNT(column) is not valid, use COUNT(*) WHERE column IS NOT NULL
        if (isLobType(columnData.getDataType())) {
            sql.append("SUM(CASE WHEN [" + columnName + "] IS NOT NULL THEN 1 ELSE 0 END) as non_null_count, ");
            sql.append("NULL as unique_count"); // COUNT(DISTINCT) is not supported for LOB types
        } else {
            sql.append("COUNT([" + columnName + "]) as non_null_count, ");
            sql.append("COUNT(DISTINCT [" + columnName + "]) as unique_count");
        }
        
        // Add min/max for numeric and date types
        if (isNumericType(columnData.getDataType()) || isDateType(columnData.getDataType())) {
            sql.append(", MIN([" + columnName + "]) as min_value");
            sql.append(", MAX([" + columnName + "]) as max_value");
        }
        
        // Add length statistics for string types (excluding LOB types)
        if (isStringType(columnData.getDataType()) && !isLobType(columnData.getDataType())) {
            sql.append(", AVG(LEN([" + columnName + "])) as avg_length");
            sql.append(", MAX(LEN([" + columnName + "])) as max_length");
            sql.append(", MIN(LEN([" + columnName + "])) as min_length");
        }
        
        sql.append(" FROM [" + tableName + "]");
        
        // Add sampling for large tables
        if (useSampling) {
            sql.append(" TABLESAMPLE(" + SAMPLE_SIZE + " ROWS)");
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString());
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                long totalCount = rs.getLong("total_count");
                long nonNullCount = rs.getLong("non_null_count");
                
                columnData.setTotalCount(totalCount);
                columnData.setNullCount(totalCount - nonNullCount);
                // For LOB types (TEXT, NTEXT, IMAGE), COUNT(DISTINCT) is not supported or meaningful
                if (isLobType(columnData.getDataType())) {
                    columnData.setUniqueCount(null); // Or 0, depending on desired representation
                } else {
                    columnData.setUniqueCount(rs.getLong("unique_count"));
                }
                
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
        }
        
        // Get sample values
        getSampleValues(connection, tableName, columnData, useSampling);
    }

    /**
     * Check if data type is a Large Object (LOB) type
     */
    private boolean isLobType(String dataType) {
        String lowerCaseType = dataType.toLowerCase();
        return lowerCaseType.contains("text") || lowerCaseType.contains("ntext") || lowerCaseType.contains("image");
    }

    /**
     * Get sample values for a column
     */
    private void getSampleValues(Connection connection, String tableName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        // Skip sample values for LOB types (TEXT, NTEXT, IMAGE) as they cannot be used with DISTINCT
        if (isLobType(columnData.getDataType())) {
            columnData.setSampleValues(new ArrayList<>());
            return;
        }
        
        String sql = "SELECT DISTINCT TOP 10 [" + columnData.getColumnName() + "] FROM [" + tableName + "] " + 
                    "WHERE [" + columnData.getColumnName() + "] IS NOT NULL";
        
        if (useSampling) {
            sql += " TABLESAMPLE(" + SAMPLE_SIZE + " ROWS)";
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
    private List<RawProfileDataDto.IndexData> getIndexes(Connection connection, String tableName) throws SQLException {
        List<RawProfileDataDto.IndexData> indexes = new ArrayList<>();
        
        String sql = "SELECT i.name as index_name, i.type_desc as index_type, i.is_unique, i.is_primary_key, " +
                    "c.name as column_name " +
                    "FROM sys.indexes i " +
                    "INNER JOIN sys.index_columns ic ON i.object_id = ic.object_id AND i.index_id = ic.index_id " +
                    "INNER JOIN sys.columns c ON ic.object_id = c.object_id AND ic.column_id = c.column_id " +
                    "INNER JOIN sys.objects o ON i.object_id = o.object_id " +
                    "WHERE o.name = ? " +
                    "ORDER BY i.name, ic.key_ordinal";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                
                Map<String, RawProfileDataDto.IndexData> indexMap = new HashMap<>();
                
                while (rs.next()) {
                    String indexName = rs.getString("index_name");
                    
                    RawProfileDataDto.IndexData indexData = indexMap.get(indexName);
                    if (indexData == null) {
                        indexData = new RawProfileDataDto.IndexData(indexName, rs.getString("index_type"));
                        indexData.setIsUnique(rs.getBoolean("is_unique"));
                        indexData.setIsPrimary(rs.getBoolean("is_primary_key"));
                        indexData.setColumnNames(new ArrayList<>());
                        indexMap.put(indexName, indexData);
                    }
                    
                    indexData.getColumnNames().add(rs.getString("column_name"));
                }
                
                indexes.addAll(indexMap.values());
            }
        }
        
        return indexes;
    }

    /**
     * Get database version
     */
    private String getDatabaseVersion(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT @@VERSION");
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
            dataType.toUpperCase().contains("REAL") ||
            dataType.toUpperCase().contains("NUMERIC") ||
            dataType.toUpperCase().contains("MONEY")
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
            dataType.toUpperCase().contains("NCHAR") ||
            dataType.toUpperCase().contains("NTEXT") ||
            dataType.toUpperCase().contains("VARCHAR") ||
            dataType.toUpperCase().contains("NVARCHAR")
        );
    }
}