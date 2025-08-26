package com.dataprofiler.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for Dify Workflow API responses
 * Encapsulates the structure returned by Dify's workflow execution endpoint
 */
@Schema(description = "Dify Workflow Response")
public class DifyWorkflowResponse {

    @Schema(description = "Event type from Dify stream", example = "workflow_started")
    @JsonProperty("event")
    private String event;

    @Schema(description = "Task ID from Dify", example = "task-123")
    @JsonProperty("task_id")
    private String taskId;

    @Schema(description = "Workflow ID", example = "workflow-456")
    @JsonProperty("workflow_id")
    private String workflowId;

    @Schema(description = "Workflow run ID", example = "run-789")
    @JsonProperty("workflow_run_id")
    private String workflowRunId;

    @Schema(description = "Response data content")
    @JsonProperty("data")
    private Object data;

    @Schema(description = "Response message or content", example = "Analysis completed successfully")
    @JsonProperty("answer")
    private String answer;

    @Schema(description = "Conversation ID for context continuity")
    @JsonProperty("conversation_id")
    private String conversationId;

    @Schema(description = "Message ID")
    @JsonProperty("message_id")
    private String messageId;

    @Schema(description = "Metadata associated with the response")
    @JsonProperty("metadata")
    private Object metadata;

    @Schema(description = "Error information if any")
    @JsonProperty("error")
    private String error;

    @Schema(description = "Status of the workflow execution", example = "completed")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Timestamp of the response")
    @JsonProperty("created_at")
    private Long createdAt;

    // Default constructor
    public DifyWorkflowResponse() {}

    // Constructor with basic fields
    public DifyWorkflowResponse(String event, String taskId, Object data) {
        this.event = event;
        this.taskId = taskId;
        this.data = data;
    }

    // Getters and Setters
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowRunId() {
        return workflowRunId;
    }

    public void setWorkflowRunId(String workflowRunId) {
        this.workflowRunId = workflowRunId;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Object getMetadata() {
        return metadata;
    }

    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "DifyWorkflowResponse{" +
                "event='" + event + '\'' +
                ", taskId='" + taskId + '\'' +
                ", workflowId='" + workflowId + '\'' +
                ", workflowRunId='" + workflowRunId + '\'' +
                ", answer='" + answer + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", messageId='" + messageId + '\'' +
                ", error='" + error + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}