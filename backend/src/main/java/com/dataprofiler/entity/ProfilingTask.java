package com.dataprofiler.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a profiling task
 * Tracks the lifecycle and status of data profiling operations
 */
@Entity
@Table(name = "profiling_tasks")
@Data
public class ProfilingTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Task ID is required")
    @Column(name = "task_id", nullable = false, unique = true)
    private String taskId;

    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;
    
    /**
     * Multiple data sources associated with this profiling task
     * Task will process all data sources sequentially
     */
    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER)
//    @JoinTable(
//        name = "profiling_task_datasource",
//        joinColumns = @JoinColumn(name = "task_id"),
//        inverseJoinColumns = @JoinColumn(name = "datasource_id")
//    )
    private List<DataSourceConfig> dataSourceConfigs;

    /**
     * Total number of data sources to be processed
     */
    @Column(name = "total_data_sources")
    private Integer totalDataSources = 0;

    /**
     * Number of data sources that have been processed
     */
    @Column(name = "processed_data_sources")
    private Integer processedDataSources = 0;
    
    @NotNull(message = "Task status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status = TaskStatus.PENDING;

    /**
     * 存储发起任务时的原始请求体JSON，用于追溯和调试。
     */
//    @Lob
    @Column(nullable = false)
    private String requestPayload;

    @Column(name = "info", length = 2000)
    private String info;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    // Constructors
    public ProfilingTask() {}

    public ProfilingTask(String taskId, TaskStatus status) {
        this.taskId = taskId;
        this.status = status;
    }
    
    public ProfilingTask(String name, List<DataSourceConfig> dataSourceConfigs) {
        this.name = name;
        this.dataSourceConfigs = dataSourceConfigs != null ? dataSourceConfigs : new ArrayList<>();
        this.totalDataSources = this.dataSourceConfigs.size();
        this.taskId = generateTaskId();
    }

    /**
     * Add a data source to the task
     */
    public void addDataSource(DataSourceConfig dataSourceConfig) {
        if (this.dataSourceConfigs == null) {
            this.dataSourceConfigs = new ArrayList<>();
        }
        this.dataSourceConfigs.add(dataSourceConfig);
        this.totalDataSources = this.dataSourceConfigs.size();
    }

    /**
     * Remove a data source from the task
     */
    public void removeDataSource(DataSourceConfig dataSourceConfig) {
        if (this.dataSourceConfigs != null) {
            this.dataSourceConfigs.remove(dataSourceConfig);
            this.totalDataSources = this.dataSourceConfigs.size();
        }
    }

    /**
     * Check if all data sources have been processed
     */
    public boolean isAllDataSourcesProcessed() {
        return processedDataSources != null && totalDataSources != null 
            && processedDataSources.equals(totalDataSources);
    }

    /**
     * Increment processed data sources count
     */
    public void incrementProcessedDataSources() {
        if (processedDataSources == null) {
            processedDataSources = 0;
        }
        processedDataSources++;
    }

    /**
     * Generate unique task ID
     */
    private String generateTaskId() {
        return "TASK_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }


    /**
     * Enum defining task status values
     */
    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
    }
    

}