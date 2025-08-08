package com.dataprofiler.dto;

import lombok.Data;

import java.util.List;

/**
 * Data Transfer Object for file loading results
 * Contains information about successfully loaded file data into SQLite tables
 */
@Data
public class FileLoadResult {
    
    private String dataSourceId; // Generated DataSource unique ID for this file
    private String originalFileName; // Original uploaded filename
    private long fileSize; // File size in bytes
    private List<LoadedTableInfo> loadedTables; // List of successfully loaded sheet information
    
    // Constructors
    public FileLoadResult() {}
    
    public FileLoadResult(String dataSourceId, String originalFileName, long fileSize, List<LoadedTableInfo> loadedTables) {
        this.dataSourceId = dataSourceId;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.loadedTables = loadedTables;
    }
    
    /**
     * Nested DTO for individual table loading information
     */
    @Data
    public static class LoadedTableInfo {
        private String sheetName; // Original sheet name from Excel
        private String tableName; // Generated table name in SQLite
        private int columnCount; // Number of columns in the table
        private long rowCount; // Number of data rows loaded
        private List<String> columnNames; // List of column names
        
        // Constructors
        public LoadedTableInfo() {}
        
        public LoadedTableInfo(String sheetName, String tableName, int columnCount, long rowCount, List<String> columnNames) {
            this.sheetName = sheetName;
            this.tableName = tableName;
            this.columnCount = columnCount;
            this.rowCount = rowCount;
            this.columnNames = columnNames;
        }
    }
}