package com.dataprofiler.service.impl;

import com.dataprofiler.dto.internal.RawProfileDataDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.service.ReportAssemblyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ReportAssemblyService interface
 * Core data transformation service that converts raw profiling data into structured reports
 * 
 * Key responsibilities:
 * - Transform raw profiling data into enriched structured reports
 * - Calculate derived metrics and advanced statistics
 * - Standardize report format and structure
 * - Aggregate multi-source profiling results
 */
@Service
public class ReportAssemblyServiceImpl implements ReportAssemblyService {

    private static final Logger logger = LoggerFactory.getLogger(ReportAssemblyServiceImpl.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<StructuredReportDto> assembleReport(List<RawProfileDataDto> rawDataList, String taskId) {
        logger.info("Starting report assembly for task: {} with {} raw data sources", 
                taskId, rawDataList != null ? rawDataList.size() : 0);

        if (rawDataList == null || rawDataList.isEmpty()) {
            logger.warn("No raw data provided for report assembly, task: {}", taskId);
            return Collections.emptyList();
        }

        List<StructuredReportDto> assembledReports = new ArrayList<>();

        try {
            // Process each raw data source independently
            for (int i = 0; i < rawDataList.size(); i++) {
                RawProfileDataDto rawData = rawDataList.get(i);
                
                logger.debug("Processing raw data source {} of {} for task: {}", 
                        i + 1, rawDataList.size(), taskId);
                
                StructuredReportDto report = assembleReportForSingleSource(rawData, taskId);
                if (report != null) {
                    assembledReports.add(report);
                } else {
                    logger.warn("Failed to assemble report for data source {} in task: {}", i, taskId);
                }
            }

            logger.info("Report assembly completed for task: {}. Generated {} reports from {} raw sources", 
                    taskId, assembledReports.size(), rawDataList.size());

        } catch (Exception e) {
            logger.error("Error during report assembly for task: {}", taskId, e);
            throw new RuntimeException("Failed to assemble reports: " + e.getMessage(), e);
        }

        return assembledReports;
    }

    /**
     * Assemble structured report for a single data source
     * This method implements the core transformation logic:
     * 1. Map basic information from raw data
     * 2. Process database information
     * 3. Transform table profiling results
     * 4. Calculate derived metrics and statistics
     * 5. Standardize report structure
     */
    private StructuredReportDto assembleReportForSingleSource(RawProfileDataDto rawData, String taskId) {
        try {
            logger.debug("Assembling report for data source: {}", rawData.getDataSourceId());

            StructuredReportDto report = new StructuredReportDto();

            // 1. Map basic information
            report.setTaskId(taskId);
            report.setDataSourceId(rawData.getDataSourceId());
            report.setDataSourceType(rawData.getDataSourceType());

            // 2. Process database information
            if (rawData.getDatabaseName() != null) {
                StructuredReportDto.DatabaseInfo databaseInfo = new StructuredReportDto.DatabaseInfo();
                databaseInfo.setName(rawData.getDatabaseName());
                report.setDatabase(databaseInfo);
            }

            // 3. Process table data
            if (rawData.getTables() != null && !rawData.getTables().isEmpty()) {
                report.setTables(processTableData(rawData.getTables()));
            }

            logger.debug("Successfully assembled report for data source: {}", rawData.getDataSourceId());
            return report;

        } catch (Exception e) {
            logger.error("Failed to assemble report for data source: {}", 
                    rawData != null ? rawData.getDataSourceId() : "unknown", e);
            return null;
        }
    }

    /**
     * Process table data list from raw profiling data
     */
    private List<StructuredReportDto.TableReport> processTableData(List<RawProfileDataDto.TableData> tableDataList) {
        return tableDataList.stream()
                .map(this::processTableData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Process single table data
     */
    private StructuredReportDto.TableReport processTableData(RawProfileDataDto.TableData tableData) {
        StructuredReportDto.TableReport tableReport = new StructuredReportDto.TableReport();
        
        tableReport.setName(tableData.getTableName());
        tableReport.setSchemaName(tableData.getSchemaName());
        tableReport.setRowCount(tableData.getRowCount());

        tableReport.setUseSample(tableData.isUseSample());

        // Process column data if available
        if (tableData.getColumns() != null && !tableData.getColumns().isEmpty()) {
            tableReport.setColumns(processColumnData(tableData.getColumns()));
            
            // Build sample rows from column sample values
            List<List<Object>> sampleRows = buildSampleRowsFromColumns(tableData.getColumns());
            tableReport.setSampleRows(sampleRows);
        }
        
        return tableReport;
    }

    /**
     * Process column data list from table data
     */
    private List<StructuredReportDto.ColumnReport> processColumnData(List<RawProfileDataDto.ColumnData> columnDataList) {
        return columnDataList.stream()
                .map(this::processColumnData)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Process single column data
     */
    private StructuredReportDto.ColumnReport processColumnData(RawProfileDataDto.ColumnData columnData) {
        StructuredReportDto.ColumnReport columnReport = new StructuredReportDto.ColumnReport();
        
        columnReport.setName(columnData.getColumnName());
        columnReport.setType(columnData.getDataType());
        columnReport.setIsPrimaryKey(columnData.getIsPrimaryKey());
        
        // Process column metrics
        StructuredReportDto.ColumnMetrics metrics = new StructuredReportDto.ColumnMetrics();
        metrics.setNullCount(columnData.getNullCount());
        metrics.setDistinctCount(columnData.getUniqueCount());
        
        // Calculate derived metrics
        if (columnData.getTotalCount() != null && columnData.getTotalCount() > 0) {
            // Calculate null rate
            if (columnData.getNullCount() != null) {
                metrics.setNullRate((double) columnData.getNullCount() / columnData.getTotalCount());
            }
            
            // Calculate distinct rate
            if (columnData.getUniqueCount() != null) {
                metrics.setDistinctRate((double) columnData.getUniqueCount() / columnData.getTotalCount());
            }
        }
        
        // Set value range if available
        if (columnData.getMinValue() != null || columnData.getMaxValue() != null) {
            StructuredReportDto.ValueRange range = new StructuredReportDto.ValueRange();
            range.setMin(columnData.getMinValue());
            range.setMax(columnData.getMaxValue());
            metrics.setRange(range);
        }
        
        // Set string length metrics
        if (columnData.getMinLength() != null) {
            metrics.setMinLength(columnData.getMinLength().intValue());
        }
        if (columnData.getMaxLength() != null) {
            metrics.setMaxLength(columnData.getMaxLength().intValue());
        }
        
        columnReport.setMetrics(metrics);
        
        return columnReport;
    }
    
    /**
     * Build sample rows from column sample values
     * Creates a header-rows structure with column names as headers and sample values as rows
     */
    private List<List<Object>> buildSampleRowsFromColumns(List<RawProfileDataDto.ColumnData> columns) {
        if (columns == null || columns.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            // Extract column names (headers)
            List<String> headers = columns.stream()
                    .map(RawProfileDataDto.ColumnData::getColumnName)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            if (headers.isEmpty()) {
                return new ArrayList<>();
            }
            
            // Find the maximum number of sample values across all columns
            int maxSampleCount = columns.stream()
                    .mapToInt(col -> col.getSampleValues() != null ? col.getSampleValues().size() : 0)
                    .max()
                    .orElse(0);
            
            if (maxSampleCount == 0) {
                return new ArrayList<>();
            }
            
            // Build rows by combining sample values from each column
            List<List<Object>> rows = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < Math.min(maxSampleCount, 5); rowIndex++) {
                List<Object> row = new ArrayList<>();
                for (RawProfileDataDto.ColumnData column : columns) {
                    List<Object> sampleValues = column.getSampleValues();
                    if (sampleValues != null && rowIndex < sampleValues.size()) {
                        row.add(sampleValues.get(rowIndex));
                    } else {
                        row.add(null); // Fill missing values with null
                    }
                }
                rows.add(row);
            }
            
            // Return rows directly as List<List<Object>>
            return rows;
            
        } catch (Exception e) {
            logger.error("Error building sample rows from columns", e);
            return new ArrayList<>();
        }
    }


}