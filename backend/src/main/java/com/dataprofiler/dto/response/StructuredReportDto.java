package com.dataprofiler.dto.response;

import com.dataprofiler.config.SampleRowsDeserializer;
import com.dataprofiler.entity.DataSourceConfig;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for detailed structured reports
 * Contains comprehensive profiling data with metrics and sample data
 * Structure follows the module interaction and data definition documentation
 */
@Data
@Schema(description = "Detailed structured profiling report")
public class StructuredReportDto {

    @Schema(description = "taskId")
    private String taskId;

    @Schema(description = "Data source identifier", example = "ds-pg-01")
    private String dataSourceId;

    @Schema(description = "Data source type", example = "POSTGRESQL")
    private DataSourceConfig.DataSourceType dataSourceType;

    @Schema(description = "Database information")
    private DatabaseInfo database;

    @Schema(description = "List of profiled tables")
    private List<TableReport> tables;

    @Schema(description = "Report generation timestamp")
    private LocalDateTime generatedAt;

    /**
     * Database information
     */
    @Data
    @Schema(description = "Database information")
    public static class DatabaseInfo {

        @Schema(description = "Database name", example = "sales_dw")
        private String name;

    }

    /**
     * Detailed table profiling report
     */
    @Data
    @Schema(description = "Detailed table profiling report")
    public static class TableReport {

        @Schema(description = "Table name", example = "orders")
        private String name;

        @Schema(description = "Schema name (null for file sources)", example = "public")
        private String schemaName;

        @Schema(description = "Number of rows in table", example = "1500000")
        private Long rowCount;

        @Schema(description = "Table comment/description", example = "Sales orders table")
        private String comment;

        @Schema(description = "List of column profiling reports")
        private List<ColumnReport> columns;

        @Schema(description = "Sample rows data as list of rows, each row is a list of column values")
        @JsonDeserialize(using = SampleRowsDeserializer.class)
        private List<List<Object>> sampleRows;

        private boolean useSample;
    }

    /**
     * Column profiling report with detailed metrics
     */
    @Data
    @Schema(description = "Column profiling report with detailed metrics")
    public static class ColumnReport {

        @Schema(description = "Column name", example = "order_amount")
        private String name;

        @Schema(description = "Column data type", example = "NUMERIC(10,2)")
        private String type;

        @Schema(description = "Whether column is part of primary key", example = "false")
        private Boolean isPrimaryKey;

        @Schema(description = "Column comment/description", example = "Order total amount")
        private String comment;

        @Schema(description = "Detailed column metrics")
        private ColumnMetrics metrics;

//        @Schema(description = "Sample values for this column", example = "[\"value1\", \"value2\", \"value3\"]")
//        private List<String> sampleValues;

    }

    /**
     * Detailed column metrics
     */
    @Data
    @Schema(description = "Detailed column metrics")
    public static class ColumnMetrics {

        @Schema(description = "Number of null values", example = "50")
        private Long nullCount;

        @Schema(description = "Null value rate (0.0-1.0)", example = "0.000033")
        private Double nullRate;

        @Schema(description = "Number of distinct values", example = "12800")
        private Long distinctCount;

        @Schema(description = "Distinct value rate (0.0-1.0)", example = "0.00853")
        private Double distinctRate;

        @Schema(description = "Value range information")
        private ValueRange range;

        @Schema(description = "Average value (for numeric columns)", example = "125.75")
        private Double avg;

        @Schema(description = "Standard deviation (for numeric columns)", example = "89.5")
        private Double stddev;

        @Schema(description = "Minimum string length (for text columns)", example = "4")
        private Integer minLength;

        @Schema(description = "Maximum string length (for text columns)", example = "12")
        private Integer maxLength;

    }

    /**
     * Value range information
     */
    @Data
    @Schema(description = "Value range information")
    public static class ValueRange {

        @Schema(description = "Minimum value", example = "0.50")
        private Object min;

        @Schema(description = "Maximum value", example = "9999.99")
        private Object max;

        /**
         * Compact format for arrays (header-rows structure)
         */
        @Data
        @Schema(description = "Compact format with headers and rows")
        public static class CompactFormat {

            @Schema(description = "Column headers", example = "[\"id\", \"name\", \"amount\"]")
            private List<String> headers;

            @Schema(description = "Data rows as arrays", example = "[[1, \"Alice\", 100.0], [2, \"Bob\", 200.0]]")
            private List<List<Object>> rows;

        }

        /**
         * Report summary information
         */
        @Data
        @Schema(description = "Report summary information")
        public static class ReportSummary {

            @Schema(description = "Total number of data sources", example = "3")
            private Integer totalDataSources;

            @Schema(description = "Total number of tables", example = "25")
            private Integer totalTables;

            @Schema(description = "Total number of columns", example = "150")
            private Integer totalColumns;

            @Schema(description = "Total number of rows across all tables", example = "1500000")
            private Long totalRows;

            @Schema(description = "Average null rate across all columns", example = "0.05")
            private Double avgNullRate;

            @Schema(description = "Average distinct rate across all columns", example = "0.75")
            private Double avgDistinctRate;

        }

    }
}