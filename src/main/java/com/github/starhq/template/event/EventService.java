package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.event.domain.ApiLogEvent;
import com.github.starhq.template.event.domain.AuditLogEvent;
import com.github.starhq.template.event.domain.CacheEvictEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

/**
 * Facade service for publishing domain events across the application.
 * <p>
 * This class provides a centralized, type-safe API for publishing events such as
 * cache invalidation, audit log persistence, and API log recording. By encapsulating
 * {@link ApplicationEventPublisher}, it decouples event emission from Spring framework
 * details, enabling easier testing, consistent event naming, and future extensibility.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Cache Invalidation</strong>: Publish {@link CacheEvictEvent} after data updates to maintain cache consistency</li>
 *     <li><strong>Audit Logging</strong>: Publish {@link AuditLogEvent} for asynchronous, non-blocking audit trail recording</li>
 *     <li><strong>API Telemetry</strong>: Publish {@link ApiLogEvent} for request/response logging without impacting HTTP response latency</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Single Responsibility</strong>: Focuses solely on event publication; listeners handle processing logic</li>
 *     <li><strong>Open/Closed</strong>: New event types can be added without modifying existing methods</li>
 *     <li><strong>Testability</strong>: Easy to mock {@code ApplicationEventPublisher} for unit testing business services</li>
 *     <li><strong>Type Safety</strong>: Generic method signatures prevent accidental event payload mismatches</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class UserService {
 *     @Autowired private EventService eventService;
 *
 *     @Transactional
 *     public void updateUserProfile(Long userId, UserDTO dto) {
 *         // 1. Business logic
 *         userMapper.updateById(converter.toEntity(dto));
 *
 *         // 2. Publish events (non-blocking, decoupled)
 *         eventService.notifyCacheEvict(
 *             List.of(userId),
 *             List.of("user:simple", "user:full")
 *         );
 *
 *         eventService.notifyAuditLogSave(
 *             AuditLogBuilder.build("USER_UPDATED", TargetType.USER, userId, dto, SecurityContextUtils.getUserId())
 *         );
 *     }
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-15
 * @see ApplicationEventPublisher
 * @see CacheEvictEvent
 * @see AuditLogEvent
 * @see ApiLogEvent
 */
@RequiredArgsConstructor
public class EventService {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publishes a {@link CacheEvictEvent} to invalidate cached entries across multiple regions.
     * <p>
     * This method should be called after successful data modifications to ensure cache
     * consistency with the database. The event is processed asynchronously by
     * {@link com.github.starhq.template.event.listener.CacheEvictListener} after
     * the originating transaction commits.
     * <p>
     * <strong>Generic Type Parameter:</strong>
     * <p>
     * The {@code <T>} type represents the cache key type, typically:
     * <ul>
     *     <li>{@link Long} for numeric entity IDs (e.g., {@code user:123})</li>
     *     <li>{@link String} for composite or natural keys (e.g., {@code "dict:user_status"})</li>
     *     <li>Custom serializable types for complex cache key objects</li>
     * </ul>
     * Ensure keys implement proper {@code equals()} and {@code hashCode()} for reliable matching.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep key collections small (< 1000 items) to avoid event payload bloat</li>
     *     <li>Use cache name constants from {@code CacheConstants} to avoid typos</li>
     *     <li>Publish events <strong>after</strong> business transaction commits (or use {@code @TransactionalEventListener})</li>
     *     <li>For large-scale invalidation, consider pattern-based eviction via custom cache operations</li>
     * </ul>
     *
     * @param <T>        the type of cache keys to be evicted
     * @param keys       the collection of cache keys to invalidate; must not be {@code null} or empty
     * @param cacheNames the collection of logical cache region names to target; must not be {@code null} or empty
     * @throws IllegalArgumentException if either parameter is {@code null} or empty
     * @see CacheEvictEvent
     */
    public <T> void notifyCacheEvict(List<T> keys, List<String> cacheNames) {
        eventPublisher.publishEvent(new CacheEvictEvent<>(keys, cacheNames));
    }

    /**
     * Publishes an {@link AuditLogEvent} for asynchronous audit trail persistence.
     * <p>
     * This method should be called after business operations that require audit logging.
     * The event is processed asynchronously by {@link com.github.starhq.template.event.listener.AuditLogListener}
     * after the originating transaction commits, ensuring logs are only recorded for
     * successfully committed operations.
     * <p>
     * <strong>Privacy & Security Requirements:</strong>
     * <ul>
     *     <li><strong>Mask PII</strong>: Ensure {@code auditLog.value} has sensitive fields (password, token, idCard) redacted before publishing</li>
     *     <li><strong>Limit Payload Size</strong>: Truncate {@code requestBody/responseBody} to prevent storage abuse (e.g., max 8KB)</li>
     *     <li><strong>Avoid Secrets</strong>: Never include authentication tokens, API keys, or encryption secrets</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Build audit logs via a dedicated {@code AuditLogBuilder} to enforce masking rules consistently</li>
     *     <li>Include {@code traceId} for distributed tracing correlation</li>
     *     <li>Use {@code @TransactionalEventListener(phase = AFTER_COMMIT)} in listeners to avoid logging rolled-back operations</li>
     * </ul>
     *
     * @param auditLog the {@link SysAuditLog} entry to be persisted; must not be {@code null}
     * @throws IllegalArgumentException if {@code auditLog} is {@code null}
     * @see AuditLogEvent
     * @see SysAuditLog
     */
    public void notifyAuditLogSave(SysAuditLog auditLog) {
        eventPublisher.publishEvent(new AuditLogEvent(auditLog));
    }

    /**
     * Publishes an {@link ApiLogEvent} for asynchronous API telemetry persistence.
     * <p>
     * This method should be called by API interceptors or filters after request processing
     * completes. The event is processed asynchronously by {@link com.github.starhq.template.event.listener.ApiLogEventListener}
     * to avoid blocking HTTP response latency.
     * <p>
     * <strong>Privacy & Security Requirements:</strong>
     * <ul>
     *     <li><strong>Mask PII</strong>: Ensure {@code apiLog.requestBody/responseBody} has sensitive fields redacted before publishing</li>
     *     <li><strong>Limit Payload Size</strong>: Truncate large payloads to prevent storage abuse (e.g., max 4KB)</li>
     *     <li><strong>Production Safety</strong>: Disable {@code exceptionStack} persistence in production; use {@code exceptionMessage} for alerts</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Build API logs via a dedicated {@code ApiLogBuilder} to enforce masking rules consistently</li>
     *     <li>Include {@code traceId} from MDC for distributed tracing correlation</li>
     *     <li>Use {@code @Async} with dedicated thread pool in listeners to avoid blocking request threads</li>
     *     <li>Consider external storage (Elasticsearch/ClickHouse) for high-volume log scenarios</li>
     * </ul>
     *
     * @param apiLog the {@link SysApiLog} entry to be persisted; must not be {@code null}
     * @throws IllegalArgumentException if {@code apiLog} is {@code null}
     * @see ApiLogEvent
     * @see SysApiLog
     */
    public void notifyApiLogSave(SysApiLog apiLog) {
        eventPublisher.publishEvent(new ApiLogEvent(apiLog));
    }

}