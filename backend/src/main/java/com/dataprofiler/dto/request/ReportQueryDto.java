package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;

/**
 * Request DTO for querying structured reports with pagination
 * Used for filtering and paginating report results
 */
@Data
@Schema(description = "Query parameters for structured reports with pagination")
public class ReportQueryDto {

    @Schema(description = "Task ID to filter reports", example = "task-123")
    private String taskId;

    @Schema(description = "Data source ID to filter reports", example = "ds-pg-01")
    private String dataSourceId;

    @Min(value = 0, message = "Page number must be non-negative")
    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    private Integer page;

    @Min(value = 1, message = "Page size must be positive")
    @Schema(description = "Number of items per page", example = "20", defaultValue = "20")
    private Integer size;

    // Constructors
    public ReportQueryDto() {
        this.page = 0;
        this.size = 20;
    }

    public ReportQueryDto(String taskId, String dataSourceId) {
        this();
        this.taskId = taskId;
        this.dataSourceId = dataSourceId;
    }

    public ReportQueryDto(String taskId, String dataSourceId, Integer page, Integer size) {
        this.taskId = taskId;
        this.dataSourceId = dataSourceId;
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 20;
    }
}