package com.dataprofiler.controller;

import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.FileMetadata;
import com.dataprofiler.service.DataSourceService;
import com.dataprofiler.service.FileService;
import com.dataprofiler.service.FileManagementService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.CompletableFuture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for file upload operations
 * Provides endpoints for uploading files and creating file-based data sources
 */
@RestController
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "APIs for file upload and management")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);
    private static final String UPLOAD_DIR = "uploads";

    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private FileService fileService;
    
    @Autowired
    private FileManagementService fileManagementService;

    /**
     * Upload a file and create a file-based data source (legacy endpoint)
     */
    @PostMapping("/upload")
    @Operation(
        summary = "Upload file and create data source (legacy)",
        description = "Upload a file (CSV, Excel, etc.) and automatically create a file-based data source. Use /upload-only for new implementations."
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

            // Save file with proper resource management
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

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

            // Save file metadata
            FileMetadata fileMetadata = new FileMetadata();
            fileMetadata.setFilename(uniqueFilename);
            fileMetadata.setOriginalFilename(originalFilename);
            fileMetadata.setFilePath(filePath.toAbsolutePath().toString());
            fileMetadata.setFileSize(file.getSize());
            fileMetadata.setMimeType(file.getContentType());
            fileMetadata.setUploadedAt(LocalDateTime.now());
            fileMetadata.setDescription("Uploaded file: " + originalFilename);
            fileMetadata.setDataSourceId(savedDataSource.getId());

            FileMetadata savedFileMetadata = fileService.saveFileMetadata(fileMetadata);

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
     
     /**
      * Upload file only (without creating data source)
      */
     @PostMapping("/upload-only")
     @Operation(
         summary = "Upload file only",
         description = "Upload a file without automatically creating a data source. Use /convert endpoint to convert later."
     )
     @ApiResponses(value = {
         @ApiResponse(
             responseCode = "201", 
             description = "File uploaded successfully",
             content = @Content(schema = @Schema(implementation = FileMetadata.class))
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
     public ResponseEntity<?> uploadFileOnly(
             @Parameter(description = "File to upload", required = true)
             @RequestParam("file") MultipartFile file,
             @Parameter(description = "Optional description for the file")
             @RequestParam(value = "description", required = false) String description) {

         logger.info("Uploading file only: {}", file.getOriginalFilename());

         try {
             FileMetadata uploadedFile = fileManagementService.uploadFile(file, description);
             logger.info("File uploaded successfully with ID: {}", uploadedFile.getId());
             return ResponseEntity.status(HttpStatus.CREATED).body(uploadedFile);

         } catch (IllegalArgumentException e) {
             logger.warn("Invalid file upload request: {}", e.getMessage());
             return ResponseEntity.badRequest().body(e.getMessage());
         } catch (Exception e) {
             logger.error("Failed to upload file: {}", file.getOriginalFilename(), e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Failed to upload file: " + e.getMessage());
         }
     }
     
     /**
      * Convert uploaded file to data source
      */
     @PostMapping("/convert/{fileId}")
     @Operation(
         summary = "转换文件为数据源",
         description = "将上传的文件转换为数据源"
     )
     @ApiResponses(value = {
         @ApiResponse(
             responseCode = "200", 
             description = "Conversion completed successfully",
             content = @Content(schema = @Schema(implementation = Map.class))
         ),
         @ApiResponse(
             responseCode = "400", 
             description = "Invalid conversion request",
             content = @Content(schema = @Schema(implementation = String.class))
         ),
         @ApiResponse(
             responseCode = "404", 
             description = "File not found",
             content = @Content(schema = @Schema(implementation = String.class))
         ),
         @ApiResponse(
             responseCode = "500", 
             description = "Internal server error",
             content = @Content(schema = @Schema(implementation = String.class))
         )
     })
     public ResponseEntity<Map<String, Object>> convertFile(
             @Parameter(description = "文件ID") @PathVariable Long fileId,
             @Parameter(description = "数据源名称") @RequestParam String dataSourceName) {

         logger.info("Starting async file conversion for file ID: {}", fileId);

         try {
             // Start async conversion
             convertFileAsync(fileId, dataSourceName);
             
             Map<String, Object> response = new HashMap<>();
             response.put("success", true);
             response.put("message", "文件转换已开始，请稍后查看转换状态");
             response.put("fileId", fileId);
             return ResponseEntity.accepted().body(response);
         } catch (Exception e) {
             logger.error("启动文件转换失败", e);
             Map<String, Object> response = new HashMap<>();
             response.put("success", false);
             response.put("message", "启动文件转换失败: " + e.getMessage());
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
         }
     }
     
     /**
      * Async method to convert file to data source
      */
     @Async
     public CompletableFuture<Void> convertFileAsync(Long fileId, String dataSourceName) {
         logger.info("Executing async file conversion for file ID: {}", fileId);
         
         try {
             DataSourceConfig dataSourceConfig = fileManagementService.convertFileToDataSource(fileId, dataSourceName);
             logger.info("File conversion completed successfully for file ID: {}, data source ID: {}", 
                        fileId, dataSourceConfig.getId());
         } catch (Exception e) {
             logger.error("Async file conversion failed for file ID: {}", fileId, e);
             // Here you could update the file status to indicate conversion failure
             // fileManagementService.markConversionFailed(fileId, e.getMessage());
         }
         
         return CompletableFuture.completedFuture(null);
     }
     
     
     /**
      * Search files by filename
      */
     @GetMapping("/search")
     @Operation(
         summary = "Search files",
         description = "Search files by filename"
     )
     public ResponseEntity<?> searchFiles(
             @Parameter(description = "Search term", example = "report")
             @RequestParam String searchTerm) {
         
         try {
             List<FileMetadata> files = fileService.searchFilesByName(searchTerm);
             return ResponseEntity.ok(files);
         } catch (Exception e) {
             logger.error("Failed to search files", e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Failed to search files: " + e.getMessage());
         }
     }
     
     /**
      * Get file by ID
      */
     @GetMapping("/{id}")
     @Operation(
         summary = "Get file by ID",
         description = "Get file metadata by ID"
     )
     public ResponseEntity<?> getFileById(
             @Parameter(description = "File ID", example = "1")
             @PathVariable Long id) {
         
         try {
             return fileService.getFileById(id)
                     .map(file -> ResponseEntity.ok(file))
                     .orElse(ResponseEntity.notFound().build());
         } catch (Exception e) {
             logger.error("Failed to get file by ID: {}", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Failed to get file: " + e.getMessage());
         }
     }
     
     /**
      * Delete file by ID
      */
     @DeleteMapping("/{id}")
     @Operation(
         summary = "Delete file",
         description = "Delete file and its metadata by ID"
     )
     public ResponseEntity<?> deleteFile(
             @Parameter(description = "File ID", example = "1")
             @PathVariable Long id) {
         
         try {
             boolean deleted = fileService.deleteFile(id);
             if (deleted) {
                 return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
             } else {
                 return ResponseEntity.notFound().build();
             }
         } catch (Exception e) {
             logger.error("Failed to delete file with ID: {}", id, e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Failed to delete file: " + e.getMessage());
         }
     }
     
     /**
      * Get file statistics
      */
     @GetMapping("/statistics")
     @Operation(
         summary = "Get file statistics",
         description = "Get file upload statistics"
     )
     public ResponseEntity<?> getFileStatistics() {
         
         try {
             FileService.FileStatistics statistics = fileService.getFileStatistics();
             return ResponseEntity.ok(statistics);
         } catch (Exception e) {
             logger.error("Failed to get file statistics", e);
             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                     .body("Failed to get file statistics: " + e.getMessage());
         }
     }
     
     /**
     * Get list of uploaded files with pagination
     */
    @GetMapping("/list")
    @Operation(
        summary = "Get file list",
        description = "Get paginated list of uploaded files"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "File list retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = String.class))
        )
    })
    public ResponseEntity<?> getFileList(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size) {
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileMetadata> files = fileService.getAllFiles(pageable);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            logger.error("Failed to retrieve file list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve file list: " + e.getMessage());
        }
    }
    
}