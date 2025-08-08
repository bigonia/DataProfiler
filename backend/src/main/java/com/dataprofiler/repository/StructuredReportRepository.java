package com.dataprofiler.repository;

import com.dataprofiler.entity.StructuredReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for StructuredReport entity
 * Optimized for task-based querying with performance indexes
 * <p>
 * Core query patterns:
 * - Task-based queries: All operations centered around taskId
 * - Summary optimization: Uses pre-computed summaryJson for fast retrieval
 * - Indexed filtering: Leverages dataSourceId, dataSourceType indexes
 * - Maintenance operations: Cleanup and statistics
 */
@Repository
public interface StructuredReportRepository extends JpaRepository<StructuredReport, Long> {

    /**
     * Find reports by task ID with pagination support
     * @param taskId the task identifier
     * @param pageable pagination parameters
     * @return page of reports for the specified task
     */
    Page<StructuredReport> findByTaskId(String taskId, Pageable pageable);

    /**
     * Find reports by task ID ordered by generation time (descending)
     * @param taskId the task identifier
     * @return list of reports ordered by generation time
     */
    List<StructuredReport> findByTaskIdOrderByGeneratedAtDesc(String taskId);

    /**
     * Find reports by data source ID with pagination support
     * @param dataSourceId the data source identifier
     * @param pageable pagination parameters
     * @return page of reports for the specified data source
     */
    Page<StructuredReport> findByDataSourceId(String dataSourceId, Pageable pageable);

    /**
     * Find reports by both task ID and data source ID with pagination
     * @param taskId the task identifier
     * @param dataSourceId the data source identifier
     * @param pageable pagination parameters
     * @return page of reports matching both criteria
     */
    Page<StructuredReport> findByTaskIdAndDataSourceId(String taskId, String dataSourceId, Pageable pageable);

    /**
     * Count reports by task ID
     * @param taskId the task identifier
     * @return number of reports for the task
     */
    long countByTaskId(String taskId);

    /**
     * Count reports by data source ID
     * @param dataSourceId the data source identifier
     * @return number of reports for the data source
     */
    long countByDataSourceId(String dataSourceId);

    /**
     * Delete reports by task ID
     * @param taskId the task identifier
     * @return number of deleted reports
     */
    @Modifying
    @Query("DELETE FROM StructuredReport r WHERE r.taskId = :taskId")
    int deleteByTaskId(@Param("taskId") String taskId);

    /**
     * Delete reports by data source ID
     * @param dataSourceId the data source identifier
     * @return number of deleted reports
     */
    @Modifying
    @Query("DELETE FROM StructuredReport r WHERE r.dataSourceId = :dataSourceId")
    int deleteByDataSourceId(@Param("dataSourceId") String dataSourceId);



}