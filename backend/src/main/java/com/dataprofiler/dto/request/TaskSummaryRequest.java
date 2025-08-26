package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * Request DTO for retrieving task-based report summaries
 * Provides lightweight summary information for a specific task
 */
@Data
@Schema(description = "Request for retrieving task-based report summaries")
public class TaskSummaryRequest {

    @NotBlank(message = "Task ID is required")
    @Schema(description = "Profiling task unique identifier", example = "task-123", required = true)
    private String taskId;

    @Schema(description = "List of data source IDs to filter within the task", 
            example = "[\"ds-pg-01\", \"ds-mysql-01\"]")
    private List<String> dataSourceIds;

    // Constructors
    public TaskSummaryRequest() {}

    public TaskSummaryRequest(String taskId) {
        this.taskId = taskId;
    }

    public TaskSummaryRequest(String taskId, List<String> dataSourceIds) {
        this.taskId = taskId;
        this.dataSourceIds = dataSourceIds;
    }
}