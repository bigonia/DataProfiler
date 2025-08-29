package com.dataprofiler.service.impl;

import com.dataprofiler.dto.FileLoadResult;
import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.dto.response.TaskStatusResponse;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.ProfilingTask;
import com.dataprofiler.profiler.IDatabaseProfiler;
import com.dataprofiler.repository.ProfilingTaskRepository;
import com.dataprofiler.service.DataSourceService;
import com.dataprofiler.service.FileAsTableService;
import com.dataprofiler.service.ProfilingService;
import com.dataprofiler.service.ReportAssemblyService;
import com.dataprofiler.service.StructuredReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of ProfilingService interface
 * Core profiling service that orchestrates data profiling operations
 * <p>
 * Key responsibilities:
 * - Task lifecycle management (PENDING -> RUNNING -> SUCCESS/FAILED)
 * - Async execution coordination
 * - Multi-data source profiling orchestration
 * - Service coordination (DataSourceService, IDatabaseProfiler, ReportAssemblyService, StructuredReportService)
 */
@Service
@Transactional
public class ProfilingServiceImpl implements ProfilingService {

    private static final Logger logger = LoggerFactory.getLogger(ProfilingServiceImpl.class);

    @Autowired
    private ProfilingTaskRepository profilingTaskRepository;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private ReportAssemblyService reportAssemblyService;

    @Autowired
    private StructuredReportService structuredReportService;

    @Autowired
    private List<IDatabaseProfiler> profilers;

    @Autowired
    private FileAsTableService fileAsTableService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ProfilingTask> getAllProfilingTasks() {
        logger.debug("Getting all profiling tasks");
        return profilingTaskRepository.findAll();
    }

