package com.dataprofiler.service.impl;

import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.dto.request.ReportSummaryRequest;
import com.dataprofiler.dto.response.ReportInfoDto;
import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.StructuredReport;
import com.dataprofiler.repository.DataSourceConfigRepository;
import com.dataprofiler.repository.StructuredReportRepository;
import com.dataprofiler.service.ReportTransformService;
import com.dataprofiler.service.StructuredReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of StructuredReportService interface
 * Core report persistence and retrieval service with performance optimizations
 * 
 * Key responsibilities:
 * - Persist structured reports with optimized storage
 * - Provide efficient report retrieval with filtering and pagination
 * - Generate lightweight report summaries for overview operations
 * - Manage report lifecycle and cleanup operations
 */
@Service
@Transactional
public class StructuredReportServiceImpl implements StructuredReportService {

    private static final Logger logger = LoggerFactory.getLogger(StructuredReportServiceImpl.class);

    @Autowired
    private StructuredReportRepository structuredReportRepository;

    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;

    @Autowired
    private ReportTransformService reportTransformService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void saveReports(String taskId, List<StructuredReportDto> reports) {
        logger.info("Starting to save {} structured reports for task: {}", reports != null ? reports.size() : 0, taskId);

        if (reports == null || reports.isEmpty()) {
            logger.warn("No reports provided for saving");
            return;
        }
        try {
            List<StructuredReport> entities = new ArrayList<>();

            for (StructuredReportDto reportDto : reports) {
                try {
                    StructuredReport entity = convertToEntity(reportDto);
                    if (entity != null) {
                        entities.add(entity);
                    }
                } catch (Exception e) {
                    logger.error("Failed to convert report DTO to entity for task: {}, dataSource: {}", 
                            reportDto.getTaskId(), reportDto.getDataSourceId(), e);
                    // Continue processing other reports
                }
            }

            if (!entities.isEmpty()) {
                // Batch save for performance optimization
                List<StructuredReport> savedReports = structuredReportRepository.saveAll(entities);
                logger.info("Successfully saved {} structured reports out of {} provided", 
                        savedReports.size(), reports.size());
            } else {
                logger.warn("No valid reports to save after conversion");
            }

        } catch (Exception e) {
            logger.error("Error during batch report saving", e);
            throw new RuntimeException("Failed to save structured reports: " + e.getMessage(), e);
        }
    }


