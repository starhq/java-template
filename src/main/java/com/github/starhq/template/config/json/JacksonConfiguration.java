package com.github.starhq.template.config.json;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import com.github.starhq.template.config.json.serializer.SensitivePropertyFilter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * Global Jackson JSON serialization configuration.
 *
 * <p>Customizes the auto-configured {@link tools.jackson.databind.json.JsonMapper} (Jackson 3.x)
 * to integrate the data masking security feature seamlessly into the Spring Boot application.
 *
 * @author starhq
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Customizes the Jackson {@link tools.jackson.databind.json.JsonMapper} builder to register
     * the sensitive data masking filter.
     *
     * <p><b>Why {@link JsonMapperBuilderCustomizer}?</b> In Spring Boot 3.x with Jackson 3.x,
     * the auto-configuration creates the {@code JsonMapper} internally. Using this customizer interface
     * allows us to intercept and modify the builder before the final {@code JsonMapper} is instantiated,
     * ensuring our filter is applied without completely overriding Spring Boot's default Jackson settings
     * (like date formats, naming strategies, etc.).
     *
     * @param sensitivePropertyFilter the injected custom filter responsible for masking logic
     * @return a customizer instance to be applied by Spring Boot's auto-configuration
     */
    @Bean
    JsonMapperBuilderCustomizer jacksonCustomizer(SensitivePropertyFilter sensitivePropertyFilter) {
        return builder -> {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                    // Bind our custom masking filter to the specific ID defined in SensitivePropertyFilter
                    .addFilter(SensitivePropertyFilter.FILTER_NAME, sensitivePropertyFilter)
                    // Fail-safe: If a class has @JsonFilter("someUnknownId"), Jackson 3.x throws an exception by default.
                    // Setting this to false prevents the application from crashing due to unregistered filter IDs.
                    .setFailOnUnknownId(false)
                    // CRITICAL FALLBACK: If a class does NOT have @JsonFilter at all, Jackson will fail by default
                    // when a FilterProvider is registered. This sets a default "pass-through" filter so normal
                    // DTOs (without the annotation) can still be serialized normally without throwing InvalidDefinitionException.
                    .setDefaultFilter(SimpleBeanPropertyFilter.serializeAll());

            // Apply the configured provider to the global ObjectMapper/JsonMapper
            builder.filterProvider(filterProvider);
        };
    }

    /**
     * Creates and exposes the {@link SensitivePropertyFilter} as a Spring-managed Bean.
     *
     * <p>Defining it as a {@code @Bean} allows Spring to handle its lifecycle and dependency injection
     * (injecting {@link SensitiveFieldProperties}), making it easy to wire into the customizer above.
     *
     * @param sensitiveFieldProperties the configuration properties containing the masking rules
     * @return a fully initialized {@link SensitivePropertyFilter} instance
     */
    @Bean
    SensitivePropertyFilter sensitiveFilter(SensitiveFieldProperties sensitiveFieldProperties) {
        return new SensitivePropertyFilter(sensitiveFieldProperties);
    }
}
