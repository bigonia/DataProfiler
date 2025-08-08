package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating profiling tasks
 * Supports both single-source and multi-source profiling
 */
@Schema(description = "Request for creating a profiling task")
public class ProfilingTaskRequest {

    @NotNull(message = "Data sources configuration is required")
    @NotEmpty(message = "At least one data source must be specified")
    @Schema(description = "Map of data source ID to schema/table configuration", 
            example = "{\"ds-pg-01\": {\"schemas\": {\"public\": [\"orders\", \"customers\"], \"marketing\": []}}, \"ds-file-01\": {}}")
    private Map<String, DataSourceScope> datasources;

    // Constructors
    public ProfilingTaskRequest() {}

    public ProfilingTaskRequest(Map<String, DataSourceScope> datasources) {
        this.datasources = datasources;
    }

    // Getters and Setters
    public Map<String, DataSourceScope> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, DataSourceScope> datasources) {
        this.datasources = datasources;
    }



    /**
     * Represents the scope of profiling for a single data source
     */
    @Schema(description = "Profiling scope for a data source")
    public static class DataSourceScope {
        
        @Schema(description = "Map of schema name to list of table names. Empty list means all tables in schema.",
                example = "{\"public\": [\"orders\", \"customers\"], \"marketing\": []}")
        private Map<String, List<String>> schemas;

        // Constructors
        public DataSourceScope() {}

        public DataSourceScope(Map<String, List<String>> schemas) {
            this.schemas = schemas;
        }

        // Getters and Setters
        public Map<String, List<String>> getSchemas() {
            return schemas;
        }

        public void setSchemas(Map<String, List<String>> schemas) {
            this.schemas = schemas;
        }
    }
}