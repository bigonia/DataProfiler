package com.dataprofiler.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a structured profiling report
 * Stores comprehensive data profiling results for database tables
 * 
 * Optimized for performance with:
 * - Indexed fields for fast querying (taskId, dataSourceId, dataSourceType)
 * - Pre-computed summary data in summaryJson field
 * - Full report data in reportJson field
 * - Metadata fields for efficient filtering and sorting
 */
@Entity
@Table(name = "structured_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StructuredReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Task ID for efficient querying and indexing
     */
    @Column(name = "task_id", nullable = false)
    private String taskId;

    /**
     * Data source identifier for filtering and grouping
     */
    @Column(name = "data_source_id", nullable = false)
    private String dataSourceId;

    /**
     * Report generation timestamp
     */
    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    /**
     * Profiling execution start time
     */
    @Column(name = "profiling_start_time")
    private LocalDateTime profilingStartTime;

    /**
     * Profiling execution end time
     */
    @Column(name = "profiling_end_time")
    private LocalDateTime profilingEndTime;

    /**
     * Database profile information as JSON
     */
    @Lob
    @Column(name = "database_profile_json")
    private String databaseProfileJson;

    /**
     * Schema profiles information as JSON
     */
    @Lob
    @Column(name = "schema_profiles_json")
    private String schemaProfilesJson;

    /**
     * Table profiles information as JSON
     */
    @Lob
    @Column(name = "table_profiles_json")
    private String tableProfilesJson;

    /**
     * Summary statistics as JSON
     */
    @Lob
    @Column(name = "summary_statistics_json")
    private String summaryStatisticsJson;

    /**
     * Report metadata as JSON
     */
    @Lob
    @Column(name = "metadata_json")
    private String metadataJson;

    // Denormalized fields for efficient querying and summary operations
    @Column(name = "total_tables")
    private Integer totalTables;

    @Column(name = "total_columns")
    private Integer totalColumns;

    @Column(name = "estimated_total_rows")
    private Long estimatedTotalRows;

    @Column(name = "estimated_total_size_bytes")
    private Long estimatedTotalSizeBytes;

    @Column(name = "profiling_duration_seconds")
    private Long profilingDurationSeconds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}