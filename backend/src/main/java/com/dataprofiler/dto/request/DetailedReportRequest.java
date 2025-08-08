package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for retrieving detailed reports
 * Supports batch querying with pagination
 */
@Schema(description = "Request for retrieving detailed profiling reports")
public class DetailedReportRequest {

    @NotNull(message = "Data sources configuration is required")
    @NotEmpty(message = "At least one data source must be specified")
    @Schema(description = "Map of data source ID to schema/table configuration")
    private Map<String, ProfilingTaskRequest.DataSourceScope> dataSources;

    @Schema(description = "Pagination configuration")
    private PaginationConfig pagination;

    // Constructors
    public DetailedReportRequest() {
        this.pagination = new PaginationConfig();
    }

    public DetailedReportRequest(Map<String, ProfilingTaskRequest.DataSourceScope> dataSources) {
        this.dataSources = dataSources;
        this.pagination = new PaginationConfig();
    }

    // Getters and Setters
    public Map<String, ProfilingTaskRequest.DataSourceScope> getDataSources() {
        return dataSources;
    }

    public void setDataSources(Map<String, ProfilingTaskRequest.DataSourceScope> dataSources) {
        this.dataSources = dataSources;
    }

    // Backward compatibility method
    @Deprecated
    public Map<String, ProfilingTaskRequest.DataSourceScope> getDatasources() {
        return dataSources;
    }

    @Deprecated
    public void setDatasources(Map<String, ProfilingTaskRequest.DataSourceScope> dataSources) {
        this.dataSources = dataSources;
    }

    public PaginationConfig getPagination() {
        return pagination;
    }

    public void setPagination(PaginationConfig pagination) {
        this.pagination = pagination;
    }

    /**
     * Pagination configuration for detailed reports
     */
    @Schema(description = "Pagination configuration")
    public static class PaginationConfig {
        
        @Min(value = 1, message = "Page number must be at least 1")
        @Schema(description = "Page number (1-based)", example = "1", defaultValue = "1")
        private int page = 1;

        @Min(value = 1, message = "Page size must be at least 1")
        @Schema(description = "Number of items per page", example = "10", defaultValue = "10")
        private int pageSize = 10;

        // Constructors
        public PaginationConfig() {}

        public PaginationConfig(int page, int pageSize) {
            this.page = page;
            this.pageSize = pageSize;
        }

        // Getters and Setters
        public int getPage() {
            return page;
        }

        public void setPage(int page) {
            this.page = page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}