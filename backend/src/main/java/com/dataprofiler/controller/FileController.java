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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for file upload operations
 * Provides endpoints for uploading files and creating file-based data sources
 */
@RestController
@RequestMapping("/files")
@Tag(name = "File Management", description = "APIs for file upload and management")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private DataSourceService dataSourceService;

    /**
     * Upload a file and create a file-based data source
     */
    @PostMapping("/upload")
    @Operation(
        summary = "Upload file and create data source",
        description = "Upload a file (CSV, Excel, etc.) and automatically create a file-based data source"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "File uploaded and data source created successfully",
            content = @Content(schema = @Schema(implementation = DataSourceConfig.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid file or upload failed",
            content = @Content(schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file) {
        
        logger.info("Uploading file: {}", file.getOriginalFilename());
        
        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }
            
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid filename");
            }
            
            // Create upload directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generate unique filename
            String fileExtension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFilename.substring(dotIndex);
            }
            
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = uploadPath.resolve(uniqueFilename);
            
            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            // Create file-based data source
            DataSourceConfig dataSource = new DataSourceConfig();
            dataSource.setName("File: " + originalFilename);
            dataSource.setType(DataSourceConfig.DataSourceType.FILE);
            
            // Set file properties
            Map<String, String> properties = new HashMap<>();
            properties.put("filePath", filePath.toAbsolutePath().toString());
            properties.put("originalFilename", originalFilename);
            properties.put("fileSize", String.valueOf(file.getSize()));
            properties.put("contentType", file.getContentType() != null ? file.getContentType() : "unknown");
            dataSource.setProperties(properties);
            
            // Save data source
            DataSourceConfig savedDataSource = dataSourceService.createDataSource(dataSource);
            
            logger.info("File uploaded successfully and data source created with ID: {}", 
                       savedDataSource.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(savedDataSource);
            
        } catch (IOException e) {
            logger.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to create data source for uploaded file: {}", 
                        file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create data source: " + e.getMessage());
        }
    }
    
}