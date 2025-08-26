package com.dataprofiler.service.impl;

import com.dataprofiler.entity.FileMetadata;
import com.dataprofiler.repository.FileMetadataRepository;
import com.dataprofiler.service.FileManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of FileManagementService interface
 * Handles file upload, storage, and metadata management operations
 * Separated from data source conversion logic for better modularity
 */
@Service
@Transactional
public class FileManagementServiceImpl implements FileManagementService {

    private static final Logger logger = LoggerFactory.getLogger(FileManagementServiceImpl.class);
    
    @Value("${app.upload.directory:uploads}")
    private String uploadDirectory;
    
    @Autowired
    private FileMetadataRepository fileMetadataRepository;
    
    @Autowired
    private com.dataprofiler.util.FileConversionUtil fileConversionUtil;
    
    // Supported file types for upload
    private static final List<String> SUPPORTED_MIME_TYPES = Arrays.asList(
        "text/csv",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/json",
        "text/plain",
        "application/xml",
        "text/xml"
    );

    @Override
    public FileMetadata uploadFile(MultipartFile file, String description) throws IOException {
        logger.info("Uploading file: {}", file.getOriginalFilename());
        
        // Validate file
        validateFile(file);
        
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String uniqueFilename = generateUniqueFilename(originalFilename);
        Path filePath = uploadPath.resolve(uniqueFilename);
        
        // Save file
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        // Create and save file metadata
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFilename(uniqueFilename);
        fileMetadata.setOriginalFilename(originalFilename);
        fileMetadata.setFilePath(filePath.toAbsolutePath().toString());
        fileMetadata.setFileSize(file.getSize());
        fileMetadata.setMimeType(file.getContentType());
        fileMetadata.setUploadedAt(LocalDateTime.now());
        fileMetadata.setDescription(description != null ? description : "Uploaded file: " + originalFilename);
        
        FileMetadata savedMetadata = fileMetadataRepository.save(fileMetadata);
        logger.info("File uploaded successfully with ID: {}", savedMetadata.getId());
        
        return savedMetadata;
    }

    @Override
    public List<FileMetadata> uploadFiles(MultipartFile[] files, String[] descriptions) throws IOException {
        List<FileMetadata> uploadedFiles = new ArrayList<>();
        
        for (int i = 0; i < files.length; i++) {
            String description = (descriptions != null && i < descriptions.length) ? descriptions[i] : null;
            try {
                FileMetadata uploaded = uploadFile(files[i], description);
                uploadedFiles.add(uploaded);
            } catch (IOException e) {
                logger.error("Failed to upload file: {}", files[i].getOriginalFilename(), e);
                // Continue with other files, but log the error
                throw new IOException("Failed to upload file: " + files[i].getOriginalFilename(), e);
            }
        }
        
        return uploadedFiles;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileMetadata> getFileById(Long fileId) {
        return fileMetadataRepository.findById(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileMetadata> getFileByFilename(String filename) {
        return fileMetadataRepository.findByFilename(filename);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadata> getAllFiles(Pageable pageable) {
        return fileMetadataRepository.findAllByOrderByUploadedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> searchFilesByName(String searchTerm) {
        return fileMetadataRepository.findByOriginalFilenameContainingIgnoreCase(searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> getFilesByMimeType(String mimeType) {
        return fileMetadataRepository.findByMimeTypeContainingIgnoreCase(mimeType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> getFilesByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return fileMetadataRepository.findByUploadedAtBetween(startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> getFilesByDataSourceId(Long dataSourceId) {
        return fileMetadataRepository.findByDataSourceId(dataSourceId);
    }

    @Override
    public boolean deleteFile(Long fileId) {
        try {
            Optional<FileMetadata> fileMetadata = fileMetadataRepository.findById(fileId);
            if (fileMetadata.isPresent()) {
                FileMetadata file = fileMetadata.get();
                
                // Delete physical file
                deletePhysicalFile(file.getFilePath());
                
                // Delete metadata
                fileMetadataRepository.deleteById(fileId);
                logger.info("File deleted successfully: {}", file.getOriginalFilename());
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to delete file with ID: {}", fileId, e);
            return false;
        }
    }

    @Override
    public int deleteFiles(List<Long> fileIds) {
        int deletedCount = 0;
        for (Long fileId : fileIds) {
            if (deleteFile(fileId)) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    @Override
    @Transactional(readOnly = true)
    public FileStatistics getFileStatistics() {
        long totalFiles = fileMetadataRepository.count();
        long totalSize = fileMetadataRepository.getTotalFileSize();
        
        // Count files by type
        long imageFiles = fileMetadataRepository.countByMimeTypePattern("image%");
        long documentFiles = fileMetadataRepository.countByMimeTypePattern("%pdf%") +
                           fileMetadataRepository.countByMimeTypePattern("%document%") +
                           fileMetadataRepository.countByMimeTypePattern("%text%");
        long spreadsheetFiles = fileMetadataRepository.countByMimeTypePattern("%spreadsheet%") +
                              fileMetadataRepository.countByMimeTypePattern("%excel%") +
                              fileMetadataRepository.countByMimeTypePattern("%csv%");
        long otherFiles = totalFiles - imageFiles - documentFiles - spreadsheetFiles;
        
        return new FileStatistics(totalFiles, totalSize, imageFiles, documentFiles, spreadsheetFiles, otherFiles);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean fileExists(Long fileId) {
        return fileMetadataRepository.existsById(fileId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> getFilePath(Long fileId) {
        return fileMetadataRepository.findById(fileId)
                .map(FileMetadata::getFilePath);
    }

    @Override
    public Optional<FileMetadata> updateFileMetadata(Long fileId, String description) {
        Optional<FileMetadata> fileMetadata = fileMetadataRepository.findById(fileId);
        if (fileMetadata.isPresent()) {
            FileMetadata file = fileMetadata.get();
            file.setDescription(description);
            FileMetadata updated = fileMetadataRepository.save(file);
            logger.info("File metadata updated for ID: {}", fileId);
            return Optional.of(updated);
        }
        return Optional.empty();
    }

    @Override
    public List<String> getSupportedFileTypes() {
        return new ArrayList<>(SUPPORTED_MIME_TYPES);
    }

    @Override
    public boolean isFileTypeSupported(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return SUPPORTED_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    @Override
    public com.dataprofiler.entity.DataSourceConfig convertFileToDataSource(Long fileId, String dataSourceName) {
        logger.info("Converting file to data source: fileId={}, dataSourceName={}", fileId, dataSourceName);
        
        if (fileId == null) {
            throw new IllegalArgumentException("File ID cannot be null");
        }
        
        if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Data source name cannot be null or empty");
        }
        
        return fileConversionUtil.convertFileToDataSource(fileId, dataSourceName.trim());
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IOException("Invalid filename");
        }
        
        // Optional: Check file type
        String contentType = file.getContentType();
        if (contentType != null && !isFileTypeSupported(contentType)) {
            logger.warn("Unsupported file type: {} for file: {}", contentType, originalFilename);
            // Don't throw exception, just log warning for flexibility
        }
    }

    /**
     * Generate unique filename to avoid conflicts
     */
    private String generateUniqueFilename(String originalFilename) {
        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + fileExtension;
    }

    /**
     * Delete physical file from filesystem
     */
    private void deletePhysicalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                logger.info("Physical file deleted: {}", filePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete physical file: {}", filePath, e);
            // Don't throw exception, continue with metadata deletion
        }
    }
}