package com.github.starhq.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main entry point for the Spring Boot application.
 * <p>
 * This class serves as the bootstrap configuration for the application, enabling:
 * <ul>
 *     <li><strong>Component Scanning</strong>: Auto-detection of Spring components in {@code com.github.starhq.template} and sub-packages</li>
 *     <li><strong>Auto-Configuration</strong>: Automatic setup of Spring Boot starters (Web, Security, MyBatis-Plus, etc.)</li>
 *     <li><strong>Async Support</strong>: Enables {@code @Async} annotation processing for asynchronous method execution</li>
 *     <li><strong>Configuration Properties</strong>: Scans for {@code @ConfigurationProperties} beans without explicit {@code @EnableConfigurationProperties}</li>
 * </ul>
 * <p>
 * <strong>Startup Workflow:</strong>
 * <ol>
 *     <li>Initializes Spring Application Context</li>
 *     <li>Loads application properties from {@code application.yml} / {@code application-{profile}.yml}</li>
 *     <li>Registers all discovered beans (Controllers, Services, Mappers, etc.)</li>
 *     <li>Starts embedded web server (Tomcat/Jetty/Undertow)</li>
 *     <li>Executes {@code CommandLineRunner} / {@code ApplicationRunner} beans if present</li>
 * </ol>
 * <p>
 * <strong>Configuration Notes:</strong>
 * <ul>
 *     <li><strong>@EnableAsync</strong>: Required for methods annotated with {@code @Async} to run in separate threads via task executor</li>
 *     <li><strong>@ConfigurationPropertiesScan</strong>: Automatically registers classes annotated with {@code @ConfigurationProperties} found in the base package</li>
 *     <li><strong>Profile Activation</strong>: Use {@code --spring.profiles.active=dev} to activate specific environment configurations</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see SpringApplication
 * @see SpringBootApplication
 * @see EnableAsync
 * @see ConfigurationPropertiesScan
 */
@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class JavaTemplateApplication {

    /**
     * Main method to launch the Spring Boot application.
     * <p>
     * This method delegates to {@link SpringApplication#run(Class, String...)} to:
     * <ul>
     *     <li>Create and configure the Spring ApplicationContext</li>
     *     <li>Start the embedded web server</li>
     *     <li>Initialize all Spring beans and auto-configurations</li>
     * </ul>
     * <p>
     * <strong>Usage:</strong>
     * <pre>
     * {@code
     * // Run with default profile
     * java -jar template.jar
     *
     * // Run with specific profile
     * java -jar template.jar --spring.profiles.active=prod
     *
     * // Run with custom port
     * java -jar template.jar --server.port=8081
     * }
     * </pre>
     *
     * @param args command-line arguments passed to the application (e.g., {@code --server.port=8080})
     * @see SpringApplication#run(Class, String...)
     */
    public static void main(String[] args) {
        SpringApplication.run(JavaTemplateApplication.class, args);
    }

}