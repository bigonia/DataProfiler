-- Migration script for multi-datasource support
-- This script updates the database schema to support one-to-many relationship
-- between ProfilingTask and DataSourceConfig

-- Step 1: Add new columns to profiling_task table
ALTER TABLE profiling_task 
ADD COLUMN total_data_sources INT DEFAULT 0,
ADD COLUMN processed_data_sources INT DEFAULT 0;

-- Step 2: Create junction table for many-to-many relationship
CREATE TABLE profiling_task_datasource (
    task_id BIGINT NOT NULL,
    datasource_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, datasource_id),
    CONSTRAINT fk_task_datasource_task 
        FOREIGN KEY (task_id) REFERENCES profiling_task(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_task_datasource_source 
        FOREIGN KEY (datasource_id) REFERENCES data_source_config(id) 
        ON DELETE CASCADE
);

-- Step 3: Create indexes for performance optimization
CREATE INDEX idx_profiling_task_datasource_task_id 
    ON profiling_task_datasource(task_id);
    
CREATE INDEX idx_profiling_task_datasource_datasource_id 
    ON profiling_task_datasource(datasource_id);

-- Step 4: Migrate existing data from old single datasource relationship
-- Insert existing relationships into the new junction table
INSERT INTO profiling_task_datasource (task_id, datasource_id)
SELECT pt.id, pt.data_source_id
FROM profiling_task pt
WHERE pt.data_source_id IS NOT NULL;

-- Step 5: Update progress tracking fields for existing tasks
UPDATE profiling_task 
SET total_data_sources = 1, 
    processed_data_sources = CASE 
        WHEN status IN ('COMPLETED', 'FAILED', 'TIMEOUT') THEN 1 
        ELSE 0 
    END
WHERE data_source_id IS NOT NULL;

-- Step 6: Handle tasks without data sources
UPDATE profiling_task 
SET total_data_sources = 0, 
    processed_data_sources = 0
WHERE data_source_id IS NULL;

-- Step 7: Add comments for documentation
COMMENT ON TABLE profiling_task_datasource IS 'Junction table for many-to-many relationship between profiling tasks and data source configurations';
COMMENT ON COLUMN profiling_task.total_data_sources IS 'Total number of data sources assigned to this profiling task';
COMMENT ON COLUMN profiling_task.processed_data_sources IS 'Number of data sources that have been processed (completed or failed)';

-- Step 8: Create view for backward compatibility (optional)
CREATE VIEW profiling_task_with_datasources AS
SELECT 
    pt.*,
    COUNT(ptds.datasource_id) as actual_datasource_count,
    GROUP_CONCAT(ptds.datasource_id) as datasource_ids
FROM profiling_task pt
LEFT JOIN profiling_task_datasource ptds ON pt.id = ptds.task_id
GROUP BY pt.id;

COMMENT ON VIEW profiling_task_with_datasources IS 'View providing profiling tasks with aggregated datasource information for reporting purposes';

-- Note: The old data_source_id column in profiling_task table is kept for backward compatibility
-- It can be removed in a future migration after ensuring all applications are updated
-- ALTER TABLE profiling_task DROP COLUMN data_source_id; -- Uncomment in future migration