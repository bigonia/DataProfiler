package com.dataprofiler.controller;

import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.dto.request.ReportSummaryRequest;
import com.dataprofiler.dto.response.ReportInfoDto;
import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.SimplePaginationResponse;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.service.StructuredReportService;
import com.dataprofiler.util.TablePaginationUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
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

    @GetMapping("/summary")
    @Operation(
            summary = "Get report summaries by task ID",
            description = "Retrieve lightweight summary information for specified task. " +
                    "Uses pre-computed summary data for optimal performance. " +
                    "Supports table-based pagination."
    )
    public ResponseEntity<SimplePaginationResponse<ReportSummaryDto>> getReportsSummary(
            @Parameter(description = "Task ID", example = "f6713573-fe11-4de7-8c06-8da2792454c4")
            @RequestParam String taskId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "Page size (number of tables per page)", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) Integer pageSize) {

        log.info("Getting report summaries, taskId: {}, page: {}, pageSize: {}",
                taskId, page, pageSize);

        // Create request object from taskId
        ReportSummaryRequest request = new ReportSummaryRequest();
        request.setTaskId(taskId);

        // Get original summaries from service
        List<ReportSummaryDto> summaries = reportService.getReportsSummary(request);

        // Apply table-based pagination
        Page<ReportSummaryDto> paginatedSummaries = TablePaginationUtil.paginateSummaryReports(
                summaries, page, pageSize);



        log.debug("Retrieved {} summary reports (page {} of {}), total tables: {}", 
                paginatedSummaries.getNumberOfElements(), paginatedSummaries.getNumber() + 1, 
                paginatedSummaries.getTotalPages(), paginatedSummaries.getTotalElements());
        
        // Convert to simplified pagination response
        SimplePaginationResponse<ReportSummaryDto> response = SimplePaginationResponse.from(paginatedSummaries);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/detailed")
    @Operation(
            summary = "Get detailed reports with filtering and pagination",
            description = "Retrieve detailed profiling reports with advanced filtering and pagination. " +
                    "Supports data source scope filtering, schema/table selection, and table-based pagination."
    )
    public ResponseEntity<SimplePaginationResponse<StructuredReportDto>> getDetailedReport(
            @Valid @RequestBody DetailedReportRequest request) {

        log.info("Getting detailed reports with page={}, pageSize={}, format={}",
                request.getPage(), request.getPageSize(), request.getFormat());

        // Get all matching reports from service (without pagination)
        List<StructuredReportDto> allReports = reportService.getAllDetailedReports(request);

        // Apply table-based pagination
        Page<StructuredReportDto> paginatedReports = TablePaginationUtil.paginateDetailedReports(
                allReports, request.getPage(), request.getPageSize());



        log.debug("Retrieved {} detailed reports (page {} of {}), total tables: {}", 
                paginatedReports.getNumberOfElements(), paginatedReports.getNumber() + 1, 
                paginatedReports.getTotalPages(), paginatedReports.getTotalElements());
        
        // Convert to simplified pagination response
        SimplePaginationResponse<StructuredReportDto> response = SimplePaginationResponse.from(paginatedReports);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/infolist")
    @Operation(
            summary = "Get all report basic information",
            description = "Retrieve basic information for all reports including task info, data source info, " +
                    "timestamps, report size, table count, etc. Used for report listing pages."
    )
    public ResponseEntity<Page<ReportInfoDto>> getReportInfoList(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) Integer page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) Integer size) {

        log.info("Getting report info list with page={}, size={}", page, size);

        Page<ReportInfoDto> reportInfoPage = reportService.getReportInfoList(page, size);

        log.debug("Retrieved {} report info records (page {} of {})", 
                reportInfoPage.getNumberOfElements(), reportInfoPage.getNumber() + 1, reportInfoPage.getTotalPages());
        return ResponseEntity.ok(reportInfoPage);
    }



}