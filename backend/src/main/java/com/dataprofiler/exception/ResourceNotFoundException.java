package com.dataprofiler.exception;

/**
 * Resource not found exception
 * Used when a requested resource cannot be found
 */
public class ResourceNotFoundException extends RuntimeException {

    private String resourceType;
    private String resourceId;

    /**
     * Constructor with message only
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     */
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with resource type and ID
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found with id: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Constructor with resource type, ID and cause
     */
    public ResourceNotFoundException(String resourceType, String resourceId, Throwable cause) {
        super(String.format("%s not found with id: %s", resourceType, resourceId), cause);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Get resource type
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Get resource ID
     */
    public String getResourceId() {
        return resourceId;
    }

    // Factory methods for common resources

    /**
     * Data source not found
     */
    public static ResourceNotFoundException dataSource(String dataSourceId) {
        return new ResourceNotFoundException("DataSource", dataSourceId);
    }

    /**
     * Profiling task not found
     */
    public static ResourceNotFoundException profilingTask(String taskId) {
        return new ResourceNotFoundException("ProfilingTask", taskId);
    }

    /**
     * Structured report not found
     */
    public static ResourceNotFoundException structuredReport(String reportId) {
        return new ResourceNotFoundException("StructuredReport", reportId);
    }

    /**
     * Schema not found
     */
    public static ResourceNotFoundException schema(String schemaName) {
        return new ResourceNotFoundException("Schema", schemaName);
    }

    /**
     * Table not found
     */
    public static ResourceNotFoundException table(String tableName) {
        return new ResourceNotFoundException("Table", tableName);
    }

    /**
     * Column not found
     */
    public static ResourceNotFoundException column(String columnName) {
        return new ResourceNotFoundException("Column", columnName);
    }

    /**
     * Generic resource not found with custom message
     */
    public static ResourceNotFoundException withMessage(String message) {
        return new ResourceNotFoundException(message);
    }
}