package com.dataprofiler.service;

import com.dataprofiler.dto.FileLoadResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for converting files to database tables
 * Supports Excel file processing and conversion to SQLite tables
 */
public interface FileAsTableService {

    /**
     * Load Excel file to SQLite database tables
     * @param fileId Unique identifier for the file data source
     * @param filePath Path to the Excel file to be processed
     * @return FileLoadResult containing information about loaded tables
     */
    FileLoadResult loadExcelFileToDatabase(String fileId, String filePath);

    /**
     * Load Excel file to database (legacy method for file upload)
     * @param file MultipartFile containing Excel data
     * @param dataSourceName Name for the data source
     */
    void loadExcelFileToDatabase(MultipartFile file, String dataSourceName);

}
