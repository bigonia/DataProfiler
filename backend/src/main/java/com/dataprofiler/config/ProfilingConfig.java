package com.dataprofiler.config;

import com.dataprofiler.profiler.IDatabaseProfiler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for profiling service
 * Enables async execution and configures profiler beans
 */
@Configuration
@EnableAsync
public class ProfilingConfig {

    @Autowired(required = false)
    private List<IDatabaseProfiler> profilers;



    /**
     * Create a map of profilers by their supported types
     * This allows the ProfilingService to easily lookup profilers by data source type
     */
    @Bean
    public Map<String, IDatabaseProfiler> profilerMap() {
        Map<String, IDatabaseProfiler> profilerMap = new HashMap<>();
        
        if (profilers != null) {
            for (IDatabaseProfiler profiler : profilers) {
                String key = profiler.getSupportedType().toLowerCase() + "Profiler";
                profilerMap.put(key, profiler);
            }
        }
        
        return profilerMap;
    }
}