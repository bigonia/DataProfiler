package com.dataprofiler.dto.response;


import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.service.impl.ReportTransformServiceImpl;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for summary reports
 * Provides lightweight overview of data sources and their tables
 * Updated to support target format with databases array structure
 */
@Data
@Schema(description = "Summary report for a data source")
public class ReportSummaryDto {

    @Schema(description = "taskId")
    private String taskId;

    @Schema(description = "Data source identifier", example = "ds-pg-01")
    private String dataSourceId;

    @Schema(description = "Data source name", example = "Production Database")
    private String dataSourceName;

    @Schema(description = "Data source type", example = "POSTGRESQL")
    private DataSourceConfig.DataSourceType dataSourceType;

    @Schema(description = "Databases array in target format")
    private List<ReportTransformServiceImpl.DatabaseSummary> databases;

    @Schema(description = "Total number of tables")
    private Integer totalTables;

    @Schema(description = "Total number of columns")
    private Integer totalColumns;

    @Schema(description = "Estimated total number of rows")
    private Long estimatedTotalRows;

    @Schema(description = "Estimated total size in bytes")
    private Long estimatedTotalSizeBytes;


    @Schema(description = "Formatted data size (human readable)")
    private String formattedDataSize;


    // Constructors
    public ReportSummaryDto() {}

    public ReportSummaryDto(String taskId, String dataSourceId, String dataSourceName, 
                           DataSourceConfig.DataSourceType dataSourceType) {
        this.taskId = taskId;
        this.dataSourceId = dataSourceId;
        this.dataSourceName = dataSourceName;
        this.dataSourceType = dataSourceType;
    }

}