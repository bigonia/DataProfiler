package com.dataprofiler.service;

import com.dataprofiler.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for file management operations
 * Provides business logic for file metadata management
 */
public interface FileService {

    /**
     * Save file metadata
     */
    FileMetadata saveFileMetadata(FileMetadata fileMetadata);

    /**
     * Get all files with pagination
     */
    Page<FileMetadata> getAllFiles(Pageable pageable);

    /**
     * Get all files ordered by upload date
     */
    List<FileMetadata> getAllFiles();

    /**
     * Get file by ID
     */
    Optional<FileMetadata> getFileById(Long id);

    /**
     * Get file by filename
     */
    Optional<FileMetadata> getFileByFilename(String filename);

    /**
     * Search files by original filename
     */
    List<FileMetadata> searchFilesByName(String searchTerm);

    /**
     * Get files by MIME type
     */
    List<FileMetadata> getFilesByMimeType(String mimeType);

    /**
     * Get files uploaded within date range
     */
    List<FileMetadata> getFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get files by data source ID
     */
    List<FileMetadata> getFilesByDataSourceId(Long dataSourceId);

    /**
     * Delete file metadata by ID
     */
    boolean deleteFile(Long id);

    /**
     * Get file statistics
     */
    FileStatistics getFileStatistics();

    /**
     * Inner class for file statistics
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

        // Getters and Setters
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