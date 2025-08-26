-- MySQL Database Initialization Script for Data Profiler Application
-- Generated based on JPA entity classes
-- Database: dbcrawler
-- Charset: utf8mb4
-- Collation: utf8mb4_unicode_ci

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS `dbcrawler` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `dbcrawler`;

-- ========================================
-- Table: data_sources
-- Purpose: Store data source configuration information
-- ========================================
CREATE TABLE IF NOT EXISTS `data_sources` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `source_id` VARCHAR(255) NOT NULL COMMENT 'Unique identifier for the data source',
    `name` VARCHAR(255) NOT NULL COMMENT 'Display name of the data source',
    `type` ENUM('MYSQL', 'POSTGRESQL', 'SQLSERVER', 'SQLITE', 'ORACLE', 'FILE') NOT NULL COMMENT 'Type of data source',
    `properties` JSON COMMENT 'Dynamic properties stored as JSON (host, port, database, username, password, etc.)',
    `active` BOOLEAN DEFAULT TRUE COMMENT 'Whether the data source is active',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation timestamp',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_data_sources_source_id` (`source_id`),
    KEY `idx_data_sources_type` (`type`),
    KEY `idx_data_sources_active` (`active`),
    KEY `idx_data_sources_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Data source configuration table';

-- ========================================
-- Table: profiling_tasks
-- Purpose: Store profiling task information
-- ========================================
CREATE TABLE IF NOT EXISTS `profiling_tasks` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(255) NOT NULL COMMENT 'Unique task identifier',
    `name` VARCHAR(255) COMMENT 'Task name',
    `description` TEXT COMMENT 'Task description',
    `status` ENUM('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'TIMEOUT') NOT NULL DEFAULT 'PENDING' COMMENT 'Task execution status',
    `info` TEXT COMMENT 'Task execution information and logs',
    `total_data_sources` INT DEFAULT 0 COMMENT 'Total number of data sources to process',
    `processed_data_sources` INT DEFAULT 0 COMMENT 'Number of processed data sources',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Task creation timestamp',
    `started_at` TIMESTAMP NULL COMMENT 'Task start timestamp',
    `completed_at` TIMESTAMP NULL COMMENT 'Task completion timestamp',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_profiling_tasks_task_id` (`task_id`),
    KEY `idx_profiling_tasks_status` (`status`),
    KEY `idx_profiling_tasks_created_at` (`created_at`),
    KEY `idx_profiling_tasks_completed_at` (`completed_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Profiling task management table';

-- ========================================
-- Table: task_data_sources
-- Purpose: Many-to-many relationship between tasks and data sources
-- ========================================
CREATE TABLE IF NOT EXISTS `task_data_sources` (
    `task_id` BIGINT NOT NULL COMMENT 'Reference to profiling_tasks.id',
    `data_source_id` BIGINT NOT NULL COMMENT 'Reference to data_sources.id',
    PRIMARY KEY (`task_id`, `data_source_id`),
    KEY `idx_task_data_sources_task_id` (`task_id`),
    KEY `idx_task_data_sources_data_source_id` (`data_source_id`),
    CONSTRAINT `fk_task_data_sources_task` FOREIGN KEY (`task_id`) REFERENCES `profiling_tasks` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_task_data_sources_data_source` FOREIGN KEY (`data_source_id`) REFERENCES `data_sources` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Task and data source relationship table';

-- ========================================
-- Table: structured_reports
-- Purpose: Store structured analysis reports
-- ========================================
CREATE TABLE IF NOT EXISTS `structured_reports` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(255) NOT NULL COMMENT 'Associated task identifier',
    `data_source_id` BIGINT NOT NULL COMMENT 'Associated data source ID',
    `report_data` JSON COMMENT 'Report content stored as JSON',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Report creation timestamp',
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update timestamp',
    PRIMARY KEY (`id`),
    KEY `idx_structured_reports_task_id` (`task_id`),
    KEY `idx_structured_reports_data_source_id` (`data_source_id`),
    KEY `idx_structured_reports_created_at` (`created_at`),
    CONSTRAINT `fk_structured_reports_data_source` FOREIGN KEY (`data_source_id`) REFERENCES `data_sources` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Structured analysis reports table';

-- ========================================
-- Table: file_metadata
-- Purpose: Store file upload and conversion metadata
-- ========================================
CREATE TABLE IF NOT EXISTS `file_metadata` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `filename` VARCHAR(255) NOT NULL COMMENT 'Current filename',
    `original_filename` VARCHAR(255) NOT NULL COMMENT 'Original uploaded filename',
    `file_path` VARCHAR(500) NOT NULL COMMENT 'File storage path',
    `file_size` BIGINT NOT NULL COMMENT 'File size in bytes',
    `mime_type` VARCHAR(100) COMMENT 'MIME type of the file',
    `uploaded_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'File upload timestamp',
    `converted_at` TIMESTAMP NULL COMMENT 'File conversion timestamp',
    `converted` BOOLEAN DEFAULT FALSE COMMENT 'Whether file has been converted',
    `conversion_error` TEXT COMMENT 'Conversion error message if any',
    `table_count` INT DEFAULT 0 COMMENT 'Number of tables/sheets in the file',
    `data_source_id` BIGINT COMMENT 'Associated data source ID after conversion',
    `description` TEXT COMMENT 'File description',
    PRIMARY KEY (`id`),
    KEY `idx_file_metadata_filename` (`filename`),
    KEY `idx_file_metadata_uploaded_at` (`uploaded_at`),
    KEY `idx_file_metadata_converted` (`converted`),
    KEY `idx_file_metadata_data_source_id` (`data_source_id`),
    CONSTRAINT `fk_file_metadata_data_source` FOREIGN KEY (`data_source_id`) REFERENCES `data_sources` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='File metadata and conversion tracking table';

-- ========================================
-- Sample Data (Optional)
-- ========================================

-- Insert sample data sources
INSERT IGNORE INTO `data_sources` (`source_id`, `name`, `type`, `properties`, `active`) VALUES
('mysql-sample-01', 'Sample MySQL Database', 'MYSQL', JSON_OBJECT(
    'host', 'localhost',
    'port', 3306,
    'database', 'sample_db',
    'username', 'root',
    'password', 'password'
), TRUE),
('postgresql-sample-01', 'Sample PostgreSQL Database', 'POSTGRESQL', JSON_OBJECT(
    'host', 'localhost',
    'port', 5432,
    'database', 'sample_db',
    'username', 'postgres',
    'password', 'password'
), TRUE),
('sqlite-sample-01', 'Sample SQLite Database', 'SQLITE', JSON_OBJECT(
    'path', './data/sample.db'
), TRUE);

-- ========================================
-- Indexes for Performance Optimization
-- ========================================

-- Additional composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS `idx_profiling_tasks_status_created` ON `profiling_tasks` (`status`, `created_at`);
CREATE INDEX IF NOT EXISTS `idx_structured_reports_task_datasource` ON `structured_reports` (`task_id`, `data_source_id`);
CREATE INDEX IF NOT EXISTS `idx_file_metadata_converted_uploaded` ON `file_metadata` (`converted`, `uploaded_at`);

-- ========================================
-- Views for Common Queries
-- ========================================

-- View for task summary with data source count
CREATE OR REPLACE VIEW `v_task_summary` AS
SELECT 
    pt.id,
    pt.task_id,
    pt.name,
    pt.status,
    pt.total_data_sources,
    pt.processed_data_sources,
    pt.created_at,
    pt.started_at,
    pt.completed_at,
    CASE 
        WHEN pt.completed_at IS NOT NULL AND pt.started_at IS NOT NULL 
        THEN TIMESTAMPDIFF(SECOND, pt.started_at, pt.completed_at)
        ELSE NULL 
    END AS duration_seconds,
    COUNT(sr.id) as report_count
FROM `profiling_tasks` pt
LEFT JOIN `structured_reports` sr ON pt.task_id = sr.task_id
GROUP BY pt.id, pt.task_id, pt.name, pt.status, pt.total_data_sources, 
         pt.processed_data_sources, pt.created_at, pt.started_at, pt.completed_at;

-- View for data source usage statistics
CREATE OR REPLACE VIEW `v_data_source_stats` AS
SELECT 
    ds.id,
    ds.source_id,
    ds.name,
    ds.type,
    ds.active,
    ds.created_at,
    COUNT(DISTINCT tds.task_id) as task_count,
    COUNT(sr.id) as report_count,
    MAX(sr.created_at) as last_report_date
FROM `data_sources` ds
LEFT JOIN `task_data_sources` tds ON ds.id = tds.data_source_id
LEFT JOIN `structured_reports` sr ON ds.id = sr.data_source_id
GROUP BY ds.id, ds.source_id, ds.name, ds.type, ds.active, ds.created_at;

-- ========================================
-- Stored Procedures for Common Operations
-- ========================================

DELIMITER //

-- Procedure to clean up old completed tasks and reports
CREATE PROCEDURE IF NOT EXISTS `sp_cleanup_old_data`(
    IN task_retention_days INT DEFAULT 30,
    IN report_retention_days INT DEFAULT 90
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;
    
    START TRANSACTION;
    
    -- Delete old reports
    DELETE FROM `structured_reports` 
    WHERE `created_at` < DATE_SUB(NOW(), INTERVAL report_retention_days DAY);
    
    -- Delete old completed/failed tasks
    DELETE FROM `profiling_tasks` 
    WHERE `status` IN ('COMPLETED', 'FAILED', 'CANCELLED') 
    AND `completed_at` < DATE_SUB(NOW(), INTERVAL task_retention_days DAY);
    
    COMMIT;
END //

-- Procedure to get task statistics
CREATE PROCEDURE IF NOT EXISTS `sp_get_task_statistics`()
BEGIN
    SELECT 
        COUNT(*) as total_tasks,
        SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending_tasks,
        SUM(CASE WHEN status = 'RUNNING' THEN 1 ELSE 0 END) as running_tasks,
        SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks,
        SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) as failed_tasks,
        AVG(CASE 
            WHEN completed_at IS NOT NULL AND started_at IS NOT NULL 
            THEN TIMESTAMPDIFF(SECOND, started_at, completed_at) 
            ELSE NULL 
        END) as avg_duration_seconds
    FROM `profiling_tasks`
    WHERE `created_at` >= DATE_SUB(NOW(), INTERVAL 30 DAY);
END //

DELIMITER ;

-- ========================================
-- Triggers for Audit and Data Integrity
-- ========================================

-- Trigger to update processed_data_sources count when reports are created
DELIMITER //
CREATE TRIGGER IF NOT EXISTS `tr_update_task_progress`
AFTER INSERT ON `structured_reports`
FOR EACH ROW
BEGIN
    UPDATE `profiling_tasks` 
    SET `processed_data_sources` = (
        SELECT COUNT(DISTINCT data_source_id) 
        FROM `structured_reports` 
        WHERE task_id = NEW.task_id
    )
    WHERE `task_id` = NEW.task_id;
END //
DELIMITER ;

-- ========================================
-- Grant Permissions (Adjust as needed)
-- ========================================

-- Create application user (uncomment and modify as needed)
-- CREATE USER IF NOT EXISTS 'dbcrawler_app'@'localhost' IDENTIFIED BY 'secure_password';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON `dbcrawler`.* TO 'dbcrawler_app'@'localhost';
-- GRANT EXECUTE ON PROCEDURE `dbcrawler`.`sp_cleanup_old_data` TO 'dbcrawler_app'@'localhost';
-- GRANT EXECUTE ON PROCEDURE `dbcrawler`.`sp_get_task_statistics` TO 'dbcrawler_app'@'localhost';
-- FLUSH PRIVILEGES;

-- ========================================
-- Optimization Notes
-- ========================================
/*
Performance Optimization Recommendations:

1. Indexing Strategy:
   - Primary keys are automatically indexed
   - Foreign keys have dedicated indexes
   - Composite indexes for common query patterns
   - Consider partitioning for large tables (structured_reports, file_metadata)

2. JSON Column Optimization:
   - Use JSON_EXTRACT() for querying JSON fields
   - Consider virtual columns for frequently queried JSON attributes
   - Example: ALTER TABLE data_sources ADD COLUMN host VARCHAR(255) AS (JSON_UNQUOTE(JSON_EXTRACT(properties, '$.host'))) VIRTUAL;

3. Data Archiving:
   - Implement regular cleanup procedures
   - Consider separate archive tables for historical data
   - Use the provided sp_cleanup_old_data procedure

4. Connection Pool Optimization:
   - Current Hikari settings in application.yml are reasonable
   - Monitor connection usage and adjust pool size as needed
   - Consider read replicas for reporting queries

5. Query Optimization:
   - Use the provided views for common aggregations
   - Implement proper pagination for large result sets
   - Monitor slow query log and optimize accordingly
*/

-- Script completed successfully
SELECT 'MySQL Database Initialization Completed Successfully' AS status;