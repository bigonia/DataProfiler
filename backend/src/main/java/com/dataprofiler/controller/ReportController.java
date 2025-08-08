package com.dataprofiler.controller;

import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.service.StructuredReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * REST Controller for managing structured profiling reports
 * Simplified interface with 3 core endpoints based on task ID
 * <p>
 * Core endpoints:
 * - GET /api/reports/{taskId}/summary - Get lightweight summary reports
 * - POST /api/reports/{taskId}/detailed - Get detailed reports with filtering
 * - DELETE /api/reports/cleanup - Maintenance endpoint for old report cleanup
 * <p>
 * Design principles:
 * - Task-centric querying: All operations are based on profiling task ID
 * - Performance optimized: Uses pre-computed summaries and indexed queries
 * - Flexible filtering: Supports data source scope and pagination in detailed queries
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Report Management", description = "Simplified APIs for structured profiling report management")
@Validated
public class ReportController {

    @Autowired
    private StructuredReportService reportService;

    @GetMapping("/{taskId}/summary")
    @Operation(
            summary = "Get report summary by task ID",
            description = "Retrieve lightweight summary information for all reports in a profiling task. " +
                    "Uses pre-computed summary data for optimal performance."
    )
    public ResponseEntity<List<ReportSummaryDto>> getReportsSummary(
            @PathVariable @NotBlank
            @Parameter(description = "Profiling task ID") String taskId) {

        log.info("Getting report summary for task: {}", taskId);

        List<ReportSummaryDto> summaries = reportService.getReportsSummary(taskId);

        log.debug("Retrieved {} summary reports for task: {}", summaries.size(), taskId);
        return ResponseEntity.ok(summaries);
    }

    @PostMapping("/{taskId}/detailed")
    @Operation(
            summary = "Get detailed reports by task ID",
            description = "Retrieve detailed profiling reports with advanced filtering and pagination. " +
                    "Supports data source scope filtering, table selection, and format conversion."
    )
    public ResponseEntity<List<StructuredReportDto>> getDetailedReport(
            @PathVariable @NotBlank
            @Parameter(description = "Profiling task ID") String taskId,
            @Valid @RequestBody DetailedReportRequest request,
            @RequestParam(defaultValue = "standard")
            @Parameter(description = "Report format: 'standard' (full data) or 'compact' (summary only)") String format) {

        log.info("Getting detailed report for task: {} with {} data sources in {} format",
                taskId, request.getDataSources().size(), format);

        List<StructuredReportDto> reports = reportService.getDetailedReport(taskId, request, format);

        log.debug("Retrieved {} detailed reports for task: {}", reports.size(), taskId);
        return ResponseEntity.ok(reports);
    }

}