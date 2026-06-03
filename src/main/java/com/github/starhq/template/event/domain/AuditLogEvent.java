package com.github.starhq.template.event.domain;

import com.github.starhq.template.entity.SysAuditLog;

/**
 * Application event for asynchronous audit log persistence.
 * <p>
 * This record encapsulates a {@link SysAuditLog} instance that should be
 * persisted to the database or external audit storage. By publishing this
 * event via Spring's {@link org.springframework.context.ApplicationEventPublisher},
 * business logic can decouple audit recording from critical transaction paths,
 * improving response time and system resilience.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Non-blocking Audit</strong>: Record user actions without delaying business response</li>
 *     <li><strong>Tx Boundary Safety</strong>: Persist audit logs after business transaction commits to avoid rollback coupling</li>
 *     <li><strong>Batch Aggregation</strong>: Aggregate multiple events for bulk insert optimization</li>
 *     <li><strong>Failure Isolation</strong>: Audit persistence failures do not affect core business logic</li>
 * </ul>
 * <p>
 * <strong>Event Lifecycle:</strong>
 * <pre>
 * {@code
 * // 1. Business service publishes event (within or after transaction)
 * @Service
 * public class UserService {
 *     @Autowired private ApplicationEventPublisher eventPublisher;
 *
 *     @Transactional
 *     public void updateUserProfile(Long userId, UserDTO dto) {
 *         // Business logic
 *         userMapper.updateById(converter.toEntity(dto));
 *
 *         // Publish audit event (non-blocking)
 *         SysAuditLog auditLog = buildAuditLog("USER_UPDATED", userId, dto);
 *         eventPublisher.publishEvent(new AuditLogEvent(auditLog));
 *     }
 * }
 *
 * // 2. Async listener persists audit log
 * @Component
 * public class AuditLogPersistenceListener {
 *     @Autowired private AuditLogMapper auditLogMapper;
 *
 *     @EventListener
 *     @Async("auditExecutor") // Dedicated thread pool for audit tasks
 *     @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Ensure business tx committed first
 *     public void handleAuditLog(AuditLogEvent event) {
 *         try {
 *             auditLogMapper.insert(event.auditLog());
 *         } catch (Exception e) {
 *             // Fallback: log to local file or dead-letter queue
 *             log.error("Failed to persist audit log: {}", event.auditLog(), e);
 *             auditFallbackService.recordToLocalFile(event.auditLog());
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * @param auditLog the audit log entry to be persisted
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-16
 * @see SysAuditLog
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.transaction.event.TransactionalEventListener
 */
public record AuditLogEvent(
        SysAuditLog auditLog
) {

}