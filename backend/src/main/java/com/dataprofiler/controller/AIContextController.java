package com.dataprofiler.controller;

import com.dataprofiler.service.AIContextService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for AI Context operations
 * Provides endpoints for building LLM-optimized context from profiling reports
 */
@RestController
@RequestMapping("/api/ai-context")
@Tag(name = "AI Context", description = "AI Context Management APIs")
@CrossOrigin(origins = "*")
public class AIContextController {

    private static final Logger logger = LoggerFactory.getLogger(AIContextController.class);

    @Autowired
    private AIContextService aiContextService;

    /**
     * Build context for LLM based on task ID
     */
    @GetMapping("/build/{taskId}")
    @Operation(summary = "Build AI context for LLM", 
               description = "Converts structured profiling report to LLM-optimized Markdown format")
    public ResponseEntity<Map<String, Object>> buildContext(
            @Parameter(description = "Profiling task ID", required = true)
            @PathVariable String taskId,
            @Parameter(description = "Maximum context length in characters")
            @RequestParam(required = false) Integer maxLength) {
        
        try {
            logger.info("Building AI context for task: {}, maxLength: {}", taskId, maxLength);
            
            String context;
            if (maxLength != null && maxLength > 0) {
                context = aiContextService.buildContextForLLM(taskId, maxLength);
            } else {
                context = aiContextService.buildContextForLLM(taskId);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("context", context);
            response.put("contextLength", context.length());
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("AI context built successfully for task: {}, length: {}", taskId, context.length());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to build AI context for task: {}", taskId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("taskId", taskId);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Check if context can be built for the given task ID
     */
    @GetMapping("/check/{taskId}")
    @Operation(summary = "Check if AI context can be built", 
               description = "Validates that required report data exists for context building")
    public ResponseEntity<Map<String, Object>> checkContext(
            @Parameter(description = "Profiling task ID", required = true)
            @PathVariable String taskId) {
        
        try {
            logger.info("Checking AI context availability for task: {}", taskId);
            
            boolean canBuild = aiContextService.canBuildContext(taskId);
            int estimatedLength = aiContextService.getEstimatedContextLength(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("canBuildContext", canBuild);
            response.put("estimatedLength", estimatedLength);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("AI context check completed for task: {}, canBuild: {}, estimatedLength: {}", 
                       taskId, canBuild, estimatedLength);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to check AI context for task: {}", taskId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("taskId", taskId);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get AI context metadata without building full context
     */
    @GetMapping("/metadata/{taskId}")
    @Operation(summary = "Get AI context metadata", 
               description = "Returns metadata about the context without building the full content")
    public ResponseEntity<Map<String, Object>> getContextMetadata(
            @Parameter(description = "Profiling task ID", required = true)
            @PathVariable String taskId) {
        
        try {
            logger.info("Getting AI context metadata for task: {}", taskId);
            
            boolean canBuild = aiContextService.canBuildContext(taskId);
            int estimatedLength = aiContextService.getEstimatedContextLength(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("taskId", taskId);
            response.put("available", canBuild);
            response.put("estimatedLength", estimatedLength);
            response.put("maxRecommendedLength", 8000);
            response.put("compressionNeeded", estimatedLength > 8000);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("AI context metadata retrieved for task: {}", taskId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to get AI context metadata for task: {}", taskId, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("taskId", taskId);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

}