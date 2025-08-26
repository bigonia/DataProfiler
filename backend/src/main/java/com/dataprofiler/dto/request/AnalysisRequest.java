package com.dataprofiler.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request DTO for AI analysis operations
 * Contains user question and data source context for intelligent analysis
 */
@Data
@Schema(description = "AI Analysis Request")
public class AnalysisRequest {

    @Schema(description = "User's question or analysis request", example = "What are the data quality issues in this dataset?")
    @JsonProperty("question")
//    @NotBlank(message = "Question cannot be blank")
    private String question;

    @Schema(description = "User identifier for tracking", example = "user-123")
    @JsonProperty("userId")
    private String userId = "default-user";

    @Schema(description = "Task ID for context (optional)", example = "task-20250127-001")
    @JsonProperty("taskId")
    private String taskId;

    @Schema(description = "Analysis type or category (optional)", example = "data_quality")
    @JsonProperty("analysisType")
    private String analysisType;

    @Schema(description = "Additional context or parameters (optional)")
    @JsonProperty("context")
    private String context;

}