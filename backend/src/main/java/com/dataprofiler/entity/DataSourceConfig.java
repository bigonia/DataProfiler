package com.dataprofiler.entity;

import com.dataprofiler.utils.JpaMapConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity representing a data source configuration
 * Supports multiple data source types including databases and files
 */
@Entity
@Table(name = "data_sources")
@Data
public class DataSourceConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Data source name is required")
    @Column(nullable = false)
    private String name;

    @Column(name = "source_id", unique = true, length = 50)
    private String sourceId;

    @NotNull(message = "Data source type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataSourceType type;

    /**
     * Dynamic properties stored as JSON
     * Content varies based on data source type:
     * - For databases: host, port, username, password, database, schema
     * - For files: originalFileName, internalFileId, fileSize, mimeType
     */
    @Column(nullable = false)
    @Convert(converter = JpaMapConverter.class)
    private Map<String, String> properties;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // Constructors
    public DataSourceConfig() {}

    public DataSourceConfig(String name, DataSourceType type, Map<String, String> properties) {
        this.name = name;
        this.type = type;
        this.properties = properties;
    }

    // Convenience methods for database properties
    public String getHost() {
        return properties != null ? properties.get("host") : null;
    }

    public Integer getPort() {
        String portStr = properties != null ? properties.get("port") : null;
        return portStr != null ? Integer.parseInt(portStr) : null;
    }

    public String getUsername() {
        return properties != null ? properties.get("username") : null;
    }

    public String getPassword() {
        return properties != null ? properties.get("password") : null;
    }

    public String getDatabaseName() {
        return properties != null ? properties.get("database") : null;
    }

    public String getConnectionUrl() {
        return properties != null ? properties.get("connectionUrl") : null;
    }

    /**
     * Enum for supported data source types
     */
    public enum DataSourceType {
        MYSQL,
        POSTGRESQL,
        SQLSERVER,
        SQLITE,
        ORACLE,
        FILE
    }
}