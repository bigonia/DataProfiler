package com.dataprofiler.repository;

import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.entity.ProfilingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProfilingTask entity
 * Provides data access methods for profiling task management
 */
@Repository
public interface ProfilingTaskRepository extends JpaRepository<ProfilingTask, Long> {

    /**
     * Find task by unique task ID
     * @param taskId the unique task identifier
     * @return Optional containing the task if found
     */
    Optional<ProfilingTask> findByTaskId(String taskId);

    /**
     * Find tasks that contain the specified data source configuration
     * @param dataSourceConfig the data source configuration
     * @return list of tasks associated with the data source
     */
    @Query("SELECT t FROM ProfilingTask t JOIN t.dataSourceConfigs d WHERE d = :dataSourceConfig")
    List<ProfilingTask> findByDataSourceConfigsContaining(@Param("dataSourceConfig") DataSourceConfig dataSourceConfig);

    /**
     * Find tasks by data source ID
     * @param dataSourceId the data source identifier
     * @return list of tasks associated with the data source
     */
    @Query("SELECT t FROM ProfilingTask t JOIN t.dataSourceConfigs d WHERE d.sourceId = :dataSourceId")
    List<ProfilingTask> findByDataSourceId(@Param("dataSourceId") String dataSourceId);

    /**
     * Find tasks by status
     * @param status task status enum value
     * @return list of tasks with the specified status
     */
    List<ProfilingTask> findByStatus(ProfilingTask.TaskStatus status);

    /**
     * Get all profiling tasks
     * @return list of all profiling tasks
     */
    List<ProfilingTask> findAll();

    /**
     * Delete profiling task by ID
     * @param id task ID
     */
    void deleteById(Long id);

}