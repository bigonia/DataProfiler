package com.dataprofiler.dto.response;

import com.dataprofiler.entity.DataSourceConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Response DTO for report basic information
 * Provides lightweight overview of report metadata for listing purposes
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Basic report information for listing")
public class ReportInfoDto {

    @Schema(description = "Report ID", example = "1")
    private Long id;

    @Schema(description = "Task ID", example = "task-123")
    private String taskId;

    @Schema(description = "Data source identifier", example = "ds-pg-01")
    private String dataSourceId;

    @Schema(description = "Data source name", example = "Production Database")
    private String dataSourceName;

    @Schema(description = "Data source type", example = "POSTGRESQL")
    private DataSourceConfig.DataSourceType dataSourceType;

    @Schema(description = "Report generation timestamp")
    private LocalDateTime generatedAt;

    @Schema(description = "Total number of tables", example = "25")
    private Integer totalTables;

    @Schema(description = "Total number of columns", example = "150")
    private Integer totalColumns;

    @Schema(description = "Estimated total number of rows", example = "1500000")
    private Long estimatedTotalRows;

    @Schema(description = "Estimated total size in bytes", example = "104857600")
    private Long estimatedTotalSizeBytes;

}