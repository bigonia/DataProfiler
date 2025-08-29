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
 * PostgreSQL database profiler implementation
 * Implements adaptive profiling strategy for PostgreSQL databases
 */
@Component
public class PostgreSqlProfiler implements IDatabaseProfiler {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSqlProfiler.class);
    private static final long LARGE_TABLE_THRESHOLD = 1000000; // 1M rows
    private static final int SAMPLE_SIZE = 1000;

    @Override
    public RawProfileDataDto profile(DataSourceConfig dataSource, ProfilingTaskRequest.DataSourceScope scope) throws Exception {
        logger.info("Starting PostgreSQL profiling for data source: {}", dataSource.getSourceId());

        RawProfileDataDto rawData = new RawProfileDataDto(
            dataSource.getSourceId(), 
            DataSourceConfig.DataSourceType.POSTGRESQL
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
            metadata.put("postgresql_version", getDatabaseVersion(connection));
            metadata.put("total_tables_profiled", tables.size());
            rawData.setMetadata(metadata);
            
        } catch (Exception e) {
            logger.error("Failed to profile PostgreSQL data source: {}", dataSource.getSourceId(), e);
            throw e;
        }

        logger.info("Completed PostgreSQL profiling for data source: {}, profiled {} tables", 
                   dataSource.getSourceId(), rawData.getTables().size());
        return rawData;
    }

    @Override
    public boolean testConnection(DataSourceConfig dataSource) {
        try (Connection connection = createConnection(dataSource)) {
            return connection.isValid(5); // 5 second timeout
        } catch (Exception e) {
            logger.warn("PostgreSQL connection test failed for data source: {}", dataSource.getSourceId(), e);
            return false;
        }
    }

    @Override
    public String getSupportedType() {
        return "POSTGRESQL";
    }

    @Override
    public boolean supports(String dataSourceType) {
        return "POSTGRESQL".equalsIgnoreCase(dataSourceType) || "POSTGRES".equalsIgnoreCase(dataSourceType);
    }

    @Override
    public Map<String, List<String>> getDatabaseMetadata(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting database metadata for PostgreSQL data source: {}", dataSourceConfig.getSourceId());
        
        Map<String, List<String>> schemasWithTables = new LinkedHashMap<>();
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            // Get all schemas first
            List<String> schemas = getSchemasInternal(connection,dataSourceConfig.getDatabaseName());
            
            // For each schema, get its tables
            for (String schema : schemas) {
                List<String> tables = getTablesForSchema(connection, schema);
                schemasWithTables.put(schema, tables);
            }
            
            logger.info("Retrieved {} schemas with total tables for PostgreSQL data source: {}", 
                schemas.size(), dataSourceConfig.getSourceId());
            return schemasWithTables;
            
        } catch (SQLException e) {
            logger.error("Error getting database metadata for PostgreSQL data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve PostgreSQL database metadata", e);
        }
    }

    @Override
    public List<String> getSchemas(DataSourceConfig dataSourceConfig) throws Exception {
        logger.info("Getting schemas for PostgreSQL data source: {}", dataSourceConfig.getSourceId());
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> schemas = getSchemasInternal(connection, dataSourceConfig.getDatabaseName());
            
            logger.info("Retrieved {} schemas for PostgreSQL data source: {}", schemas.size(), dataSourceConfig.getSourceId());
            return schemas;
            
        } catch (SQLException e) {
            logger.error("Error getting schemas for PostgreSQL data source: {}", dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve PostgreSQL schemas", e);
        }
    }

    @Override
    public List<String> getTables(DataSourceConfig dataSourceConfig, String schema) throws Exception {
        logger.info("Getting tables for PostgreSQL schema: {} in data source: {}", schema, dataSourceConfig.getSourceId());
        
        try (Connection connection = createConnection(dataSourceConfig)) {
            List<String> tables = getTablesForSchema(connection, schema);
            
            logger.info("Retrieved {} tables for PostgreSQL schema: {}", tables.size(), schema);
            return tables;
            
        } catch (SQLException e) {
            logger.error("Error getting tables for PostgreSQL schema: {} in data source: {}", schema, dataSourceConfig.getSourceId(), e);
            throw new Exception("Failed to retrieve PostgreSQL tables for schema: " + schema, e);
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
        props.setProperty("ssl", "false");
        
        return DriverManager.getConnection(url, props);
    }

    /**
     * Build connection URL
     */
    private String buildConnectionUrl(DataSourceConfig dataSource) {
        if (dataSource.getConnectionUrl() != null && !dataSource.getConnectionUrl().isEmpty()) {
            return dataSource.getConnectionUrl();
        }
        
        return String.format("jdbc:postgresql://%s:%d/%s", 
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
        
        String sql = "SELECT schema_name FROM information_schema.schemata " +
                    "WHERE schema_name NOT IN ('information_schema', 'pg_catalog', 'pg_toast') " +
                    "AND schema_name NOT LIKE 'pg_temp_%' AND schema_name NOT LIKE 'pg_toast_temp_%' ";
        
        // If databaseName is specified, filter by it (for PostgreSQL, this would be schema filtering)
        if (StringUtils.hasText(databaseName)) {
            sql += "AND schema_name = ? ";
        }
        
        sql += "ORDER BY schema_name";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (StringUtils.hasText(databaseName)) {
                stmt.setString(1, databaseName);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schemas.add(rs.getString("schema_name"));
                }
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
            // Profile all tables in public schema by default
            String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'";
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
                    String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'";
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
                        tables.add(schema + "." + table);
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
        String schemaName = parts.length > 1 ? parts[0] : "public";
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
        String sql = "SELECT table_type FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableData.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    tableData.setTableType(rs.getString("table_type"));
                }
            }
        }
        
        // Get additional PostgreSQL-specific metadata
        String pgStatsSql = "SELECT n_tup_ins, n_tup_upd, n_tup_del, n_live_tup, n_dead_tup " +
                           "FROM pg_stat_user_tables WHERE schemaname = ? AND relname = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(pgStatsSql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableData.getTableName());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("n_tup_ins", rs.getLong("n_tup_ins"));
                    metadata.put("n_tup_upd", rs.getLong("n_tup_upd"));
                    metadata.put("n_tup_del", rs.getLong("n_tup_del"));
                    metadata.put("n_live_tup", rs.getLong("n_live_tup"));
                    metadata.put("n_dead_tup", rs.getLong("n_dead_tup"));
                    tableData.setTableMetadata(metadata);
                }
            }
        } catch (SQLException e) {
            logger.debug("Could not retrieve pg_stat_user_tables data for {}.{}", schemaName, tableData.getTableName());
        }
    }

    /**
     * Get row count using adaptive strategy
     */
    private long getRowCount(Connection connection, String schemaName, String tableName) throws SQLException {
        // First try to get approximate count from pg_class (fast)
        String approxSql = "SELECT c.reltuples::bigint as estimate " +
                          "FROM pg_class c " +
                          "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                          "WHERE n.nspname = ? AND c.relname = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(approxSql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long approxCount = rs.getLong("estimate");
                    
                    // If approximate count is reasonable, use exact count for small tables
                    if (approxCount < LARGE_TABLE_THRESHOLD && approxCount > 0) {
                        return getExactRowCount(connection, schemaName, tableName);
                    } else if (approxCount > 0) {
                        logger.debug("Using approximate row count for large table {}.{}: {}", schemaName, tableName, approxCount);
                        return approxCount;
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
        
        // Get column metadata from information_schema
        String sql = "SELECT column_name, data_type, character_maximum_length, numeric_precision, " +
                    "numeric_scale, is_nullable, column_default " +
                    "FROM information_schema.columns " +
                    "WHERE table_schema = ? AND table_name = ? " +
                    "ORDER BY ordinal_position";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RawProfileDataDto.ColumnData columnData = new RawProfileDataDto.ColumnData();
                    columnData.setColumnName(rs.getString("column_name"));
                    columnData.setDataType(rs.getString("data_type"));
                    columnData.setNativeType(rs.getString("data_type"));
                    
                    Integer maxLength = rs.getObject("character_maximum_length", Integer.class);
                    if (maxLength != null) {
                        columnData.setColumnSize(maxLength);
                    }
                    
                    Integer precision = rs.getObject("numeric_precision", Integer.class);
                    if (precision != null) {
                        columnData.setColumnSize(precision);
                    }
                    
                    Integer scale = rs.getObject("numeric_scale", Integer.class);
                    if (scale != null) {
                        columnData.setDecimalDigits(scale);
                    }
                    
                    columnData.setNullable("YES".equals(rs.getString("is_nullable")));
                    columnData.setDefaultValue(rs.getString("column_default"));
                    
                    // Profile column data
                    profileColumnData(connection, schemaName, tableName, columnData, useSampling);
                    
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
        
        // Add sampling for large tables using TABLESAMPLE
        if (useSampling) {
            sql.append(" TABLESAMPLE SYSTEM(1)"); // Sample approximately 1% of rows
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
        
        String sql = "SELECT DISTINCT " + quotedColumnName + " FROM " + fullTableName + 
                    " WHERE " + quotedColumnName + " IS NOT NULL";
        
        if (useSampling) {
            sql += " ORDER BY RANDOM() LIMIT 10";
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
    private List<RawProfileDataDto.IndexData> getIndexes(Connection connection, String schemaName, String tableName) throws SQLException {
        List<RawProfileDataDto.IndexData> indexes = new ArrayList<>();
        
        String sql = "SELECT i.relname as index_name, " +
                    "am.amname as index_type, " +
                    "ix.indisunique as is_unique, " +
                    "ix.indisprimary as is_primary, " +
                    "array_to_string(array_agg(a.attname ORDER BY c.ordinality), ',') as column_names " +
                    "FROM pg_class t " +
                    "JOIN pg_namespace n ON n.oid = t.relnamespace " +
                    "JOIN pg_index ix ON t.oid = ix.indrelid " +
                    "JOIN pg_class i ON i.oid = ix.indexrelid " +
                    "JOIN pg_am am ON i.relam = am.oid " +
                    "JOIN unnest(ix.indkey) WITH ORDINALITY c(attnum, ordinality) ON true " +
                    "JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = c.attnum " +
                    "WHERE n.nspname = ? AND t.relname = ? " +
                    "GROUP BY i.relname, am.amname, ix.indisunique, ix.indisprimary " +
                    "ORDER BY i.relname";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schemaName);
            stmt.setString(2, tableName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    RawProfileDataDto.IndexData indexData = new RawProfileDataDto.IndexData(
                        rs.getString("index_name"), 
                        rs.getString("index_type")
                    );
                    indexData.setIsUnique(rs.getBoolean("is_unique"));
                    indexData.setIsPrimary(rs.getBoolean("is_primary"));
                    
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
     * Get database version
     */
    private String getDatabaseVersion(Connection connection) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT version()");
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
            dataType.toLowerCase().contains("integer") ||
            dataType.toLowerCase().contains("bigint") ||
            dataType.toLowerCase().contains("smallint") ||
            dataType.toLowerCase().contains("decimal") ||
            dataType.toLowerCase().contains("numeric") ||
            dataType.toLowerCase().contains("real") ||
            dataType.toLowerCase().contains("double") ||
            dataType.toLowerCase().contains("money")
        );
    }

    /**
     * Check if data type is date/time
     */
    private boolean isDateType(String dataType) {
        return dataType != null && (
            dataType.toLowerCase().contains("date") ||
            dataType.toLowerCase().contains("time") ||
            dataType.toLowerCase().contains("timestamp") ||
            dataType.toLowerCase().contains("interval")
        );
    }

    /**
     * Check if data type is string
     */
    private boolean isStringType(String dataType) {
        return dataType != null && (
            dataType.toLowerCase().contains("character") ||
            dataType.toLowerCase().contains("varchar") ||
            dataType.toLowerCase().contains("char") ||
            dataType.toLowerCase().contains("text") ||
            dataType.toLowerCase().contains("json") ||
            dataType.toLowerCase().contains("xml")
        );
    }
}