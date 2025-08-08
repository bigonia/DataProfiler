package com.dataprofiler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Swagger/OpenAPI configuration
 * Configures API documentation generation and UI
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.name:Data Profiler}")
    private String appName;

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.description:Database Data Profiling and Analysis System}")
    private String appDescription;

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${app.contact.name:Development Team}")
    private String contactName;

    @Value("${app.contact.email:dev@example.com}")
    private String contactEmail;

    @Value("${app.contact.url:https://example.com}")
    private String contactUrl;

    /**
     * OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .tags(tagList());
    }

    /**
     * API information
     */
    private Info apiInfo() {
        return new Info()
                .title(appName + " API")
                .version(appVersion)
                .description(appDescription + "\n\n" +
                        "This API provides comprehensive database profiling and analysis capabilities including:\n" +
                        "- Data source management and connection testing\n" +
                        "- Automated profiling task execution and monitoring\n" +
                        "- Structured report generation and retrieval\n" +
                        "- Statistical analysis and data quality assessment\n\n" +
                        "**Key Features:**\n" +
                        "- Support for multiple database types (MySQL, PostgreSQL, Oracle, SQL Server, etc.)\n" +
                        "- Asynchronous profiling task execution\n" +
                        "- Flexible report formats (object/compact)\n" +
                        "- Comprehensive data statistics and sampling\n" +
                        "- RESTful API design with proper HTTP status codes\n\n" +
                        "**Authentication:** Currently disabled for development. Will be enabled in production.")
                .contact(contactInfo())
                .license(licenseInfo());
    }

    /**
     * Contact information
     */
    private Contact contactInfo() {
        return new Contact()
                .name(contactName)
                .email(contactEmail)
                .url(contactUrl);
    }

    /**
     * License information
     */
    private License licenseInfo() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    /**
     * Server list
     */
    private List<Server> serverList() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Local development server");

        Server productionServer = new Server()
                .url("https://api.dataprofiler.com")
                .description("Production server");

        return Arrays.asList(localServer, productionServer);
    }

    /**
     * API tags for grouping endpoints
     */
    private List<Tag> tagList() {
        Tag dataSourceTag = new Tag()
                .name("Data Sources")
                .description("Data source management operations including CRUD, connection testing, and statistics");

        Tag profilingTag = new Tag()
                .name("Profiling Tasks")
                .description("Profiling task management including creation, monitoring, cancellation, and cleanup");

        Tag reportTag = new Tag()
                .name("Reports")
                .description("Report generation and retrieval including summary reports, detailed reports, and statistics");

        Tag systemTag = new Tag()
                .name("System")
                .description("System health checks, monitoring, and administrative operations");

        return Arrays.asList(dataSourceTag, profilingTag, reportTag, systemTag);
    }
}