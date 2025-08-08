package com.dataprofiler.service.impl;

import com.dataprofiler.dto.request.DetailedReportRequest;
import com.dataprofiler.dto.request.ReportQueryDto;
import com.dataprofiler.dto.response.ReportSummaryDto;
import com.dataprofiler.dto.response.StructuredReportDto;
import com.dataprofiler.entity.StructuredReport;
import com.dataprofiler.repository.StructuredReportRepository;
import com.dataprofiler.service.StructuredReportService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    public List<ReportSummaryDto> getReportsSummary(String taskId) {
        logger.debug("Retrieving reports summary for task: {}", taskId);

        try {
            List<StructuredReport> reports = structuredReportRepository.findByTaskIdOrderByGeneratedAtDesc(taskId);
            
            List<ReportSummaryDto> summaries = reports.stream()
                    .map(this::convertToSummaryDto)
                    .filter(summary -> summary != null)
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} report summaries for task: {}", summaries.size(), taskId);
            return summaries;

        } catch (Exception e) {
            logger.error("Error retrieving reports summary for task: {}", taskId, e);
            throw new RuntimeException("Failed to retrieve reports summary: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructuredReportDto> getDetailedReport(String taskId, DetailedReportRequest request, String format) {
        logger.debug("Retrieving detailed reports for task: {} with format: {}", taskId, format);

        try {
            List<StructuredReport> reports = structuredReportRepository.findByTaskIdOrderByGeneratedAtDesc(taskId);
            
            List<StructuredReportDto> detailedReports = reports.stream()
                    .map(this::convertToDto)
                    .filter(report -> report != null)
                    .collect(Collectors.toList());

            logger.debug("Retrieved {} detailed reports for task: {}", detailedReports.size(), taskId);
            return detailedReports;

        } catch (Exception e) {
            logger.error("Error retrieving detailed reports for task: {}", taskId, e);
            throw new RuntimeException("Failed to retrieve detailed reports: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StructuredReport> getReportsByTaskId(String taskId) {
        logger.debug("Retrieving report entities for task: {}", taskId);

        try {
            return structuredReportRepository.findByTaskIdOrderByGeneratedAtDesc(taskId);
        } catch (Exception e) {
            logger.error("Error retrieving report entities for task: {}", taskId, e);
            throw new RuntimeException("Failed to retrieve report entities: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<StructuredReportDto> getDetailedReportWithPagination(ReportQueryDto queryDto) {
        logger.debug("Retrieving detailed reports with query: taskId={}, dataSourceId={}, page={}, size={}", 
                queryDto.getTaskId(), queryDto.getDataSourceId(), 
                queryDto.getPage(), queryDto.getSize());

        try {
            // Create pageable with sorting
            Sort sort = Sort.by(Sort.Direction.DESC, "generatedAt");
            Pageable pageable = PageRequest.of(
                    queryDto.getPage() != null ? queryDto.getPage() : 0,
                    queryDto.getSize() != null ? queryDto.getSize() : 20,
                    sort
            );

            Page<StructuredReport> reportPage;

            // Apply filtering based on query parameters
            if (queryDto.getTaskId() != null && queryDto.getDataSourceId() != null) {
                // Filter by both task ID and data source ID
                reportPage = structuredReportRepository.findByTaskIdAndDataSourceId(
                        queryDto.getTaskId(), queryDto.getDataSourceId(), pageable);
            } else if (queryDto.getTaskId() != null) {
                // Filter by task ID only
                reportPage = structuredReportRepository.findByTaskId(queryDto.getTaskId(), pageable);
            } else if (queryDto.getDataSourceId() != null) {
                // Filter by data source ID only
                reportPage = structuredReportRepository.findByDataSourceId(queryDto.getDataSourceId(), pageable);
            } else {
                // No specific filters, get all reports
                reportPage = structuredReportRepository.findAll(pageable);
            }

            // Convert entities to DTOs
            Page<StructuredReportDto> resultPage = reportPage.map(this::convertToDto);

            logger.debug("Retrieved {} detailed reports (page {} of {})", 
                    resultPage.getNumberOfElements(), 
                    resultPage.getNumber() + 1, 
                    resultPage.getTotalPages());

            return resultPage;

        } catch (Exception e) {
            logger.error("Error retrieving detailed reports with query: {}", queryDto, e);
            throw new RuntimeException("Failed to retrieve detailed reports: " + e.getMessage(), e);
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
            entity.setProfilingStartTime(dto.getProfilingStartTime());
            entity.setProfilingEndTime(dto.getProfilingEndTime());
            
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
            dto.setProfilingStartTime(entity.getProfilingStartTime());
            dto.setProfilingEndTime(entity.getProfilingEndTime());
            
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
     */
    private ReportSummaryDto convertToSummaryDto(StructuredReport entity) {
        try {
            // Parse the JSON data to get database and table information
            StructuredReportDto fullReport = convertToDto(entity);
            if (fullReport == null) {
                return null;
            }

            ReportSummaryDto summary = new ReportSummaryDto(
                entity.getTaskId(),
                entity.getDataSourceId(),
                null, // dataSourceName not available in StructuredReportDto
                fullReport.getDataSourceType(),
                fullReport.getDatabase(),
                fullReport.getTables()
            );
            
            // Summary statistics from denormalized fields
            summary.setTotalTables(entity.getTotalTables());
            summary.setTotalColumns(entity.getTotalColumns());
            summary.setEstimatedTotalRows(entity.getEstimatedTotalRows());
            summary.setEstimatedTotalSizeBytes(entity.getEstimatedTotalSizeBytes());
            summary.setProfilingDurationSeconds(entity.getProfilingDurationSeconds());
            
            // Calculate data size in human-readable format
            if (entity.getEstimatedTotalSizeBytes() != null) {
                summary.setFormattedDataSize(formatBytes(entity.getEstimatedTotalSizeBytes()));
            }
            
            // Calculate profiling duration in human-readable format
            if (entity.getProfilingDurationSeconds() != null) {
                summary.setFormattedDuration(formatDuration(entity.getProfilingDurationSeconds()));
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
            int deletedCount = structuredReportRepository.deleteByTaskId(taskId);
            logger.info("Deleted {} reports for task: {}", deletedCount, taskId);
        } catch (Exception e) {
            logger.error("Error deleting reports for task: {}", taskId, e);
            throw new RuntimeException("Failed to delete reports: " + e.getMessage(), e);
        }
    }

    /**
     * Delete reports by data source ID (for cleanup operations)
     */
    @Transactional
    public void deleteReportsByDataSourceId(String dataSourceId) {
        logger.info("Deleting reports for data source: {}", dataSourceId);
        
        try {
            int deletedCount = structuredReportRepository.deleteByDataSourceId(dataSourceId);
            logger.info("Deleted {} reports for data source: {}", deletedCount, dataSourceId);
        } catch (Exception e) {
            logger.error("Error deleting reports for data source: {}", dataSourceId, e);
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

    /**
     * Get report count by data source ID
     */
    @Transactional(readOnly = true)
    public long getReportCountByDataSourceId(String dataSourceId) {
        try {
            return structuredReportRepository.countByDataSourceId(dataSourceId);
        } catch (Exception e) {
            logger.error("Error counting reports for data source: {}", dataSourceId, e);
            return 0;
        }
    }
}