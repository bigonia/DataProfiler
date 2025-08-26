package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for retrieving detailed reports
 * Supports filtering by data sources, schemas, and tables with pagination
 */
@Data
@Schema(description = "Request for retrieving detailed profiling reports")
public class DetailedReportRequest {

    @Schema(description = "taskId for reports")
    private String taskId;

    @Schema(description = "Filtering criteria for reports")
    private FilterCriteria filters;

    @Min(value = 0, message = "Page number must be non-negative")
    @Schema(description = "Page number (0-based)", example = "0", defaultValue = "0")
    private int page = 0;

    @Min(value = 1, message = "Page size must be positive")
    @Schema(description = "Number of items per page", example = "20", defaultValue = "20")
    private int pageSize = 20;

    @Schema(description = "Output format", example = "standard", allowableValues = {"standard", "compact"})
    private String format = "standard";

    // Constructors
    public DetailedReportRequest() {}

    public DetailedReportRequest(FilterCriteria filters, int page, int pageSize) {
        this.filters = filters;
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * Filtering criteria for detailed reports
     */
    @Data
    @Schema(description = "Filtering criteria for detailed reports")
    public static class FilterCriteria {
        
        @Schema(description = "Map of data source ID to schema/table configuration",
                example = "{\"ds-pg-01\": {\"schemas\": {\"public\": [\"orders\", \"customers\"], \"marketing\": []}}, \"ds-file-01\": {}}")
        private Map<String, DataSourceScope> dataSources;

        // Constructors
        public FilterCriteria() {}

        public FilterCriteria(Map<String, DataSourceScope> dataSources) {
            this.dataSources = dataSources;
        }
    }

    /**
     * Data source scope configuration
     */
    @Data
    @Schema(description = "Data source scope configuration")
    public static class DataSourceScope {
        
        @Schema(description = "Map of schema name to list of table names. Empty list means all tables in schema.")
        private Map<String, List<String>> schemas;

        // Constructors
        public DataSourceScope() {}

        public DataSourceScope(Map<String, List<String>> schemas) {
            this.schemas = schemas;
        }
    }
}