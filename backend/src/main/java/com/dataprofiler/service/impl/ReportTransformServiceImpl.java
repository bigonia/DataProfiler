package com.dataprofiler.service.impl;

import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.service.ReportTransformService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of ReportTransformService
 * Transforms detailed reports to target summary format as specified in design document
 */
@Service
public class ReportTransformServiceImpl implements ReportTransformService {

    private static final Logger logger = LoggerFactory.getLogger(ReportTransformServiceImpl.class);
    private static final int DEFAULT_MAX_SAMPLE_ROWS = 5;

    @Override
    public ReportSummaryDto transformToTargetFormat(StructuredReportDto detailedReport, String dataSourceName) {
        if (detailedReport == null) {
            return null;
        }

        try {
            ReportSummaryDto summary = new ReportSummaryDto();
            
            // Set basic data source information
            summary.setTaskId(detailedReport.getTaskId());
            summary.setDataSourceId(detailedReport.getDataSourceId());
            summary.setDataSourceName(dataSourceName);
            summary.setDataSourceType(detailedReport.getDataSourceType());
            
            // Transform database structure to target format
            if (detailedReport.getDatabase() != null) {
                // Create databases array structure as per target format
                List<DatabaseSummary> databases = new ArrayList<>();
                DatabaseSummary dbSummary = new DatabaseSummary();
                dbSummary.setName(detailedReport.getDatabase().getName());
                
                // Transform tables to target format
                if (detailedReport.getTables() != null && !detailedReport.getTables().isEmpty()) {
                    List<TableSummary> tables = detailedReport.getTables().stream()
                            .map(this::transformTableToSummary)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    dbSummary.setTables(tables);
                }
                
                databases.add(dbSummary);
                summary.setDatabases(databases);
            }
            
            return summary;
            
        } catch (Exception e) {
            logger.error("Error transforming detailed report to target format for dataSource: {}", 
                    detailedReport.getDataSourceId(), e);
            return null;
        }
    }

    @Override
    public List<ReportSummaryDto> transformToTargetFormat(List<StructuredReportDto> detailedReports) {
        if (detailedReports == null || detailedReports.isEmpty()) {
            return new ArrayList<>();
        }

        return detailedReports.stream()
                .map(report -> transformToTargetFormat(report, null)) // dataSourceName will be resolved later
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> extractColumnNames(StructuredReportDto.TableReport tableReport) {
        if (tableReport == null || tableReport.getColumns() == null) {
            return new ArrayList<>();
        }

        return tableReport.getColumns().stream()
                .map(StructuredReportDto.ColumnReport::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public Object extractSampleRows(StructuredReportDto.TableReport tableReport, int maxSampleRows) {
        if (tableReport == null || tableReport.getSampleRows() == null) {
            return new ArrayList<>();
        }

        try {
            // Check if sampleRows is in header-rows format
            if (tableReport.getSampleRows() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sampleRowsMap = (Map<String, Object>) tableReport.getSampleRows();
                
                if (sampleRowsMap.containsKey("headers") && sampleRowsMap.containsKey("rows")) {
                    // Convert from header-rows format to simple array format for target
                    @SuppressWarnings("unchecked")
                    List<List<Object>> rows = (List<List<Object>>) sampleRowsMap.get("rows");
                    
                    if (rows != null) {
                        // Limit the number of rows if needed
                        if (rows.size() > maxSampleRows) {
                            rows = rows.subList(0, maxSampleRows);
                        }
                        return rows;
                    }
                }
            }
            
            // If sampleRows is already a list, use it directly
            if (tableReport.getSampleRows() instanceof List) {
                @SuppressWarnings("unchecked")
                List<List<Object>> rows = (List<List<Object>>) tableReport.getSampleRows();
                if (rows.size() > maxSampleRows) {
                    rows = rows.subList(0, maxSampleRows);
                }
                return rows;
            }
            
            // Fallback: return empty array
            logger.warn("Sample rows not in expected format for table: {}", tableReport.getName());
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Error extracting sample rows for table: {}", tableReport.getName(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Transform table report to summary format
     */
    private TableSummary transformTableToSummary(StructuredReportDto.TableReport tableReport) {
        if (tableReport == null) {
            return null;
        }

        try {
            TableSummary tableSummary = new TableSummary();
            tableSummary.setName(tableReport.getName());
            tableSummary.setSchemaName(tableReport.getSchemaName());
            tableSummary.setRowCount(tableReport.getRowCount());
            tableSummary.setComment(tableReport.getComment());
            
            // Extract column names
            List<String> columnNames = extractColumnNames(tableReport);
            tableSummary.setColumns(columnNames);
            
            // Extract sample rows in target format
            Object sampleRows = extractSampleRows(tableReport, DEFAULT_MAX_SAMPLE_ROWS);
            tableSummary.setSampleRows(sampleRows);
            
            return tableSummary;
            
        } catch (Exception e) {
            logger.error("Error transforming table to summary: {}", tableReport.getName(), e);
            return null;
        }
    }

    /**
     * Inner class for database summary structure
     */
    public static class DatabaseSummary {
        private String name;
        private List<TableSummary> tables;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<TableSummary> getTables() { return tables; }
        public void setTables(List<TableSummary> tables) { this.tables = tables; }
    }

    /**
     * Inner class for table summary structure
     */
    public static class TableSummary {
        private String name;
        private String schemaName;
        private Long rowCount;
        private List<String> columns;
        private Object sampleRows;
        private String comment;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getSchemaName() { return schemaName; }
        public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
        public Long getRowCount() { return rowCount; }
        public void setRowCount(Long rowCount) { this.rowCount = rowCount; }
        public List<String> getColumns() { return columns; }
        public void setColumns(List<String> columns) { this.columns = columns; }
        public Object getSampleRows() { return sampleRows; }
        public void setSampleRows(Object sampleRows) { this.sampleRows = sampleRows; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}