package com.dataprofiler.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for service layer components
 * Ensures proper component scanning for service implementations
 */
@Configuration
@ComponentScan(basePackages = {
    "com.dataprofiler.service.impl",
    "com.dataprofiler.service"
})
public class ServiceConfig {
    // Configuration class for service layer
    // All service implementations will be automatically discovered and registered
}