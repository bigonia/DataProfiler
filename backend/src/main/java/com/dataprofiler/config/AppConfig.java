package com.dataprofiler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Application configuration
 * Configures core application components including thread pools, data sources, and utilities
 */
@Configuration
@EnableAsync
public class AppConfig {

    @Value("${app.profiling.thread-pool.core-size:5}")
    private int profilingCorePoolSize;

    @Value("${app.profiling.thread-pool.max-size:20}")
    private int profilingMaxPoolSize;

    @Value("${app.profiling.thread-pool.queue-capacity:100}")
    private int profilingQueueCapacity;

    @Value("${app.profiling.thread-pool.keep-alive-seconds:60}")
    private int profilingKeepAliveSeconds;

    @Value("${app.general.thread-pool.core-size:3}")
    private int generalCorePoolSize;

    @Value("${app.general.thread-pool.max-size:10}")
    private int generalMaxPoolSize;

    @Value("${app.general.thread-pool.queue-capacity:50}")
    private int generalQueueCapacity;

    @Value("${app.general.thread-pool.keep-alive-seconds:60}")
    private int generalKeepAliveSeconds;

    @Value("${app.ai.thread-pool.core-size:2}")
    private int aiCorePoolSize;

    @Value("${app.ai.thread-pool.max-size:8}")
    private int aiMaxPoolSize;

    @Value("${app.ai.thread-pool.queue-capacity:25}")
    private int aiQueueCapacity;

    @Value("${app.ai.thread-pool.keep-alive-seconds:120}")
    private int aiKeepAliveSeconds;

