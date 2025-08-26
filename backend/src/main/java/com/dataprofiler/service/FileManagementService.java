package com.dataprofiler.service;

import com.dataprofiler.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for file management operations
 * Handles file upload, storage, metadata management, and basic file operations
 * Separated from data source conversion logic for better modularity
 */
public interface FileManagementService {

    /**
     * Upload and store a single file
     * 
     * @param file The multipart file to upload
     * @param description Optional description for the file
     * @return FileMetadata of the uploaded file
     * @throws IOException if file upload fails
     */
    FileMetadata uploadFile(MultipartFile file, String description) throws IOException;

    /**
     * Upload and store multiple files
     * 
     * @param files Array of multipart files to upload
     * @param descriptions Optional descriptions for each file (can be null)
     * @return List of FileMetadata for uploaded files
     * @throws IOException if any file upload fails
     */
    List<FileMetadata> uploadFiles(MultipartFile[] files, String[] descriptions) throws IOException;

    /**
     * Get file metadata by ID
     * 
     * @param fileId The file ID
     * @return Optional FileMetadata
     */
    Optional<FileMetadata> getFileById(Long fileId);

    /**
     * Get file metadata by filename
     * 
     * @param filename The original filename
     * @return Optional FileMetadata
     */
    Optional<FileMetadata> getFileByFilename(String filename);

    /**
     * Get all files with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of FileMetadata
     */
    Page<FileMetadata> getAllFiles(Pageable pageable);

    /**
     * Search files by filename pattern
     * 
     * @param searchTerm Search term for filename matching
     * @return List of matching FileMetadata
     */
    List<FileMetadata> searchFilesByName(String searchTerm);

    /**
     * Get files by MIME type
     * 
     * @param mimeType The MIME type to filter by
     * @return List of FileMetadata with matching MIME type
     */
    List<FileMetadata> getFilesByMimeType(String mimeType);

    /**
     * Get files uploaded within a date range
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of FileMetadata within date range
     */
    List<FileMetadata> getFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get files associated with a specific data source
     * 
     * @param dataSourceId The data source ID
     * @return List of FileMetadata associated with the data source
     */
    List<FileMetadata> getFilesByDataSourceId(Long dataSourceId);

    /**
     * Delete a file and its metadata
     * 
     * @param fileId The file ID to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean deleteFile(Long fileId);

    /**
     * Delete multiple files
     * 
     * @param fileIds List of file IDs to delete
     * @return Number of successfully deleted files
     */
    int deleteFiles(List<Long> fileIds);

    /**
     * Get file statistics
     * 
     * @return FileStatistics containing file counts and sizes
     */
    FileStatistics getFileStatistics();

    /**
     * Check if a file exists by ID
     * 
     * @param fileId The file ID
     * @return true if file exists, false otherwise
     */
    boolean fileExists(Long fileId);

    /**
     * Get the physical file path for a file ID
     * 
     * @param fileId The file ID
     * @return Optional file path string
     */
    Optional<String> getFilePath(Long fileId);

    /**
     * Update file metadata
     * 
     * @param fileId The file ID
     * @param description New description
     * @return Updated FileMetadata
     */
    Optional<FileMetadata> updateFileMetadata(Long fileId, String description);

    /**
     * Get supported file types for upload
     * 
     * @return List of supported MIME types
     */
    List<String> getSupportedFileTypes();

    /**
     * Validate if a file type is supported
     * 
     * @param mimeType The MIME type to validate
     * @return true if supported, false otherwise
     */
    boolean isFileTypeSupported(String mimeType);

    /**
     * Convert file to data source
     * 
     * @param fileId The file ID to convert
     * @param dataSourceName Custom data source name
     * @return DataSourceConfig for the converted data source
     */
    com.dataprofiler.entity.DataSourceConfig convertFileToDataSource(Long fileId, String dataSourceName);

    /**
     * File statistics data class
     */
    class FileStatistics {
        private long totalFiles;
        private long totalSize;
        private long imageFiles;
        private long documentFiles;
        private long spreadsheetFiles;
        private long otherFiles;

        public FileStatistics() {}

        public FileStatistics(long totalFiles, long totalSize, long imageFiles,
                            long documentFiles, long spreadsheetFiles, long otherFiles) {
            this.totalFiles = totalFiles;
            this.totalSize = totalSize;
            this.imageFiles = imageFiles;
            this.documentFiles = documentFiles;
            this.spreadsheetFiles = spreadsheetFiles;
            this.otherFiles = otherFiles;
        }

        // Getters and setters
        public long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(long totalFiles) { this.totalFiles = totalFiles; }

        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }

        public long getImageFiles() { return imageFiles; }
        public void setImageFiles(long imageFiles) { this.imageFiles = imageFiles; }

        public long getDocumentFiles() { return documentFiles; }
        public void setDocumentFiles(long documentFiles) { this.documentFiles = documentFiles; }

        public long getSpreadsheetFiles() { return spreadsheetFiles; }
        public void setSpreadsheetFiles(long spreadsheetFiles) { this.spreadsheetFiles = spreadsheetFiles; }

        public long getOtherFiles() { return otherFiles; }
        public void setOtherFiles(long otherFiles) { this.otherFiles = otherFiles; }
    }
}