package com.dataprofiler.service.impl;

import com.dataprofiler.client.DifyApiClient;
import com.dataprofiler.dto.request.AnalysisRequest;
import com.dataprofiler.dto.request.DifyWorkflowRequest;
import com.dataprofiler.dto.request.ReportSummaryRequest;
import com.dataprofiler.dto.response.DifyWorkflowResponse;
import com.dataprofiler.entity.DataSourceConfig;
import com.dataprofiler.service.AIService;
import com.dataprofiler.service.DataSourceService;
import com.dataprofiler.service.StructuredReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of AIService interface
 * Provides AI-powered data analysis using Dify workflow integration
 * Supports streaming analysis for real-time user interaction
 */
@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    // SSE timeout configuration (30 minutes)
    private static final long SSE_TIMEOUT = TimeUnit.MINUTES.toMillis(30);

    // Maximum context length for AI processing
    private static final int MAX_CONTEXT_LENGTH = 50000;

    @Autowired
    private DifyApiClient difyApiClient;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private StructuredReportService structuredReportService;

    @Override
    public SseEmitter streamAnalysis(AnalysisRequest request) {
        logger.info("Starting streaming AI analysis for user: {}, taskId: {}",
                request.getUserId(), request.getTaskId());

        // Validate request parameters
        validateAnalysisRequest(request);

        // Create SSE emitter with timeout
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 定义一个 Executor 封装 Scheduler
        Executor schedulerExecutor = command -> Schedulers.boundedElastic().schedule(command);
        // Process analysis asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                processStreamingAnalysis(request, emitter);
            } catch (Exception e) {
                logger.error("Error in streaming analysis: {}", e.getMessage(), e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("error")
                            .data("Analysis failed: " + e.getMessage()));
                    emitter.complete();
                } catch (IOException ioException) {
                    logger.error("Failed to send error event: {}", ioException.getMessage());
                    emitter.completeWithError(ioException);
                }
            }
        }, schedulerExecutor);

        // Set completion and error handlers
        emitter.onCompletion(() -> logger.info("SSE stream completed for user: {}", request.getUserId()));
        emitter.onTimeout(() -> {
            logger.warn("SSE stream timed out for user: {}", request.getUserId());
            emitter.complete();
        });
        emitter.onError(throwable ->
                logger.error("SSE stream error for user: {}, error: {}",
                        request.getUserId(), throwable.getMessage()));

        return emitter;
    }

    /**
     * Process streaming analysis with Dify API
     */
    private void processStreamingAnalysis(AnalysisRequest request, SseEmitter emitter) throws IOException {
        // Build analysis context
//        String context = buildAnalysisContext(request);

        // Create Dify workflow request
        DifyWorkflowRequest difyRequest = createDifyWorkflowRequest(request);

        // Send initial status
        emitter.send(SseEmitter.event()
                .name("status")
                .data("Starting AI analysis..."));

        // Subscribe to Dify streaming response
        Flux<String> responseStream = difyApiClient.invokeWorkflowStream(difyRequest);

        responseStream
                .doOnSubscribe(subscription -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("connected")
                                .data("Connected to AI service"));
                    } catch (IOException e) {
                        logger.error("Failed to send connection event: {}", e.getMessage());
                    }
                })
                .subscribe(
                        sseEvent -> {
                            try {
                                // Parse and forward SSE events
                                DifyWorkflowResponse response = difyApiClient.parseSseEvent(sseEvent);
                                if (response != null) {
                                    forwardDifyResponse(emitter, response);
                                } else {
                                    // Forward raw event if parsing fails
                                    emitter.send(SseEmitter.event()
                                            .name("data")
                                            .data(sseEvent));
                                }
                            } catch (IOException e) {
                                logger.error("Failed to forward SSE event: {}", e.getMessage());
                            }
                        },
                        error -> {
                            logger.error("Error in Dify stream: {}", error.getMessage(), error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data("AI service error: " + error.getMessage()));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            logger.info("Dify stream completed successfully");
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("complete")
                                        .data("Analysis completed"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        }
                );
    }

    /**
     * Forward Dify response to SSE client
     */
    private void forwardDifyResponse(SseEmitter emitter, DifyWorkflowResponse response) throws IOException {
        String eventName = "data";
        Object eventData = response.getAnswer();

        // Handle different event types from Dify
        if (StringUtils.hasText(response.getEvent())) {
            switch (response.getEvent()) {
                case "workflow_started":
                    eventName = "started";
                    eventData = "Workflow started: " + response.getWorkflowRunId();
                    break;
                case "workflow_finished":
                    eventName = "finished";
                    eventData = response.getAnswer();
                    break;
                case "node_started":
                case "node_finished":
                    eventName = "progress";
                    eventData = response.getData();
                    break;
                case "text_chunk":
                    eventName = "chunk";
                    eventData = response.getAnswer();
                    break;
                default:
                    eventName = "data";
                    eventData = response;
            }
        }

        // Send event to client
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(eventData != null ? eventData : ""));
    }

    /**
     * Create Dify workflow request from analysis request
     */
    private DifyWorkflowRequest createDifyWorkflowRequest(AnalysisRequest request) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("task_id", request.getTaskId());

        if (StringUtils.hasText(request.getQuestion())) {
            inputs.put("user_question", request.getQuestion());
        }

        if (StringUtils.hasText(request.getAnalysisType())) {
            inputs.put("analysis_type", request.getAnalysisType());
        }

        if (StringUtils.hasText(request.getContext())) {
            inputs.put("additional_context", request.getContext());
        }

        return new DifyWorkflowRequest(inputs, request.getUserId(), true);
    }

    @Override
    public boolean isAIServiceAvailable() {
        try {
            return difyApiClient.healthCheck().block();
        } catch (Exception e) {
            logger.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getAIServiceStatus() {
        boolean available = isAIServiceAvailable();
        return String.format("AI Service Status: %s", available ? "Available" : "Unavailable");
    }


    private void validateAnalysisRequest(AnalysisRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Analysis request cannot be null");
        }

    }

    @Override
    public String buildAnalysisContext(AnalysisRequest request) {
        try {
            StringBuilder contextBuilder = new StringBuilder();

            // Get data source information
//            DataSourceConfig dataSource = dataSourceService.getDataSourceBySourceId(request.getDataSourceId());
//            contextBuilder.append("Data Source Information:\n");
//            contextBuilder.append("- Name: ").append(dataSource.getName()).append("\n");
//            contextBuilder.append("- Type: ").append(dataSource.getType()).append("\n");
//
//            if (dataSource.getDatabaseName() != null) {
//                contextBuilder.append("- Database: ").append(dataSource.getDatabaseName()).append("\n");
//            }

            contextBuilder.append("\n");

            // Get profiling reports if task ID is provided
            if (StringUtils.hasText(request.getTaskId())) {
                try {
                    // Get structured reports for the task
                    var reports = structuredReportService.getReportsSummary(new ReportSummaryRequest(request.getTaskId()));
                    if (!reports.isEmpty()) {
                        contextBuilder.append("Profiling Data Summary:\n");

//                        reports.forEach(report -> {
//                            contextBuilder.append("- Report ID: ").append(report.getTaskId()).append("\n");
//                            contextBuilder.append("- Generated: ").append(report.g()).append("\n");
//                            // Add more report details as needed
//                        });

                        contextBuilder.append("\n");
                    }
                } catch (Exception e) {
                    logger.warn("Failed to retrieve profiling reports for task {}: {}",
                            request.getTaskId(), e.getMessage());
                }
            }

            // Add user context if provided
            if (StringUtils.hasText(request.getContext())) {
                contextBuilder.append("Additional Context:\n");
                contextBuilder.append(request.getContext()).append("\n\n");
            }

            String context = contextBuilder.toString();

            // Truncate if too long
            if (context.length() > MAX_CONTEXT_LENGTH) {
                context = context.substring(0, MAX_CONTEXT_LENGTH) + "\n[Context truncated due to length]";
                logger.warn("Analysis context truncated for request from user: {}", request.getUserId());
            }

            return context;

        } catch (Exception e) {
            logger.error("Failed to build analysis context: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build analysis context", e);
        }
    }
}