package com.dataprofiler.exception;

/**
 * Business logic exception
 * Used for handling business rule violations and domain-specific errors
 */
public class BusinessException extends RuntimeException {

    private String errorCode;
    private Object[] args;

    /**
     * Constructor with message only
     */
    public BusinessException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with error code and message
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with error code, message and cause
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Constructor with error code, message and arguments
     */
    public BusinessException(String errorCode, String message, Object... args) {
        super(message);
        this.errorCode = errorCode;
        this.args = args;
    }

    /**
     * Get error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Get arguments
     */
    public Object[] getArgs() {
        return args;
    }

    // Common business exception factory methods
    
    /**
     * Data source not found exception
     */
    public static BusinessException dataSourceNotFound(String dataSourceId) {
        return new BusinessException("DATA_SOURCE_NOT_FOUND", 
                "Data source not found: " + dataSourceId);
    }

    /**
     * Data source name already exists exception
     */
    public static BusinessException dataSourceNameExists(String name) {
        return new BusinessException("DATA_SOURCE_NAME_EXISTS", 
                "Data source name already exists: " + name);
    }

    /**
     * Data source connection failed exception
     */
    public static BusinessException dataSourceConnectionFailed(String dataSourceId, String reason) {
        return new BusinessException("DATA_SOURCE_CONNECTION_FAILED", 
                "Failed to connect to data source " + dataSourceId + ": " + reason);
    }

    /**
     * Profiling task not found exception
     */
    public static BusinessException profilingTaskNotFound(String taskId) {
        return new BusinessException("PROFILING_TASK_NOT_FOUND", 
                "Profiling task not found: " + taskId);
    }

    /**
     * Profiling task cannot be cancelled exception
     */
    public static BusinessException profilingTaskCannotBeCancelled(String taskId, String status) {
        return new BusinessException("PROFILING_TASK_CANNOT_BE_CANCELLED", 
                "Profiling task " + taskId + " cannot be cancelled in status: " + status);
    }

    /**
     * Report not found exception
     */
    public static BusinessException reportNotFound(String reportId) {
        return new BusinessException("REPORT_NOT_FOUND", 
                "Report not found: " + reportId);
    }

    /**
     * Invalid report format exception
     */
    public static BusinessException invalidReportFormat(String format) {
        return new BusinessException("INVALID_REPORT_FORMAT", 
                "Invalid report format: " + format);
    }

    /**
     * Invalid date range exception
     */
    public static BusinessException invalidDateRange(String startDate, String endDate) {
        return new BusinessException("INVALID_DATE_RANGE", 
                "Invalid date range: start date " + startDate + " is after end date " + endDate);
    }

    /**
     * Invalid pagination parameters exception
     */
    public static BusinessException invalidPaginationParameters(int page, int size) {
        return new BusinessException("INVALID_PAGINATION_PARAMETERS", 
                "Invalid pagination parameters: page=" + page + ", size=" + size);
    }

    /**
     * Resource limit exceeded exception
     */
    public static BusinessException resourceLimitExceeded(String resource, int limit) {
        return new BusinessException("RESOURCE_LIMIT_EXCEEDED", 
                "Resource limit exceeded for " + resource + ": " + limit);
    }

    /**
     * Operation not allowed exception
     */
    public static BusinessException operationNotAllowed(String operation, String reason) {
        return new BusinessException("OPERATION_NOT_ALLOWED", 
                "Operation " + operation + " not allowed: " + reason);
    }
}