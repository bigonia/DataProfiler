package com.dataprofiler.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for Dify text_chunk event data
 * Represents streaming text content from LLM responses
 */
@Schema(description = "Dify Text Chunk Data")
public class DifyTextChunk {

    @Schema(description = "Text content chunk", example = "Hello")
    @JsonProperty("text")
    private String text;

    @Schema(description = "Chunk index in the stream")
    @JsonProperty("index")
    private Integer index;

    @Schema(description = "Delta content for incremental updates")
    @JsonProperty("delta")
    private String delta;

    @Schema(description = "Finish reason if this is the last chunk")
    @JsonProperty("finish_reason")
    private String finishReason;

    // Default constructor
    public DifyTextChunk() {}

    // Constructor with text
    public DifyTextChunk(String text) {
        this.text = text;
    }

    // Constructor with all fields
    public DifyTextChunk(String text, Integer index, String delta, String finishReason) {
        this.text = text;
        this.index = index;
        this.delta = delta;
        this.finishReason = finishReason;
    }

    // Getters and Setters
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getDelta() {
        return delta;
    }

    public void setDelta(String delta) {
        this.delta = delta;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    @Override
    public String toString() {
        return "DifyTextChunk{" +
                "text='" + text + '\'' +
                ", index=" + index +
                ", delta='" + delta + '\'' +
                ", finishReason='" + finishReason + '\'' +
                '}';
    }
}