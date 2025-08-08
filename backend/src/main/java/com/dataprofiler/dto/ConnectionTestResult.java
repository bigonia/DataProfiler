package com.dataprofiler.dto;

/**
 * Data Transfer Object for connection test results
 * Encapsulates the result of testing a data source connection
 */
public class ConnectionTestResult {
    
    private boolean success;
    private String message;
    private long durationMs;
    
    // Default constructor
    public ConnectionTestResult() {}
    
    // Constructor with all fields
    public ConnectionTestResult(boolean success, String message, long durationMs) {
        this.success = success;
        this.message = message;
        this.durationMs = durationMs;
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public long getDurationMs() {
        return durationMs;
    }
    
    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }
    
    @Override
    public String toString() {
        return "ConnectionTestResult{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", durationMs=" + durationMs +
                '}';
    }
}