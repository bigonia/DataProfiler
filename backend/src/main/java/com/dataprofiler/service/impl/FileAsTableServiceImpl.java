package com.dataprofiler.service.impl;

import com.dataprofiler.dto.FileLoadResult;
import com.dataprofiler.service.FileAsTableService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of FileAsTableService for converting Excel files to SQLite tables
 * Follows the design pattern of converting file sheets to database tables with format [fileID]_[sheetName]
 */
@Slf4j
@Service
public class FileAsTableServiceImpl implements FileAsTableService {
    
    @Value("${app.sqlite.database.path:data/core.db}")
    private String sqliteDatabasePath;
    
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final int MAX_COLUMN_NAME_LENGTH = 64;
    private static final int MAX_ROWS_TO_PROCESS = 100000; // Limit for large files
    
    @Override
    public FileLoadResult loadExcelFileToDatabase(String fileId, String filePath) {
        log.info("Starting to load file to database: fileId={}, filePath={}", fileId, filePath);
        
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("File not found: " + filePath);
        }
        
        String fileName = file.getName().toLowerCase();
        
        // Handle CSV files
        if (fileName.endsWith(".csv")) {
            return loadCsvFileToDatabase(fileId, file);
        }
        // Handle Excel files
        else if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            return loadExcelFileToDatabase(fileId, file);
        }
        else {
            throw new IllegalArgumentException("Unsupported file format. Only .csv, .xlsx and .xls files are supported.");
        }
    }
    
    /**
     * Load CSV file to database
     */
    private FileLoadResult loadCsvFileToDatabase(String fileId, File file) {
        log.info("Processing CSV file: {}", file.getName());
        
        List<FileLoadResult.LoadedTableInfo> loadedTables = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String tableName = generateTableName(fileId, "data");
            
            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new RuntimeException("CSV file is empty or has no header");
            }
            
            // Parse column names
            String[] headers = headerLine.split(",");
            List<String> columnNames = new ArrayList<>();
            for (String header : headers) {
                String cleanHeader = sanitizeIdentifier(header.trim().replace("\"", ""));
                columnNames.add(ensureUniqueColumnName(columnNames, cleanHeader));
            }
            
            try (Connection conn = getSqliteConnection()) {
                // Drop table if exists
                dropTableIfExists(conn, tableName);
                
                // Create table
                createTable(conn, tableName, columnNames);
                
                // Insert data
                long rowCount = insertCsvData(conn, tableName, reader, columnNames);
                
                FileLoadResult.LoadedTableInfo tableInfo = new FileLoadResult.LoadedTableInfo(
                    "data", tableName, columnNames.size(), rowCount, columnNames
                );
                loadedTables.add(tableInfo);
                
                log.info("Successfully loaded CSV as table '{}' with {} rows", tableName, rowCount);
                
            } catch (SQLException e) {
                log.error("Database error while processing CSV file: {}", e.getMessage(), e);
                throw new RuntimeException("Database error: " + e.getMessage(), e);
            }
            
        } catch (IOException e) {
            log.error("Failed to read CSV file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read CSV file: " + e.getMessage(), e);
        }
        
        FileLoadResult result = new FileLoadResult(fileId, file.getName(), file.length(), loadedTables);
        log.info("CSV file loading completed successfully");
        
        return result;
    }
    
    /**
     * Load Excel file to database
     */
    private FileLoadResult loadExcelFileToDatabase(String fileId, File file) {
        log.info("Processing Excel file: {}", file.getName());
        
        List<FileLoadResult.LoadedTableInfo> loadedTables = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(file)) {
            Workbook workbook = createWorkbook(file, fis);
            
            // Process each sheet in the workbook
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                log.debug("Processing sheet: {}", sheetName);
                
                try {
                    FileLoadResult.LoadedTableInfo tableInfo = processSheet(fileId, sheet);
                    if (tableInfo != null) {
                        loadedTables.add(tableInfo);
                        log.info("Successfully loaded sheet '{}' as table '{}' with {} rows", 
                                sheetName, tableInfo.getTableName(), tableInfo.getRowCount());
                    }
                } catch (Exception e) {
                    log.error("Failed to process sheet '{}': {}", sheetName, e.getMessage(), e);
                    // Continue processing other sheets
                }
            }
            
            workbook.close();
            
        } catch (IOException e) {
            log.error("Failed to read Excel file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read Excel file: " + e.getMessage(), e);
        }
        
        if (loadedTables.isEmpty()) {
            throw new RuntimeException("No sheets were successfully loaded from the Excel file");
        }
        
        FileLoadResult result = new FileLoadResult(fileId, file.getName(), file.length(), loadedTables);
        log.info("Excel file loading completed: {} sheets loaded successfully", loadedTables.size());
        
        return result;
    }
    
    /**
     * Create appropriate workbook instance based on file extension
     */
    private Workbook createWorkbook(File file, FileInputStream fis) throws IOException {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IllegalArgumentException("Unsupported Excel file format. Only .xlsx and .xls files are supported.");
        }
    }
    
    /**
     * Insert CSV data into database table
     */
    private long insertCsvData(Connection conn, String tableName, BufferedReader reader, List<String> columnNames) throws SQLException, IOException {
        String placeholders = String.join(",", columnNames.stream().map(c -> "?").toArray(String[]::new));
        String insertSql = String.format("INSERT INTO %s (%s) VALUES (%s)", 
                tableName, String.join(",", columnNames), placeholders);
        
        long rowCount = 0;
        String line;
        
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                
                String[] values = line.split(",");
                
                // Handle cases where there are fewer values than columns
                for (int i = 0; i < columnNames.size(); i++) {
                    String value = "";
                    if (i < values.length) {
                        value = values[i].trim().replace("\"", ""); // Remove quotes
                    }
                    
                    if (value.isEmpty()) {
                        pstmt.setNull(i + 1, Types.VARCHAR);
                    } else {
                        pstmt.setString(i + 1, value);
                    }
                }
                
                pstmt.executeUpdate();
                rowCount++;
                
                // Batch processing for better performance
                if (rowCount % 1000 == 0) {
                    log.debug("Processed {} rows", rowCount);
                }
            }
        }
        
        log.debug("Total rows inserted: {}", rowCount);
        return rowCount;
    }
    
    /**
     * Process a single sheet and convert it to SQLite table
     */
    private FileLoadResult.LoadedTableInfo processSheet(String fileId, Sheet sheet) {
        String sheetName = sheet.getSheetName();
        String tableName = generateTableName(fileId, sheetName);
        
        // Skip empty sheets
        if (sheet.getPhysicalNumberOfRows() == 0) {
            log.warn("Skipping empty sheet: {}", sheetName);
            return null;
        }
        
        try (Connection conn = getSqliteConnection()) {
            // Drop table if exists
            dropTableIfExists(conn, tableName);
            
            // Analyze sheet structure
            SheetAnalysis analysis = analyzeSheet(sheet);
            if (analysis.columnNames.isEmpty()) {
                log.warn("No valid columns found in sheet: {}", sheetName);
                return null;
            }
            
            // Create table
            createTable(conn, tableName, analysis.columnNames);
            
            // Insert data
            long rowCount = insertData(conn, tableName, sheet, analysis);
            
            return new FileLoadResult.LoadedTableInfo(
                sheetName, 
                tableName, 
                analysis.columnNames.size(), 
                rowCount, 
                analysis.columnNames
            );
            
        } catch (SQLException e) {
            log.error("Database error while processing sheet '{}': {}", sheetName, e.getMessage(), e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }
    
    /**
     * Generate table name following the format [fileID]_[sheetName]
     */
    private String generateTableName(String fileId, String sheetName) {
        // Sanitize names for SQL table naming
        String sanitizedFileId = sanitizeIdentifier(fileId);
        String sanitizedSheetName = sanitizeIdentifier(sheetName);
        return sanitizedFileId + "_" + sanitizedSheetName;
    }
    
    /**
     * Sanitize identifier for SQL usage
     */
    private String sanitizeIdentifier(String identifier) {
        String sanitized = identifier.replaceAll("[^a-zA-Z0-9_]", "_")
                                   .replaceAll("_{2,}", "_")
                                   .replaceAll("^_|_$", "");
        return sanitized.substring(0, Math.min(sanitized.length(), MAX_COLUMN_NAME_LENGTH));
    }
    
    /**
     * Analyze sheet structure to determine columns
     */
    private SheetAnalysis analyzeSheet(Sheet sheet) {
        SheetAnalysis analysis = new SheetAnalysis();
        
        // Use first row as header
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            return analysis;
        }
        
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            String columnName = getCellValueAsString(cell);
            
            if (columnName == null || columnName.trim().isEmpty()) {
                columnName = "Column_" + (i + 1);
            }
            
            columnName = sanitizeIdentifier(columnName.trim());
            if (columnName.isEmpty()) {
                columnName = "Column_" + (i + 1);
            }
            
            // Ensure unique column names
            String uniqueColumnName = ensureUniqueColumnName(analysis.columnNames, columnName);
            analysis.columnNames.add(uniqueColumnName);
        }
        
        analysis.dataStartRow = sheet.getFirstRowNum() + 1;
        return analysis;
    }
    
    /**
     * Ensure column name is unique within the list
     */
    private String ensureUniqueColumnName(List<String> existingNames, String columnName) {
        String uniqueName = columnName;
        int counter = 1;
        
        while (existingNames.contains(uniqueName)) {
            uniqueName = columnName + "_" + counter;
            counter++;
        }
        
        return uniqueName;
    }
    
    /**
     * Get SQLite database connection
     */
    private Connection getSqliteConnection() throws SQLException {
        try {
            Class.forName(SQLITE_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
        
        // Ensure directory exists
        File dbFile = new File(sqliteDatabasePath);
        dbFile.getParentFile().mkdirs();
        
        String url = "jdbc:sqlite:" + sqliteDatabasePath;
        Connection conn = DriverManager.getConnection(url);
        
        // Enable foreign keys and set pragmas for better performance and prevent locking
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA busy_timeout = 30000"); // 30 seconds timeout to prevent database locking
        }
        
        return conn;
    }
    
    /**
     * Drop table if it exists
     */
    private void dropTableIfExists(Connection conn, String tableName) throws SQLException {
        String sql = "DROP TABLE IF EXISTS " + tableName;
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    /**
     * Create table with dynamic columns
     */
    private void createTable(Connection conn, String tableName, List<String> columnNames) throws SQLException {
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        sql.append(tableName).append(" (");
        
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(columnNames.get(i)).append(" TEXT");
        }
        
        sql.append(")");
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql.toString());
        }
    }
    
    /**
     * Insert data from sheet to table
     */
    private long insertData(Connection conn, String tableName, Sheet sheet, SheetAnalysis analysis) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(tableName).append(" VALUES (");
        
        for (int i = 0; i < analysis.columnNames.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append("?");
        }
        sql.append(")");
        
        long rowCount = 0;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            conn.setAutoCommit(false);
            
            for (int rowIndex = analysis.dataStartRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                if (rowCount >= MAX_ROWS_TO_PROCESS) {
                    log.warn("Reached maximum row limit ({}), stopping data insertion", MAX_ROWS_TO_PROCESS);
                    break;
                }
                
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                
                // Check if row is empty
                boolean isEmpty = true;
                for (int colIndex = 0; colIndex < analysis.columnNames.size(); colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    String value = getCellValueAsString(cell);
                    if (value != null && !value.trim().isEmpty()) {
                        isEmpty = false;
                        break;
                    }
                }
                
                if (isEmpty) {
                    continue;
                }
                
                // Set parameter values
                for (int colIndex = 0; colIndex < analysis.columnNames.size(); colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    String value = getCellValueAsString(cell);
                    pstmt.setString(colIndex + 1, value);
                }
                
                pstmt.addBatch();
                rowCount++;
                
                // Execute batch every 1000 rows
                if (rowCount % 1000 == 0) {
                    pstmt.executeBatch();
                }
            }
            
            // Execute remaining batch
            pstmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        }
        
        return rowCount;
    }
    
    /**
     * Convert cell value to string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Check if it's a whole number
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (Exception e) {
                    try {
                        return cell.getStringCellValue();
                    } catch (Exception e2) {
                        return cell.getCellFormula();
                    }
                }
            case BLANK:
            case _NONE:
            default:
                return null;
        }
    }
    
    @Override
    public void loadExcelFileToDatabase(MultipartFile file, String dataSourceName) {
        // Legacy method implementation - for backward compatibility
        // This method is kept for existing file upload functionality
        // Implementation can be added if needed for file upload scenarios
        throw new UnsupportedOperationException("Legacy file upload method not implemented. Use loadExcelFileToDatabase(String, String) instead.");
    }
    
    /**
     * Inner class for sheet analysis results
     */
    private static class SheetAnalysis {
        List<String> columnNames = new ArrayList<>();
        int dataStartRow = 1;
    }
}