package com.dataprofiler.service;

import com.dataprofiler.dto.response.DashboardStatsDto;

import java.util.List;
import java.util.Map;

/**
 * Service interface for dashboard statistics and overview data
 * Provides methods to retrieve system metrics, charts data, and activities
 */
public interface DashboardService {

    /**
     * Get comprehensive dashboard statistics
     *
     * @return dashboard statistics including counts and distributions
     */
    DashboardStatsDto getDashboardStatistics();

    /**
     * Get task status distribution for charts
     *
     * @return list of task status counts
     */
    List<DashboardStatsDto.StatusCount> getTaskStatusDistribution();

    /**
     * Get data source types distribution for charts
     *
     * @return list of data source type counts
     */
    List<DashboardStatsDto.TypeCount> getDataSourceTypesDistribution();

    /**
     * Get recent system activities
     *
     * @param limit maximum number of activities to return
     * @return list of recent activities
     */
    List<DashboardStatsDto.ActivityDto> getRecentActivities(int limit);
}