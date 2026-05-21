package com.github.starhq.template.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Utility class for common HTTP and Servlet-related operations.
 *
 * <p>Provides helper methods to extract client context (IP, Tokens, Fingerprints)
 * from requests using multi-level fallback strategies, and to write raw JSON directly
 * to the response stream. Primarily designed for use within Servlet Filters or interceptors,
 * where standard Spring MVC response handling is not yet available.
 *
 * @author starhq
 */
@UtilityClass
public class HttpUtils {

    // ==================== Constants ====================

    // IP Related
    /**
     * Identifier used by reverse proxies to indicate an unknown or masked client IP.
     */
    private static final String UNKNOWN_IP_IDENTIFIER = "unknown";

    /**
     * Common HTTP headers inspected to find the real client IP.
     * Ordered by standard usage priority.
     */
    private static final List<String> IP_HEADERS = Arrays.asList(
            "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR");

    // Auth Related
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_COOKIE_NAME = "jwt_token";
    private static final String TOKEN_QUERY_PARAM = "token";

    // Fingerprint Related
    private static final String FINGERPRINT_HEADER = "X-Device-Fingerprint";

    // ==================== IP Address Methods ====================

    /**
     * Retrieves the client IP from the current thread-bound HTTP request.
     *
     * @return the parsed client IP address
     * @throws IllegalStateException if called outside of a servlet request thread (e.g., in an @Async method)
     */
    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        return getClientIp(request);
    }

    /**
     * Parses the real client IP address from the request, taking reverse proxies (like Nginx) into account.
     *
     * <p><b>Security Context:</b> In a proxied environment, {@code request.getRemoteAddr()} only returns
     * the proxy's IP. This method inspects standard forwarding headers (e.g., {@code X-Forwarded-For}).
     * If the header contains multiple IPs (format: {@code client, proxy1, proxy2}), the first IP is extracted
     * as it represents the original client.
     *
     * @param request the current HTTP request
     * @return the parsed client IP address, or {@code null} if completely unresolvable
     * @throws IllegalArgumentException if the request is null
     */
    public static String getClientIp(HttpServletRequest request) {
        Objects.requireNonNull(request, "HttpServletRequest must not be null");

        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                return getFirstIp(ip);
            }
        }

        // Fallback to direct connection IP if no proxy headers are present
        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : null;
    }

    // ==================== Token & Fingerprint Methods ====================

    /**
     * Extracts the authentication token using a three-tier fallback strategy.
     *
     * <p><b>Resolution Order:</b>
     * <ol>
     *   <li><b>Header:</b> Standard OAuth2 Bearer token (e.g., {@code Authorization: Bearer xxx}).</li>
     *   <li><b>Cookie:</b> Fallback for browser environments where injecting headers is complex
     *       (e.g., during file downloads via {@code <a>}` tags, or legacy AJAX implementations).</li>
     *   <li><b>Query Param:</b> Last resort for WebSocket connections or server-side rendering (SSR)
     *       where headers/cookies cannot be easily passed.</li>
     * </ol>
     *
     * @param request the current HTTP request
     * @return the extracted token string, or {@code null} if not found in any source
     */
    public static String extractToken(HttpServletRequest request) {
        // 1. Header (Bearer Token)
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }

        // 2. Cookie
        String cookieToken = getCookieValue(request, TOKEN_COOKIE_NAME);
        if (StringUtils.hasText(cookieToken)) {
            return cookieToken;
        }

        // 3. Query Parameter
        return request.getParameter(TOKEN_QUERY_PARAM);
    }

    /**
     * Extracts the device fingerprint using a fallback strategy (Header -> Cookie -> Query).
     *
     * <p>Device fingerprints are used to detect suspicious login activities, such as
     * a token being used from a different browser or IP address than it was issued for.
     *
     * @param request the current HTTP request
     * @return the extracted fingerprint string, or {@code null} if not found
     */
    public static String extractDeviceFingerprint(HttpServletRequest request) {
        // 1. Header
        String header = request.getHeader(FINGERPRINT_HEADER);
        if (StringUtils.hasText(header)) {
            return header;
        }

        // 2. Cookie (Reusing logic)
        String cookieVal = getCookieValue(request, FINGERPRINT_HEADER);
        if (StringUtils.hasText(cookieVal)) {
            return cookieVal;
        }

        // 3. Query Parameter
        return request.getParameter(FINGERPRINT_HEADER);
    }

    // ==================== Response Writing ====================

    /**
     * Writes a raw JSON string directly to the HTTP response stream.
     *
     * <p><b>Usage Context:</b> This bypasses Spring MVC's message converters. It should only be used
     * in Filters or early-stage interceptors where you must manually construct the response
     * (e.g., returning a 401 JSON error before the request reaches the DispatcherServlet).
     *
     * @param response   the HTTP response object
     * @param httpStatus the HTTP status code to set (e.g., 401, 403)
     * @param message    the pre-serialized JSON string to write
     * @throws IOException if an I/O error occurs while writing to the response output stream
     */
    public static void write(HttpServletResponse response, int httpStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Using try-with-resources ensures the PrintWriter is flushed and closed properly
        try (PrintWriter writer = response.getWriter()) {
            writer.write(message);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Retrieves a header value from the current thread-bound HTTP request.
     *
     * @param name the name of the HTTP header
     * @return the header value, or {@code null} if not present
     */
    public static String getHeader(String name) {
        return getCurrentRequest().getHeader(name);
    }

    /**
     * Retrieves the current HTTP request from the Spring thread-local context.
     *
     * @return the current {@link HttpServletRequest}
     * @throws IllegalStateException if no request is bound to the current thread
     *                               (e.g., if called asynchronously outside the servlet thread)
     */
    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current HTTP request context found.");
        }
        return attributes.getRequest();
    }

    /**
     * Validates that a provided IP string is not blank and is not explicitly marked as 'unknown'.
     *
     * @param ip the string representing an IP address
     * @return {@code true} if the IP is valid and usable, {@code false} otherwise
     */
    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN_IP_IDENTIFIER.equalsIgnoreCase(ip);
    }

    /**
     * Extracts the first IP address from a potentially comma-separated list.
     *
     * <p>In proxy chains, the {@code X-Forwarded-For} header format is typically
     * {@code client, proxy1, proxy2}. The first element is the true client IP.
     *
     * @param ip the raw IP string from the header
     * @return the trimmed first IP address
     */
    private static String getFirstIp(String ip) {
        if (ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Iterates through the request's cookies to find a value by name.
     *
     * @param request the HTTP request containing the cookies
     * @param name    the exact name of the cookie to find
     * @return the cookie's value, or {@code null} if the cookie does not exist
     */
    private static String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}