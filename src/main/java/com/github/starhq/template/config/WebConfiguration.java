package com.github.starhq.template.config;

import com.github.starhq.template.config.converter.SortEnumConverter;
import com.github.starhq.template.config.converter.TargetTypeConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for customizing Spring Web MVC infrastructure.
 *
 * <p>Implements {@link WebMvcConfigurer` to hook into Spring MVC's initialization phase to register custom
 * {@link org.springframework.core.convert.converter.Converter}s. Without this configuration, custom converters
 * like {@link SortEnumConverter} would not be automatically recognized by the framework when resolving
 * controller method arguments (e.g., {@code @RequestParam SortEnum sortEnum}).
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    /**
     * Registers custom Spring MVC parameter type converters.
     *
     * <p><b>Why is this necessary?</b> Spring Boot's auto-configuration only registers default converters
     * (String -> Enum, Date -> String, etc.). If a custom enum implements a custom interface (like our {@link com.github.starhq.template.common.enums.BaseEnum}),
     * Spring doesn't know how to convert an incoming string (e.g., "ACTIVE") to the custom enum type.
     * This method explicitly teaches Spring how to delegate to our custom converters.
     *
     * @param registry the {@link FormatterRegistry} provided by Spring to register converters
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Registers the converter for database target types (e.g., String "user" -> TargetType.USER)
        registry.addConverter(new TargetTypeConverter());
        // Registers the converter for sorting directions (e.g., String "asc" -> SortEnum.ASC)
        registry.addConverter(new SortEnumConverter());
    }
}