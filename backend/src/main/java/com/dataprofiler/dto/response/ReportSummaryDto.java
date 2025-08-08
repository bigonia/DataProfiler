package com.dataprofiler.dto.response;

import com.dataprofiler.dto.response.StructuredReportDto.DatabaseInfo;
import com.dataprofiler.dto.response.StructuredReportDto.TableReport;
import com.dataprofiler.entity.DataSourceConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for summary reports
 * Provides lightweight overview of data sources and their tables
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

    @Schema(description = "Database information")
    private DatabaseInfo database;

    @Schema(description = "List of profiled tables")
    private List<TableReport> tables;

    @Schema(description = "Total number of tables")
    private Integer totalTables;

    @Schema(description = "Total number of columns")
    private Integer totalColumns;

    @Schema(description = "Estimated total number of rows")
    private Long estimatedTotalRows;

    @Schema(description = "Estimated total size in bytes")
    private Long estimatedTotalSizeBytes;

    @Schema(description = "Profiling duration in seconds")
    private Long profilingDurationSeconds;

    @Schema(description = "Formatted data size (human readable)")
    private String formattedDataSize;

    @Schema(description = "Formatted duration (human readable)")
    private String formattedDuration;

    // Constructors
    public ReportSummaryDto() {}

    public ReportSummaryDto(String taskId, String dataSourceId, String dataSourceName, 
                           DataSourceConfig.DataSourceType dataSourceType, 
                           DatabaseInfo database, List<TableReport> tables) {
        this.taskId = taskId;
        this.dataSourceId = dataSourceId;
        this.dataSourceName = dataSourceName;
        this.dataSourceType = dataSourceType;
        this.database = database;
        this.tables = tables;
    }

    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public DataSourceConfig.DataSourceType getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(DataSourceConfig.DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public DatabaseInfo getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseInfo database) {
        this.database = database;
    }

    public List<TableReport> getTables() {
        return tables;
    }

    public void setTables(List<TableReport> tables) {
        this.tables = tables;
    }

    public Integer getTotalTables() {
        return totalTables;
    }

    public void setTotalTables(Integer totalTables) {
        this.totalTables = totalTables;
    }

    public Integer getTotalColumns() {
        return totalColumns;
    }

    public void setTotalColumns(Integer totalColumns) {
        this.totalColumns = totalColumns;
    }

    public Long getEstimatedTotalRows() {
        return estimatedTotalRows;
    }

    public void setEstimatedTotalRows(Long estimatedTotalRows) {
        this.estimatedTotalRows = estimatedTotalRows;
    }

    public Long getEstimatedTotalSizeBytes() {
        return estimatedTotalSizeBytes;
    }

    public void setEstimatedTotalSizeBytes(Long estimatedTotalSizeBytes) {
        this.estimatedTotalSizeBytes = estimatedTotalSizeBytes;
    }

    public Long getProfilingDurationSeconds() {
        return profilingDurationSeconds;
    }

    public void setProfilingDurationSeconds(Long profilingDurationSeconds) {
        this.profilingDurationSeconds = profilingDurationSeconds;
    }

    public String getFormattedDataSize() {
        return formattedDataSize;
    }

    public void setFormattedDataSize(String formattedDataSize) {
        this.formattedDataSize = formattedDataSize;
    }

    public String getFormattedDuration() {
        return formattedDuration;
    }

    public void setFormattedDuration(String formattedDuration) {
        this.formattedDuration = formattedDuration;
    }
}