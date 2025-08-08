package com.dataprofiler.controller;

import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.service.DataSourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing data source configurations
 * Provides endpoints for CRUD operations on data sources
 */
@RestController
@RequestMapping("/datasources")
@Tag(name = "Data Source Management", description = "APIs for managing data source configurations")
@Validated
public class DataSourceController {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceController.class);

    @Autowired
    private DataSourceService dataSourceService;

    /**
     * Create a new data source configuration
     */
    @PostMapping()
    @Operation(summary = "Create data source", description = "Create a new data source configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Data source created successfully",
                    content = @Content(schema = @Schema(implementation = DataSourceConfig.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or data source name already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<DataSourceConfig> createDataSource(
            @Valid @RequestBody DataSourceConfig dataSourceConfig) {
        
        logger.info("Creating new data source: {}", dataSourceConfig.getName());
        
        try {
            DataSourceConfig created = dataSourceService.createDataSource(dataSourceConfig);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create data source: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error creating data source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all active data sources
     */
    @GetMapping
    @Operation(summary = "Get all data sources", description = "Retrieve all active data source configurations")
    @ApiResponse(responseCode = "200", description = "Data sources retrieved successfully")
    public ResponseEntity<List<DataSourceConfig>> getAllDataSources() {
        logger.debug("Retrieving all active data sources");
        
        List<DataSourceConfig> dataSources = dataSourceService.getAllDataSources();
        return ResponseEntity.ok(dataSources);
    }

    /**
     * Get data source by ID
     */
    @GetMapping("/{sourceId}")
    @Operation(summary = "Get data source by source ID", description = "Retrieve a specific data source configuration by source ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data source found",
                    content = @Content(schema = @Schema(implementation = DataSourceConfig.class))),
        @ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<DataSourceConfig> getDataSourceBySourceId(
            @Parameter(description = "Data source unique identifier") @PathVariable @NotBlank String sourceId) {
        
        logger.debug("Retrieving data source by source ID: {}", sourceId);
        
        try {
            DataSourceConfig dataSource = dataSourceService.getDataSourceBySourceId(sourceId);
            return ResponseEntity.ok(dataSource);
        } catch (IllegalArgumentException e) {
            logger.warn("Data source not found: {}", sourceId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Update data source configuration
     */
    @PutMapping("/{sourceId}")
    @Operation(summary = "Update data source", description = "Update an existing data source configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Data source updated successfully",
                    content = @Content(schema = @Schema(implementation = DataSourceConfig.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input or name conflict"),
        @ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<DataSourceConfig> updateDataSource(
            @Parameter(description = "Data source unique identifier") @PathVariable @NotBlank String sourceId,
            @Valid @RequestBody DataSourceConfig updatedConfig) {
        
        logger.info("Updating data source: {}", sourceId);
        
        try {
            DataSourceConfig updated = dataSourceService.updateDataSource(sourceId, updatedConfig);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update data source: {}", e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            logger.error("Error updating data source", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete data source (soft delete)
     */
    @DeleteMapping("/{sourceId}")
    @Operation(summary = "Delete data source", description = "Delete a data source configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Data source deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<Void> deleteDataSource(
            @Parameter(description = "Data source unique identifier") @PathVariable @NotBlank String sourceId) {
        
        logger.info("Deleting data source: {}", sourceId);
        
        try {
            dataSourceService.deleteDataSource(sourceId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Data source not found for deletion: {}", sourceId);
            return ResponseEntity.notFound().build();
        }
    }


    /**
     * Test data source connection
     */
    @PostMapping("/{sourceId}/test")
    @Operation(summary = "Test data source connection", description = "Test connectivity to a data source")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connection test completed"),
        @ApiResponse(responseCode = "404", description = "Data source not found")
    })
    public ResponseEntity<Map<String, Object>> testDataSourceConnection(
            @Parameter(description = "Data source unique identifier") @PathVariable @NotBlank String sourceId) {
        
        logger.info("Testing connection for data source: {}", sourceId);
        
        try {
            DataSourceConfig dataSource = dataSourceService.getDataSourceBySourceId(sourceId);
            
            // Use detailed connection test if available
            if (dataSourceService instanceof com.dataprofiler.service.impl.DataSourceServiceImpl) {
                com.dataprofiler.service.impl.DataSourceServiceImpl serviceImpl = 
                    (com.dataprofiler.service.impl.DataSourceServiceImpl) dataSourceService;
                com.dataprofiler.dto.ConnectionTestResult testResult = serviceImpl.testConnectionDetailed(dataSource);
                
                Map<String, Object> result = Map.of(
                    "success", testResult.isSuccess(),
                    "message", testResult.getMessage(),
                    "durationMs", testResult.getDurationMs(),
                    "timestamp", java.time.LocalDateTime.now()
                );
                
                return ResponseEntity.ok(result);
            } else {
                // Fallback to basic test
                boolean connectionSuccess = dataSourceService.testConnection(dataSource);
                
                Map<String, Object> result = Map.of(
                    "success", connectionSuccess,
                    "message", connectionSuccess ? "Connection successful" : "Connection failed",
                    "timestamp", java.time.LocalDateTime.now()
                );
                
                return ResponseEntity.ok(result);
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Data source not found for connection test: {}", sourceId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Test data source connection with configuration
     */
    @PostMapping("/test")
    @Operation(summary = "Test data source connection with config", 
              description = "Test connectivity using provided configuration without saving")
    @ApiResponse(responseCode = "200", description = "Connection test completed")
    public ResponseEntity<Map<String, Object>> testDataSourceConnectionWithConfig(
            @Valid @RequestBody DataSourceConfig dataSourceConfig) {
        
        logger.info("Testing connection with provided configuration: {}", dataSourceConfig.getName());
        
        // Use detailed connection test if available
        if (dataSourceService instanceof com.dataprofiler.service.impl.DataSourceServiceImpl) {
            com.dataprofiler.service.impl.DataSourceServiceImpl serviceImpl = 
                (com.dataprofiler.service.impl.DataSourceServiceImpl) dataSourceService;
            com.dataprofiler.dto.ConnectionTestResult testResult = serviceImpl.testConnectionDetailed(dataSourceConfig);
            
            Map<String, Object> result = Map.of(
                "success", testResult.isSuccess(),
                "message", testResult.getMessage(),
                "durationMs", testResult.getDurationMs(),
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(result);
        } else {
            // Fallback to basic test
            boolean connectionSuccess = dataSourceService.testConnection(dataSourceConfig);
            
            Map<String, Object> result = Map.of(
                "success", connectionSuccess,
                "message", connectionSuccess ? "Connection successful" : "Connection failed",
                "timestamp", java.time.LocalDateTime.now()
            );
            
            return ResponseEntity.ok(result);
        }
    }

    /**
     * Get data sources by type
     */
    @GetMapping("/type/{type}")
    @Operation(summary = "Get data sources by type", description = "Retrieve data sources of a specific type")
    @ApiResponse(responseCode = "200", description = "Data sources retrieved successfully")
    public ResponseEntity<List<DataSourceConfig>> getDataSourcesByType(
            @Parameter(description = "Data source type") @PathVariable DataSourceConfig.DataSourceType type) {
        
        logger.debug("Retrieving data sources by type: {}", type);
        
        List<DataSourceConfig> dataSources = dataSourceService.getDataSourcesByType(type);
        return ResponseEntity.ok(dataSources);
    }

}