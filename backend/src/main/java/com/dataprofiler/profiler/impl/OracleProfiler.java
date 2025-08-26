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
import java.util.LinkedHashMap;

/**
 * Oracle database profiler implementation
 * Implements adaptive profiling strategy for Oracle databases
 */
@Component
public class OracleProfiler implements IDatabaseProfiler {

    private static final Logger logger = LoggerFactory.getLogger(OracleProfiler.class);
    private static final long LARGE_TABLE_THRESHOLD = 1000000; // 1M rows
    private static final int SAMPLE_SIZE = 1000;

    @Override
    public RawProfileDataDto profile(DataSourceConfig dataSource, ProfilingTaskRequest.DataSourceScope scope) throws Exception {
        logger.info("Starting Oracle profiling for data source: {}", dataSource.getSourceId());

        RawProfileDataDto rawData = new RawProfileDataDto(
            dataSource.getSourceId(), 
            DataSourceConfig.DataSourceType.ORACLE
        );

        try (Connection connection = createConnection(dataSource)) {
            rawData.setDatabaseName(getCurrentSchema(connection));
            
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
            metadata.put("oracle_version", getDatabaseVersion(connection));
            metadata.put("total_tables_profiled", tables.size());
            rawData.setMetadata(metadata);
            
        } catch (Exception e) {
            logger.error("Failed to profile Oracle data source: {}", dataSource.getSourceId(), e);
            throw e;
        }

        logger.info("Completed Oracle profiling for data source: {}, profiled {} tables", 
                   dataSource.getSourceId(), rawData.getTables().size());
        return rawData;
    }

    @Override
    public boolean testConnection(DataSourceConfig dataSource) {
        try (Connection connection = createConnection(dataSource)) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            logger.warn("Oracle connection test failed for data source: {}", dataSource.getSourceId(), e);
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return "ORACLE";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return "ORACLE".equalsIgnoreCase(dataSourceType);
    }

