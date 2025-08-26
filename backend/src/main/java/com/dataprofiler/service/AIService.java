package com.dataprofiler.service;

import com.dataprofiler.dto.request.AnalysisRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service interface for AI-powered data analysis
 * Handles integration with Dify workflow API for intelligent data profiling analysis
 * Provides streaming analysis capabilities for real-time user interaction
 */
public interface AIService {

    /**
     * Perform streaming AI analysis on profiling data
     * Integrates with Dify workflow to provide intelligent insights
     * Returns real-time streaming response for enhanced user experience
     *
     * @param request the analysis request containing question, data source info, and context
     * @return SseEmitter for streaming analysis results
     * @throws IllegalArgumentException if request parameters are invalid
     * @throws RuntimeException if AI service is unavailable or analysis fails
     */
    SseEmitter streamAnalysis(AnalysisRequest request);

    /**
     * Check if AI service is available and healthy
     * Performs connectivity test with Dify API
     *
     * @return true if AI service is available, false otherwise
     */
    boolean isAIServiceAvailable();

    /**
     * Get AI service status information
     * Provides detailed status including connectivity, configuration, and performance metrics
     *
     * @return status information as a formatted string
     */
    String getAIServiceStatus();


    /**
     * Build context information for AI analysis
     * Retrieves and formats profiling data context based on request parameters
     *
     * @param request the analysis request
     * @return formatted context string for AI processing
     * @throws RuntimeException if context building fails
     */
    String buildAnalysisContext(AnalysisRequest request);
}