package com.dataprofiler.service.impl;

import com.dataprofiler.entity.FileMetadata;
import com.dataprofiler.repository.FileMetadataRepository;
import com.dataprofiler.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of FileService interface
 * Provides business logic for file metadata management
 */
@Service
@Transactional
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Autowired
    private FileMetadataRepository fileMetadataRepository;

    @Override
    public FileMetadata saveFileMetadata(FileMetadata fileMetadata) {
        logger.info("Saving file metadata: {}", fileMetadata.getOriginalFilename());
        return fileMetadataRepository.save(fileMetadata);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FileMetadata> getAllFiles(Pageable pageable) {
        return fileMetadataRepository.findAllByOrderByUploadedAtDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadata> getAllFiles() {
        return fileMetadataRepository.findAllByOrderByUploadedAtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileMetadata> getFileById(Long id) {
        return fileMetadataRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileMetadata> getFileByFilename(String filename) {
        return fileMetadataRepository.findByFilename(filename);
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
    public boolean deleteFile(Long id) {
        try {
            Optional<FileMetadata> fileMetadata = fileMetadataRepository.findById(id);
            if (fileMetadata.isPresent()) {
                FileMetadata file = fileMetadata.get();
                
                // Delete physical file
                try {
                    Path filePath = Paths.get(file.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        logger.info("Physical file deleted: {}", file.getFilePath());
                    }
                } catch (IOException e) {
                    logger.warn("Failed to delete physical file: {}", file.getFilePath(), e);
                    // Continue with metadata deletion even if physical file deletion fails
                }
                
                // Delete metadata
                fileMetadataRepository.deleteById(id);
                logger.info("File metadata deleted: {}", file.getOriginalFilename());
                return true;
            }
            return false;
        } catch (Exception e) {
            logger.error("Failed to delete file with ID: {}", id, e);
            return false;
        }
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
}