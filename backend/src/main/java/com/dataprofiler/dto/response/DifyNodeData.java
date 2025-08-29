package com.dataprofiler.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for Dify node event data
 * Represents workflow node execution information
 */
@Schema(description = "Dify Node Execution Data")
public class DifyNodeData {

    @Schema(description = "Node ID", example = "node-123")
    @JsonProperty("id")
    private String id;

    @Schema(description = "Node title or name", example = "SQL Generation")
    @JsonProperty("title")
    private String title;

    @Schema(description = "Node type", example = "llm")
    @JsonProperty("node_type")
    private String nodeType;

    @Schema(description = "Node execution status", example = "running")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Node execution start time")
    @JsonProperty("created_at")
    private Long createdAt;

    @Schema(description = "Node execution finish time")
    @JsonProperty("finished_at")
    private Long finishedAt;

    @Schema(description = "Node execution duration in milliseconds")
    @JsonProperty("elapsed_time")
    private Long elapsedTime;

    @Schema(description = "Node execution result or output")
    @JsonProperty("outputs")
    private Object outputs;

    @Schema(description = "Error information if node failed")
    @JsonProperty("error")
    private String error;

    @Schema(description = "Node execution index")
    @JsonProperty("index")
    private Integer index;

    @Schema(description = "Node execution metadata")
    @JsonProperty("execution_metadata")
    private Object executionMetadata;

    @Schema(description = "Node process data")
    @JsonProperty("process_data")
    private java.util.Map<String, Object> processData;

    // Default constructor
    public DifyNodeData() {}

    // Constructor with basic fields
    public DifyNodeData(String id, String title, String nodeType) {
        this.id = id;
        this.title = title;
        this.nodeType = nodeType;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
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

    public Long getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Long finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getElapsedTime() {
        return elapsedTime;
    }

    public void setElapsedTime(Long elapsedTime) {
        this.elapsedTime = elapsedTime;
    }

    public Object getOutputs() {
        return outputs;
    }

    public void setOutputs(Object outputs) {
        this.outputs = outputs;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public Object getExecutionMetadata() {
        return executionMetadata;
    }

    public void setExecutionMetadata(Object executionMetadata) {
        this.executionMetadata = executionMetadata;
    }

    public java.util.Map<String, Object> getProcessData() {
        return processData;
    }

    public void setProcessData(java.util.Map<String, Object> processData) {
        this.processData = processData;
    }

    @Override
    public String toString() {
        return "DifyNodeData{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", nodeType='" + nodeType + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", finishedAt=" + finishedAt +
                ", elapsedTime=" + elapsedTime +
                ", outputs=" + outputs +
                ", error='" + error + '\'' +
                '}';
    }
}