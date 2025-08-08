package com.dataprofiler.exception;

/**
 * Profiling task exception
 * Used for handling errors during profiling task execution
 */
public class ProfilingTaskException extends RuntimeException {

    private String taskId;
    private String phase;
    private String errorCode;

    /**
     * Constructor with message only
     */
    public ProfilingTaskException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     */
    public ProfilingTaskException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with task ID and message
     */
    public ProfilingTaskException(String taskId, String message) {
        super(String.format("Profiling task %s failed: %s", taskId, message));
        this.taskId = taskId;
    }

    /**
     * Constructor with task ID, message and cause
     */
    public ProfilingTaskException(String taskId, String message, Throwable cause) {
        super(String.format("Profiling task %s failed: %s", taskId, message), cause);
        this.taskId = taskId;
    }

    /**
     * Constructor with task ID, phase and message
     */
    public ProfilingTaskException(String taskId, String phase, String message) {
        super(String.format("Profiling task %s failed in phase %s: %s", taskId, phase, message));
        this.taskId = taskId;
        this.phase = phase;
    }

    /**
     * Constructor with task ID, phase, message and cause
     */
    public ProfilingTaskException(String taskId, String phase, String message, Throwable cause) {
        super(String.format("Profiling task %s failed in phase %s: %s", taskId, phase, message), cause);
        this.taskId = taskId;
        this.phase = phase;
    }

    /**
     * Constructor with all parameters
     */
    public ProfilingTaskException(String taskId, String phase, String errorCode, String message, Throwable cause) {
        super(String.format("Profiling task %s failed in phase %s: %s", taskId, phase, message), cause);
        this.taskId = taskId;
        this.phase = phase;
        this.errorCode = errorCode;
    }

    /**
     * Get task ID
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Get phase
     */
    public String getPhase() {
        return phase;
    }

    /**
     * Get error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    // Factory methods for common profiling task errors

    /**
     * Task initialization failed
     */
    public static ProfilingTaskException initializationFailed(String taskId, Throwable cause) {
        return new ProfilingTaskException(taskId, "INITIALIZATION", 
                "Failed to initialize profiling task", cause);
    }

    /**
     * Data source connection failed during profiling
     */
    public static ProfilingTaskException dataSourceConnectionFailed(String taskId, String dataSourceId, Throwable cause) {
        return new ProfilingTaskException(taskId, "CONNECTION", 
                "Failed to connect to data source: " + dataSourceId, cause);
    }

    /**
     * Schema discovery failed
     */
    public static ProfilingTaskException schemaDiscoveryFailed(String taskId, String dataSourceId, Throwable cause) {
        return new ProfilingTaskException(taskId, "SCHEMA_DISCOVERY", 
                "Failed to discover schema for data source: " + dataSourceId, cause);
    }

    /**
     * Table profiling failed
     */
    public static ProfilingTaskException tableProfilingFailed(String taskId, String tableName, Throwable cause) {
        return new ProfilingTaskException(taskId, "TABLE_PROFILING", 
                "Failed to profile table: " + tableName, cause);
    }

    /**
     * Column profiling failed
     */
    public static ProfilingTaskException columnProfilingFailed(String taskId, String columnName, Throwable cause) {
        return new ProfilingTaskException(taskId, "COLUMN_PROFILING", 
                "Failed to profile column: " + columnName, cause);
    }

    /**
     * Report generation failed
     */
    public static ProfilingTaskException reportGenerationFailed(String taskId, Throwable cause) {
        return new ProfilingTaskException(taskId, "REPORT_GENERATION", 
                "Failed to generate profiling report", cause);
    }

    /**
     * Report persistence failed
     */
    public static ProfilingTaskException reportPersistenceFailed(String taskId, Throwable cause) {
        return new ProfilingTaskException(taskId, "REPORT_PERSISTENCE", 
                "Failed to persist profiling report", cause);
    }

    /**
     * Task timeout
     */
    public static ProfilingTaskException taskTimeout(String taskId, long timeoutMinutes) {
        return new ProfilingTaskException(taskId, "TIMEOUT", 
                "Task timed out after " + timeoutMinutes + " minutes");
    }

    /**
     * Task cancelled
     */
    public static ProfilingTaskException taskCancelled(String taskId) {
        return new ProfilingTaskException(taskId, "CANCELLATION", 
                "Task was cancelled by user or system");
    }

    /**
     * Insufficient resources
     */
    public static ProfilingTaskException insufficientResources(String taskId, String resource) {
        return new ProfilingTaskException(taskId, "RESOURCE_LIMITATION", 
                "Insufficient resources: " + resource);
    }

    /**
     * Invalid configuration
     */
    public static ProfilingTaskException invalidConfiguration(String taskId, String configError) {
        return new ProfilingTaskException(taskId, "CONFIGURATION", 
                "Invalid configuration: " + configError);
    }

    /**
     * Data sampling failed
     */
    public static ProfilingTaskException dataSamplingFailed(String taskId, String tableName, Throwable cause) {
        return new ProfilingTaskException(taskId, "DATA_SAMPLING", 
                "Failed to sample data from table: " + tableName, cause);
    }

    /**
     * Statistics calculation failed
     */
    public static ProfilingTaskException statisticsCalculationFailed(String taskId, String columnName, Throwable cause) {
        return new ProfilingTaskException(taskId, "STATISTICS_CALCULATION", 
                "Failed to calculate statistics for column: " + columnName, cause);
    }

    /**
     * Generic profiling error with custom message
     */
    public static ProfilingTaskException withMessage(String taskId, String message, Throwable cause) {
        return new ProfilingTaskException(taskId, message, cause);
    }
}