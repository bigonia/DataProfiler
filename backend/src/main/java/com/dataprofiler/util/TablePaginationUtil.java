package com.dataprofiler.util;

import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.service.impl.ReportTransformServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for table-based pagination
 * Implements pagination logic based on tables while maintaining data source structure
 * Supports cross-datasource pagination with proper data source grouping
 */
@Slf4j
public class TablePaginationUtil {

    /**
     * Paginate summary reports based on tables
     * Maintains data source structure while paginating at table level
     * 
     * @param summaries list of summary reports
     * @param page page number (0-based)
     * @param pageSize number of tables per page
     * @return paginated summary reports
     */
    public static Page<ReportSummaryDto> paginateSummaryReports(
            List<ReportSummaryDto> summaries, int page, int pageSize) {
        
        if (summaries == null || summaries.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), 0);
        }

        // Extract all tables with their data source information
        List<TableWithDataSource> allTables = extractTablesFromSummaries(summaries);
        
        // Calculate pagination
        int totalTables = allTables.size();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalTables);
        
        if (startIndex >= totalTables) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), totalTables);
        }
        
        // Get tables for current page
        List<TableWithDataSource> pageTables = allTables.subList(startIndex, endIndex);
        
        // Group tables back by data source
        Map<String, List<TableWithDataSource>> tablesByDataSource = pageTables.stream()
                .collect(Collectors.groupingBy(TableWithDataSource::getDataSourceId));
        
        // Reconstruct summary reports with paginated tables
        List<ReportSummaryDto> paginatedSummaries = reconstructSummaryReports(
                summaries, tablesByDataSource);
        
        return new PageImpl<>(paginatedSummaries, PageRequest.of(page, pageSize), totalTables);
    }

    /**
     * Paginate detailed reports based on tables
     * Maintains data source structure while paginating at table level
     * 
     * @param reports list of detailed reports
     * @param page page number (0-based)
     * @param pageSize number of tables per page
     * @return paginated detailed reports
     */
    public static Page<StructuredReportDto> paginateDetailedReports(
            List<StructuredReportDto> reports, int page, int pageSize) {
        
        if (reports == null || reports.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), 0);
        }

        // Extract all tables with their data source information
        List<TableWithDetailedDataSource> allTables = extractTablesFromDetailedReports(reports);
        
        // Calculate pagination
        int totalTables = allTables.size();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalTables);
        
        if (startIndex >= totalTables) {
            return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, pageSize), totalTables);
        }
        
        // Get tables for current page
        List<TableWithDetailedDataSource> pageTables = allTables.subList(startIndex, endIndex);
        
        // Group tables back by data source
        Map<String, List<TableWithDetailedDataSource>> tablesByDataSource = pageTables.stream()
                .collect(Collectors.groupingBy(TableWithDetailedDataSource::getDataSourceId));
        
        // Reconstruct detailed reports with paginated tables
        List<StructuredReportDto> paginatedReports = reconstructDetailedReports(
                reports, tablesByDataSource);
        
        return new PageImpl<>(paginatedReports, PageRequest.of(page, pageSize), totalTables);
    }

    /**
     * Extract all tables from summary reports with data source information
     * 
     * @param summaries list of summary reports
     * @return list of tables with data source information
     */
    private static List<TableWithDataSource> extractTablesFromSummaries(
            List<ReportSummaryDto> summaries) {
        
        List<TableWithDataSource> allTables = new ArrayList<>();
        
        for (ReportSummaryDto summary : summaries) {
            if (summary.getDatabases() != null) {
                for (ReportTransformServiceImpl.DatabaseSummary database : summary.getDatabases()) {
                    if (database.getTables() != null) {
                        for (ReportTransformServiceImpl.TableSummary table : database.getTables()) {
                            allTables.add(new TableWithDataSource(
                                    summary.getDataSourceId(),
                                    summary,
                                    database,
                                    table
                            ));
                        }
                    }
                }
            }
        }
        
        return allTables;
    }

    /**
     * Extract all tables from detailed reports with data source information
     * 
     * @param reports list of detailed reports
     * @return list of tables with data source information
     */
    private static List<TableWithDetailedDataSource> extractTablesFromDetailedReports(
            List<StructuredReportDto> reports) {
        
        List<TableWithDetailedDataSource> allTables = new ArrayList<>();
        
        for (StructuredReportDto report : reports) {
            if (report.getTables() != null) {
                for (StructuredReportDto.TableReport table : report.getTables()) {
                    allTables.add(new TableWithDetailedDataSource(
                            report.getDataSourceId(),
                            report,
                            table
                    ));
                }
            }
        }
        
        return allTables;
    }

    /**
     * Reconstruct summary reports with paginated tables
     * 
     * @param originalSummaries original summary reports
     * @param tablesByDataSource tables grouped by data source
     * @return reconstructed summary reports
     */
    private static List<ReportSummaryDto> reconstructSummaryReports(
            List<ReportSummaryDto> originalSummaries,
            Map<String, List<TableWithDataSource>> tablesByDataSource) {
        
        List<ReportSummaryDto> result = new ArrayList<>();
        
        // Maintain original order of data sources
        for (ReportSummaryDto originalSummary : originalSummaries) {
            String dataSourceId = originalSummary.getDataSourceId();
            
            if (tablesByDataSource.containsKey(dataSourceId)) {
                List<TableWithDataSource> tables = tablesByDataSource.get(dataSourceId);
                
                // Create new summary with filtered tables
                ReportSummaryDto newSummary = createSummaryWithFilteredTables(
                        originalSummary, tables);
                
                if (newSummary != null && hasNonEmptyDatabases(newSummary)) {
                    result.add(newSummary);
                }
            }
        }
        
        return result;
    }

    /**
     * Reconstruct detailed reports with paginated tables
     * 
     * @param originalReports original detailed reports
     * @param tablesByDataSource tables grouped by data source
     * @return reconstructed detailed reports
     */
    private static List<StructuredReportDto> reconstructDetailedReports(
            List<StructuredReportDto> originalReports,
            Map<String, List<TableWithDetailedDataSource>> tablesByDataSource) {
        
        List<StructuredReportDto> result = new ArrayList<>();
        
        // Maintain original order of data sources
        for (StructuredReportDto originalReport : originalReports) {
            String dataSourceId = originalReport.getDataSourceId();
            
            if (tablesByDataSource.containsKey(dataSourceId)) {
                List<TableWithDetailedDataSource> tables = tablesByDataSource.get(dataSourceId);
                
                // Create new report with filtered tables
                StructuredReportDto newReport = createReportWithFilteredTables(
                        originalReport, tables);
                
                if (newReport != null && newReport.getTables() != null && !newReport.getTables().isEmpty()) {
                    result.add(newReport);
                }
            }
        }
        
        return result;
    }

    /**
     * Create summary report with filtered tables
     * 
     * @param originalSummary original summary
     * @param tables filtered tables
     * @return new summary with filtered tables
     */
    private static ReportSummaryDto createSummaryWithFilteredTables(
            ReportSummaryDto originalSummary, List<TableWithDataSource> tables) {
        
        ReportSummaryDto newSummary = new ReportSummaryDto();
        newSummary.setTaskId(originalSummary.getTaskId());
        newSummary.setDataSourceId(originalSummary.getDataSourceId());
        newSummary.setDataSourceName(originalSummary.getDataSourceName());
        newSummary.setDataSourceType(originalSummary.getDataSourceType());
        
        // Group tables by database
        Map<String, List<TableWithDataSource>> tablesByDatabase = tables.stream()
                .collect(Collectors.groupingBy(t -> 
                        t.getDatabase().getName() != null ? t.getDatabase().getName() : ""));
        
        List<ReportTransformServiceImpl.DatabaseSummary> databases = new ArrayList<>();
        
        for (Map.Entry<String, List<TableWithDataSource>> entry : tablesByDatabase.entrySet()) {
            String databaseName = entry.getKey();
            List<TableWithDataSource> databaseTables = entry.getValue();
            
            ReportTransformServiceImpl.DatabaseSummary database = new ReportTransformServiceImpl.DatabaseSummary();
            database.setName(databaseName);
            
            List<ReportTransformServiceImpl.TableSummary> tableSummaries = databaseTables.stream()
                    .map(TableWithDataSource::getTable)
                    .collect(Collectors.toList());
            
            database.setTables(tableSummaries);
            databases.add(database);
        }
        
        newSummary.setDatabases(databases);
        return newSummary;
    }

    /**
     * Create detailed report with filtered tables
     * 
     * @param originalReport original report
     * @param tables filtered tables
     * @return new report with filtered tables
     */
    private static StructuredReportDto createReportWithFilteredTables(
            StructuredReportDto originalReport, List<TableWithDetailedDataSource> tables) {
        
        StructuredReportDto newReport = new StructuredReportDto();
        newReport.setTaskId(originalReport.getTaskId());
        newReport.setDataSourceId(originalReport.getDataSourceId());
        newReport.setDataSourceType(originalReport.getDataSourceType());
        newReport.setDatabase(originalReport.getDatabase());
        newReport.setGeneratedAt(originalReport.getGeneratedAt());
        
        List<StructuredReportDto.TableReport> tableReports = tables.stream()
                .map(TableWithDetailedDataSource::getTable)
                .collect(Collectors.toList());
        
        newReport.setTables(tableReports);
        return newReport;
    }

    /**
     * Check if summary has non-empty databases
     * 
     * @param summary the summary to check
     * @return true if has non-empty databases
     */
    private static boolean hasNonEmptyDatabases(ReportSummaryDto summary) {
        return summary.getDatabases() != null && 
               summary.getDatabases().stream()
                       .anyMatch(db -> db.getTables() != null && !db.getTables().isEmpty());
    }

    /**
     * Helper class to hold table with data source information for summary reports
     */
    private static class TableWithDataSource {
        private final String dataSourceId;
        private final ReportSummaryDto dataSource;
        private final ReportTransformServiceImpl.DatabaseSummary database;
        private final ReportTransformServiceImpl.TableSummary table;

        public TableWithDataSource(String dataSourceId, ReportSummaryDto dataSource,
                                 ReportTransformServiceImpl.DatabaseSummary database,
                                 ReportTransformServiceImpl.TableSummary table) {
            this.dataSourceId = dataSourceId;
            this.dataSource = dataSource;
            this.database = database;
            this.table = table;
        }

        public String getDataSourceId() { return dataSourceId; }
        public ReportSummaryDto getDataSource() { return dataSource; }
        public ReportTransformServiceImpl.DatabaseSummary getDatabase() { return database; }
        public ReportTransformServiceImpl.TableSummary getTable() { return table; }
    }

    /**
     * Helper class to hold table with data source information for detailed reports
     */
    private static class TableWithDetailedDataSource {
        private final String dataSourceId;
        private final StructuredReportDto dataSource;
        private final StructuredReportDto.TableReport table;

        public TableWithDetailedDataSource(String dataSourceId, StructuredReportDto dataSource,
                                         StructuredReportDto.TableReport table) {
            this.dataSourceId = dataSourceId;
            this.dataSource = dataSource;
            this.table = table;
        }

        public String getDataSourceId() { return dataSourceId; }
        public StructuredReportDto getDataSource() { return dataSource; }
        public StructuredReportDto.TableReport getTable() { return table; }
    }
}