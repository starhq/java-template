package com.github.starhq.template.common.constant;

import com.github.starhq.template.common.enums.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * Constants for filter configuration.
 *
 * <p>Defines specific endpoints, HTTP method mappings, and exclusion paths
 * used primarily by Spring Security filters (e.g., JWT authentication filters)
 * to determine which requests should be intercepted or bypassed.
 *
 * @author wangjian
 */
public interface FilterConstant {

    /**
     * Endpoint triggered to exchange a valid refresh token for a new access token.
     */
    String REFRESH_ENDPOINT = "auth/refresh";

    /**
     * Endpoint triggered to explicitly invalidate the current user session or token.
     */
    String LOGOUT_ENDPOINT = "auth/logout";

    /**
     * Immutable cache mapping standard HTTP method strings to their corresponding {@link HttpMethod} enums.
     *
     * <p>This avoids the overhead of repeatedly calling {@code HttpMethod.valueOf()} during
     * high-frequency filter chain executions, providing O(1) lookup performance.
     */
    Map<String, HttpMethod> HTTP_METHOD_CACHE = Map.of(
            "GET", HttpMethod.GET,
            "POST", HttpMethod.POST,
            "PUT", HttpMethod.PUT,
            "DELETE", HttpMethod.DELETE,
            "PATCH", HttpMethod.PATCH,
            "HEAD", HttpMethod.HEAD,
            "OPTIONS", HttpMethod.OPTIONS
    );

    /**
     * Paths excluded from security filter interception.
     *
     * <p>Requests matching these prefixes or exact paths (e.g., health checks,
     * static web resources, and Swagger UI assets) will bypass authentication and authorization.
     */
    List<String> EXCLUDE_PATHS = List.of(
            "/actuator/health",
            "/favicon.ico",
            "/static/",
            "/webjars/",
            "/error"
    );
}