package com.dataprofiler.util;

import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.FileMetadata;
import com.dataprofiler.repository.DataSourceConfigRepository;
import com.dataprofiler.repository.FileMetadataRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 文件转换工具类
 * 负责将文件转换为数据库表结构，使用系统配置的数据库连接
 */
@Component
public class FileConversionUtil implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(FileConversionUtil.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    private DatabaseUtil databaseUtil;

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Value("${file.conversion.table-prefix:file_}")
    private String tablePrefix;

    @Value("${file.conversion.schema-name:file_data}")
    private String schemaName;

    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
        ".xlsx", ".xls", ".csv", ".json", ".xml"
    );

    /**
     * 将文件转换为数据源
     * @param fileId 文件ID
     * @param dataSourceName 自定义数据源名称
     * @return 转换后的数据源配置
     */
    public DataSourceConfig convertFileToDataSource(Long fileId, String dataSourceName) {
        logger.info("Starting file conversion for fileId: {}", fileId);
        
        Optional<FileMetadata> fileMetadataOpt = fileMetadataRepository.findById(fileId);
        if (!fileMetadataOpt.isPresent()) {
            throw new IllegalArgumentException("File not found with ID: " + fileId);
        }
        
        FileMetadata fileMetadata = fileMetadataOpt.get();
        
        try {
            // 检查文件格式是否支持
            if (!isFormatSupported(fileMetadata.getOriginalFilename())) {
                throw new UnsupportedOperationException("Unsupported file format: " + fileMetadata.getOriginalFilename());
            }
            
            // 创建Schema（如果不存在）
            databaseUtil.createSchemaIfNotExists(dataSource, schemaName);
            
            // 根据文件类型进行转换
            String fileExtension = getFileExtension(fileMetadata.getOriginalFilename());
            int tableCount = 0;
            
            switch (fileExtension.toLowerCase()) {
                case ".xlsx":
                case ".xls":
                    tableCount = processExcelFile(fileMetadata, dataSourceName);
                    break;
                case ".csv":
                    tableCount = processCsvFile(fileMetadata, dataSourceName);
                    break;
                default:
                    throw new UnsupportedOperationException("File format not yet implemented: " + fileExtension);
            }
            
            // 创建数据源配置
            DataSourceConfig dataSourceConfig = createDataSourceConfig(fileMetadata, dataSourceName, tableCount);
            dataSourceConfig = dataSourceConfigRepository.save(dataSourceConfig);
            
            // 更新文件元数据
            updateFileMetadata(fileMetadata, dataSourceConfig.getId(), tableCount, null);
            
            logger.info("File conversion completed successfully for fileId: {}, created {} tables", fileId, tableCount);
            return dataSourceConfig;
            
        } catch (Exception e) {
            logger.error("File conversion failed for fileId: {}", fileId, e);
            // 更新文件元数据记录错误
            updateFileMetadata(fileMetadata, null, 0, e.getMessage());
            throw new RuntimeException("File conversion failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理Excel文件
     */
    private int processExcelFile(FileMetadata fileMetadata, String dataSourceName) throws Exception {
        logger.info("Processing Excel file: {}", fileMetadata.getOriginalFilename());
        
        try (FileInputStream fis = new FileInputStream(fileMetadata.getFilePath());
             Workbook workbook = createWorkbook(fis, fileMetadata.getOriginalFilename())) {
            
            int tableCount = 0;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet.getPhysicalNumberOfRows() > 0) {
                    processSheet(sheet, dataSourceName, i);
                    tableCount++;
                }
            }
            return tableCount;
        }
    }
    
    /**
     * 处理Excel工作表
     */
    private void processSheet(Sheet sheet, String dataSourceName, int sheetIndex) throws SQLException {
        String tableName = tablePrefix + dataSourceName.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_") + "_sheet_" + sheet.getSheetName();
        
        // 获取列信息
        List<String> columns = extractColumns(sheet);
        if (columns.isEmpty()) {
            logger.warn("No columns found in sheet: {}", sheet.getSheetName());
            return;
        }
        
        // 创建表
        String createTableSql = databaseUtil.buildCreateTableSql(schemaName, tableName, columns);
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSql);
            logger.info("Created table: {}.{}", schemaName, tableName);
        }
        
        // 插入数据
        insertSheetData(sheet, tableName, columns);
    }
    
    /**
     * 提取Excel列信息
     */
    private List<String> extractColumns(Sheet sheet) {
        List<String> columns = new ArrayList<>();
        Row headerRow = sheet.getRow(0);
        if (headerRow != null) {
            for (Cell cell : headerRow) {
                String columnName = getCellValueAsString(cell);
                if (columnName != null && !columnName.trim().isEmpty()) {
                    // 清理列名
                    columnName = columnName.trim().replaceAll("[^a-zA-Z0-9_]", "_");
                    if (columnName.matches("^\\d.*")) {
                        columnName = "col_" + columnName;
                    }
                    columns.add(columnName);
                }
            }
        }
        return columns;
    }
    
    /**
     * 插入Excel数据
     */
    private void insertSheetData(Sheet sheet, String tableName, List<String> columns) throws SQLException {
        String insertSql = buildInsertSql(schemaName, tableName, columns);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            
            int batchSize = 0;
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                
                for (int colIndex = 0; colIndex < columns.size(); colIndex++) {
                    Cell cell = row.getCell(colIndex);
                    String value = getCellValueAsString(cell);
                    pstmt.setString(colIndex + 1, value);
                }
                
                pstmt.addBatch();
                batchSize++;
                
                if (batchSize % 1000 == 0) {
                    pstmt.executeBatch();
                    batchSize = 0;
                }
            }
            
            if (batchSize > 0) {
                pstmt.executeBatch();
            }
            
            logger.info("Inserted data into table: {}.{}", schemaName, tableName);
        }
    }
    
    /**
     * 处理CSV文件
     */
    private int processCsvFile(FileMetadata fileMetadata, String dataSourceName) throws Exception {
        logger.info("Processing CSV file: {}", fileMetadata.getOriginalFilename());
        
        String tableName = tablePrefix + dataSourceName.toLowerCase().replaceAll("[^a-zA-Z0-9_]", "_");
        
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileMetadata.getFilePath()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV file is empty");
            }
            
            // 解析列
            List<String> columns = parseColumns(headerLine);
            
            // 创建表
            String createTableSql = databaseUtil.buildCreateTableSql(schemaName, tableName, columns);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSql);
                logger.info("Created table: {}.{}", schemaName, tableName);
            }
            
            // 插入数据
            insertCsvData(reader, tableName, columns);
            
            return 1;
        }
    }
    
    /**
     * 解析CSV列
     */
    private List<String> parseColumns(String headerLine) {
        List<String> columns = new ArrayList<>();
        String[] headers = headerLine.split(",");
        
        for (String header : headers) {
            String columnName = header.trim().replaceAll("[\"']", "").replaceAll("[^a-zA-Z0-9_]", "_");
            if (columnName.matches("^\\d.*")) {
                columnName = "col_" + columnName;
            }
            columns.add(columnName);
        }
        
        return columns;
    }
    
    /**
     * 插入CSV数据
     */
    private void insertCsvData(BufferedReader reader, String tableName, List<String> columns) throws SQLException, IOException {
        String insertSql = buildInsertSql(schemaName, tableName, columns);
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {

            int count = 0;
            String line;
            int batchSize = 0;
            
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                
                for (int i = 0; i < columns.size(); i++) {
                    String value = i < values.length ? values[i].trim().replaceAll("[\"']", "") : "";
                    pstmt.setString(i + 1, value);
                    count++;
                }
                
                pstmt.addBatch();
                batchSize++;
                
                if (batchSize % 1000 == 0) {
                    pstmt.executeBatch();
                    batchSize = 0;
                }
            }
            
            if (batchSize > 0) {
                pstmt.executeBatch();
            }
            
            logger.info("Inserted CSV data into table: {}.{} {}", schemaName, tableName,count);
        }
    }

    @Value("${spring.datasource.username}")
    public String username;
    @Value("${spring.datasource.password}")
    public String password;

    /**
     * 创建数据源配置
     */
    private DataSourceConfig createDataSourceConfig(FileMetadata fileMetadata, String dataSourceName, int tableCount) {
        DataSourceConfig config = new DataSourceConfig();
        config.setSourceId(fileMetadata.getFilename());
        config.setName(dataSourceName);
        config.setType(DataSourceConfig.DataSourceType.FILE);
        config.setDescription("Converted from file: " + fileMetadata.getOriginalFilename() + ", Tables: " + tableCount);
        
        // 使用当前系统数据库配置
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            config.setProperties(new HashMap<>(){{
                put("host", extractHostFromUrl(url));
                put("port", String.valueOf(extractPortFromUrl(url)));
                put("database", schemaName);
                put("username", username);
                put("password", password);

            }});
        } catch (SQLException e) {
            logger.warn("Failed to extract database info from connection", e);
        }
        
        return config;
    }
    
    /**
     * 更新文件元数据
     */
    private void updateFileMetadata(FileMetadata fileMetadata, Long dataSourceId, int tableCount, String error) {
        fileMetadata.setConverted(error == null);
        fileMetadata.setDataSourceId(dataSourceId);
        fileMetadata.setConvertedAt(LocalDateTime.now());
        fileMetadata.setTableCount(tableCount);
        fileMetadata.setConversionError(error);
        
        fileMetadataRepository.save(fileMetadata);
    }
    
    // 辅助方法
    
    private Workbook createWorkbook(FileInputStream fis, String filename) throws IOException {
        if (filename.endsWith(".xlsx")) {
            return new XSSFWorkbook(fis);
        } else if (filename.endsWith(".xls")) {
            return new HSSFWorkbook(fis);
        } else {
            throw new IllegalArgumentException("Unsupported Excel format: " + filename);
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    private String buildInsertSql(String schema, String tableName, List<String> columns) {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(schema).append(".").append(tableName).append(" (");
        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", Collections.nCopies(columns.size(), "?")));
        sql.append(")");
        return sql.toString();
    }
    
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex) : "";
    }
    
    private String extractHostFromUrl(String url) {
        // 简单的URL解析，实际项目中可能需要更复杂的逻辑
        try {
            if (url.contains("://")) {
                String[] parts = url.split("://")[1].split("/")[0].split(":");
                return parts[0];
            }
        } catch (Exception e) {
            logger.warn("Failed to extract host from URL: {}", url);
        }
        return "localhost";
    }
    
    private Integer extractPortFromUrl(String url) {
        try {
            if (url.contains("://")) {
                String[] parts = url.split("://")[1].split("/")[0].split(":");
                if (parts.length > 1) {
                    return Integer.parseInt(parts[1]);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract port from URL: {}", url);
        }
        return 3306; // 默认MySQL端口
    }
    
    /**
     * 获取支持的文件格式
     */
    public List<String> getSupportedFormats() {
        return new ArrayList<>(SUPPORTED_FORMATS);
    }
    
    /**
     * 检查文件格式是否支持
     */
    public boolean isFormatSupported(String filename) {
        if (filename == null) return false;
        
        String extension = getFileExtension(filename).toLowerCase();
        return SUPPORTED_FORMATS.contains(extension);
    }
    
    @Override
    public void afterPropertiesSet() throws Exception {
        // 检查并创建目标schema
        databaseUtil.createSchemaIfNotExists(dataSource, schemaName);
    }
}