package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * Request DTO for retrieving summary reports
 * Contains data source IDs for batch summary retrieval
 */
@Data
@Schema(description = "Request for retrieving summary reports")
public class ReportSummaryRequest {

    @Schema(description = "taskId for reports")
    private String taskId;

//    @NotNull(message = "Data source IDs are required")
//    @NotEmpty(message = "At least one data source ID must be specified")
    @Schema(description = "List of data source IDs to retrieve summaries for", 
            example = "[\"ds-pg-01\", \"ds-mysql-02\", \"ds-file-03\"]")
    private List<String> dataSourceIds;

    // Constructors
    public ReportSummaryRequest() {}

    public ReportSummaryRequest(String taskId) {
        this.taskId = taskId;
    }

    public ReportSummaryRequest(List<String> dataSourceIds) {
        this.dataSourceIds = dataSourceIds;
    }

    // Getters and Setters
    public List<String> getDataSourceIds() {
        return dataSourceIds;
    }

    public void setDataSourceIds(List<String> dataSourceIds) {
        this.dataSourceIds = dataSourceIds;
    }
}