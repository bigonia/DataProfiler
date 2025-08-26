package com.dataprofiler.dto.response;

import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.ProfilingTask;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

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

    @Schema(description = "List of data sources associated with this task")
    private List<DataSourceInfo> dataSources;

    /**
     * Data source information for task status response
     */
    @Data
    @Schema(description = "Data source information")
    public static class DataSourceInfo {
        
        @Schema(description = "Data source ID", example = "1")
        private Long id;
        
        @Schema(description = "Data source unique identifier", example = "ds-pg-01")
        private String sourceId;
        
        @Schema(description = "Data source name", example = "Production Database")
        private String name;
        
        @Schema(description = "Data source type", example = "POSTGRESQL")
        private DataSourceConfig.DataSourceType type;
        
//        @Schema(description = "Whether the data source is active", example = "true")
//        private Boolean active;
        
        @Schema(description = "Data source creation timestamp")
        private LocalDateTime createdAt;
    }

    // Constructors
    public TaskStatusResponse() {}

    public TaskStatusResponse(String taskId, ProfilingTask.TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }

}