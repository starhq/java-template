package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysApiLog;

/**
 * Application event for asynchronous API request/response log persistence.
 * <p>
 * This record encapsulates a {@link SysApiLog} instance that should be
 * persisted to the database or external log storage (e.g., Elasticsearch, OSS).
 * By publishing this event via Spring's {@link org.springframework.context.ApplicationEventPublisher},
 * business logic can decouple API audit recording from critical request-processing paths,
 * improving response latency and system resilience under high traffic.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Non-blocking Logging</strong>: Record API telemetry without delaying HTTP response</li>
 *     <li><strong>Tx Boundary Safety</strong>: Persist logs after business transaction commits to avoid rollback coupling</li>
 *     <li><strong>High-Throughput Aggregation</strong>: Batch multiple events for bulk insert optimization in high-QPS scenarios</li>
 *     <li><strong>Failure Isolation</strong>: Log persistence failures do not affect core API functionality</li>
 * </ul>
 * <p>
 * <strong>Event Lifecycle:</strong>
 * <pre>
 * {@code
 * // 1. API interceptor publishes event (after request processing)
 * @Component
 * public class ApiLogInterceptor implements HandlerInterceptor {
 *     @Autowired private ApplicationEventPublisher eventPublisher;
 *
 *     @Override
 *     public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
 *                                Object handler, Exception ex) {
 *         SysApiLog apiLog = buildApiLog(request, response, ex);
 *         eventPublisher.publishEvent(new ApiLogEvent(apiLog)); // Non-blocking
 *     }
 * }
 *
 * // 2. Async listener persists API log
 * @Component
 * public class ApiLogPersistenceListener {
 *     @Autowired private ApiLogMapper apiLogMapper;
 *
 *     @EventListener
 *     @Async("apiLogExecutor") // Dedicated thread pool for log tasks
 *     public void handleApiLog(ApiLogEvent event) {
 *         try {
 *             apiLogMapper.insert(event.apiLog());
 *         } catch (Exception e) {
 *             // Fallback: log to local file or dead-letter queue
 *             log.error("Failed to persist API log: {}", event.apiLog().getTraceId(), e);
 *             logFallbackService.recordToLocalFile(event.apiLog());
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * @param apiLog the API log entry to be persisted
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-05
 * @see SysApiLog
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.web.servlet.HandlerInterceptor
 */
public record ApiLogEvent(
        SysApiLog apiLog
) {

}