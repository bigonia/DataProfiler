package com.dataprofiler.service;

import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.entity.DataSourceConfig;

import java.util.List;

/**
 * Service for transforming report data between different formats
 * Handles conversion from detailed reports to summary formats
 */
public interface ReportTransformService {

    /**
     * Transform detailed report to summary format as specified in design document
     * Converts from current format to the target format with datasourceId, dataSourceName, 
     * dataSourceType, databases structure
     * 
     * @param detailedReport The detailed report to transform
     * @param dataSourceName The data source name to include in summary
     * @return Transformed summary report in target format
     */
    ReportSummaryDto transformToTargetFormat(StructuredReportDto detailedReport, String dataSourceName);

    /**
     * Transform list of detailed reports to target summary format
     * 
     * @param detailedReports List of detailed reports to transform
     * @return List of transformed summary reports in target format
     */
    List<ReportSummaryDto> transformToTargetFormat(List<StructuredReportDto> detailedReports);

    /**
     * Extract column names from table report for summary
     * 
     * @param tableReport The table report to extract column names from
     * @return List of column names
     */
    List<String> extractColumnNames(StructuredReportDto.TableReport tableReport);

    /**
     * Extract sample rows in target format (header-rows structure)
     * 
     * @param tableReport The table report to extract sample rows from
     * @param maxSampleRows Maximum number of sample rows to include
     * @return Sample rows in target format with headers and rows arrays
     */
    Object extractSampleRows(StructuredReportDto.TableReport tableReport, int maxSampleRows);
}