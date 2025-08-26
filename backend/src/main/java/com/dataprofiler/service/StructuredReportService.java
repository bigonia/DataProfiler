package com.dataprofiler.service;

import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.dto.request.ReportSummaryRequest;
import com.dataprofiler.dto.response.ReportInfoDto;
import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Service interface for managing structured profiling reports
 * Handles report storage and retrieval operations based on task ID
 * <p>
 * Core responsibilities:
 * - Report persistence: Save assembled reports from ReportAssemblyService
 * - Report retrieval: Provide summary and detailed reports based on task ID
 * - Performance optimization: Use indexed fields and pre-computed summaries
 */
public interface StructuredReportService {

    /**
     * Save one or more assembled structured reports for a profiling task
     * This method stores complete report data with performance optimizations:
     * - Full report JSON in reportJson field
     * - Pre-computed summary data in summaryJson field
     * - Indexed fields for fast querying
     *
     * @param taskId  the profiling task ID
     * @param reports list of structured reports to save
     */
    void saveReports(String taskId, List<StructuredReportDto> reports);

    /**
     * Get summary reports for a specific profiling task
     * This method provides lightweight overview data without loading full report content
     * Uses pre-computed summary data for optimal performance
     *
     * @param request The request containing the data source IDs.
     * @return list of summary reports
     */
    List<ReportSummaryDto> getReportsSummary(ReportSummaryRequest request);

    /**
     * Get detailed reports for a specific profiling task with filtering and pagination
     * Supports complex querying with data source, schema, and table filtering
     * Provides format conversion (standard/compact) and pagination
     *
     * @param request detailed report request with filtering criteria
     * @return Page of detailed structured reports
     */
    Page<StructuredReportDto> getDetailedReport(DetailedReportRequest request);

    /**
     * Get all detailed reports for a specific profiling task with filtering (without pagination)
     * Used for table-based pagination in controller layer
     * Supports complex querying with data source, schema, and table filtering
     *
     * @param request detailed report request with filtering criteria
     * @return List of all matching detailed structured reports
     */
    List<StructuredReportDto> getAllDetailedReports(DetailedReportRequest request);

    /**
     * Get basic information for all reports with pagination
     * Provides lightweight report metadata for listing purposes
     * Includes task info, data source info, timestamps, report size, table count, etc.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return paginated report basic information
     */
    Page<ReportInfoDto> getReportInfoList(Integer page, Integer size);

}