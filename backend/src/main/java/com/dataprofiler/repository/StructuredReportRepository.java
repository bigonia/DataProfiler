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
     * Count reports by task ID
     * @param taskId the task identifier
     * @return number of reports for the task
     */
    long countByTaskId(String taskId);



    /**
     * Delete reports by task ID
     * @param taskId the task identifier
     */
    @Modifying
    @Query("DELETE FROM StructuredReport sr WHERE sr.taskId = :taskId")
    void deleteByTaskId(@Param("taskId") String taskId);


}