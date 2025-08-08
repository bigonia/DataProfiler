package com.dataprofiler.dto.response;

import com.dataprofiler.entity.ProfilingTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO for task status information
 */
@Data
@Schema(description = "Task status response")
public class TaskStatusResponse {

    @Schema(description = "Unique task identifier", example = "task-uuid-12345")
    private String taskId;

    @Schema(description = "Current task status", example = "RUNNING")
    private ProfilingTask.TaskStatus status;

    @Schema(description = "task exe info")
    private String info;

    @Schema(description = "Task creation timestamp")
    private LocalDateTime createdAt;


    @Schema(description = "Task completion timestamp")
    private LocalDateTime completedAt;

    @Schema(description = "Total number of data sources to be processed")
    private Integer totalDataSources;

    @Schema(description = "Number of data sources that have been processed")
    private Integer processedDataSources;

    @Schema(description = "Task name")
    private String name;

    @Schema(description = "Task description")
    private String description;

    // Constructors
    public TaskStatusResponse() {}

    public TaskStatusResponse(String taskId, ProfilingTask.TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }

}