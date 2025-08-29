package com.dataprofiler.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating profiling tasks
 * Contains data source configurations and profiling scope
 */
@Data
@Schema(description = "Request for creating a profiling task")
public class ProfilingTaskRequest {

    @NotNull(message = "Data sources configuration is required")
    @NotEmpty(message = "At least one data source must be specified")
    @Schema(description = "Map of data source ID to schema/table configuration", 
            example = "{\"ds-pg-01\": {\"schemas\": {\"public\": [\"orders\", \"customers\"], \"marketing\": []}}, \"ds-file-01\": {}}")
    private Map<String, DataSourceScope> datasources;

    @Min(value = 1, message = "Field max length must be at least 1")
    @Max(value = 10000, message = "Field max length cannot exceed 10000")
    @Schema(description = "Maximum length limit for field content to avoid overly long field values", 
            example = "128", defaultValue = "128")
    private Integer fieldMaxLength = 128;

    @Min(value = 1, message = "Sample data limit must be at least 1")
    @Max(value = 1000, message = "Sample data limit cannot exceed 1000")
    @Schema(description = "Maximum number of sample data rows to return", 
            example = "5", defaultValue = "5")
    private Integer sampleDataLimit = 5;

    // Constructors
    public ProfilingTaskRequest() {}

    public ProfilingTaskRequest(Map<String, DataSourceScope> datasources) {
        this.datasources = datasources;
    }

    public ProfilingTaskRequest(Map<String, DataSourceScope> datasources, Integer fieldMaxLength, Integer sampleDataLimit) {
        this.datasources = datasources;
        this.fieldMaxLength = fieldMaxLength;
        this.sampleDataLimit = sampleDataLimit;
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