    @Override
    public Map<String, List<String>> getDatabaseMetadata(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting database metadata for Oracle data source: {}", dataSourceConfig.getSourceId());
        
        Map<String, List<String>> schemasWithTables = new LinkedHashMap<>();
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            // Get all schemas first
            List<String> schemas = getSchemasInternal(connection);
            
            // For each schema, get its tables
            for (String schema : schemas) {
                List<String> tables = getTablesForSchema(connection, schema);
                schemasWithTables.put(schema, tables);
            }
            
            logger.info("Retrieved {} schemas with total tables for Oracle data source: {}", 
                schemas.size(), dataSourceConfig.getSourceId());
            return schemasWithTables;
            
        } catch (SQLException e) {
            logger.error("Error getting database metadata for Oracle data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve Oracle database metadata", e);
        }
    }

    @Override
    public List<String> getSchemas(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting schemas for Oracle data source: {}", dataSourceConfig.getSourceId());
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> schemas = getSchemasInternal(connection);
            
            logger.info("Retrieved {} schemas for Oracle data source: {}", schemas.size(), dataSourceConfig.getSourceId());
            return schemas;
            
        } catch (SQLException e) {
            logger.error("Error getting schemas for Oracle data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve Oracle schemas", e);
        }
    }

    @Override
    public List<String> getTables(DataSourceConfig dataSourceConfig, String schema) throws Exception {
        logger.info("Getting tables for Oracle schema: {} in data source: {}", schema, dataSourceConfig.getSourceId());
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> tables = getTablesForSchema(connection, schema);
            
            logger.info("Retrieved {} tables for Oracle schema: {}", tables.size(), schema);
            return tables;
            
        } catch (SQLException e) {
            logger.error("Error getting tables for Oracle schema: {} in data source: {}", schema, dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve Oracle tables for schema: " + schema, e);
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
        props.setProperty("oracle.jdbc.ReadTimeout", "30000");
        
        return DriverManager.getConnection(url, props);
    }

    /**
     * Build connection URL
     */
    private String buildConnectionUrl(DataSourceConfig dataSource) {
        if (dataSource.getConnectionUrl() != null && !dataSource.getConnectionUrl().isEmpty()) {
            return dataSource.getConnectionUrl();
        }
        
        // Oracle connection URL format: jdbc:oracle:thin:@host:port:sid or jdbc:oracle:thin:@host:port/service
        String serviceName = dataSource.getDatabaseName();
        if (serviceName != null && !serviceName.isEmpty()) {
            return String.format("jdbc:oracle:thin:@%s:%d/%s", 
                               dataSource.getHost(), 
                               dataSource.getPort(), 
                               serviceName);
        } else {
            return String.format("jdbc:oracle:thin:@%s:%d:XE", 
                               dataSource.getHost(), 
                               dataSource.getPort());
        }
    }

    /**
     * Get all schemas from the database
     * 
     * @param connection Database connection
     * @return List of schema names
     * @throws SQLException if query fails
     */
    private List<String> getSchemasInternal(Connection connection) throws SQLException {
        List<String> schemas = new ArrayList<>();
        
        String sql = "SELECT username FROM all_users " +
                    "WHERE username NOT IN ('SYS', 'SYSTEM', 'DBSNMP', 'SYSMAN', 'OUTLN', 'MGMT_VIEW', " +
                    "'DIP', 'ORACLE_OCM', 'APPQOSSYS', 'WMSYS', 'EXFSYS', 'CTXSYS', 'XDB', 'ANONYMOUS', " +
                    "'XS$NULL', 'OJVMSYS', 'DVF', 'DVSYS', 'DBSFWUSER', 'REMOTE_SCHEDULER_AGENT', " +
                    "'DBA', 'RESOURCE', 'CONNECT', 'PUBLIC') " +
                    "ORDER BY username";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                schemas.add(rs.getString("username"));
            }
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
        
        String sql = "SELECT table_name FROM all_tables " +
                    "WHERE owner = ? " +
                    "ORDER BY table_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema.toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        }
        
        return tables;
    }

    /**
     * Get current schema name
     */
    private String getCurrentSchema(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') FROM DUAL");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        }
        return "UNKNOWN";
    }

    /**
     * Get list of tables to profile based on scope
     */
    private List<String> getTablesList(Connection connection, ProfilingTaskRequest.DataSourceScope scope) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        if (scope == null || scope.getSchemas() == null || scope.getSchemas().isEmpty()) {
            // Profile all tables in current schema
            String currentSchema = getCurrentSchema(connection);
            String sql = "SELECT table_name FROM user_tables ORDER BY table_name";
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        } else {
            // Profile specific tables from scope
            for (Map.Entry<String, List<String>> entry : scope.getSchemas().entrySet()) {
                String schema = entry.getKey().toUpperCase(); // Oracle schema names are typically uppercase
                List<String> tablesToInclude = entry.getValue();
                
                if (tablesToInclude.isEmpty()) {
                    // Include all tables from this schema
                    String sql = "SELECT table_name FROM all_tables WHERE owner = ? ORDER BY table_name";
                    try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                        stmt.setString(1, schema);
                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                tables.add(schema + "." + rs.getString("table_name"));
                            }
                        }
                    }
                } else {
                    // Include specific tables with schema prefix
                    for (String table : tablesToInclude) {
                        tables.add(schema + "." + table.toUpperCase());
                    }
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
        
        // Extract schema and table name
        String[] parts = tableName.split("\\.");
        String schemaName = parts.length > 1 ? parts[0] : getCurrentSchema(connection);
        String actualTableName = parts.length > 1 ? parts[1] : tableName;
        
        RawProfileDataDto.TableData tableData = new RawProfileDataDto.TableData(actualTableName, schemaName);
        
        // Get table metadata
        getTableMetadata(connection, tableData, schemaName);
        
        // Get row count (adaptive: exact vs approximate)
        long rowCount = getRowCount(connection, schemaName, actualTableName);
        tableData.setRowCount(rowCount);
        
        // Determine if we should use sampling for large tables
        boolean useSampling = rowCount > LARGE_TABLE_THRESHOLD;
        
        // Get column information and profile each column
        List<RawProfileDataDto.ColumnData> columns = profileColumns(connection, schemaName, actualTableName, useSampling);
        tableData.setColumns(columns);
        
        // Get index information
        List<RawProfileDataDto.IndexData> indexes = getIndexes(connection, schemaName, actualTableName);
        tableData.setIndexes(indexes);
        
        return tableData;
    }

    /**
     * Get table metadata
     */
    private void getTableMetadata(Connection connection, RawProfileDataDto.TableData tableData, String schemaName) throws SQLException {
        String sql = "SELECT tablespace_name, num_rows, blocks, avg_row_len, last_analyzed " +
                    "FROM all_tables WHERE owner = ? AND table_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableData.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tableData.setTableType("BASE TABLE");
                    
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("tablespace_name", rs.getString("tablespace_name"));
                    metadata.put("num_rows", rs.getLong("num_rows"));
                    metadata.put("blocks", rs.getLong("blocks"));
                    metadata.put("avg_row_len", rs.getLong("avg_row_len"));
                    metadata.put("last_analyzed", rs.getTimestamp("last_analyzed"));
                    tableData.setTableMetadata(metadata);
                }
            }
        }
    }

    /**
     * Get row count using adaptive strategy
     */
    private long getRowCount(Connection connection, String schemaName, String tableName) throws SQLException {
        // First try to get approximate count from all_tables (fast)
        String approxSql = "SELECT num_rows FROM all_tables WHERE owner = ? AND table_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(approxSql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long approxCount = rs.getLong("num_rows");
                    
                    // If approximate count is reasonable and recent, use it for large tables
                    if (approxCount > LARGE_TABLE_THRESHOLD && approxCount > 0) {
                        logger.debug("Using approximate row count for large table {}.{}: {}", schemaName, tableName, approxCount);
                        return approxCount;
                    } else if (approxCount > 0 && approxCount < LARGE_TABLE_THRESHOLD) {
                        // For small tables, get exact count
                        return getExactRowCount(connection, schemaName, tableName);
                    }
                }
            }
        }
        
        // Fallback to exact count
        return getExactRowCount(connection, schemaName, tableName);
    }

    /**
     * Get exact row count
     */
    private long getExactRowCount(Connection connection, String schemaName, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM \"" + schemaName + "\".\"" + tableName + "\"";
        
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
    private List<RawProfileDataDto.ColumnData> profileColumns(Connection connection, String schemaName, String tableName, boolean useSampling) throws SQLException {
        List<RawProfileDataDto.ColumnData> columns = new ArrayList<>();
        
        // Get column metadata from all_tab_columns
        String sql = "SELECT column_name, data_type, data_length, data_precision, data_scale, " +
                    "nullable, data_default, num_distinct, num_nulls, density " +
                    "FROM all_tab_columns " +
                    "WHERE owner = ? AND table_name = ? " +
                    "ORDER BY column_id";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RawProfileDataDto.ColumnData columnData = new RawProfileDataDto.ColumnData();
                    columnData.setColumnName(rs.getString("column_name"));
                    columnData.setDataType(rs.getString("data_type"));
                    columnData.setNativeType(rs.getString("data_type"));
                    columnData.setColumnSize(rs.getInt("data_length"));
                    
                    Integer precision = rs.getObject("data_precision", Integer.class);
                    if (precision != null) {
                        columnData.setColumnSize(precision);
                    }
                    
                    Integer scale = rs.getObject("data_scale", Integer.class);
                    if (scale != null) {
                        columnData.setDecimalDigits(scale);
                    }
                    
                    columnData.setNullable("Y".equals(rs.getString("nullable")));
                    columnData.setDefaultValue(rs.getString("data_default"));
                    
                    // Use Oracle statistics if available
                    Long numDistinct = rs.getObject("num_distinct", Long.class);
                    Long numNulls = rs.getObject("num_nulls", Long.class);
                    
                    if (numDistinct != null) {
                        columnData.setUniqueCount(numDistinct);
                    }
                    if (numNulls != null) {
                        columnData.setNullCount(numNulls);
                    }
                    
                    // Profile column data if statistics are not available or incomplete
                    if (numDistinct == null || numNulls == null) {
                        profileColumnData(connection, schemaName, tableName, columnData, useSampling);
                    }
                    
                    columns.add(columnData);
                }
            }
        }
        
        return columns;
    }

    /**
     * Profile individual column data
     */
    private void profileColumnData(Connection connection, String schemaName, String tableName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        String columnName = columnData.getColumnName();
        String fullTableName = "\"" + schemaName + "\".\"" + tableName + "\"";
        String quotedColumnName = "\"" + columnName + "\"";
        
        // Build profiling query
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("COUNT(*) as total_count, ");
        sql.append("COUNT(").append(quotedColumnName).append(") as non_null_count, ");
        sql.append("COUNT(DISTINCT ").append(quotedColumnName).append(") as unique_count");
        
        // Add min/max for numeric and date types
        if (isNumericType(columnData.getDataType()) || isDateType(columnData.getDataType())) {
            sql.append(", MIN(").append(quotedColumnName).append(") as min_value");
            sql.append(", MAX(").append(quotedColumnName).append(") as max_value");
        }
        
        // Add length statistics for string types
        if (isStringType(columnData.getDataType())) {
            sql.append(", AVG(LENGTH(").append(quotedColumnName).append(")) as avg_length");
            sql.append(", MAX(LENGTH(").append(quotedColumnName).append(")) as max_length");
            sql.append(", MIN(LENGTH(").append(quotedColumnName).append(")) as min_length");
        }
        
        sql.append(" FROM ").append(fullTableName);
        
        // Add sampling for large tables using SAMPLE
        if (useSampling) {
            sql.append(" SAMPLE(1)"); // Sample approximately 1% of rows
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString());
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                long totalCount = rs.getLong("total_count");
                long nonNullCount = rs.getLong("non_null_count");
                
                // Only update if we don't have statistics from all_tab_columns
                if (columnData.getTotalCount() == 0) {
                    columnData.setTotalCount(totalCount);
                }
                if (columnData.getNullCount() == 0) {
                    columnData.setNullCount(totalCount - nonNullCount);
                }
                if (columnData.getUniqueCount() == 0) {
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
        getSampleValues(connection, schemaName, tableName, columnData, useSampling);
    }

    /**
     * Get sample values for a column
     */
    private void getSampleValues(Connection connection, String schemaName, String tableName, RawProfileDataDto.ColumnData columnData, boolean useSampling) throws SQLException {
        String fullTableName = "\"" + schemaName + "\".\"" + tableName + "\"";
        String quotedColumnName = "\"" + columnData.getColumnName() + "\"";
        
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT ").append(quotedColumnName);
        sql.append(" FROM ").append(fullTableName);
        sql.append(" WHERE ").append(quotedColumnName).append(" IS NOT NULL");
        
        if (useSampling) {
            sql.append(" AND ROWNUM <= 10 ORDER BY DBMS_RANDOM.VALUE");
        } else {
            sql.append(" AND ROWNUM <= 10");
        }
        
        List<Object> sampleValues = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString());
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
    private List<RawProfileDataDto.IndexData> getIndexes(Connection connection, String schemaName, String tableName) throws SQLException {
        List<RawProfileDataDto.IndexData> indexes = new ArrayList<>();
        
        String sql = "SELECT i.index_name, i.index_type, i.uniqueness, " +
                    "LISTAGG(ic.column_name, ',') WITHIN GROUP (ORDER BY ic.column_position) as column_names " +
                    "FROM all_indexes i " +
                    "JOIN all_ind_columns ic ON i.owner = ic.index_owner AND i.index_name = ic.index_name " +
                    "WHERE i.owner = ? AND i.table_name = ? " +
                    "GROUP BY i.index_name, i.index_type, i.uniqueness " +
                    "ORDER BY i.index_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RawProfileDataDto.IndexData indexData = new RawProfileDataDto.IndexData(
                        rs.getString("index_name"), 
                        rs.getString("index_type")
                    );
                    indexData.setIsUnique("UNIQUE".equals(rs.getString("uniqueness")));
                    
                    // Check if it's a primary key index
                    String indexName = rs.getString("index_name");
                    indexData.setIsPrimary(isPrimaryKeyIndex(connection, schemaName, tableName, indexName));
                    
                    String columnNames = rs.getString("column_names");
                    if (columnNames != null) {
                        indexData.setColumnNames(Arrays.asList(columnNames.split(",")));
                    }
                    
                    indexes.add(indexData);
                }
            }
        }
        
        return indexes;
    }

    /**
     * Check if an index is a primary key index
     */
    private boolean isPrimaryKeyIndex(Connection connection, String schemaName, String tableName, String indexName) throws SQLException {
        String sql = "SELECT constraint_name FROM all_constraints " +
                    "WHERE owner = ? AND table_name = ? AND constraint_type = 'P' AND index_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            stmt.setString(3, indexName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Get database version
     */
    private String getDatabaseVersion(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT banner FROM v$version WHERE ROWNUM = 1");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            // Fallback if v$version is not accessible
            try (PreparedStatement stmt = connection.prepareStatement("SELECT version FROM product_component_version WHERE product LIKE 'Oracle%' AND ROWNUM = 1");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return "Unknown";
    }

    /**
     * Check if data type is numeric
     */
    private boolean isNumericType(String dataType) {
        return dataType != null && (
            dataType.toUpperCase().contains("NUMBER") ||
            dataType.toUpperCase().contains("INTEGER") ||
            dataType.toUpperCase().contains("FLOAT") ||
            dataType.toUpperCase().contains("BINARY_FLOAT") ||
            dataType.toUpperCase().contains("BINARY_DOUBLE")
        );
    }

    /**
     * Check if data type is date/time
     */
    private boolean isDateType(String dataType) {
        return dataType != null && (
            dataType.toUpperCase().contains("DATE") ||
            dataType.toUpperCase().contains("TIMESTAMP") ||
            dataType.toUpperCase().contains("INTERVAL")
        );
    }

    /**
     * Check if data type is string
     */
    private boolean isStringType(String dataType) {
        return dataType != null && (
            dataType.toUpperCase().contains("CHAR") ||
            dataType.toUpperCase().contains("VARCHAR") ||
            dataType.toUpperCase().contains("CLOB") ||
            dataType.toUpperCase().contains("NCHAR") ||
            dataType.toUpperCase().contains("NVARCHAR") ||
            dataType.toUpperCase().contains("NCLOB")
        );
    }
}