package com.dataprofiler.service.impl;

import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.service.AIContextService;
import com.dataprofiler.service.StructuredReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of AIContextService
 * Converts structured profiling reports to LLM-optimized Markdown format
 * Provides intelligent compression and formatting for AI processing
 */
@Service
public class AIContextServiceImpl implements AIContextService {

    private static final Logger logger = LoggerFactory.getLogger(AIContextServiceImpl.class);
    
    private static final int DEFAULT_MAX_LENGTH = 8000;
    private static final int MAX_SAMPLE_ROWS = 3;
    private static final int MAX_COLUMNS_PER_TABLE = 10;
    
    @Autowired
    private StructuredReportService structuredReportService;

    @Override
    public String buildContextForLLM(String taskId) {
        return buildContextForLLM(taskId, DEFAULT_MAX_LENGTH);
    }

    @Override
    public String buildContextForLLM(String taskId, int maxLength) {
        try {
            logger.info("Building AI context for task: {}, maxLength: {}", taskId, maxLength);
            
            DetailedReportRequest request = new DetailedReportRequest();
            request.setTaskId(taskId);
            
            List<StructuredReportDto> reports = structuredReportService.getAllDetailedReports(request);
            if (reports == null || reports.isEmpty()) {
                throw new RuntimeException("No reports found for task: " + taskId);
            }
            
            // Use the first report for context building
            StructuredReportDto report = reports.get(0);
            
            String context = convertToMarkdown(report);
            
            // Apply length compression if needed
            if (context.length() > maxLength) {
                context = compressContext(context, maxLength);
            }
            
            logger.info("AI context built successfully for task: {}, final length: {}", taskId, context.length());
            return context;
            
        } catch (Exception e) {
            logger.error("Failed to build AI context for task: {}", taskId, e);
            throw new RuntimeException("Failed to build AI context: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean canBuildContext(String taskId) {
        try {
            DetailedReportRequest request = new DetailedReportRequest();
            request.setTaskId(taskId);
            
            List<StructuredReportDto> reports = structuredReportService.getAllDetailedReports(request);
            return reports != null && !reports.isEmpty() && reports.get(0).getTables() != null && !reports.get(0).getTables().isEmpty();
            
        } catch (Exception e) {
            logger.warn("Cannot build context for task: {}, error: {}", taskId, e.getMessage());
            return false;
        }
    }

    @Override
    public int getEstimatedContextLength(String taskId) {
        try {
            DetailedReportRequest request = new DetailedReportRequest();
            request.setTaskId(taskId);
            
            List<StructuredReportDto> reports = structuredReportService.getAllDetailedReports(request);
            if (reports == null || reports.isEmpty()) {
                return -1;
            }
            
            StructuredReportDto report = reports.get(0);
            
            // Rough estimation based on report structure
            int baseLength = 500; // Header and metadata
            int tablesCount = report.getTables() != null ? report.getTables().size() : 0;
            int estimatedTableLength = tablesCount * 300; // Average per table
            
            return baseLength + estimatedTableLength;
            
        } catch (Exception e) {
            logger.warn("Cannot estimate context length for task: {}", taskId, e);
            return -1;
        }
    }

    /**
     * Convert StructuredReportDto to Markdown format
     */
    private String convertToMarkdown(StructuredReportDto report) {
        StringBuilder md = new StringBuilder();
        
        // Header
        md.append("# Database Profiling Report\n\n");
        
        // Basic information
        md.append("## Basic Information\n\n");
        md.append("- **Task ID**: ").append(report.getTaskId()).append("\n");
        md.append("- **Data Source ID**: ").append(report.getDataSourceId()).append("\n");
        md.append("- **Data Source Type**: ").append(report.getDataSourceType()).append("\n");
        if (report.getDatabase() != null && report.getDatabase().getName() != null) {
            md.append("- **Database**: ").append(report.getDatabase().getName()).append("\n");
        }
        
        if (report.getGeneratedAt() != null) {
            md.append("- **Generated At**: ")
              .append(report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
              .append("\n");
        }
        
        md.append("\n");
        
        // Summary - calculate from tables data
        if (report.getTables() != null && !report.getTables().isEmpty()) {
            md.append("## Summary\n\n");
            int totalTables = report.getTables().size();
            int totalColumns = report.getTables().stream()
                .mapToInt(table -> table.getColumns() != null ? table.getColumns().size() : 0)
                .sum();
            long totalRows = report.getTables().stream()
                .mapToLong(table -> table.getRowCount() != null ? table.getRowCount() : 0L)
                .sum();
            
            md.append("- **Total Tables**: ").append(totalTables).append("\n");
            md.append("- **Total Columns**: ").append(totalColumns).append("\n");
            md.append("- **Total Rows**: ").append(totalRows).append("\n\n");
        }
        
        // Tables
        if (report.getTables() != null && !report.getTables().isEmpty()) {
            md.append("## Tables\n\n");
            
            for (StructuredReportDto.TableReport table : report.getTables()) {
                md.append("### ").append(table.getName());
                if (table.getSchemaName() != null && !table.getSchemaName().isEmpty()) {
                    md.append(" (").append(table.getSchemaName()).append(")");
                }
                md.append("\n\n");
                
                if (table.getComment() != null && !table.getComment().isEmpty()) {
                    md.append("**Description**: ").append(table.getComment()).append("\n\n");
                }
                
                md.append("**Row Count**: ").append(table.getRowCount()).append("\n\n");
                
                // Columns
                if (table.getColumns() != null && !table.getColumns().isEmpty()) {
                    md.append("#### Columns\n\n");
                    md.append("| Column | Type | Primary Key | Comment | Null Count | Distinct Count |\n");
                    md.append("|--------|------|-------------|---------|------------|----------------|\n");
                    
                    List<StructuredReportDto.ColumnReport> columns = table.getColumns();
                    // Limit columns to avoid excessive length
                    if (columns.size() > MAX_COLUMNS_PER_TABLE) {
                        columns = columns.subList(0, MAX_COLUMNS_PER_TABLE);
                    }
                    
                    for (StructuredReportDto.ColumnReport column : columns) {
                        md.append("| ").append(column.getName())
                          .append(" | ").append(column.getType())
                          .append(" | ").append(column.getIsPrimaryKey() ? "Yes" : "No")
                          .append(" | ").append(column.getComment() != null ? column.getComment() : "")
                          .append(" | ").append(column.getMetrics() != null ? column.getMetrics().getNullCount() : "N/A")
                          .append(" | ").append(column.getMetrics() != null ? column.getMetrics().getDistinctCount() : "N/A")
                          .append(" |\n");
                    }
                    
                    if (table.getColumns().size() > MAX_COLUMNS_PER_TABLE) {
                        md.append("\n*... and ").append(table.getColumns().size() - MAX_COLUMNS_PER_TABLE)
                          .append(" more columns*\n");
                    }
                    
                    md.append("\n");
                }
                
                // Sample rows
                if (table.getSampleRows() != null && !table.getSampleRows().isEmpty()) {
                    md.append("#### Sample Data\n\n");
                    
                    List<List<Object>> sampleRows = table.getSampleRows();
                    if (sampleRows.size() > MAX_SAMPLE_ROWS) {
                        sampleRows = sampleRows.subList(0, MAX_SAMPLE_ROWS);
                    }
                    
                    if (!sampleRows.isEmpty() && table.getColumns() != null && !table.getColumns().isEmpty()) {
                        // Header
                        md.append("| ");
                        List<StructuredReportDto.ColumnReport> displayColumns = table.getColumns();
                        if (displayColumns.size() > MAX_COLUMNS_PER_TABLE) {
                            displayColumns = displayColumns.subList(0, MAX_COLUMNS_PER_TABLE);
                        }
                        
                        for (StructuredReportDto.ColumnReport column : displayColumns) {
                            md.append(column.getName()).append(" | ");
                        }
                        md.append("\n");
                        
                        // Separator
                        md.append("|");
                        for (int i = 0; i < displayColumns.size(); i++) {
                            md.append("--------|");
                        }
                        md.append("\n");
                        
                        // Data rows
                        for (List<Object> row : sampleRows) {
                            md.append("| ");
                            for (int i = 0; i < Math.min(row.size(), displayColumns.size()); i++) {
                                Object value = row.get(i);
                                String displayValue = value != null ? value.toString() : "NULL";
                                // Truncate long values
                                if (displayValue.length() > 50) {
                                    displayValue = displayValue.substring(0, 47) + "...";
                                }
                                md.append(displayValue).append(" | ");
                            }
                            md.append("\n");
                        }
                        
                        if (table.getSampleRows().size() > MAX_SAMPLE_ROWS) {
                            md.append("\n*... and ").append(table.getSampleRows().size() - MAX_SAMPLE_ROWS)
                              .append(" more rows*\n");
                        }
                    }
                    
                    md.append("\n");
                }
                
                md.append("---\n\n");
            }
        }
        
        return md.toString();
    }

    /**
     * Compress context to fit within length limit
     */
    private String compressContext(String context, int maxLength) {
        if (context.length() <= maxLength) {
            return context;
        }
        
        logger.info("Compressing context from {} to {} characters", context.length(), maxLength);
        
        // Simple truncation with ellipsis
        // More sophisticated compression could be implemented here
        String compressed = context.substring(0, maxLength - 100) + "\n\n*... (content truncated for length limit) ...*";
        
        return compressed;
    }

}