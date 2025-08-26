package com.dataprofiler.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Request DTO for Dify Workflow API calls
 * Encapsulates the structure required by Dify's workflow execution endpoint
 */
@Schema(description = "Dify Workflow Request")
public class DifyWorkflowRequest {

    @Schema(description = "Input parameters for the workflow", example = "{\"profile_context\": \"...\", \"user_question\": \"...\"}") 
    @JsonProperty("inputs")
    @NotNull(message = "Inputs cannot be null")
    private Map<String, Object> inputs;

    @Schema(description = "User identifier for tracking and personalization", example = "user-123")
    @JsonProperty("user")
    @NotNull(message = "User cannot be null")
    private String user;

    @Schema(description = "Enable streaming response", example = "true")
    @JsonProperty("stream")
    private boolean stream = true;

    @Schema(description = "Response mode (streaming or blocking)", example = "streaming")
    @JsonProperty("response_mode")
    private String responseMode = "streaming";

    @Schema(description = "Conversation ID for context continuity (optional)")
    @JsonProperty("conversation_id")
    private String conversationId;

    @Schema(description = "Additional files or attachments (optional)")
    @JsonProperty("files")
    private Object[] files;

    // Default constructor
    public DifyWorkflowRequest() {}

    // Constructor with required fields
    public DifyWorkflowRequest(Map<String, Object> inputs, String user, boolean stream) {
        this.inputs = inputs;
        this.user = user;
        this.stream = stream;
        this.responseMode = stream ? "streaming" : "blocking";
    }

    // Full constructor
    public DifyWorkflowRequest(Map<String, Object> inputs, String user, boolean stream, 
                              String conversationId, Object[] files) {
        this.inputs = inputs;
        this.user = user;
        this.stream = stream;
        this.responseMode = stream ? "streaming" : "blocking";
        this.conversationId = conversationId;
        this.files = files;
    }

    // Getters and Setters
    public Map<String, Object> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, Object> inputs) {
        this.inputs = inputs;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
        this.responseMode = stream ? "streaming" : "blocking";
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Object[] getFiles() {
        return files;
    }

    public void setFiles(Object[] files) {
        this.files = files;
    }

    @Override
    public String toString() {
        return "DifyWorkflowRequest{" +
                "inputs=" + inputs +
                ", user='" + user + '\'' +
                ", stream=" + stream +
                ", responseMode='" + responseMode + '\'' +
                ", conversationId='" + conversationId + '\'' +
                ", files=" + (files != null ? files.length : 0) + " files" +
                '}';
    }
}