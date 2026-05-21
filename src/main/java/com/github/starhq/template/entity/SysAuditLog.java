package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.common.enums.TargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

/**
 * Entity class representing a system audit log entry for tracking business operations.
 * <p>
 * This class maps to the {@code sys_audit_log} table and extends {@link BaseCreatorEntity}
 * to provide complete audit trail: who ({@code createdBy}) performed what action ({@code action})
 * on which resource ({@code targetType} + {@code targetId}) at when ({@code createdAt}),
 * with optional operation details ({@code value}).
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Security Compliance</strong>: Track sensitive operations (data export, permission changes) for regulatory audits</li>
 *     <li><strong>Troubleshooting</strong>: Reconstruct user actions leading to data inconsistencies or business errors</li>
 *     <li><strong>Behavior Analysis</strong>: Analyze feature adoption, user workflows, or anomaly detection patterns</li>
 *     <li><strong>Accountability</strong>: Attribute actions to specific users for internal governance</li>
 * </ul>
 * <p>
 * <strong>Data Integrity Guarantee:</strong>
 * <p>
 * Audit logs are <strong>append-only</strong> and should NEVER be modified or deleted after creation
 * (except for compliance-mandated retention policies). Implement database-level constraints or
 * application-layer safeguards to enforce immutability.
 * <p>
 * <strong>Privacy & Security:</strong>
 * <p>
 * The {@code value} field may contain sensitive business data. Always apply field-level masking
 * for PII (personal identifiable information) and business secrets before persistence.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseCreatorEntity
 * @see TargetType
 * @see TableName
 * @see <a href="https://en.wikipedia.org/wiki/Audit_trail">Audit Trail (Wikipedia)</a>
 */
@Data
@Alias("auditLog")
@TableName("sys_audit_log")
@EqualsAndHashCode(callSuper = false)
public class SysAuditLog extends BaseCreatorEntity {

    /**
     * The human-readable description of the operation performed.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code "USER_LOGIN"} — User authentication event</li>
     *     <li>{@code "ROLE_ASSIGNED"} — Permission modification</li>
     *     <li>{@code "DATA_EXPORT"} — Sensitive data extraction</li>
     *     <li>{@code "CONFIG_UPDATED"} — System configuration change</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use {@code UPPER_SNAKE_CASE} for consistency and easy filtering</li>
     *     <li>Maintain a centralized action dictionary for documentation and i18n</li>
     *     <li>Avoid dynamic/user-generated content to prevent injection risks</li>
     * </ul>
     * <p>
     * <strong>Query Tips:</strong>
     * <pre>
     * {@code
     * -- Find all permission changes
     * SELECT * FROM sys_audit_log WHERE action LIKE 'ROLE_%' OR action LIKE 'PERM_%';
     *
     * -- Audit trail for specific user
     * SELECT action, target_type, target_id, created_at
     * FROM sys_audit_log
     * WHERE created_by = 1001
     * ORDER BY created_at DESC;
     * }
     * </pre>
     */
    private String action;

    /**
     * The unique identifier of the business entity affected by this operation.
     * <p>
     * Interpreted in conjunction with {@link #targetType}:
     * <ul>
     *     <li>If {@code targetType = USER}, then {@code targetId} references {@code sys_user.id}</li>
     *     <li>If {@code targetType = ROLE}, then {@code targetId} references {@code sys_role.id}</li>
     *     <li>If {@code targetType = CONFIG}, then {@code targetId} may be {@code null} for global settings</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: May be {@code null} for operations on collections or global resources</li>
     *     <li>Index Recommendation: Add composite index {@code (target_type, target_id)} for efficient reverse lookups</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Find all operations on a specific user
     * SELECT action, created_by, created_at
     * FROM sys_audit_log
     * WHERE target_type = 'USER' AND target_id = 12345
     * ORDER BY created_at DESC;
     * }
     * </pre>
     *
     * @see TargetType
     */
    private Long targetId;

    /**
     * The categorized type of the target entity being operated on.
     * <p>
     * Uses the {@link TargetType} enum to ensure type safety and enable efficient
     * filtering without string matching. Common values include:
     * <ul>
     *     <li>{@code USER} — User account operations</li>
     *     <li>{@code ROLE} — Role/permission management</li>
     *     <li>{@code MENU} — Navigation/resource configuration</li>
     *     <li>{@code DICT} — Dictionary data maintenance</li>
     *     <li>{@code CONFIG} — System parameter updates</li>
     * </ul>
     * <p>
     * <strong>Extensibility:</strong>
     * <p>
     * When adding new business modules, extend the {@link TargetType} enum rather than
     * using arbitrary strings. This enables:
     * <ul>
     *     <li>Compile-time validation of audit targets</li>
     *     <li>Auto-completion in IDEs for audit logging code</li>
     *     <li>Centralized documentation of auditable entity types</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong>
     * <p>
     * MyBatis-Plus automatically converts enum to its {@code name()} or configured
     * value via {@code EnumTypeHandler}. Ensure database column type is {@code VARCHAR}
     * with sufficient length for enum names.
     *
     * @see TargetType
     * @see com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler
     */
    private TargetType targetType;

    /**
     * The serialized JSON representation of operation details or state changes.
     * <p>
     * Typical structures:
     * <pre>
     * {@code
     * // For CREATE operations
     * {"operation":"CREATE","newState":{"username":"alice","email":"a@example.com"}}
     *
     * // For UPDATE operations
     * {"operation":"UPDATE","changes":{"email":{"old":"a@old.com","new":"a@new.com"}}}
     *
     * // For DELETE operations
     * {"operation":"DELETE","deletedState":{"id":123,"username":"alice"}}
     * }
     * </pre>
     * <p>
     * <strong>Privacy & Security Requirements:</strong>
     * <ul>
     *     <li><strong>Mask PII</strong>: Redact fields like {@code password}, {@code idCard}, {@code phone}, {@code email} before serialization</li>
     *     <li><strong>Limit Size</strong>: Truncate to max length (e.g., 8KB) to prevent storage abuse or DoS</li>
     *     <li><strong>Avoid Secrets</strong>: Never log authentication tokens, API keys, or encryption secrets</li>
     * </ul>
     * <p>
     * <strong>Implementation Pattern:</strong>
     * <pre>
     * {@code
     * // Service layer example with masking
     * public void logUserUpdate(UserDTO dto, Long operatorId) {
     *     Map<String, Object> changes = new HashMap<>();
     *     changes.put("email", dto.getEmail());
     *     // Mask sensitive fields
     *     if (dto.getPassword() != null) {
     *         changes.put("password", "***"); // Never log actual password
     *     }
     *
     *     SysAuditLog log = new SysAuditLog();
     *     log.setAction("USER_UPDATED");
     *     log.setTargetType(TargetType.USER);
     *     log.setTargetId(dto.getId());
     *     log.setValue(JsonUtils.toJson(changes)); // Use centralized JSON utility
     *     log.setCreatedBy(operatorId);
     *     auditLogMapper.insert(log);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Storage Optimization:</strong>
     * <ul>
     *     <li>Use {@code JSON} column type (MySQL 5.7+) for efficient querying</li>
     *     <li>Consider external storage (Elasticsearch) for high-volume audit scenarios</li>
     *     <li>Implement TTL-based archival for logs older than retention period</li>
     * </ul>
     *
     */
    private String value;

}