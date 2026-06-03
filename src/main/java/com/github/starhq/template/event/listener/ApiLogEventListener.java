package com.github.starhq.template.event.listener;

import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.event.domain.ApiLogEvent;
import com.github.starhq.template.mapper.SysApiLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

/**
 * Asynchronous event listener for persisting API request/response logs.
 * <p>
 * This component subscribes to {@link ApiLogEvent} published by API interceptors
 * or filters, and persists {@link SysApiLog} entries to the database in a
 * non-blocking manner. Designed to decouple audit logging from critical
 * request-processing paths, ensuring low-latency HTTP responses even under
 * high traffic or slow log storage.
 * <p>
 * <strong>Processing Flow:</strong>
 * <ol>
 *     <li>API interceptor captures request/response metadata and publishes {@code ApiLogEvent}</li>
 *     <li>This listener receives the event asynchronously via dedicated thread pool</li>
 *     <li>Log entry is persisted to {@code sys_api_log} table via MyBatis-Plus mapper</li>
 *     <li>Failures are logged with WARN level and optionally routed to fallback storage</li>
 * </ol>
 * <p>
 * <strong>Thread Safety & Performance:</strong>
 * <ul>
 *     <li>Annotated with {@code @Async} to execute in separate thread pool (configure via {@code apiLogExecutor})</li>
 *     <li>Stateless design: safe for concurrent event processing without synchronization</li>
 *     <li>Exception isolation: log persistence failures do not propagate to business logic</li>
 * </ul>
 * <p>
 * <strong>Fallback Strategy:</strong>
 * <p>
 * If database insertion fails (e.g., connection timeout, constraint violation), the event is:
 * <ul>
 *     <li>Logged at WARN level with full stack trace for troubleshooting</li>
 *     <li>Optionally routed to local file or dead-letter queue for later replay (implement via extension)</li>
 *     <li>Never re-thrown to avoid blocking the async thread pool</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-05
 * @see ApiLogEvent
 * @see SysApiLog
 * @see SysApiLogMapper
 * @see org.springframework.scheduling.annotation.Async
 */
@Slf4j
@RequiredArgsConstructor
public class ApiLogEventListener {

    private final SysApiLogMapper apiLogMapper;

    /**
     * Handles {@link ApiLogEvent} asynchronously to persist API audit logs.
     * <p>
     * This method is invoked by Spring's event multicaster when an {@code ApiLogEvent}
     * is published. Execution is delegated to a dedicated async thread pool to avoid
     * blocking the original request thread.
     * <p>
     * <strong>Processing Steps:</strong>
     * <ol>
     *     <li>Null-check: Skip processing if event or payload is null (defensive programming)</li>
     *     <li>Persistence: Insert {@link SysApiLog} into database via MyBatis-Plus mapper</li>
     *     <li>Error Handling: Catch all exceptions, log with context, and prevent propagation</li>
     * </ol>
     * <p>
     * <strong>Async Configuration:</strong>
     * <p>
     * Ensure a thread pool bean named {@code apiLogExecutor} is configured:
     * <pre>
     * {@code
     * @Bean("apiLogExecutor")
     * public Executor apiLogExecutor() {
     *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
     *     executor.setCorePoolSize(8);
     *     executor.setMaxPoolSize(32);
     *     executor.setQueueCapacity(2000);
     *     executor.setThreadNamePrefix("api-log-async-");
     *     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
     *     executor.initialize();
     *     return executor;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Security & Privacy:</strong>
     * <ul>
     *     <li>Ensure {@code apiLog} has sensitive fields (password, token, PII) masked before event publication</li>
     *     <li>Never log raw request/response bodies at ERROR level to prevent credential leakage</li>
     *     <li>Use structured logging (JSON) for easier parsing by log aggregation systems</li>
     * </ul>
     *
     * @param event the {@link ApiLogEvent} containing the {@link SysApiLog} to persist, may be {@code null}
     * @see ApiLogEvent#apiLog()
     * @see SysApiLogMapper#insert(Object)
     * @see Async
     */
    @Async
    public void handleEvictEvent(ApiLogEvent event) {
        // Defensive null check: skip if event is null (should not happen with proper publishing)
        if (event == null) {
            return;
        }

        SysApiLog apiLog = event.apiLog();
        // Additional null check for payload
        if (apiLog == null) {
            return;
        }

        try {
            // Persist to database (MyBatis-Plus auto-fills audit fields if configured)
            apiLogMapper.insert(apiLog);
        } catch (Exception e) {
            // Log failure with context but do not re-throw to avoid blocking async pool
            // Include traceId for correlation if available
            String traceId = apiLog.getTraceId() != null ? apiLog.getTraceId() : "unknown";
            log.warn("Failed to persist API log [traceId={}]: {}", traceId, e.getMessage(), e);
        }
    }
}
