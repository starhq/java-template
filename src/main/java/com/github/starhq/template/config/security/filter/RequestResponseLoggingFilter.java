package com.github.starhq.template.config.security.filter;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.starhq.template.common.constant.LogConstant;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.LoggerJsonSensitiveHelper;
import com.github.starhq.template.model.dto.RequestContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

import static com.github.starhq.template.common.constant.FilterConstant.EXCLUDE_PATHS;

/**
 * Advanced HTTP logging filter that intercepts requests to capture comprehensive auditing data.
 *
 * <p>This filter serves two distinct purposes:
 * <ol>
 *   <li><b>Context Population:</b> Extracts client IP and Device Fingerprint, binding them to the current thread
 *       via {@link RequestContextUtil} for downstream filters (like {@link com.github.starhq.template.config.security.filter.JwtAuthenticationFilter}) to access.</li>
 *   <li><b>Auditing & Tracing:</b> Wraps request/response streams to capture bodies, calculates execution time,
 *       generates a unique Trace ID (placed in SLF4J MDC), and asynchronously persists the data to the database.</li>
 * </ol>
 *
 * <p><b>⚠️ Critical Implementation Note:</b> Because HTTP streams can only be read once, this filter uses
 * {@link ContentCachingRequestWrapper} and {@link ContentCachingResponseWrapper}. This stores the payload in memory.
 * The {@link #MAX_CONTENT_LENGTH} is strictly enforced to prevent OutOfMemoryError attacks (e.g., someone uploading a 10GB file).
 *
 * @author starhq
 */