    @Override
    @Transactional(readOnly = true)
    public List<ReportSummaryDto> getReportsSummary(ReportSummaryRequest request) {
        logger.debug("Retrieving reports summary for data source IDs: {}", request.getDataSourceIds());

        try {
            List<StructuredReport> reports = structuredReportRepository.findByTaskIdOrderByGeneratedAtDesc(request.getTaskId());

            return reports.stream()
                    .map(this::convertToSummaryDto)
                    .filter(summary -> summary != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.error("Error retrieving reports summary for data source IDs: {}", request.getDataSourceIds(), e);
            throw new RuntimeException("Failed to retrieve reports summary: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StructuredReportDto> getDetailedReport(DetailedReportRequest request) {
        logger.debug("Retrieving detailed reports with request: {}", request);

        try {
            // Create pageable with sorting
            Sort sort = Sort.by(Sort.Direction.DESC, "generatedAt");
            Pageable pageable = PageRequest.of(
                    request.getPage(),
                    request.getPageSize(),
                    sort
            );

            Page<StructuredReport> reportPage;

            // Query by data source IDs with pagination
            reportPage = structuredReportRepository.findByTaskId(request.getTaskId(), pageable);

            // Apply additional filtering for schemas and tables if needed
            List<StructuredReport> filteredReports = reportPage.getContent().stream()
                    .filter(report -> matchesFilterCriteria(report, request.getFilters()))
                    .collect(Collectors.toList());

            // Create new page with filtered results
            reportPage = new PageImpl<>(filteredReports, pageable, filteredReports.size());

            // Convert entities to DTOs
            Page<StructuredReportDto> resultPage = reportPage.map(this::convertToDto);

            logger.debug("Retrieved {} detailed reports (page {} of {})",
                    resultPage.getNumberOfElements(),
                    resultPage.getNumber() + 1,
                    resultPage.getTotalPages());

            return resultPage;

        } catch (Exception e) {
            logger.error("Error retrieving detailed reports with request: {}", request, e);
            throw new RuntimeException("Failed to retrieve detailed reports: " + e.getMessage(), e);
        }
    }

    /**
      * Check if a report matches the filter criteria
      */
    private boolean matchesFilterCriteria(StructuredReport report, DetailedReportRequest.FilterCriteria filters) {

        if(filters==null || filters.getDataSources()==null || filters.getDataSources().isEmpty()){
            return true;
        }

        try {
            // Convert entity to DTO to access structured data
            StructuredReportDto reportDto = convertToDto(report);
            if (reportDto == null || reportDto.getTables() == null) {
                return false;
            }
            
            // Get filter configuration for this data source
            DetailedReportRequest.DataSourceScope dataSourceScope = filters.getDataSources().get(report.getDataSourceId());
            if (dataSourceScope == null) {
                return false;
            }
            
            // If no specific schemas/tables are specified, include all tables from this data source
            if (dataSourceScope.getSchemas() == null || dataSourceScope.getSchemas().isEmpty()) {
                return true;
            }
            
            // Check if any table in the report matches the schema/table filters
            return reportDto.getTables().stream().anyMatch(table -> {
                String schemaName = table.getSchemaName();
                String tableName = table.getName();
                
                // Check if this table's schema is in the filter
                List<String> allowedTables = dataSourceScope.getSchemas().get(schemaName);
                if (allowedTables == null) {
                    return false;
                }
                
                // If no specific tables are specified for this schema, include all tables
                if (allowedTables.isEmpty()) {
                    return true;
                }
                
                // Check if this specific table is in the filter
                return allowedTables.contains(tableName);
            });
            
        } catch (Exception e) {
            logger.warn("Error checking filter criteria for report: {}", report.getTaskId(), e);
            return false;
        }
    }

    /**
     * Convert StructuredReportDto to StructuredReport entity
     */
    private StructuredReport convertToEntity(StructuredReportDto dto) {
        try {
            StructuredReport entity = new StructuredReport();
            
            // Basic fields
            entity.setTaskId(dto.getTaskId());
            entity.setDataSourceId(dto.getDataSourceId());
            entity.setGeneratedAt(dto.getGeneratedAt() != null ? dto.getGeneratedAt() : LocalDateTime.now());
            
            // Serialize complex objects to JSON for storage
            if (dto.getDatabase() != null) {
                entity.setDatabaseProfileJson(objectMapper.writeValueAsString(dto.getDatabase()));
            }
            
            if (dto.getTables() != null && !dto.getTables().isEmpty()) {
                entity.setTableProfilesJson(objectMapper.writeValueAsString(dto.getTables()));
            }
            
            // Extract summary fields for efficient querying from tables data
            if (dto.getTables() != null) {
                entity.setTotalTables(dto.getTables().size());
                
                int totalColumns = dto.getTables().stream()
                    .mapToInt(table -> table.getColumns() != null ? table.getColumns().size() : 0)
                    .sum();
                entity.setTotalColumns(totalColumns);
                
                Long totalRows = dto.getTables().stream()
                    .mapToLong(table -> table.getRowCount() != null ? table.getRowCount() : 0L)
                    .sum();
                entity.setEstimatedTotalRows(totalRows);
            }
            
            return entity;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize report DTO to JSON for task: {}, dataSource: {}", 
                    dto.getTaskId(), dto.getDataSourceId(), e);
            return null;
        }
    }

    /**
     * Convert StructuredReport entity to StructuredReportDto
     */
    private StructuredReportDto convertToDto(StructuredReport entity) {
        try {
            StructuredReportDto dto = new StructuredReportDto();
            
            // Basic fields
            dto.setTaskId(entity.getTaskId());
            dto.setDataSourceId(entity.getDataSourceId());
            dto.setGeneratedAt(entity.getGeneratedAt());

            // Get data source type from DataSourceConfig
            DataSourceConfig dataSourceConfig = dataSourceConfigRepository.findBySourceId(entity.getDataSourceId());
            if (dataSourceConfig != null) {
                dto.setDataSourceType(dataSourceConfig.getType());
            } else {
                logger.warn("DataSourceConfig not found for sourceId: {}", entity.getDataSourceId());
            }
            
            // Deserialize JSON fields to objects
            if (entity.getDatabaseProfileJson() != null) {
                dto.setDatabase(objectMapper.readValue(
                        entity.getDatabaseProfileJson(), 
                        StructuredReportDto.DatabaseInfo.class));
            }
            
            if (entity.getTableProfilesJson() != null) {
                dto.setTables(objectMapper.readValue(
                        entity.getTableProfilesJson(), 
                        objectMapper.getTypeFactory().constructCollectionType(
                                List.class, StructuredReportDto.TableReport.class)));
            }
            
            return dto;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize report entity JSON for task: {}, dataSource: {}", 
                    entity.getTaskId(), entity.getDataSourceId(), e);
            return null;
        }
    }

    /**
     * Convert StructuredReport entity to lightweight ReportSummaryDto
     * Uses ReportTransformService to generate target format as specified in design document
     */
    private ReportSummaryDto convertToSummaryDto(StructuredReport entity) {
        try {
            // Parse the JSON data to get database and table information
            StructuredReportDto fullReport = convertToDto(entity);
            if (fullReport == null) {
                return null;
            }

            // Get data source name from repository
            String dataSourceName = null;
            try {
                DataSourceConfig dataSourceConfig = dataSourceConfigRepository.findBySourceId(entity.getDataSourceId());
                if (dataSourceConfig != null) {
                    dataSourceName = dataSourceConfig.getName();
                }
            } catch (Exception e) {
                logger.warn("Could not retrieve data source name for ID: {}", entity.getDataSourceId(), e);
            }

            // Use transform service to convert to target format
            ReportSummaryDto summary = reportTransformService.transformToTargetFormat(fullReport, dataSourceName);
            if (summary == null) {
                return null;
            }
            
            // Set summary statistics from denormalized fields
            summary.setTotalTables(entity.getTotalTables());
            summary.setTotalColumns(entity.getTotalColumns());
            summary.setEstimatedTotalRows(entity.getEstimatedTotalRows());
            summary.setEstimatedTotalSizeBytes(entity.getEstimatedTotalSizeBytes());
            
            // Calculate data size in human-readable format
            if (entity.getEstimatedTotalSizeBytes() != null) {
                summary.setFormattedDataSize(formatBytes(entity.getEstimatedTotalSizeBytes()));
            }
            
            return summary;
            
        } catch (Exception e) {
            logger.error("Failed to convert entity to summary DTO for task: {}, dataSource: {}", 
                    entity.getTaskId(), entity.getDataSourceId(), e);
            return null;
        }
    }

    /**
     * Format bytes to human-readable string
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes < 0) {
            return "0 B";
        }
        
        String[] units = {"B", "KB", "MB", "GB", "TB", "PB"};
        int unitIndex = 0;
        double size = bytes.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * Format duration seconds to human-readable string
     */
    private String formatDuration(Long seconds) {
        if (seconds == null || seconds < 0) {
            return "0s";
        }
        
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            long hours = seconds / 3600;
            long remainingMinutes = (seconds % 3600) / 60;
            long remainingSeconds = seconds % 60;
            return hours + "h " + remainingMinutes + "m " + remainingSeconds + "s";
        }
    }

