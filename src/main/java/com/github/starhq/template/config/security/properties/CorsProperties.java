package com.github.starhq.template.config.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS configuration properties.
 *
 * @author wangjian
 */
@Data
@Component
@ConfigurationProperties(prefix = "star.cors")
public class CorsProperties {

    /**
     * Default allowed origin patterns for CORS (common frontend ports: Vue/React).
     * Note: Using patterns instead of origins to配合 allowCredentials=true below.
     */
    private List<String> allowedOriginPatterns = List.of(
            "http://localhost:8080",
            "http://localhost:3000",
            "http://localhost:5173"  // Vite default port
    );

    /**
     * Default allowed HTTP methods.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /**
     * Default allowed headers.
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * Default allow credentials (Cookie, Authorization header, etc.).
     * If false, frontend axios withCredentials: true will not work.
     */
    private boolean allowCredentials = true;

    /**
     * Default preflight request cache time (1 hour / 3600 seconds).
     */
    private long maxAge = 3600L;
}
