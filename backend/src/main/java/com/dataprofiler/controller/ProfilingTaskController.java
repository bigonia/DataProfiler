package com.dataprofiler.controller;

import com.dataprofiler.dto.request.ProfilingTaskRequest;
import com.dataprofiler.dto.response.TaskStatusResponse;
import com.dataprofiler.entity.ProfilingTask;
import com.dataprofiler.service.ProfilingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for profiling task operations
 * Provides endpoints for starting profiling tasks and checking their status
 */
@RestController
@RequestMapping("/api/profiling")
@Validated
@Tag(name = "Profiling Tasks", description = "API for managing data profiling tasks")
public class ProfilingTaskController {

    private static final Logger logger = LoggerFactory.getLogger(ProfilingTaskController.class);

    @Autowired
    private ProfilingService profilingService;

    /**
     * Start a new profiling task
     */
    @PostMapping("/profiling-tasks")
    @Operation(
            summary = "Start a new profiling task",
            description = "Creates and starts a new data profiling task for one or more data sources. The task runs asynchronously."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Profiling task created successfully",
                    content = @Content(schema = @Schema(implementation = ProfilingTask.class))
            )
    })
    public ResponseEntity<?> startProfilingTask(
            @Valid @RequestBody ProfilingTaskRequest request) {

        logger.info("Received profiling task request for {} data sources",
                request.getDatasources() != null ? request.getDatasources().size() : 0);

        try {
            ProfilingTask task = profilingService.startProfilingTask(request);

            logger.info("Profiling task started successfully with taskId: {}", task.getTaskId());
            return ResponseEntity.status(HttpStatus.CREATED).body(task);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid profiling task request: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Failed to start profiling task", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start profiling task: " + e.getMessage());
        }
    }

    @GetMapping("/profiling-tasks/list")
    @Operation(
            summary = "获取所有分析任务",
            description = "获取所有分析任务"
    )
    public ResponseEntity<?> getAllProfilingTask() {

        logger.info("获取所有分析任务请求");

        try {
            return ResponseEntity.ok(profilingService.getAllProfilingTasks());
        } catch (Exception e) {
            logger.error("获取所有分析任务失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取所有分析任务失败: " + e.getMessage());
        }
    }

    /**
     * delete profiling task
     */
    /**
     * 删除分析任务
     */
    @DeleteMapping("/profiling-tasks/{id}")
    @Operation(
            summary = "删除分析任务",
            description = "根据任务ID删除指定的分析任务"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "任务删除成功",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "任务未找到",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    public ResponseEntity<?> deleteProfilingTask(
            @Parameter(description = "任务唯一标识", required = true)
            @PathVariable  Long id) {

        logger.info("删除分析任务请求，任务ID: {}", id);

        try {
            profilingService.deleteTask(id);
            logger.info("分析任务删除成功，任务ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("无效的任务ID: {}", id);
            return ResponseEntity.badRequest().body("无效的任务ID: " + e.getMessage());
        } catch (Exception e) {
            logger.error("删除分析任务失败，任务ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除分析任务失败: " + e.getMessage());
        }
    }


    @GetMapping("/task-status/{id}")
    @Operation(
            summary = "Get task status",
            description = "Retrieves the current status and progress information of a profiling task"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task status retrieved successfully",
                    content = @Content(schema = @Schema(implementation = TaskStatusResponse.class))
            )
    })
    public ResponseEntity<?> getTaskStatus(
            @Parameter(description = "Unique task identifier", required = true)
            @PathVariable Long id) {

        logger.debug("Getting status for task: {}", id);

        try {
            ProfilingTask task = profilingService.getTask(id);

            if (task != null) {
                // Convert to DTO to avoid lazy loading issues
                TaskStatusResponse response = new TaskStatusResponse();
                response.setTaskId(task.getTaskId());
                response.setStatus(task.getStatus());
                response.setInfo(task.getInfo());
                response.setCreatedAt(task.getCreatedAt());
                response.setCompletedAt(task.getCompletedAt());
                response.setTotalDataSources(task.getTotalDataSources());
                response.setProcessedDataSources(task.getProcessedDataSources());
                response.setName(task.getName());
                response.setDescription(task.getDescription());
                
                // Fill data source information
                if (task.getDataSourceConfigs() != null && !task.getDataSourceConfigs().isEmpty()) {
                    List<TaskStatusResponse.DataSourceInfo> dataSourceInfos = task.getDataSourceConfigs().stream()
                            .map(ds -> {
                                TaskStatusResponse.DataSourceInfo info = new TaskStatusResponse.DataSourceInfo();
                                info.setId(ds.getId());
                                info.setSourceId(ds.getSourceId());
                                info.setName(ds.getName());
                                info.setType(ds.getType());
//                                info.setActive(ds.getActive());
                                info.setCreatedAt(ds.getCreatedAt());
                                return info;
                            })
                            .collect(Collectors.toList());
                    response.setDataSources(dataSourceInfos);
                }
                
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Task not found: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid task ID: {}", id);
            return ResponseEntity.badRequest().body("Invalid task ID: " + e.getMessage());

        } catch (Exception e) {
            logger.error("Failed to get task status for taskId: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to get task status: " + e.getMessage());
        }
    }

}