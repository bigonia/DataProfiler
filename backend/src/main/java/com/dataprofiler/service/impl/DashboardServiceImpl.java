package com.dataprofiler.service.impl;

import com.dataprofiler.dto.response.DashboardStatsDto;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.ProfilingTask;
import com.dataprofiler.repository.DataSourceConfigRepository;
import com.dataprofiler.repository.FileMetadataRepository;
import com.dataprofiler.repository.ProfilingTaskRepository;
import com.dataprofiler.repository.StructuredReportRepository;
import com.dataprofiler.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of DashboardService interface
 * Provides real-time system statistics and overview data for dashboard
 */
@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DataSourceConfigRepository dataSourceRepository;

    @Autowired
    private FileMetadataRepository fileRepository;

    @Autowired
    private ProfilingTaskRepository taskRepository;

    @Autowired
    private StructuredReportRepository reportRepository;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public DashboardStatsDto getDashboardStatistics() {
        log.debug("Collecting dashboard statistics");

        try {
            // Get basic counts
            long dataSourceCount = dataSourceRepository.count();
            long fileCount = fileRepository.count();
            long taskCount = taskRepository.count();
            long reportCount = reportRepository.count();

            // Get distributions
            List<DashboardStatsDto.StatusCount> taskStatusDistribution = getTaskStatusDistribution();
            List<DashboardStatsDto.TypeCount> dataSourceTypesDistribution = getDataSourceTypesDistribution();
            List<DashboardStatsDto.ActivityDto> recentActivities = getRecentActivities(10);

            return DashboardStatsDto.builder()
                    .dataSourceCount(dataSourceCount)
                    .fileCount(fileCount)
                    .taskCount(taskCount)
                    .reportCount(reportCount)
                    .taskStatusDistribution(taskStatusDistribution)
                    .dataSourceTypesDistribution(dataSourceTypesDistribution)
                    .recentActivities(recentActivities)
                    .build();

        } catch (Exception e) {
            log.error("Error collecting dashboard statistics", e);
            // Return empty stats on error
            return DashboardStatsDto.builder()
                    .dataSourceCount(0L)
                    .fileCount(0L)
                    .taskCount(0L)
                    .reportCount(0L)
                    .taskStatusDistribution(new ArrayList<>())
                    .dataSourceTypesDistribution(new ArrayList<>())
                    .recentActivities(new ArrayList<>())
                    .build();
        }
    }

    @Override
    public List<DashboardStatsDto.StatusCount> getTaskStatusDistribution() {
        log.debug("Getting task status distribution");

        try {
            List<ProfilingTask> allTasks = taskRepository.findAll();
            
            Map<ProfilingTask.TaskStatus, Long> statusCounts = allTasks.stream()
                    .collect(Collectors.groupingBy(
                            ProfilingTask::getStatus,
                            Collectors.counting()
                    ));

            return statusCounts.entrySet().stream()
                    .map(entry -> DashboardStatsDto.StatusCount.builder()
                            .name(entry.getKey().name())
                            .value(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting task status distribution", e);
            return Arrays.asList(
                    DashboardStatsDto.StatusCount.builder().name("COMPLETED").value(0L).build(),
                    DashboardStatsDto.StatusCount.builder().name("RUNNING").value(0L).build(),
                    DashboardStatsDto.StatusCount.builder().name("FAILED").value(0L).build(),
                    DashboardStatsDto.StatusCount.builder().name("PENDING").value(0L).build()
            );
        }
    }

    @Override
    public List<DashboardStatsDto.TypeCount> getDataSourceTypesDistribution() {
        log.debug("Getting data source types distribution");

        try {
            List<DataSourceConfig> allDataSources = dataSourceRepository.findAll();
            
            Map<DataSourceConfig.DataSourceType, Long> typeCounts = allDataSources.stream()
                    .collect(Collectors.groupingBy(
                            DataSourceConfig::getType,
                            Collectors.counting()
                    ));

            return typeCounts.entrySet().stream()
                    .map(entry -> DashboardStatsDto.TypeCount.builder()
                            .name(entry.getKey().name())
                            .value(entry.getValue())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting data source types distribution", e);
            return Arrays.asList(
                    DashboardStatsDto.TypeCount.builder().name("MYSQL").value(0L).build(),
                    DashboardStatsDto.TypeCount.builder().name("POSTGRESQL").value(0L).build(),
                    DashboardStatsDto.TypeCount.builder().name("ORACLE").value(0L).build(),
                    DashboardStatsDto.TypeCount.builder().name("SQLSERVER").value(0L).build(),
                    DashboardStatsDto.TypeCount.builder().name("FILE").value(0L).build()
            );
        }
    }

    @Override
    public List<DashboardStatsDto.ActivityDto> getRecentActivities(int limit) {
        log.debug("Getting recent activities with limit: {}", limit);

        try {
            List<DashboardStatsDto.ActivityDto> activities = new ArrayList<>();

            // Get recent tasks (limited to prevent performance issues)
            List<ProfilingTask> recentTasks = taskRepository.findAll().stream()
                    .sorted((t1, t2) -> {
                        LocalDateTime time1 = t1.getCreatedAt();
                        LocalDateTime time2 =  t2.getCreatedAt();
                        return time2.compareTo(time1); // Descending order
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

            // Convert tasks to activities
            for (ProfilingTask task : recentTasks) {
                String title = getTaskActivityTitle(task);
                String description = getTaskActivityDescription(task);
                LocalDateTime timestamp = task.getCreatedAt();
                
                activities.add(DashboardStatsDto.ActivityDto.builder()
                        .id(task.getId())
                        .type("task")
                        .title(title)
                        .description(description)
                        .timestamp(timestamp.format(DATETIME_FORMATTER))
                        .status(getActivityStatus(task.getStatus()))
                        .build());
            }

            return activities;

        } catch (Exception e) {
            log.error("Error getting recent activities", e);
            return new ArrayList<>();
        }
    }

    /**
     * Generate activity title based on task status
     */
    private String getTaskActivityTitle(ProfilingTask task) {
        switch (task.getStatus()) {
            case COMPLETED:
                return "Data profiling task completed";
            case RUNNING:
                return "Data profiling task in progress";
            case FAILED:
                return "Data profiling task failed";
            case PENDING:
                return "Data profiling task created";
            default:
                return "Data profiling task updated";
        }
    }

    /**
     * Generate activity description based on task details
     */
    private String getTaskActivityDescription(ProfilingTask task) {
        if (task.getDataSourceConfigs() != null && !task.getDataSourceConfigs().isEmpty()) {
            int dataSourceCount = task.getDataSourceConfigs().size();
            if (dataSourceCount == 1) {
                DataSourceConfig ds = task.getDataSourceConfigs().get(0);
                return String.format("Task for %s data source \"%s\" %s", 
                        ds.getType().name(), 
                        ds.getName(),
                        getStatusDescription(task.getStatus()));
            } else {
                return String.format("Task for %d data sources %s", 
                        dataSourceCount,
                        getStatusDescription(task.getStatus()));
            }
        }
        return String.format("Profiling task %s", getStatusDescription(task.getStatus()));
    }

    /**
     * Get status description for activity
     */
    private String getStatusDescription(ProfilingTask.TaskStatus status) {
        switch (status) {
            case COMPLETED:
                return "completed successfully";
            case RUNNING:
                return "is currently running";
            case FAILED:
                return "failed with errors";
            case PENDING:
                return "is pending execution";
            default:
                return "status updated";
        }
    }

    /**
     * Convert task status to activity status
     */
    private String getActivityStatus(ProfilingTask.TaskStatus status) {
        switch (status) {
            case COMPLETED:
                return "completed";
            case RUNNING:
                return "running";
            case FAILED:
                return "failed";
            case PENDING:
                return "pending";
            default:
                return "unknown";
        }
    }
}