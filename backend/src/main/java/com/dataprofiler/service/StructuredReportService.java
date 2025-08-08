package com.dataprofiler.service;

import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.entity.StructuredReport;

import java.util.List;
import java.util.Map;

/**
 * Service interface for managing structured profiling reports
 * Handles report storage and retrieval operations based on task ID
 * 
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
     * @param taskId the profiling task ID
     * @param reports list of structured reports to save
     */
    void saveReports(String taskId, List<StructuredReportDto> reports);

    /**
     * Get summary reports for a specific profiling task
     * This method provides lightweight overview data without loading full report content
     * Uses pre-computed summary data for optimal performance
     * 
     * @param taskId the profiling task ID
     * @return list of summary reports
     */
    List<ReportSummaryDto> getReportsSummary(String taskId);

    /**
     * Get detailed reports for a specific profiling task with filtering and pagination
     * Supports complex querying with data source, schema, and table filtering
     * Provides format conversion (standard/compact) and pagination
     * 
     * @param taskId the profiling task ID
     * @param request detailed report request with filtering criteria
     * @param format output format ("standard" or "compact")
     * @return list of detailed structured reports
     */
    List<StructuredReportDto> getDetailedReport(String taskId, DetailedReportRequest request, String format);
    
    // Maintenance Operations
    
    /**
     * Get reports by task ID (for internal use)
     * 
     * @param taskId the task ID
     * @return list of report entities
     */
    List<StructuredReport> getReportsByTaskId(String taskId);

}