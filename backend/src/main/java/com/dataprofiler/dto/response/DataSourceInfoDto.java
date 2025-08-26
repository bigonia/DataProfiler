package com.dataprofiler.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for data source information including schemas and tables
 * Used by the unified getDatasourceInfo API endpoint
 */
@Schema(description = "Data source information with schemas and tables")
public class DataSourceInfoDto {

    @Schema(description = "Data source unique identifier")
    private String sourceId;

    @Schema(description = "Data source name")
    private String name;

    @Schema(description = "Data source type")
    private String type;

    @Schema(description = "Map of schema names to their table lists")
    private Map<String, List<String>> schemas;

    @Schema(description = "Timestamp when this information was last updated")
    private LocalDateTime lastUpdated;

    @Schema(description = "Whether this data is from cache")
    private boolean fromCache;

    @Schema(description = "Total number of schemas")
    private int schemaCount;

    @Schema(description = "Total number of tables across all schemas")
    private int totalTableCount;

    // Constructors
    public DataSourceInfoDto() {}

    public DataSourceInfoDto(String sourceId, String name, String type, Map<String, List<String>> schemas) {
        this.sourceId = sourceId;
        this.name = name;
        this.type = type;
        this.schemas = schemas;
        this.lastUpdated = LocalDateTime.now();
        this.fromCache = false;
        this.schemaCount = schemas != null ? schemas.size() : 0;
        this.totalTableCount = schemas != null ? 
            schemas.values().stream().mapToInt(List::size).sum() : 0;
    }

    // Getters and Setters
    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, List<String>> getSchemas() {
        return schemas;
    }

    public void setSchemas(Map<String, List<String>> schemas) {
        this.schemas = schemas;
        this.schemaCount = schemas != null ? schemas.size() : 0;
        this.totalTableCount = schemas != null ? 
            schemas.values().stream().mapToInt(List::size).sum() : 0;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    public int getSchemaCount() {
        return schemaCount;
    }

    public void setSchemaCount(int schemaCount) {
        this.schemaCount = schemaCount;
    }

    public int getTotalTableCount() {
        return totalTableCount;
    }

    public void setTotalTableCount(int totalTableCount) {
        this.totalTableCount = totalTableCount;
    }

    @Override
    public String toString() {
        return "DataSourceInfoDto{" +
                "sourceId='" + sourceId + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", schemaCount=" + schemaCount +
                ", totalTableCount=" + totalTableCount +
                ", lastUpdated=" + lastUpdated +
                ", fromCache=" + fromCache +
                '}';
    }
}