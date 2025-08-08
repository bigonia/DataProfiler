package com.dataprofiler.dto;

import com.dataprofiler.entity.DataSourceConfig;

import java.util.Map;

/**
 * Data Transfer Object for data source statistics
 * Contains aggregated information about data sources in the system
 */
public class DataSourceStatistics {
    
    private long totalCount;
    private long activeCount;
    private long inactiveCount;
    private Map<DataSourceConfig.DataSourceType, Long> countByType;
    private long healthyCount;
    private long unhealthyCount;
    
    public DataSourceStatistics() {
    }
    
    public DataSourceStatistics(long totalCount, long activeCount, long inactiveCount, 
                               Map<DataSourceConfig.DataSourceType, Long> countByType,
                               long healthyCount, long unhealthyCount) {
        this.totalCount = totalCount;
        this.activeCount = activeCount;
        this.inactiveCount = inactiveCount;
        this.countByType = countByType;
        this.healthyCount = healthyCount;
        this.unhealthyCount = unhealthyCount;
    }
    
    // Getters and Setters
    public long getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
    
    public long getActiveCount() {
        return activeCount;
    }
    
    public void setActiveCount(long activeCount) {
        this.activeCount = activeCount;
    }
    
    public long getInactiveCount() {
        return inactiveCount;
    }
    
    public void setInactiveCount(long inactiveCount) {
        this.inactiveCount = inactiveCount;
    }
    
    public Map<DataSourceConfig.DataSourceType, Long> getCountByType() {
        return countByType;
    }
    
    public void setCountByType(Map<DataSourceConfig.DataSourceType, Long> countByType) {
        this.countByType = countByType;
    }
    
    public long getHealthyCount() {
        return healthyCount;
    }
    
    public void setHealthyCount(long healthyCount) {
        this.healthyCount = healthyCount;
    }
    
    public long getUnhealthyCount() {
        return unhealthyCount;
    }
    
    public void setUnhealthyCount(long unhealthyCount) {
        this.unhealthyCount = unhealthyCount;
    }
    
    @Override
    public String toString() {
        return "DataSourceStatistics{" +
                "totalCount=" + totalCount +
                ", activeCount=" + activeCount +
                ", inactiveCount=" + inactiveCount +
                ", countByType=" + countByType +
                ", healthyCount=" + healthyCount +
                ", unhealthyCount=" + unhealthyCount +
                '}';
    }
}