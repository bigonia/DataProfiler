package com.dataprofiler.controller;

import com.dataprofiler.dto.response.DashboardStatsDto;
import com.dataprofiler.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for dashboard statistics and overview data
 * Provides endpoints for dashboard metrics, charts data, and system overview
 */
@RestController
@RequestMapping("/api/dashboard")
@Slf4j
@Tag(name = "Dashboard", description = "Dashboard statistics and overview APIs")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * Get dashboard statistics overview
     */
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", description = "Retrieve overall system statistics for dashboard")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        log.debug("Retrieving dashboard statistics");
        
        DashboardStatsDto stats = dashboardService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get task status distribution for charts
     */
    @GetMapping("/task-status")
    @Operation(summary = "Get task status distribution", description = "Retrieve task status distribution data for charts")
    @ApiResponse(responseCode = "200", description = "Task status data retrieved successfully")
    public ResponseEntity<?> getTaskStatusDistribution() {
        log.debug("Retrieving task status distribution");
        
        return ResponseEntity.ok(dashboardService.getTaskStatusDistribution());
    }

    /**
     * Get data source types distribution for charts
     */
    @GetMapping("/datasource-types")
    @Operation(summary = "Get data source types distribution", description = "Retrieve data source types distribution for charts")
    @ApiResponse(responseCode = "200", description = "Data source types data retrieved successfully")
    public ResponseEntity<?> getDataSourceTypesDistribution() {
        log.debug("Retrieving data source types distribution");
        
        return ResponseEntity.ok(dashboardService.getDataSourceTypesDistribution());
    }

    /**
     * Get recent activities
     */
    @GetMapping("/activities")
    @Operation(summary = "Get recent activities", description = "Retrieve recent system activities")
    @ApiResponse(responseCode = "200", description = "Recent activities retrieved successfully")
    public ResponseEntity<?> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Retrieving recent activities with limit: {}", limit);
        
        return ResponseEntity.ok(dashboardService.getRecentActivities(limit));
    }
}