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
     * Find task by unique task ID
     * @param id the unique task identifier
     * @return Optional containing the task if found
     */
    Optional<ProfilingTask> findById(Long id);


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