package com.dataprofiler.dto.internal;

import com.dataprofiler.entity.DataSourceConfig;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Internal DTO for raw profiling data
 * Contains unprocessed metadata and basic metrics from database profilers
 * This is the "raw material" produced by Profilers and consumed by AssemblyService
 */
@Data
public class RawProfileDataDto {

    private String dataSourceId;
    private DataSourceConfig.DataSourceType dataSourceType;
    private String databaseName;
    private String schemaName;
    private LocalDateTime profilingTimestamp;
    private List<TableData> tables;
    private Map<String, Object> metadata;

    public RawProfileDataDto(String dataSourceId, DataSourceConfig.DataSourceType dataSourceType) {
        this.dataSourceId = dataSourceId;
        this.dataSourceType = dataSourceType;
    }

    /**
     * Raw table data structure
     */
    @Data
    public static class TableData {
        private String tableName;
        private String schemaName;
        private Long rowCount;
        private Long approximateRowCount;
        private String tableType; // TABLE, VIEW, etc.
        private List<ColumnData> columns;
        private List<IndexData> indexes;
        private Map<String, Object> tableMetadata;

        // Constructors
        public TableData() {}

        public TableData(String tableName, String schemaName) {
            this.tableName = tableName;
            this.schemaName = schemaName;
        }
    }

    /**
     * Raw column data structure
     */
    @Data
    public static class ColumnData {
        private String columnName;
        private String dataType;
        private String nativeType;
        private Integer columnSize;
        private Integer decimalDigits;
        private Boolean nullable;
        private String defaultValue;
        private Boolean isPrimaryKey;
        private Boolean isForeignKey;
        private Boolean isUnique;
        private Boolean isIndexed;
        
        // Basic profiling metrics (raw counts)
        private Long nullCount;
        private Long uniqueCount;
        private Long totalCount;
        private Object minValue;
        private Object maxValue;
        private Double avgLength;
        private Long maxLength;
        private Long minLength;
        
        // Sample data
        private List<Object> sampleValues;
        private Map<String, Object> columnMetadata;

        // Constructors
        public ColumnData() {}

        public ColumnData(String columnName, String dataType) {
            this.columnName = columnName;
            this.dataType = dataType;
        }
    }

    /**
     * Raw index data structure
     */
    @Data
    public static class IndexData {
        private String indexName;
        private String indexType;
        private Boolean isUnique;
        private Boolean isPrimary;
        private List<String> columnNames;
        private Map<String, Object> indexMetadata;

        // Constructors
        public IndexData() {}

        public IndexData(String indexName, String indexType) {
            this.indexName = indexName;
            this.indexType = indexType;
        }


    }
}