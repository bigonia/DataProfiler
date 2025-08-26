package com.dataprofiler.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * 数据库工具类
 * 提供数据库操作的抽象，支持多种数据库类型
 */
@Component
public class DatabaseUtil {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);

    /**
     * 构建创建表的SQL
     */
    public String buildCreateTableSql(String schemaName, String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ");
        
        if (schemaName != null && !schemaName.isEmpty()) {
            sql.append(schemaName).append(".");
        }
        
        sql.append(tableName).append(" (\n");
        sql.append("  id BIGINT AUTO_INCREMENT PRIMARY KEY,\n");
        
        for (String column : columns) {
            sql.append("  ").append(column).append(" TEXT,\n");
        }
        
        // 添加元数据列
        sql.append("  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n");
        sql.append("  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP\n");
        sql.append(")");
        
        return sql.toString();
    }

    /**
     * 获取数据库类型
     */
    public String getDatabaseType(Connection conn) throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        String productName = metaData.getDatabaseProductName().toLowerCase();
        
        if (productName.contains("mysql")) {
            return "mysql";
        } else if (productName.contains("postgresql")) {
            return "postgresql";
        } else if (productName.contains("sqlite")) {
            return "sqlite";
        } else if (productName.contains("oracle")) {
            return "oracle";
        } else if (productName.contains("sql server")) {
            return "sqlserver";
        } else {
            return "unknown";
        }
    }

    /**
     * 构建创建Schema的SQL
     */
    private String buildCreateSchemaSql(String databaseType, String schemaName) {
        switch (databaseType.toLowerCase()) {
            case "mysql":
                return "CREATE DATABASE IF NOT EXISTS " + schemaName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
            case "postgresql":
                return "CREATE SCHEMA IF NOT EXISTS " + schemaName;
            case "sqlite":
                // SQLite不需要显式创建Schema
                return null;
            case "oracle":
                return "CREATE USER " + schemaName + " IDENTIFIED BY password DEFAULT TABLESPACE users";
            case "sqlserver":
                return "IF NOT EXISTS (SELECT * FROM sys.schemas WHERE name = '" + schemaName + "') EXEC('CREATE SCHEMA " + schemaName + "')";
            default:
                logger.warn("Unsupported database type for schema creation: {}", databaseType);
                return null;
        }
    }

    /**
     * 映射数据类型到特定数据库
     */
    public String mapDataType(String databaseType, String genericType) {
        switch (databaseType.toLowerCase()) {
            case "mysql":
                return mapToMySqlType(genericType);
            case "postgresql":
                return mapToPostgreSqlType(genericType);
            case "sqlite":
                return mapToSqliteType(genericType);
            default:
                return "TEXT"; // 默认类型
        }
    }

    private String mapToMySqlType(String genericType) {
        switch (genericType.toLowerCase()) {
            case "integer":
            case "int":
                return "INT";
            case "bigint":
            case "long":
                return "BIGINT";
            case "decimal":
            case "numeric":
                return "DECIMAL(10,2)";
            case "float":
                return "FLOAT";
            case "double":
                return "DOUBLE";
            case "boolean":
            case "bool":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "datetime":
            case "timestamp":
                return "DATETIME";
            case "time":
                return "TIME";
            case "varchar":
            case "string":
                return "VARCHAR(255)";
            case "text":
                return "TEXT";
            case "longtext":
                return "LONGTEXT";
            default:
                return "TEXT";
        }
    }

    private String mapToPostgreSqlType(String genericType) {
        switch (genericType.toLowerCase()) {
            case "integer":
            case "int":
                return "INTEGER";
            case "bigint":
            case "long":
                return "BIGINT";
            case "decimal":
            case "numeric":
                return "NUMERIC(10,2)";
            case "float":
                return "REAL";
            case "double":
                return "DOUBLE PRECISION";
            case "boolean":
            case "bool":
                return "BOOLEAN";
            case "date":
                return "DATE";
            case "datetime":
            case "timestamp":
                return "TIMESTAMP";
            case "time":
                return "TIME";
            case "varchar":
            case "string":
                return "VARCHAR(255)";
            case "text":
                return "TEXT";
            default:
                return "TEXT";
        }
    }

    private String mapToSqliteType(String genericType) {
        switch (genericType.toLowerCase()) {
            case "integer":
            case "int":
            case "bigint":
            case "long":
                return "INTEGER";
            case "decimal":
            case "numeric":
            case "float":
            case "double":
                return "REAL";
            case "boolean":
            case "bool":
                return "INTEGER"; // SQLite使用INTEGER存储布尔值
            case "date":
            case "datetime":
            case "timestamp":
            case "time":
                return "TEXT"; // SQLite使用TEXT存储日期时间
            case "varchar":
            case "string":
            case "text":
            case "longtext":
            default:
                return "TEXT";
        }
    }

    /**
     * 检查表是否存在
     */
    public boolean tableExists(DataSource dataSource, String schemaName, String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            
            // 根据数据库类型调整查询
            String databaseType = getDatabaseType(conn);
            String catalog = null;
            String schema = schemaName;
            
            if ("mysql".equals(databaseType)) {
                catalog = schemaName;
                schema = null;
            }
            
            var resultSet = metaData.getTables(catalog, schema, tableName, new String[]{"TABLE"});
            return resultSet.next();
        }
    }

    /**
     * 创建Schema（如果不存在）
     */
    public void createSchemaIfNotExists(DataSource dataSource, String schemaName) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            String databaseType = getDatabaseType(conn);
            String sql = buildCreateSchemaSql(databaseType, schemaName);

            if (sql != null) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(sql);
                    logger.info("Schema created or already exists: {}", schemaName);
                }
            }
        }
    }

    /**
     * 删除表
     */
    public void dropTable(DataSource dataSource, String schemaName, String tableName) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            String sql = "DROP TABLE IF EXISTS ";
            if (schemaName != null && !schemaName.isEmpty()) {
                sql += schemaName + ".";
            }
            sql += tableName;
            
            stmt.execute(sql);
            logger.info("Table dropped: {}.{}", schemaName, tableName);
        }
    }
}