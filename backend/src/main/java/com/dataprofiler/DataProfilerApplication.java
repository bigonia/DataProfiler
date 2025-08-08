package com.dataprofiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.RequestContextFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Main application class for Data Profiler
 * 
 * This application provides comprehensive database profiling and analysis capabilities including:
 * - Data source management and connection testing
 * - Automated profiling task execution and monitoring
 * - Structured report generation and retrieval
 * - Statistical analysis and data quality assessment
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
public class DataProfilerApplication {

    private static final Logger logger = LoggerFactory.getLogger(DataProfilerApplication.class);

    public static void main(String[] args) {
        try {
            ConfigurableApplicationContext context = SpringApplication.run(DataProfilerApplication.class, args);
            logApplicationStartup(context.getEnvironment());
        } catch (Exception e) {
            logger.error("Failed to start Data Profiler application", e);
            System.exit(1);
        }
    }

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilter() {
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestContextFilter());
        registration.setOrder(1);
        return registration;
    }

    /**
     * Log application startup information
     */
    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        
        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";
        
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            logger.warn("The host name could not be determined, using `localhost` as fallback");
        }
        
        String applicationName = env.getProperty("spring.application.name", "Data Profiler");
        String activeProfiles = String.join(", ", env.getActiveProfiles());
        if (activeProfiles.isEmpty()) {
            activeProfiles = "default";
        }
        
        logger.info("\n----------------------------------------------------------\n\t" +
                "Application '{}' is running! Access URLs:\n\t" +
                "Local: \t\t{}://localhost:{}{}\n\t" +
                "External: \t{}://{}:{}{}\n\t" +
                "Profile(s): \t{}\n\t" +
                "API Docs: \t{}://localhost:{}{}/swagger-ui.html\n" +
                "----------------------------------------------------------",
                applicationName,
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                activeProfiles,
                protocol, serverPort, contextPath);
        
        // Log important configuration
        logConfiguration(env);
    }

    /**
     * Log important application configuration
     */
    private static void logConfiguration(Environment env) {
        logger.info("Configuration Summary:");
        
        // Database configuration
        String datasourceUrl = env.getProperty("spring.datasource.url", "Not configured");
        String datasourceDriver = env.getProperty("spring.datasource.driver-class-name", "Not configured");
        logger.info("  Database URL: {}", datasourceUrl);
        logger.info("  Database Driver: {}", datasourceDriver);
        
        // JPA configuration
        String ddlAuto = env.getProperty("spring.jpa.hibernate.ddl-auto", "update");
        String showSql = env.getProperty("spring.jpa.show-sql", "false");
        logger.info("  JPA DDL Auto: {}", ddlAuto);
        logger.info("  JPA Show SQL: {}", showSql);
        
        // Application specific configuration
        String maxConcurrentTasks = env.getProperty("app.profiling.max-concurrent-tasks", "5");
        String taskTimeout = env.getProperty("app.profiling.task-timeout-minutes", "60");
        logger.info("  Max Concurrent Tasks: {}", maxConcurrentTasks);
        logger.info("  Task Timeout (minutes): {}", taskTimeout);
        
        // Thread pool configuration
        String corePoolSize = env.getProperty("app.profiling.thread-pool.core-size", "5");
        String maxPoolSize = env.getProperty("app.profiling.thread-pool.max-size", "20");
        logger.info("  Thread Pool Core Size: {}", corePoolSize);
        logger.info("  Thread Pool Max Size: {}", maxPoolSize);
        
        logger.info("----------------------------------------------------------");
    }
}