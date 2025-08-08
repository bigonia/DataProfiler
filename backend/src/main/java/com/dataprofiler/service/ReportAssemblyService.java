package com.dataprofiler.service;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.response.StructuredReportDto;

import java.util.List;

/**
 * Service interface for assembling raw profiling data into structured reports
 * This service acts as the data transformation layer in the profiling pipeline
 * 
 * Core responsibility: Transform raw profiling data into enriched, structured reports
 * with calculated metrics, derived indicators, and standardized format
 */
public interface ReportAssemblyService {

    /**
     * Assemble one or more raw profiling data objects into standardized report objects
     * This is the core method that transforms raw data into enriched structured reports
     * 
     * @param rawDataList List of raw profiling data from Profiler, each element corresponds to one data source
     * @param taskId Global ID of the current profiling task
     * @return List of assembled structured reports, each element corresponds to one input data source
     */
    List<StructuredReportDto> assembleReport(List<RawProfileDataDto> rawDataList, String taskId);

}