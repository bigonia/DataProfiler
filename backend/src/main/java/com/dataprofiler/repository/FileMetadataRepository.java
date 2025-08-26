package com.dataprofiler.repository;

import com.dataprofiler.entity.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FileMetadata entity
 * Provides data access methods for file metadata operations
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Find file metadata by original filename
     */
    Optional<FileMetadata> findByOriginalFilename(String originalFilename);

    /**
     * Find file metadata by filename (unique filename)
     */
    Optional<FileMetadata> findByFilename(String filename);

    /**
     * Find files by MIME type
     */
    List<FileMetadata> findByMimeTypeContainingIgnoreCase(String mimeType);

    /**
     * Find files uploaded within a date range
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.uploadedAt BETWEEN :startDate AND :endDate ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findByUploadedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Find files by filename containing search term (case insensitive)
     */
    @Query("SELECT f FROM FileMetadata f WHERE LOWER(f.originalFilename) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY f.uploadedAt DESC")
    List<FileMetadata> findByOriginalFilenameContainingIgnoreCase(@Param("searchTerm") String searchTerm);

    /**
     * Find all files ordered by upload date (newest first)
     */
    List<FileMetadata> findAllByOrderByUploadedAtDesc();

    /**
     * Find files with pagination
     */
    Page<FileMetadata> findAllByOrderByUploadedAtDesc(Pageable pageable);

    /**
     * Find files by data source ID
     */
    List<FileMetadata> findByDataSourceId(Long dataSourceId);

    /**
     * Count files by MIME type
     */
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.mimeType LIKE :mimeTypePattern")
    long countByMimeTypePattern(@Param("mimeTypePattern") String mimeTypePattern);

    /**
     * Get total file size
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f")
    long getTotalFileSize();
}