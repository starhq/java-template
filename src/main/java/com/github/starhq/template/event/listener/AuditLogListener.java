package com.github.starhq.template.event.listener;

import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.event.AuditLogEvent;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Asynchronous transactional event listener for persisting system audit logs.
 * <p>
 * This component subscribes to {@link AuditLogEvent} published by business services,
 * and persists {@link SysAuditLog} entries to the database <strong>after the
 * originating transaction commits</strong>. By using {@link TransactionalEventListener}
 * with default {@link TransactionPhase#AFTER_COMMIT} phase, it ensures:
 * <ul>
 *     <li><strong>Tx Boundary Safety</strong>: Audit logs are only recorded for successfully committed operations</li>
 *     <li><strong>Non-blocking Design</strong>: Async execution decouples audit persistence from business response latency</li>
 *     <li><strong>Failure Isolation</strong>: Log persistence errors do not rollback or affect core business transactions</li>
 * </ul>
 * <p>
 * <strong>Processing Flow:</strong>
 * <ol>
 *     <li>Business service performs operation and publishes {@code AuditLogEvent} (within transaction)</li>
 *     <li>Spring transaction manager commits the business transaction</li>
 *     <li>This listener receives the event asynchronously via dedicated thread pool</li>
 *     <li>{@link SysAuditLog} is persisted to {@code sys_audit_log} table via MyBatis-Plus mapper</li>
 *     <li>Failures are logged with WARN level and optionally routed to fallback storage</li>
 * </ol>
 * <p>
 * <strong>Thread Safety & Performance:</strong>
 * <ul>
 *     <li>Annotated with {@code @Async} to execute in separate thread pool (configure via {@code auditLogExecutor})</li>
 *     <li>Stateless design: safe for concurrent event processing without synchronization</li>
 *     <li>Exception isolation: caught exceptions never propagate to avoid blocking async thread pool</li>
 * </ul>
 * <p>
 * <strong>Fallback Strategy:</strong>
 * <p>
 * If database insertion fails (e.g., connection timeout, constraint violation), the event is:
 * <ul>
 *     <li>Logged at WARN level with traceId/context for troubleshooting</li>
 *     <li>Optionally routed to local file or dead-letter queue for later replay (implement via extension)</li>
 *     <li>Never re-thrown to prevent async thread pool exhaustion</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-29
 * @see AuditLogEvent
 * @see SysAuditLog
 * @see SysAuditLogMapper
 * @see TransactionalEventListener
 * @see org.springframework.scheduling.annotation.Async
 */
@Slf4j
@RequiredArgsConstructor
public class AuditLogListener {

    private final SysAuditLogMapper auditLogMapper;

    /**
     * Handles {@link AuditLogEvent} asynchronously after transaction commit to persist audit logs.
     * <p>
     * This method is invoked by Spring's transactional event multicaster <strong>after the
     * originating transaction successfully commits</strong>. Execution is delegated to a
     * dedicated async thread pool to avoid blocking business threads.
     * <p>
     * <strong>Processing Steps:</strong>
     * <ol>
     *     <li>Null-check: Skip processing if event or payload is null (defensive programming)</li>
     *     <li>Persistence: Insert {@link SysAuditLog} into database via MyBatis-Plus mapper</li>
     *     <li>Error Handling: Catch all exceptions, log with context (traceId/action), and prevent propagation</li>
     * </ol>
     * <p>
     * <strong>Async Configuration:</strong>
     * <p>
     * Ensure a thread pool bean named {@code auditLogExecutor} is configured:
     * <pre>
     * {@code
     * @Bean("auditLogExecutor")
     * public Executor auditLogExecutor() {
     *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
     *     executor.setCorePoolSize(4); // Audit logs are lower priority than business logic
     *     executor.setMaxPoolSize(16);
     *     executor.setQueueCapacity(1000);
     *     executor.setThreadNamePrefix("audit-log-async-");
     *     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
     *     executor.initialize();
     *     return executor;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Transaction Phase:</strong>
     * <p>
     * By default, {@code @TransactionalEventListener} uses {@link TransactionPhase#AFTER_COMMIT}.
     * This ensures audit logs are only recorded for successfully committed operations.
     * If different behavior is needed (e.g., log before commit), specify explicitly:
     * <pre>
     * {@code
     * @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
     * }
     * </pre>
     * <p>
     * <strong>Security & Privacy:</strong>
     * <ul>
     *     <li>Ensure {@code sysAuditLog} has sensitive fields (password, token, PII) masked before event publication</li>
     *     <li>Never log raw {@code value} field at ERROR level to prevent credential leakage</li>
     *     <li>Use structured logging (JSON) for easier parsing by log aggregation systems</li>
     * </ul>
     *
     * @param event the {@link AuditLogEvent} containing the {@link SysAuditLog} to persist, may be {@code null}
     * @see AuditLogEvent#auditLog()
     * @see SysAuditLogMapper#insert(Object)
     * @see Async
     * @see TransactionalEventListener
     */
    @Async
    @TransactionalEventListener
    public void handleEvictEvent(AuditLogEvent event) {
        // Defensive null check: skip if event is null (should not happen with proper publishing)
        if (event == null) {
            return;
        }

        SysAuditLog auditLog = event.auditLog();
        // Additional null check for payload
        if (auditLog == null) {
            return;
        }

        try {
            // Persist to database (MyBatis-Plus auto-fills audit fields if MetaObjectHandler configured)
            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // Log failure with context but do not re-throw to avoid blocking async pool
            // Include traceable identifiers for correlation
            String action = auditLog.getAction() != null ? auditLog.getAction() : "unknown";
            Long targetId = auditLog.getTargetId();
            log.warn("Failed to persist audit log [action={}, targetId={}]: {}",
                    action, targetId, e.getMessage(), e);
        }
    }
}
