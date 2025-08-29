package com.dataprofiler.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic SSE event container for Dify streaming responses
 * Encapsulates the parsed event type and data from Dify's SSE stream
 */
@Schema(description = "Dify SSE Event Container")
public class DifySseEvent {

    @Schema(description = "Event type from Dify SSE stream", example = "text_chunk")
    @JsonProperty("event")
    private String event;

    @Schema(description = "Event data payload")
    @JsonProperty("data")
    private Object data;

    @Schema(description = "Raw SSE line for debugging")
    @JsonProperty("raw")
    private String raw;

    // Default constructor
    public DifySseEvent() {}

    // Constructor with event and data
    public DifySseEvent(String event, Object data) {
        this.event = event;
        this.data = data;
    }

    // Constructor with all fields
    public DifySseEvent(String event, Object data, String raw) {
        this.event = event;
        this.data = data;
        this.raw = raw;
    }

    // Getters and Setters
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getRaw() {
        return raw;
    }

    public void setRaw(String raw) {
        this.raw = raw;
    }

    @Override
    public String toString() {
        return "DifySseEvent{" +
                "event='" + event + '\'' +
                ", data=" + data +
                ", raw='" + raw + '\'' +
                '}';
    }
}