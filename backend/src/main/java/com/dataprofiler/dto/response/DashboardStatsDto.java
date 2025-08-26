package com.dataprofiler.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for dashboard statistics
 * Provides comprehensive system overview data for dashboard display
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Dashboard statistics overview")
public class DashboardStatsDto {

    @Schema(description = "Total number of data sources", example = "15")
    private Long dataSourceCount;

    @Schema(description = "Total number of files", example = "128")
    private Long fileCount;

    @Schema(description = "Total number of active tasks", example = "8")
    private Long taskCount;

    @Schema(description = "Total number of reports", example = "45")
    private Long reportCount;

    @Schema(description = "Task status distribution")
    private List<StatusCount> taskStatusDistribution;

    @Schema(description = "Data source types distribution")
    private List<TypeCount> dataSourceTypesDistribution;

    @Schema(description = "Recent activities")
    private List<ActivityDto> recentActivities;

    /**
     * Status count for task distribution
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Status count data")
    public static class StatusCount {
        @Schema(description = "Status name", example = "COMPLETED")
        private String name;
        
        @Schema(description = "Count value", example = "35")
        private Long value;
    }

    /**
     * Type count for data source distribution
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Type count data")
    public static class TypeCount {
        @Schema(description = "Type name", example = "MYSQL")
        private String name;
        
        @Schema(description = "Count value", example = "12")
        private Long value;
    }

    /**
     * Activity information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Activity information")
    public static class ActivityDto {
        @Schema(description = "Activity ID", example = "1")
        private Long id;
        
        @Schema(description = "Activity type", example = "task")
        private String type;
        
        @Schema(description = "Activity title", example = "Data profiling task completed")
        private String title;
        
        @Schema(description = "Activity description", example = "Task for MySQL database completed successfully")
        private String description;
        
        @Schema(description = "Activity timestamp", example = "2025-01-21T10:30:00")
        private String timestamp;
        
        @Schema(description = "Activity status", example = "completed")
        private String status;
    }
}