    @Override
    public ProfilingTask startProfilingTask(ProfilingTaskRequest request) {
        logger.info("Starting profiling task for {} data sources",
                request.getDatasources() != null ? request.getDatasources().size() : 0);

        try {
            // Create and save task with PENDING status
            ProfilingTask task = createProfilingTask(request);
            logger.info("Created profiling task : {}", Json.pretty(task));
            profilingTaskRepository.save(task);


            // Start async execution
            CompletableFuture<Void> asyncTask = executeTaskAsync(task.getTaskId());
            
            // Log async task initiation
            asyncTask.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    logger.error("Async task execution failed for task: {}", task.getTaskId(), throwable);
                } else {
                    logger.debug("Async task execution completed for task: {}", task.getTaskId());
                }
            });

            return task;

        } catch (Exception e) {
            logger.error("Failed to start profiling task", e);
            throw new RuntimeException("Failed to start profiling task: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional
    public ProfilingTask getTask(Long id) {
        logger.debug("Getting  task: {}", id);
        Optional<ProfilingTask> taskOpt = profilingTaskRepository.findById(id);
        if (taskOpt.isPresent()) {
            return taskOpt.get();
        }
        return null;
    }

    @Override
    public void deleteTask(Long id) {
        logger.info("Deleting task: {}", id);
        profilingTaskRepository.deleteById(id);
    }

    @Override
    public List<ProfilingTask> findByDataSourceId(String dataSourceId) {
        logger.debug("Finding tasks by data source ID: {}", dataSourceId);

        try {
            return null;
        } catch (Exception e) {
            logger.error("Error finding tasks by data source ID: {}", dataSourceId, e);
            throw new RuntimeException("Failed to find tasks by data source ID", e);
        }
    }

    /**
     * Create a new ProfilingTask entity from request
     */
    private ProfilingTask createProfilingTask(ProfilingTaskRequest request) throws Exception {
        ProfilingTask task = new ProfilingTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setStatus(ProfilingTask.TaskStatus.PENDING);
        task.setRequestPayload(objectMapper.writeValueAsString(request));
        
        // Set configuration parameters with default values if not provided
        task.setFieldMaxLength(request.getFieldMaxLength() != null ? request.getFieldMaxLength() : 128);
        task.setSampleDataLimit(request.getSampleDataLimit() != null ? request.getSampleDataLimit() : 10);

        // Set data source configurations for the task
        if (request.getDatasources() != null && !request.getDatasources().isEmpty()) {
            List<DataSourceConfig> dataSourceConfigs = new ArrayList<>();
            
            for (String dataSourceId : request.getDatasources().keySet()) {
                try {
                    DataSourceConfig dataSource = dataSourceService.getDataSourceBySourceId(dataSourceId);
                    if (dataSource != null) {
                        dataSourceConfigs.add(dataSource);
                    }
                } catch (Exception e) {
                    logger.warn("Could not load data source with ID: {}", dataSourceId, e);
                }
            }
            
            if (!dataSourceConfigs.isEmpty()) {
                task.setDataSourceConfigs(dataSourceConfigs);
                task.setTotalDataSources(dataSourceConfigs.size());
                task.setProcessedDataSources(0);
                
                // Set task name and description based on data source count
                if (dataSourceConfigs.size() == 1) {
                    DataSourceConfig singleDs = dataSourceConfigs.get(0);
                    task.setName("Profiling task for " + singleDs.getName());
                    task.setDescription("Automated profiling task for data source: " + singleDs.getName());
                } else {
                    task.setName("Multi-source profiling task (" + dataSourceConfigs.size() + " sources)");
                    task.setDescription("Automated profiling task for " + dataSourceConfigs.size() + " data sources:"+dataSourceConfigs.stream().map(DataSourceConfig::getName).collect(Collectors.joining(", ", " [", "]")));
                }
            } else {
                task.setName("Invalid profiling task");
                task.setDescription("Profiling task with no valid data sources");
            }
        } else {
            task.setName("Empty profiling task");
            task.setDescription("Profiling task with no data sources specified");
        }

        return task;
    }

    /**
     * Execute profiling task asynchronously
     * This method implements the core profiling workflow:
     * 1. Task initialization
     * 2. Request parsing and parallel scheduling
     * 3. Data source processing
     * 4. Data aggregation and assembly
     * 5. Report persistence
     * 6. Task status finalization
     */
    @Async
    public CompletableFuture<Void> executeTaskAsync(String taskId) {
        logger.info("Starting async execution for task: {}", taskId);

        Optional<ProfilingTask> taskOpt = profilingTaskRepository.findByTaskId(taskId);

        ProfilingTask task = taskOpt.get();

        try {
            // 1. Task initialization - update status to RUNNING
            updateTaskStatus(task, ProfilingTask.TaskStatus.RUNNING, "Task execution started");
            profilingTaskRepository.save(task);

            // 2. Parse request payload
            ProfilingTaskRequest request = objectMapper.readValue(task.getRequestPayload(), ProfilingTaskRequest.class);

            if (request.getDatasources() == null || request.getDatasources().isEmpty()) {
                throw new IllegalArgumentException("No data sources specified in request");
            }

            // 3. Process each data source (parallel execution for multiple sources)
            List<RawProfileDataDto> rawDataList = processDataSources(request, taskId);

            if (rawDataList.isEmpty()) {
                throw new RuntimeException("No data sources were successfully profiled");
            }

            // 4. Data aggregation and assembly
            updateTaskStatus(task, ProfilingTask.TaskStatus.RUNNING, "Assembling profiling reports");
            List<StructuredReportDto> reports = reportAssemblyService.assembleReport(rawDataList, taskId);

            // 5. Report persistence
            updateTaskStatus(task, ProfilingTask.TaskStatus.RUNNING, "Saving profiling reports");
            structuredReportService.saveReports(taskId, reports);

            // 6. Task completion
            task.setCompletedAt(LocalDateTime.now());
            updateTaskStatus(task, ProfilingTask.TaskStatus.COMPLETED,
                    String.format("Task completed successfully. Processed %d data sources, generated %d reports.",
                            rawDataList.size(), reports.size()));

            logger.info("Task {} completed successfully", taskId);

        } catch (Exception e) {
            logger.error("Task {} failed with error", taskId, e);

            task.setCompletedAt(LocalDateTime.now());
            updateTaskStatus(task, ProfilingTask.TaskStatus.FAILED,
                    "Task failed: " + e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Process all data sources in the request
     * Uses sequential execution for multiple data sources to ensure proper task status tracking
     */
    private List<RawProfileDataDto> processDataSources(ProfilingTaskRequest request, String taskId) {
        Map<String, ProfilingTaskRequest.DataSourceScope> dataSources = request.getDatasources();
        List<RawProfileDataDto> results = new ArrayList<>();
        
        Optional<ProfilingTask> taskOpt = profilingTaskRepository.findByTaskId(taskId);
        if (!taskOpt.isPresent()) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        
        ProfilingTask task = taskOpt.get();
        int totalSources = dataSources.size();
        int processedCount = 0;
        
        logger.info("Processing {} data sources sequentially for task: {}", totalSources, taskId);
        
        // Process each data source sequentially
        for (Map.Entry<String, ProfilingTaskRequest.DataSourceScope> entry : dataSources.entrySet()) {
            String dataSourceId = entry.getKey();
            ProfilingTaskRequest.DataSourceScope scope = entry.getValue();
            
            try {
                logger.info("Processing data source {} ({}/{}) for task: {}", 
                           dataSourceId, processedCount + 1, totalSources, taskId);
                
                updateTaskStatus(task, ProfilingTask.TaskStatus.RUNNING, 
                    String.format("Processing data source %s (%d/%d)", dataSourceId, processedCount + 1, totalSources));
                
                RawProfileDataDto result = processSingleDataSource(dataSourceId, scope, taskId);
                if (result != null) {
                    results.add(result);
                }
                
                // Update progress
                processedCount++;
                task.setProcessedDataSources(processedCount);
                profilingTaskRepository.save(task);
                
                logger.info("Completed processing data source {} for task: {}", dataSourceId, taskId);
                
            } catch (Exception e) {
                logger.error("Failed to process data source {} for task: {}", dataSourceId, taskId, e);
                // Continue processing other data sources even if one fails
                processedCount++;
                task.setProcessedDataSources(processedCount);
                profilingTaskRepository.save(task);
            }
        }
        
        logger.info("Completed processing all data sources for task: {}. Successfully processed: {}/{}", 
                   taskId, results.size(), totalSources);
        
        return results;
    }

    /**
     * Process a single data source
     */
    private RawProfileDataDto processSingleDataSource(String dataSourceId,
                                                      ProfilingTaskRequest.DataSourceScope scope,
                                                      String taskId) {
        try {
            logger.info("Processing data source: {} for task: {}", dataSourceId, taskId);

            // Get data source configuration by source ID
            DataSourceConfig dataSourceConfig = dataSourceService.getDataSourceBySourceId(dataSourceId);

            // Special handling for FILE type data sources
            if (DataSourceConfig.DataSourceType.FILE.equals(dataSourceConfig.getType())) {
//                return processFileDataSource(dataSourceConfig, scope, taskId);
                dataSourceConfig.setType(DataSourceConfig.DataSourceType.MYSQL);
            }

            // Get appropriate profiler for database types
            IDatabaseProfiler profiler = getProfiler(dataSourceConfig.getType().name());

            // Execute profiling
            RawProfileDataDto rawData = profiler.profile(dataSourceConfig, scope);

            logger.info("Successfully profiled data source: {} for task: {}", dataSourceId, taskId);
            return rawData;

        } catch (Exception e) {
            logger.error("Failed to profile data source: {} for task: {}", dataSourceId, taskId, e);
            // Return null to indicate failure - will be filtered out
            return null;
        }
    }

    /**
     * Process FILE type data source by converting to SQLite and then profiling
     */
    private RawProfileDataDto processFileDataSource(DataSourceConfig dataSourceConfig,
                                                   ProfilingTaskRequest.DataSourceScope scope,
                                                   String taskId) throws Exception {
        logger.info("Processing FILE data source: {} for task: {}", dataSourceConfig.getSourceId(), taskId);
        
        // Get file path from properties
        String filePath = dataSourceConfig.getProperties().get("filePath");
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("File path not found in data source properties");
        }
        
        // Convert file to SQLite tables
        logger.info("Converting file to SQLite tables: {}", filePath);
        FileLoadResult loadResult = fileAsTableService.loadExcelFileToDatabase(
            dataSourceConfig.getSourceId(), filePath);
        
        logger.info("File conversion completed. Loaded {} tables", loadResult.getLoadedTables().size());
        
        // Create a temporary SQLite data source configuration
        DataSourceConfig sqliteConfig = createSqliteConfigFromFile(dataSourceConfig, loadResult);
        
        // Get SQLite profiler and execute profiling
        IDatabaseProfiler sqliteProfiler = getProfiler("SQLITE");
        RawProfileDataDto rawData = sqliteProfiler.profile(sqliteConfig, scope);
        
        // Update metadata to reflect original file source
        Map<String, Object> metadata = rawData.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.put("original_source_type", "FILE");
        metadata.put("original_file_name", loadResult.getOriginalFileName());
        metadata.put("file_size_bytes", loadResult.getFileSize());
        metadata.put("loaded_sheets", loadResult.getLoadedTables().size());
        rawData.setMetadata(metadata);
        
        logger.info("Successfully profiled FILE data source: {} for task: {}", 
                   dataSourceConfig.getSourceId(), taskId);
        return rawData;
    }
    
    /**
     * Create SQLite data source configuration from file load result
     */
    private DataSourceConfig createSqliteConfigFromFile(DataSourceConfig originalConfig, 
                                                       FileLoadResult loadResult) {
        DataSourceConfig sqliteConfig = new DataSourceConfig();
        sqliteConfig.setSourceId(originalConfig.getSourceId());
        sqliteConfig.setName(originalConfig.getName() + " (SQLite)");
        sqliteConfig.setType(DataSourceConfig.DataSourceType.SQLITE);
        
        // Set SQLite connection properties
        Map<String, String> properties = new HashMap<>();
        properties.put("database", "data/core.db");
        sqliteConfig.setProperties(properties);
        
        return sqliteConfig;
    }

    /**
     * Get appropriate profiler for data source type
     */
    private IDatabaseProfiler getProfiler(String dataSourceType) {
        return profilers.stream()
                .filter(p -> p.supports(dataSourceType))
                .findFirst()
                .orElseThrow(() -> new UnsupportedOperationException(
                        "Unsupported data source type: " + dataSourceType));
    }

    /**
     * Update task status and info
     */
    private void updateTaskStatus(ProfilingTask task, ProfilingTask.TaskStatus status, String info) {
        task.setStatus(status);
        task.setInfo(info);
        profilingTaskRepository.save(task);
        logger.debug("Updated task {} status to {} with info: {}", task.getTaskId(), status, info);
    }
}