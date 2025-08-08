package com.dataprofiler.exception;

/**
 * Custom exception for profiling operations
 * Provides specific error handling for data profiling tasks
 */
public class ProfilingException extends RuntimeException {

    private final String errorCode;
    private final String dataSourceId;

    public ProfilingException(String message) {
        super(message);
        this.errorCode = "PROFILING_ERROR";
        this.dataSourceId = null;
    }

    public ProfilingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "PROFILING_ERROR";
        this.dataSourceId = null;
    }

    public ProfilingException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.dataSourceId = null;
    }

    public ProfilingException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.dataSourceId = null;
    }

    public ProfilingException(String errorCode, String message, String dataSourceId) {
        super(message);
        this.errorCode = errorCode;
        this.dataSourceId = dataSourceId;
    }

    public ProfilingException(String errorCode, String message, String dataSourceId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.dataSourceId = dataSourceId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    /**
     * Common error codes for profiling operations
     */
    public static class ErrorCodes {
        public static final String CONNECTION_FAILED = "CONNECTION_FAILED";
        public static final String UNSUPPORTED_DATABASE = "UNSUPPORTED_DATABASE";
        public static final String INVALID_CONFIGURATION = "INVALID_CONFIGURATION";
        public static final String PROFILING_TIMEOUT = "PROFILING_TIMEOUT";
        public static final String INSUFFICIENT_PERMISSIONS = "INSUFFICIENT_PERMISSIONS";
        public static final String TABLE_NOT_FOUND = "TABLE_NOT_FOUND";
        public static final String SCHEMA_NOT_FOUND = "SCHEMA_NOT_FOUND";
        public static final String TASK_EXECUTION_FAILED = "TASK_EXECUTION_FAILED";
        public static final String REPORT_GENERATION_FAILED = "REPORT_GENERATION_FAILED";
    }

    /**
     * Factory methods for common profiling exceptions
     */
    public static ProfilingException connectionFailed(String dataSourceId, Throwable cause) {
        return new ProfilingException(
            ErrorCodes.CONNECTION_FAILED,
            "Failed to connect to data source: " + dataSourceId,
            dataSourceId,
            cause
        );
    }

    public static ProfilingException unsupportedDatabase(String dataSourceType) {
        return new ProfilingException(
            ErrorCodes.UNSUPPORTED_DATABASE,
            "Unsupported database type: " + dataSourceType
        );
    }

    public static ProfilingException invalidConfiguration(String message) {
        return new ProfilingException(
            ErrorCodes.INVALID_CONFIGURATION,
            "Invalid profiling configuration: " + message
        );
    }

    public static ProfilingException profilingTimeout(String dataSourceId, long timeoutMinutes) {
        return new ProfilingException(
            ErrorCodes.PROFILING_TIMEOUT,
            "Profiling task timed out after " + timeoutMinutes + " minutes",
            dataSourceId
        );
    }

    public static ProfilingException insufficientPermissions(String dataSourceId, String operation) {
        return new ProfilingException(
            ErrorCodes.INSUFFICIENT_PERMISSIONS,
            "Insufficient permissions for operation: " + operation,
            dataSourceId
        );
    }

    public static ProfilingException tableNotFound(String dataSourceId, String tableName) {
        return new ProfilingException(
            ErrorCodes.TABLE_NOT_FOUND,
            "Table not found: " + tableName,
            dataSourceId
        );
    }

    public static ProfilingException schemaNotFound(String dataSourceId, String schemaName) {
        return new ProfilingException(
            ErrorCodes.SCHEMA_NOT_FOUND,
            "Schema not found: " + schemaName,
            dataSourceId
        );
    }

    public static ProfilingException taskExecutionFailed(String taskId, Throwable cause) {
        return new ProfilingException(
            ErrorCodes.TASK_EXECUTION_FAILED,
            "Task execution failed: " + taskId,
            null,
            cause
        );
    }

    public static ProfilingException reportGenerationFailed(String taskId, Throwable cause) {
        return new ProfilingException(
            ErrorCodes.REPORT_GENERATION_FAILED,
            "Report generation failed for task: " + taskId,
            null,
            cause
        );
    }
}