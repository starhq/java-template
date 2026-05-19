package com.github.starhq.template.config.security.filter;

import static com.github.starhq.template.common.constant.FilterConstant.EXCLUDE_PATHS;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.time.Instant;

import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.model.dto.RequestContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.github.starhq.template.common.constant.LogConstant;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.LoggerJsonSensitiveHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * A Spring Boot filter that logs detailed information about incoming HTTP
 * requests
 * and outgoing HTTP responses. This includes request path, method, headers,
 * parameters,
 * request body, response status, headers, response body, and processing time.
 * <p>
 * This filter uses {@link ContentCachingRequestWrapper} and
 * {@link ContentCachingResponseWrapper}
 * to enable re-reading of streams for logging purposes without interfering with
 * downstream filters
 * or controllers.
 * <p>
 * Log detail level can be controlled by Spring profiles (e.g., 'prod' will log
 * less verbosely).
 */
@Slf4j
@RequiredArgsConstructor
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private final LoggerJsonSensitiveHelper sensitiveHelper;
    private final EventService eventService; // 注入 Service
    private final boolean isProd;

    private static final int MAX_CONTENT_LENGTH = 2000;

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

    @Override
    protected @NullMarked void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                FilterChain filterChain) throws ServletException, IOException {

        if (!log.isInfoEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        String traceId = IdWorker.getIdStr();
        MDC.put(LogConstant.TRACE, traceId);


        String ip = HttpUtils.getClientIp(request);
        String fingerprint = request.getHeader("X-Device-Fingerprint");
        RequestContext context = new RequestContext(fingerprint, ip);
        RequestContextUtil.setContext(context);

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_CONTENT_LENGTH);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        Instant startTime = Instant.now();
        Exception caughtException = null;

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception e) {
            caughtException = e;
            throw e;
        } finally {
            try {
                long duration = Duration.between(startTime, Instant.now()).toMillis();

                // 1. 控制台日志 (开发调试用)
                logToConsole(traceId, wrappedRequest, wrappedResponse, duration, caughtException);

                // 2. 数据库日志 (前端查询用)
                if (!isProd) {
                    saveToDatabase(traceId, wrappedRequest, wrappedResponse, duration, caughtException);
                }

            } catch (Exception loggingException) {
                log.warn("Failed to log request/response", loggingException);
            } finally {
                wrappedResponse.copyBodyToResponse();
                MDC.remove(LogConstant.TRACE);
                RequestContextUtil.clear();
            }
        }
    }

    /**
     * 构建日志对象并存入数据库
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

            // 处理请求信息
            apiLog.setHeaders(getHeadersAsJson(request));
            apiLog.setParams(getParamsAsJson(request));

            apiLog.setRequestBody(getRequestBodyAsJson(request));

            // 处理响应信息
            apiLog.setResponseBody(getResponseBodyAsJson(response));

            // 处理异常信息
            if (exception != null) {
                apiLog.setExceptionMessage(exception.getMessage());
                apiLog.setExceptionStack(getStackTrace(exception));
            }

            // 异步保存
            eventService.notifyApiLogSave(apiLog);

        } catch (Exception e) {
            log.error("Failed to construct the API log entity", e);
        }
    }

    // --- 辅助方法 ---

    private String getRequestBodyAsJson(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (contentType != null && (contentType.contains("multipart/form-data")
                || contentType.contains("application/octet-stream"))) {
            return "[Binary Data]";
        } else {
            return getBodyString(request.getContentAsByteArray());
        }
    }

    private String getResponseBodyAsJson(ContentCachingResponseWrapper response) {
        String responseContentType = response.getContentType();
        if (responseContentType != null &&
                (responseContentType.startsWith("image/")
                        || responseContentType.startsWith("application/octet-stream"))) {
            return "[Binary Data]";
        } else {
            return getBodyString(response.getContentAsByteArray());
        }
    }

    private String getHeadersAsJson(HttpServletRequest request) {
        String json = sensitiveHelper.mask(request, true, isProd);

        return truncateContent(json);
    }

    private String getParamsAsJson(HttpServletRequest request) {
        String json = sensitiveHelper.mask(request, false, isProd);

        return truncateContent(json);
    }

    private String getBodyString(byte[] content) {
        return truncateContent(filterSensitiveData(content));
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return truncateContent(sw.toString());
    }

    private String filterSensitiveData(byte[] contents) {
        return sensitiveHelper.mask(contents, isProd);
    }

    private String truncateContent(String content) {
        if (StringUtils.hasText(content)) {
            return content.length() > MAX_CONTENT_LENGTH ? content.substring(0, MAX_CONTENT_LENGTH) + "..." : content;
        } else {
            return null;
        }
    }

    // 保留控制台打印逻辑，供开发人员实时查看
    private void logToConsole(String traceId, ContentCachingRequestWrapper request,
                              ContentCachingResponseWrapper response,
                              long duration, Exception exception) {
        // Determine if we should log detailed body/params (only in non-prod debug mode)
        boolean logDetails = !isProd && log.isDebugEnabled();

        if (logDetails) {
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
                logBuilder.append(" - ").append(exception.getMessage());
                logBuilder.append(")");
            }

            appendDetailedLogs(logBuilder, request, response);
            log.debug(logBuilder.toString());
        } else {
            log.info(
                    "HTTP {} {} status={} duration={}ms traceId={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration,
                    traceId);
        }

    }

    private void appendDetailedLogs(StringBuilder builder,
                                    ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response) {

        // 1. Headers
        builder.append("\nHeaders      :").append(getHeadersAsJson(request));

        // 2. Request Body
        builder.append("\nRequest Body: ").append(getRequestBodyAsJson(request));

        // 3. Response Body
        builder.append("\nResponse Body: ").append(getResponseBodyAsJson(response));
    }

}