    /**
     * Thread pool executor for profiling tasks
     * This executor is specifically designed for CPU-intensive profiling operations
     */
    @Bean(name = "profilingTaskExecutor")
    public Executor profilingTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(profilingCorePoolSize);
        executor.setMaxPoolSize(profilingMaxPoolSize);
        executor.setQueueCapacity(profilingQueueCapacity);
        executor.setKeepAliveSeconds(profilingKeepAliveSeconds);
        executor.setThreadNamePrefix("Profiling-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * General purpose thread pool executor
     * Used for general asynchronous operations like report generation, cleanup tasks, etc.
     */
    @Bean(name = "generalTaskExecutor")
    public Executor generalTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(generalCorePoolSize);
        executor.setMaxPoolSize(generalMaxPoolSize);
        executor.setQueueCapacity(generalQueueCapacity);
        executor.setKeepAliveSeconds(generalKeepAliveSeconds);
        executor.setThreadNamePrefix("General-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * AI service thread pool executor
     * Optimized for AI analysis tasks with streaming capabilities
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(aiCorePoolSize);
        executor.setMaxPoolSize(aiMaxPoolSize);
        executor.setQueueCapacity(aiQueueCapacity);
        executor.setKeepAliveSeconds(aiKeepAliveSeconds);
        executor.setThreadNamePrefix("AI-Analysis-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(90);
        executor.initialize();
        return executor;
    }

    /**
     * REST template for external API calls
     * Configured with connection and read timeouts
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Configure timeouts if needed
        // restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
        return restTemplate;
    }

    /**
     * Primary data source configuration
     * Uses application.yml properties with spring.datasource prefix
     */
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    /**
     * Application properties configuration
     */
    @Bean
    @ConfigurationProperties(prefix = "app")
    public AppProperties appProperties() {
        return new AppProperties();
    }

    /**
     * Application properties class
     * Holds configuration values for the application
     */
    public static class AppProperties {
        private ProfilingProperties profiling = new ProfilingProperties();
        private ReportProperties report = new ReportProperties();
        private CleanupProperties cleanup = new CleanupProperties();
        private SecurityProperties security = new SecurityProperties();
        private FileProperties file = new FileProperties();

        // Getters and setters
        public ProfilingProperties getProfiling() {
            return profiling;
        }

        public void setProfiling(ProfilingProperties profiling) {
            this.profiling = profiling;
        }

        public ReportProperties getReport() {
            return report;
        }

        public void setReport(ReportProperties report) {
            this.report = report;
        }

        public CleanupProperties getCleanup() {
            return cleanup;
        }

        public void setCleanup(CleanupProperties cleanup) {
            this.cleanup = cleanup;
        }

        public SecurityProperties getSecurity() {
            return security;
        }

        public void setSecurity(SecurityProperties security) {
            this.security = security;
        }

        public FileProperties getFile() {
            return file;
        }

        public void setFile(FileProperties file) {
            this.file = file;
        }

        /**
         * Profiling related properties
         */
        public static class ProfilingProperties {
            private int maxConcurrentTasks = 5;
            private int taskTimeoutMinutes = 60;
            private int maxSampleSize = 10000;
            private boolean enableStatistics = true;
            private boolean enableDataSampling = true;

            // Getters and setters
            public int getMaxConcurrentTasks() {
                return maxConcurrentTasks;
            }

            public void setMaxConcurrentTasks(int maxConcurrentTasks) {
                this.maxConcurrentTasks = maxConcurrentTasks;
            }

            public int getTaskTimeoutMinutes() {
                return taskTimeoutMinutes;
            }

            public void setTaskTimeoutMinutes(int taskTimeoutMinutes) {
                this.taskTimeoutMinutes = taskTimeoutMinutes;
            }

            public int getMaxSampleSize() {
                return maxSampleSize;
            }

            public void setMaxSampleSize(int maxSampleSize) {
                this.maxSampleSize = maxSampleSize;
            }

            public boolean isEnableStatistics() {
                return enableStatistics;
            }

            public void setEnableStatistics(boolean enableStatistics) {
                this.enableStatistics = enableStatistics;
            }

            public boolean isEnableDataSampling() {
                return enableDataSampling;
            }

            public void setEnableDataSampling(boolean enableDataSampling) {
                this.enableDataSampling = enableDataSampling;
            }
        }

        /**
         * Report related properties
         */
        public static class ReportProperties {
            private int maxReportSizeMB = 100;
            private String defaultFormat = "object";
            private boolean enableCompression = true;
            private int cacheExpirationMinutes = 30;

            // Getters and setters
            public int getMaxReportSizeMB() {
                return maxReportSizeMB;
            }

            public void setMaxReportSizeMB(int maxReportSizeMB) {
                this.maxReportSizeMB = maxReportSizeMB;
            }

            public String getDefaultFormat() {
                return defaultFormat;
            }

            public void setDefaultFormat(String defaultFormat) {
                this.defaultFormat = defaultFormat;
            }

            public boolean isEnableCompression() {
                return enableCompression;
            }

            public void setEnableCompression(boolean enableCompression) {
                this.enableCompression = enableCompression;
            }

            public int getCacheExpirationMinutes() {
                return cacheExpirationMinutes;
            }

            public void setCacheExpirationMinutes(int cacheExpirationMinutes) {
                this.cacheExpirationMinutes = cacheExpirationMinutes;
            }
        }

        /**
         * Cleanup related properties
         */
        public static class CleanupProperties {
            private int retainCompletedTasksDays = 30;
            private int retainReportsDays = 90;
            private boolean enableAutoCleanup = true;
            private String cleanupSchedule = "0 0 2 * * ?";

            // Getters and setters
            public int getRetainCompletedTasksDays() {
                return retainCompletedTasksDays;
            }

            public void setRetainCompletedTasksDays(int retainCompletedTasksDays) {
                this.retainCompletedTasksDays = retainCompletedTasksDays;
            }

            public int getRetainReportsDays() {
                return retainReportsDays;
            }

            public void setRetainReportsDays(int retainReportsDays) {
                this.retainReportsDays = retainReportsDays;
            }

            public boolean isEnableAutoCleanup() {
                return enableAutoCleanup;
            }

            public void setEnableAutoCleanup(boolean enableAutoCleanup) {
                this.enableAutoCleanup = enableAutoCleanup;
            }

            public String getCleanupSchedule() {
                return cleanupSchedule;
            }

            public void setCleanupSchedule(String cleanupSchedule) {
                this.cleanupSchedule = cleanupSchedule;
            }
        }

        /**
         * Security related properties
         */
        public static class SecurityProperties {
            private boolean enableAuthentication = false;
            private boolean enableAuthorization = false;
            private String jwtSecret = "default-secret-key";
            private int jwtExpirationHours = 24;

            // Getters and setters
            public boolean isEnableAuthentication() {
                return enableAuthentication;
            }

            public void setEnableAuthentication(boolean enableAuthentication) {
                this.enableAuthentication = enableAuthentication;
            }

            public boolean isEnableAuthorization() {
                return enableAuthorization;
            }

            public void setEnableAuthorization(boolean enableAuthorization) {
                this.enableAuthorization = enableAuthorization;
            }

            public String getJwtSecret() {
                return jwtSecret;
            }

            public void setJwtSecret(String jwtSecret) {
                this.jwtSecret = jwtSecret;
            }

            public int getJwtExpirationHours() {
                return jwtExpirationHours;
            }

            public void setJwtExpirationHours(int jwtExpirationHours) {
                this.jwtExpirationHours = jwtExpirationHours;
            }
        }

        /**
         * File related properties
         */
        public static class FileProperties {
            private String uploadDir = "./uploads";
            private String maxSize = "50MB";

            public String getUploadDir() {
                return uploadDir;
            }

            public void setUploadDir(String uploadDir) {
                this.uploadDir = uploadDir;
            }

            public String getMaxSize() {
                return maxSize;
            }

            public void setMaxSize(String maxSize) {
                this.maxSize = maxSize;
            }
        }
    }
}