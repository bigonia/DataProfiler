package com.dataprofiler.client;

import com.dataprofiler.dto.request.DifyWorkflowRequest;
import com.dataprofiler.dto.response.DifyWorkflowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Dify API Client for workflow execution
 * Provides streaming and non-streaming workflow invocation capabilities
 */
@Component
public class DifyApiClient {

    private static final Logger logger = LoggerFactory.getLogger(DifyApiClient.class);

    @Value("${dify.api.base-url}")
    private String baseUrl;

    @Value("${dify.api.key}")
    private String apiKey;

    @Value("${dify.api.timeout:30000}")
    private int timeout;

    @Value("${dify.api.max-retries:3}")
    private int maxRetries;

    private WebClient webClient;
    private final ObjectMapper objectMapper;

    public DifyApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeWebClient() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB buffer
                .build();
        
        logger.info("DifyApiClient initialized with base URL: {}", baseUrl);
    }

    /**
     * Invoke Dify workflow with streaming response
     * Returns a Flux of server-sent events from Dify API
     * 
     * @param request The workflow request containing inputs and configuration
     * @return Flux of raw SSE strings from Dify API
     */
    public Flux<String> invokeWorkflowStream(DifyWorkflowRequest request) {
        logger.debug("Invoking Dify workflow stream with request: {}", request);
        
        return webClient.post()
                .uri("/workflows/run")
                .bodyValue(request)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("Retry exhausted for workflow stream after {} attempts", maxRetries);
                            return new RuntimeException("Failed to invoke Dify workflow after " + maxRetries + " retries", 
                                    retrySignal.failure());
                        }))
                .doOnSubscribe(subscription -> logger.info("Starting Dify workflow stream subscription"))
                .doOnNext(event -> logger.trace("Received SSE event: {}", event))
                .doOnError(error -> logger.error("Error in Dify workflow stream: {}", error.getMessage(), error))
                .doOnComplete(() -> logger.info("Dify workflow stream completed successfully"))
                .onErrorMap(TimeoutException.class, ex -> 
                        new RuntimeException("Dify workflow request timed out after " + timeout + "ms", ex));
    }

    /**
     * Invoke Dify workflow with blocking response
     * Returns a single response object for non-streaming requests
     * 
     * @param request The workflow request (should have stream=false)
     * @return Mono containing the workflow response
     */
    public Mono<DifyWorkflowResponse> invokeWorkflow(DifyWorkflowRequest request) {
        logger.debug("Invoking Dify workflow (blocking) with request: {}", request);
        
        // Ensure non-streaming mode
        request.setStream(false);
        
        return webClient.post()
                .uri("/workflows/run")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DifyWorkflowResponse.class)
                .timeout(Duration.ofMillis(timeout))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                        .filter(this::isRetryableException)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            logger.error("Retry exhausted for workflow request after {} attempts", maxRetries);
                            return new RuntimeException("Failed to invoke Dify workflow after " + maxRetries + " retries", 
                                    retrySignal.failure());
                        }))
                .doOnSubscribe(subscription -> logger.info("Starting Dify workflow request"))
                .doOnSuccess(response -> logger.info("Dify workflow completed successfully: {}", response.getTaskId()))
                .doOnError(error -> logger.error("Error in Dify workflow request: {}", error.getMessage(), error))
                .onErrorMap(TimeoutException.class, ex -> 
                        new RuntimeException("Dify workflow request timed out after " + timeout + "ms", ex));
    }

    /**
     * Parse SSE event string to DifyWorkflowResponse object
     * Handles the "data: {json}" format from server-sent events
     * 
     * @param sseEvent Raw SSE event string
     * @return Parsed DifyWorkflowResponse or null if parsing fails
     */
    public DifyWorkflowResponse parseSseEvent(String sseEvent) {
        try {
            // Handle SSE format: "data: {json}"
            if (sseEvent.startsWith("data: ")) {
                String jsonData = sseEvent.substring(6).trim();
                
                // Skip empty data or keep-alive events
                if (jsonData.isEmpty() || "[DONE]".equals(jsonData)) {
                    return null;
                }
                
                return objectMapper.readValue(jsonData, DifyWorkflowResponse.class);
            }
            
            // Try to parse as direct JSON
            return objectMapper.readValue(sseEvent, DifyWorkflowResponse.class);
            
        } catch (Exception e) {
            logger.warn("Failed to parse SSE event: {}, error: {}", sseEvent, e.getMessage());
            return null;
        }
    }

    /**
     * Check if an exception is retryable
     * 
     * @param throwable The exception to check
     * @return true if the exception should trigger a retry
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException webEx = (WebClientResponseException) throwable;
            int statusCode = webEx.getStatusCode().value();
            
            // Retry on server errors (5xx) and some client errors
            return statusCode >= 500 || statusCode == 429 || statusCode == 408;
        }
        
        // Retry on network-related exceptions
        return throwable instanceof java.net.ConnectException ||
               throwable instanceof java.net.SocketTimeoutException ||
               throwable instanceof TimeoutException;
    }

    /**
     * Health check for Dify API connectivity
     * 
     * @return Mono indicating if the API is accessible
     */
    public Mono<Boolean> healthCheck() {
        return webClient.get()
                .uri("/health") // Assuming Dify has a health endpoint
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .onErrorReturn(false)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(healthy -> logger.debug("Dify API health check: {}", healthy ? "OK" : "FAILED"))
                .doOnError(error -> logger.warn("Dify API health check failed: {}", error.getMessage()));
    }



}