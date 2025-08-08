package com.dataprofiler.exception;

/**
 * Data access exception
 * Used for handling database and data persistence related errors
 */
public class DataAccessException extends RuntimeException {

    private String operation;
    private String entityType;

    /**
     * Constructor with message only
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Constructor with message and cause
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with operation, entity type and message
     */
    public DataAccessException(String operation, String entityType, String message) {
        super(String.format("Failed to %s %s: %s", operation, entityType, message));
        this.operation = operation;
        this.entityType = entityType;
    }

    /**
     * Constructor with operation, entity type, message and cause
     */
    public DataAccessException(String operation, String entityType, String message, Throwable cause) {
        super(String.format("Failed to %s %s: %s", operation, entityType, message), cause);
        this.operation = operation;
        this.entityType = entityType;
    }

    /**
     * Get operation
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Get entity type
     */
    public String getEntityType() {
        return entityType;
    }

    // Factory methods for common data access operations

    /**
     * Database connection failed
     */
    public static DataAccessException connectionFailed(String dataSourceId, Throwable cause) {
        return new DataAccessException("connect to", "database", 
                "Data source: " + dataSourceId, cause);
    }

    /**
     * Query execution failed
     */
    public static DataAccessException queryFailed(String query, Throwable cause) {
        return new DataAccessException("execute", "query", 
                "SQL: " + query, cause);
    }

    /**
     * Entity save failed
     */
    public static DataAccessException saveFailed(String entityType, Throwable cause) {
        return new DataAccessException("save", entityType, 
                "Database operation failed", cause);
    }

    /**
     * Entity update failed
     */
    public static DataAccessException updateFailed(String entityType, String entityId, Throwable cause) {
        return new DataAccessException("update", entityType, 
                "Entity ID: " + entityId, cause);
    }

    /**
     * Entity delete failed
     */
    public static DataAccessException deleteFailed(String entityType, String entityId, Throwable cause) {
        return new DataAccessException("delete", entityType, 
                "Entity ID: " + entityId, cause);
    }

    /**
     * Entity find failed
     */
    public static DataAccessException findFailed(String entityType, String criteria, Throwable cause) {
        return new DataAccessException("find", entityType, 
                "Criteria: " + criteria, cause);
    }

    /**
     * Transaction failed
     */
    public static DataAccessException transactionFailed(String operation, Throwable cause) {
        return new DataAccessException("execute", "transaction", 
                "Operation: " + operation, cause);
    }

    /**
     * Schema validation failed
     */
    public static DataAccessException schemaValidationFailed(String schemaName, String reason) {
        return new DataAccessException("validate", "schema", 
                "Schema: " + schemaName + ", Reason: " + reason);
    }

    /**
     * Data integrity violation
     */
    public static DataAccessException integrityViolation(String constraint, Throwable cause) {
        return new DataAccessException("maintain", "data integrity", 
                "Constraint: " + constraint, cause);
    }

    /**
     * Optimistic locking failure
     */
    public static DataAccessException optimisticLockingFailure(String entityType, String entityId) {
        return new DataAccessException("update", entityType, 
                "Optimistic locking failure for entity ID: " + entityId);
    }

    /**
     * Timeout exception
     */
    public static DataAccessException timeout(String operation, int timeoutSeconds) {
        return new DataAccessException("complete", "operation", 
                "Operation: " + operation + ", Timeout: " + timeoutSeconds + " seconds");
    }

    /**
     * Generic data access error with custom message
     */
    public static DataAccessException withMessage(String message, Throwable cause) {
        return new DataAccessException(message, cause);
    }
}