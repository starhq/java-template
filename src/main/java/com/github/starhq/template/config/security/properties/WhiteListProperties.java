package com.github.starhq.template.config.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for defining unauthenticated public endpoints (White List).
 *
 * <p>Binds to the {@code star} namespace (e.g., {@code star.white-list} in YAML).
 * URLs matching these paths will bypass the JWT validation and authorization filters entirely.
 *
 * <p><b>Architecture Note (List vs. Array):</b>
 * We use a {@link List} instead of a raw {@code String[]} array. This is the recommended practice
 * because:
 * <ul>
 *   <li>It allows developers to define the whitelist cleanly in YAML using the {@code -} list syntax
 *       (e.g., {@code white-list: ["/api/public/**", "/actuator/**"]}), which is much cleaner
 *       than comma-separated strings.</li>
 *   <li>It prevents {@link org.springframework.beans.factory.BeanCreationException: Invalid bean definition}
 *       errors if the list is completely omitted in the YAML file (it gracefully defaults to an empty list).</li>
 * </ul>
 *
 * <p><b>⚠️ Security Warning:</b> Items in this list bypass security checks completely. Ensure that only
 * truly public endpoints (like login, public APIs, static resources) are added here. Never add business
 * endpoints that require user context here.
 *
 * @author starhq
 */
@Data
@Component
@ConfigurationProperties(prefix = "star")
public class WhiteListProperties {

    /**
     * A collection of URL patterns that do not require authentication.
     * <p>Patterns typically use Ant-style syntax (e.g., {@code /api/public/**}).
     */
    private List<String> whiteList = new ArrayList<>();
}