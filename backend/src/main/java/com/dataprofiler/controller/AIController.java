package com.dataprofiler.controller;

import com.dataprofiler.dto.request.AnalysisRequest;
import com.dataprofiler.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for AI-powered data analysis
 * Provides streaming analysis endpoints using Server-Sent Events (SSE)
 * Integrates with Dify workflow API for intelligent data insights
 */
@RestController
@RequestMapping("/api/v1/ai")
@Validated
@Tag(name = "AI Analysis", description = "AI-powered data analysis and insights APIs")
public class AIController {

    private static final Logger logger = LoggerFactory.getLogger(AIController.class);

    @Autowired
    private AIService aiService;

    /**
     * Stream AI analysis results using Server-Sent Events
     * Provides real-time streaming of analysis insights from Dify workflow
     *
     * @param request Analysis request containing question, data source ID, and context
     * @return SseEmitter for streaming analysis results
     */
    @PostMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Stream AI analysis",
            description = "Perform streaming AI analysis on data source using Dify workflow. " +
                    "Returns real-time analysis results via Server-Sent Events (SSE)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Analysis stream started successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "404", description = "Data source not found"),
            @ApiResponse(responseCode = "503", description = "AI service unavailable")
    })
    public SseEmitter streamAnalysis(
            @Parameter(description = "Analysis request with question and data source details")
            @Valid @RequestBody AnalysisRequest request) {

        logger.info("Received streaming analysis request for user: {}, taskId: {}, question: {}",
                request.getUserId(), request.getTaskId(), request.getQuestion().substring(0, Math.min(request.getQuestion().length(), 100)));

        try {
            // Validate AI service availability
//            if (!aiService.isAIServiceAvailable()) {
//                logger.error("AI service is not available");
//                throw new RuntimeException("AI service is currently unavailable");
//            }

            // Start streaming analysis
            SseEmitter emitter = aiService.streamAnalysis(request);

            logger.debug("SSE emitter created successfully for user: {}", request.getUserId());
            return emitter;

        } catch (Exception e) {
            logger.error("Failed to start streaming analysis for user: {}, error: {}",
                    request.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Check AI service health and availability
     *
     * @return Service status information
     */
    @GetMapping("/health")
    @Operation(
            summary = "Check AI service health",
            description = "Check the availability and health status of AI analysis service"
    )
    @ApiResponse(responseCode = "200", description = "Service status retrieved successfully")
    public ResponseEntity<Map<String, Object>> checkHealth() {

        logger.debug("Checking AI service health status");

        try {
            Map<String, Object> status = new HashMap<>();

            boolean isAvailable = aiService.isAIServiceAvailable();
            status.put("status", status.put("timestamp", System.currentTimeMillis()));
            status.put("available", isAvailable);
            status.put("timestamp", System.currentTimeMillis());

            logger.debug("AI service health check completed, available: {}", isAvailable);

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            logger.error("Failed to check AI service health: {}", e.getMessage(), e);

            Map<String, Object> errorStatus = Map.of(
                    "available", false,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );

            return ResponseEntity.status(503).body(errorStatus);
        }
    }

    /**
     * Get AI service configuration and capabilities
     *
     * @return Service configuration information
     */
    @GetMapping("/info")
    @Operation(
            summary = "Get AI service information",
            description = "Retrieve AI service configuration and capabilities"
    )
    @ApiResponse(responseCode = "200", description = "Service information retrieved successfully")
    public ResponseEntity<Map<String, Object>> getServiceInfo() {

        logger.debug("Retrieving AI service information");

        try {
            Map<String, Object> info = Map.of(
                    "service", "AI Analysis Service",
                    "version", "1.0.0",
                    "provider", "Dify Workflow API",
                    "features", new String[]{"streaming_analysis", "context_aware", "multi_datasource"},
                    "supported_formats", new String[]{"text/event-stream", "application/json"},
                    "max_context_length", 50000,
                    "timeout_minutes", 30
            );

            return ResponseEntity.ok(info);

        } catch (Exception e) {
            logger.error("Failed to retrieve AI service information: {}", e.getMessage(), e);
            throw e;
        }
    }
}