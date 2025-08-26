package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request DTO for retrieving task-based reports with pagination and filtering
 * Supports filtering by data sources within a task and format options
 */
@Data
@Schema(description = "Request for retrieving task-based reports with pagination and filtering")
public class TaskReportRequest {

    @NotBlank(message = "Task ID is required")
    @Schema(description = "Profiling task unique identifier", example = "task-123", required = true)
    private String taskId;

    @Schema(description = "List of data source IDs to filter within the task", 
            example = "[\"ds-pg-01\", \"ds-mysql-01\"]")
    private List<String> dataSourceIds;

    @Min(value = 0, message = "Page number must be non-negative")
    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be positive")
    @Schema(description = "Number of items per page", example = "20", defaultValue = "20")
    private int pageSize = 20;

    @Schema(description = "Output format", example = "standard", allowableValues = {"standard", "compact"})
    private String format = "standard";

    @Schema(description = "Include summary information only", example = "false", defaultValue = "false")
    private boolean summaryOnly = false;

    // Constructors
    public TaskReportRequest() {}

    public TaskReportRequest(String taskId) {
        this.taskId = taskId;
    }

    public TaskReportRequest(String taskId, List<String> dataSourceIds, int page, int pageSize) {
        this.taskId = taskId;
        this.dataSourceIds = dataSourceIds;
        this.page = page;
        this.pageSize = pageSize;
    }
}