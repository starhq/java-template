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
 * Utility class for common HTTP-related operations.
 * <p>
 * This includes extracting client IP addresses, JWT tokens, and writing
 * standardized JSON error responses.
 * </p>
 */
@UtilityClass
public class HttpUtils {

    // ==================== Constants ====================

    // IP Related
    private static final String UNKNOWN_IP_IDENTIFIER = "unknown";
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

    public static String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        return getClientIp(request);
    }

    public static String getClientIp(HttpServletRequest request) {
        Objects.requireNonNull(request, "HttpServletRequest must not be null");

        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                return getFirstIp(ip);
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return StringUtils.hasText(remoteAddr) ? remoteAddr : null;
    }

    // ==================== Token & Fingerprint Methods ====================

    /**
     * Extracts token by checking: Header -> Cookie -> Query Param.
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
     * Extracts fingerprint by checking: Header -> Cookie -> Query Param.
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
     * Writes a standardized JSON error response.
     */
    public static void write(HttpServletResponse response, int httpStatus, String message) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        try (PrintWriter writer = response.getWriter()) {
            writer.write(message);
        }
    }

    // ==================== Helper Methods ====================

    public static String getHeader(String name) {
        return getCurrentRequest().getHeader(name);
    }

    private static HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current HTTP request context found.");
        }
        return attributes.getRequest();
    }

    private static boolean isValidIp(String ip) {
        return StringUtils.hasText(ip) && !UNKNOWN_IP_IDENTIFIER.equalsIgnoreCase(ip);
    }

    private static String getFirstIp(String ip) {
        // X-Forwarded-For format: client, proxy1, proxy2
        if (ip.contains(",")) {
            return ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Helper to extract value from cookie by name.
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
