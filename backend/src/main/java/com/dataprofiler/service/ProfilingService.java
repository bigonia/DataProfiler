package com.dataprofiler.service;

import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.dto.response.TaskStatusResponse;
import com.dataprofiler.entity.ProfilingTask;

import java.util.List;
import java.util.Optional;

/**
 * Core profiling service interface for orchestrating data profiling operations
 * This service acts as the central coordinator for end-to-end profiling tasks
 * <p>
 * Key responsibilities:
 * - Task lifecycle management (PENDING -> RUNNING -> SUCCESS/FAILED)
 * - Async execution coordination
 * - Multi-data source profiling orchestration
 * - Task CRUD operations and queries
 * - Service coordination (DataSourceService, IDatabaseProfiler, ReportAssemblyService, StructuredReportService)
 */
public interface ProfilingService {

    List<ProfilingTask> getAllProfilingTasks();

    /**
     * Start a new profiling task and return immediately, then execute the task asynchronously
     * Creates a ProfilingTask entity with PENDING status, saves it to database, and triggers async execution
     *
     * @param request Request object containing one or more data sources and their profiling scope
     * @return Newly created task entity with unique taskId and initial status
     */
    ProfilingTask startProfilingTask(ProfilingTaskRequest request);


    ProfilingTask getTask(String taskId);

    /**
     * Delete a task by taskId
     */
    void deleteTask(String taskId);


    /**
     * Find tasks by data source ID
     */
    List<ProfilingTask> findByDataSourceId(String dataSourceId);


}