    /**
     * Delete reports by task ID (for cleanup operations)
     */
    @Transactional
    public void deleteReportsByTaskId(String taskId) {
        logger.info("Deleting reports for task: {}", taskId);
        
        try {
            structuredReportRepository.deleteByTaskId(taskId);
            logger.info("Successfully deleted reports for task: {}", taskId);
        } catch (Exception e) {
            logger.error("Error deleting reports for task: {}", taskId, e);
            throw new RuntimeException("Failed to delete reports: " + e.getMessage(), e);
        }
    }


    /**
     * Get report count by task ID
     */
    @Transactional(readOnly = true)
    public long getReportCountByTaskId(String taskId) {
        try {
            return structuredReportRepository.countByTaskId(taskId);
        } catch (Exception e) {
            logger.error("Error counting reports for task: {}", taskId, e);
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructuredReportDto> getAllDetailedReports(DetailedReportRequest request) {
        logger.debug("Retrieving all detailed reports with request: {}", request);

        try {
            // Create sort for consistent ordering
            Sort sort = Sort.by(Sort.Direction.DESC, "generatedAt");

            // Query all reports by task ID without pagination
            List<StructuredReport> reports = structuredReportRepository.findByTaskIdOrderByGeneratedAtDesc(request.getTaskId());

            // Apply filtering for schemas and tables if needed
            List<StructuredReport> filteredReports = reports.stream()
                    .filter(report -> matchesFilterCriteria(report, request.getFilters()))
                    .collect(Collectors.toList());

            // Convert entities to DTOs
            List<StructuredReportDto> resultList = filteredReports.stream()
                    .map(this::convertToDto)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} detailed reports for table-based pagination", resultList.size());

            return resultList;

        } catch (Exception e) {
            logger.error("Error retrieving all detailed reports with request: {}", request, e);
            throw new RuntimeException("Failed to retrieve all detailed reports: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReportInfoDto> getReportInfoList(Integer page, Integer size) {
        logger.debug("Retrieving report info list with page: {}, size: {}", page, size);

        try {
            // Create pageable with sorting by generation time (descending)
            Sort sort = Sort.by(Sort.Direction.DESC, "generatedAt");
            Pageable pageable = PageRequest.of(
                    page != null ? page : 0,
                    size != null ? size : 10,
                    sort
            );

            // Get all reports with pagination
            Page<StructuredReport> reportPage = structuredReportRepository.findAll(pageable);

            // Convert to ReportInfoDto page
            Page<ReportInfoDto> reportInfoPage = reportPage.map(this::convertToReportInfoDto);

            logger.debug("Retrieved {} report info records (page {} of {})",
                    reportInfoPage.getNumberOfElements(),
                    reportInfoPage.getNumber() + 1,
                    reportInfoPage.getTotalPages());

            return reportInfoPage;

        } catch (Exception e) {
            logger.error("Error retrieving report info list with page: {}, size: {}", page, size, e);
            throw new RuntimeException("Failed to retrieve report info list: " + e.getMessage(), e);
        }
    }

    /**
     * Convert StructuredReport entity to ReportInfoDto
     */
    private ReportInfoDto convertToReportInfoDto(StructuredReport entity) {
        try {
            ReportInfoDto dto = new ReportInfoDto();
            dto.setId(entity.getId());
            dto.setTaskId(entity.getTaskId());
            dto.setDataSourceId(entity.getDataSourceId());
            
            // Get data source name from DataSourceConfig
            try {
                DataSourceConfig dataSourceConfig = dataSourceConfigRepository.findBySourceId(entity.getDataSourceId());
                if (dataSourceConfig != null) {
                    dto.setDataSourceName(dataSourceConfig.getName());
                    dto.setDataSourceType(dataSourceConfig.getType());
                } else {
                    dto.setDataSourceName("Unknown");
                    dto.setDataSourceType(null); // Set to null for unknown type
                }
            } catch (Exception e) {
                logger.warn("Failed to get data source info for sourceId: {}", entity.getDataSourceId(), e);
                dto.setDataSourceName("Unknown");
                dto.setDataSourceType(null); // Set to null for unknown type
            }
            
            dto.setGeneratedAt(entity.getGeneratedAt());
            dto.setTotalTables(entity.getTotalTables());
            dto.setTotalColumns(entity.getTotalColumns());
            dto.setEstimatedTotalRows(entity.getEstimatedTotalRows());
            dto.setEstimatedTotalSizeBytes(entity.getEstimatedTotalSizeBytes());
            
            return dto;
            
        } catch (Exception e) {
            logger.error("Error converting StructuredReport to ReportInfoDto for report ID: {}", entity.getId(), e);
            throw new RuntimeException("Failed to convert report to info DTO: " + e.getMessage(), e);
        }
    }

}