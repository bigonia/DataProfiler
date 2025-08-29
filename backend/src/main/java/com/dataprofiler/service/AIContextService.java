package com.dataprofiler.service;

/**
 * Service interface for AI context preparation
 * Handles conversion of structured profiling reports to LLM-friendly Markdown format
 * Provides intelligent compression and formatting for optimal AI processing
 */
public interface AIContextService {

    /**
     * Build context for LLM based on task ID
     * Retrieves structured report data and converts it to optimized Markdown format
     * Applies intelligent compression to stay within token limits while preserving key information
     *
     * @param taskId the profiling task ID
     * @return formatted Markdown string context for LLM processing
     * @throws RuntimeException if report not found or context building fails
     */
    String buildContextForLLM(String taskId);

    /**
     * Build context for LLM with custom length limit
     * Allows fine-tuning of context length based on specific requirements
     *
     * @param taskId the profiling task ID
     * @param maxLength maximum context length in characters
     * @return formatted Markdown string context for LLM processing
     * @throws RuntimeException if report not found or context building fails
     */
    String buildContextForLLM(String taskId, int maxLength);

    /**
     * Check if context can be built for the given task ID
     * Validates that required report data exists
     *
     * @param taskId the profiling task ID
     * @return true if context can be built, false otherwise
     */
    boolean canBuildContext(String taskId);

    /**
     * Get estimated context length for the given task ID
     * Useful for pre-validation before actual context building
     *
     * @param taskId the profiling task ID
     * @return estimated context length in characters, -1 if cannot estimate
     */
    int getEstimatedContextLength(String taskId);

}