@Slf4j
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private final LoggerJsonSensitiveHelper sensitiveHelper;
    private final EventService eventService;

    /**
     * Profile flag to control logging verbosity and prevent sensitive data from hitting the database.
     */
    private final boolean isProd;

    /**
     * Hard limit to prevent memory exhaustion from large payloads (e.g., file uploads).
     */
    private static final int MAX_CONTENT_LENGTH = 2000;

    /**
     * Short-circuits the filter for static resources or monitoring endpoints to prevent performance degradation.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String exclude : EXCLUDE_PATHS) {
            if (path.startsWith(exclude)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The core execution flow: Wrap -> Execute -> Measure -> Log -> Cleanup.
     *
     * <p><b>Execution Contract:</b> The wrapped response body is NOT automatically copied back to the original response
     * inside the try block. This is a critical safety measure: if the controller throws an exception, we want to
     * capture the error details first. The actual flushing to the client happens in the finally block, guaranteeing
     * the client always receives a response (whether success or error) after logging is complete.
     *
     * @param request     the original HTTP request
     * @param response    the original HTTP response
     * @param filterChain the chain to pass wrapped requests down
     */
    @Override
    protected @NullMarked void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {

        // Performance guard: Skip all logic if INFO logging is disabled at the root level
        if (!log.isInfoEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Setup Tracing Context
        String traceId = IdWorker.getIdStr();
        MDC.put(LogConstant.TRACE, traceId); // Injects TraceID into every log statement automatically

        // 2. Extract and bind Request Context (Used by JWT Filter later)
        String ip = HttpUtils.getClientIp(request);
        String fingerprint = request.getHeader("X-Device-Fingerprint");
        RequestContext context = new RequestContext(fingerprint, ip);
        RequestContextUtil.setContext(context);

        // 3. Wrap streams to enable multiple reads (Logging + Controllers)
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_CONTENT_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Instant startTime = Instant.now();
        Exception caughtException = null;

        try {
            // 4. Execute the actual controller logic
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            caughtException = e; // Capture the exception to log its stack trace, but don't swallow it
            throw e;             // Re-throw to let the GlobalExceptionHandler format the error response
        } finally {
            try {
                long duration = Duration.between(startTime, Instant.now()).toMillis();

                // 5. Output to console (controlled by log levels)
                logToConsole(traceId, wrappedRequest, wrappedResponse, duration, caughtException);

                // 6. Persist to database (controlled by profile)
                if (!isProd) {
                    saveToDatabase(traceId, wrappedRequest, wrappedResponse, duration, caughtException);
                }
            } catch (Exception loggingException) {
                // Absolute fail-safe: Never let logging failures crash the actual HTTP request
                log.warn("Failed to log request/response", loggingException);
            } finally {
                // 7. CRITICAL FLUSH: Copy the cached response body back to the original client response.
                // This MUST happen in the finally block to ensure the client receives a response even if logging failed.
                wrappedResponse.copyBodyToResponse();

                // 8. Cleanup ThreadLocals to prevent memory leaks in Tomcat thread pools
                MDC.remove(LogConstant.TRACE);
                RequestContextUtil.clear();
            }
        }
    }

    /**
     * Constructs the audit log entity and delegates asynchronous persistence to the EventService.
     *
     * <p><b>Environment Guard:</b> This method is wrapped in an {@code if (!isProd)} check at the call site.
     * Storing full HTTP bodies and headers in a production database is generally an anti-pattern due to
     * storage costs, PII compliance (GDPR), and performance overhead. Production should rely on centralized
     * log aggregation (ELK) instead.
     *
     * @param traceId   unique identifier for this request
     * @param request   the cached request wrapper
     * @param response  the cached response wrapper
     * @param duration  execution time in milliseconds
     * @param exception the exception thrown by the controller, if any
     */
    private void saveToDatabase(String traceId, ContentCachingRequestWrapper request,
                                ContentCachingResponseWrapper response,
                                long duration, Exception exception) {
        try {
            SysApiLog apiLog = new SysApiLog();
            apiLog.setTraceId(traceId);
            apiLog.setMethod(request.getMethod());
            apiLog.setUri(request.getRequestURI());
            apiLog.setQueryString(request.getQueryString());
            apiLog.setClientIp(HttpUtils.getClientIp(request));
            apiLog.setHttpStatus(response.getStatus());
            apiLog.setDuration(duration);

            // Process request metadata
            apiLog.setHeaders(getHeadersAsJson(request));
            apiLog.setParams(getParamsAsJson(request));
            apiLog.setRequestBody(getRequestBodyAsJson(request));

            // Process response payload
            apiLog.setResponseBody(getResponseBodyAsJson(response));

            // Capture error details if the controller failed
            if (exception != null) {
                apiLog.setExceptionMessage(exception.getMessage());
                apiLog.setExceptionStack(getStackTrace(exception));
            }

            // Async persistence to minimize latency impact on the HTTP response
            eventService.notifyApiLogSave(apiLog);

        } catch (Exception e) {
            log.error("Failed to construct the API log entity", e);
        }
    }

    // --- Helper methods ---

    /**
     * Extracts the request body, safely handling binary payloads like file uploads.
     *
     * @param request the cached request wrapper
     * @return the body string, or "[Binary Data]" for multipart uploads
     */
    private String getRequestBodyAsJson(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        // Prevent OutOfMemoryError by not caching file uploads
        if (contentType != null && (contentType.contains("multipart/form-data")
                || contentType.contains("application/octet-stream"))) {
            return "[Binary Data]";
        } else {
            return getBodyString(request.getContentAsByteArray());
        }
    }

    /**
     * Extracts the response body, safely handling binary downloads (images, PDFs).
     *
     * @param response the cached response wrapper
     * @return the body string, or "[Binary Data]" for non-text responses
     */
    private String getResponseBodyAsJson(ContentCachingResponseWrapper response) {
        String responseContentType = response.getContentType();
        if (responseContentType != null &&
                (responseContentType.startsWith("image/")
                        || responseContentType.startsWith("application/octet-stream"))) {
            return "[Binary Data]";
        } else if (responseContentType != null && responseContentType.contains("application/json")) {
            return getBodyString(response.getContentAsByteArray());
        } else {
            return truncateContent(new String(response.getContentAsByteArray()));
        }
    }

    /**
     * Masks sensitive header values and truncates the result.
     */
    private String getHeadersAsJson(HttpServletRequest request) {
        String json = sensitiveHelper.mask(request, true, isProd);
        return truncateContent(json);
    }

    /**
     * Masks sensitive query parameters and truncates the result.
     */
    private String getParamsAsJson(HttpServletRequest request) {
        String json = sensitiveHelper.mask(request, false, isProd);
        return truncateContent(json);
    }

    /**
     * Deserializes, masks sensitive fields, and truncates raw byte arrays (Request/Response bodies).
     */
    private String getBodyString(byte[] content) {
        return truncateContent(filterSensitiveData(content));
    }

    /**
     * Converts a Java Exception stack trace into a String format suitable for database VARCHAR columns.
     */
    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return truncateContent(sw.toString());
    }

    /**
     * Delegates raw body bytes to the JSON sensitive data masking utility.
     */
    private String filterSensitiveData(byte[] contents) {
        return sensitiveHelper.mask(contents, isProd);
    }

    /**
     * Safely truncates strings to prevent database field length overflow exceptions (e.g., DataTooLongException).
     *
     * @param content the string to truncate
     * @return the truncated string with an ellipsis, or null if empty
     */
    private String truncateContent(String content) {
        if (StringUtils.hasText(content)) {
            return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) + "..." : content;
        } else {
            return null;
        }
    }

    /**
     * Outputs the request/response details to the application console.
     *
     * <p><b>Performance Strategy:</b>
     * In non-prod environments, if DEBUG is enabled, it builds a massive, highly readable string containing
     * headers, bodies, and stack traces. In production (or if DEBUG is off), it falls back to a highly optimized,
     * single-line SLF4J parameterized log to minimize I/O blocking and string concatenation overhead.
     *
     * @param traceId   the request trace ID
     * @param request   the cached request wrapper
     * @param response  the cached response wrapper
     * @param duration  execution time in milliseconds
     * @param exception the exception thrown by the controller, if any
     */
    private void logToConsole(String traceId, ContentCachingRequestWrapper request,
                              ContentCachingResponseWrapper response,
                              long duration, Exception exception) {
        boolean logDetails = !isProd && log.isDebugEnabled();

        if (logDetails) {
            // Developer mode: Readable multi-line block for local debugging
            StringBuilder logBuilder = new StringBuilder(MAX_CONTENT_LENGTH);
            logBuilder.append("\n=== HTTP Request/Response ===\n");
            logBuilder.append("TraceID : ").append(traceId).append("\n");
            logBuilder.append("Path    : ").append(request.getMethod()).append(" ").append(request.getRequestURI());

            if (StringUtils.hasText(request.getQueryString())) {
                logBuilder.append("?").append(request.getQueryString());
            }

            logBuilder.append("\nIP      : ").append(HttpUtils.getClientIp(request));
            logBuilder.append("\nDuration: ").append(duration).append("ms");
            logBuilder.append("\nStatus  : ").append(response.getStatus());

            if (exception != null) {
                logBuilder.append(" (Exception: ").append(exception.getClass().getSimpleName());
                logBuilder.append(" - ").append(exception.getMessage()).append(")");
            }

            appendDetailedLogs(logBuilder, request, response);
            String detail = logBuilder.toString();
            log.debug(detail);
        } else {
            // Production mode: Ultra-fast parameterized logging, no string concatenation overhead
            log.info("HTTP {} {} status={} duration={}ms traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    traceId);
        }
    }

    /**
     * Appends the heavy, detailed JSON payloads to the debug log builder.
     */
    private void appendDetailedLogs(StringBuilder builder,
                                    ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response) {
        builder.append("\nHeaders      :").append(getHeadersAsJson(request));
        builder.append("\nRequest Body: ").append(getRequestBodyAsJson(request));
        builder.append("\nResponse Body: ").append(getResponseBodyAsJson(response));
    }

}