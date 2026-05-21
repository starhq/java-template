package com.github.starhq.template.model.vo.auditlog;

import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * View object for paginated audit log responses in admin console or API clients.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit common audit trail fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and adds audit-specific business fields for comprehensive operation tracking.
 * Designed for rendering audit logs in management interfaces with full context.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Admin Console</strong>: Display paginated audit logs with filtering by action, target, time range</li>
 *     <li><strong>Security Analysis</strong>: Investigate suspicious activities by reviewing detailed operation records</li>
 *     <li><strong>Compliance Reporting</strong>: Export filtered audit trails for regulatory audits (GDPR, SOX, PCI-DSS)</li>
 *     <li><strong>Troubleshooting</strong>: Reconstruct user actions leading to data inconsistencies or business errors</li>
 * </ul>
 * <p>
 * <strong>Field Sensitivity & Privacy:</strong>
 * <p>
 * The {@code value} field may contain serialized operation details with sensitive data.
 * Always ensure:
 * <ul>
 *     <li><strong>Masking at Source</strong>: Sensitive fields (password, token, PII) are redacted before persisting to {@code SysAuditLog}</li>
 *     <li><strong>Access Control</strong>: Audit log queries are restricted to authorized administrators with MFA</li>
 *     <li><strong>Response Filtering</strong>: Consider field-level filtering based on caller permissions for multi-tenant scenarios</li>
 * </ul>
 * <p>
 * <strong>Serialization Strategy:</strong>
 * <p>
 * The {@code targetId} field uses {@code @JsonSerialize(using = ToStringSerializer.class)}
 * to convert {@link Long} to {@code String} in JSON output. This prevents precision loss
 * when consuming APIs from JavaScript/TypeScript clients (which use 53-bit integers).
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-10
 * @see BaseAuditVO
 * @see TargetType
 * @see com.github.starhq.template.entity.SysAuditLog
 * @see com.github.starhq.template.service.AuditLogService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AuditLogPageVO extends BaseAuditVO {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -7698919411727493781L;

    /**
     * Human-readable action code describing the operation performed.
     * <p>
     * Typical values follow {@code MODULE:RESOURCE:ACTION} convention:
     * <ul>
     *     <li>{@code "USER:CREATE"} — New user account registration</li>
     *     <li>{@code "ROLE:ASSIGN"} — Permission assignment to role</li>
     *     <li>{@code "CONFIG:UPDATE"} — System parameter modification</li>
     *     <li>{@code "DATA:EXPORT"} — Sensitive data extraction</li>
     * </ul>
     * <p>
     * <strong>Query & Display:</strong>
     * <ul>
     *     <li>Used for filtering audit logs by operation type in admin UI</li>
     *     <li>Should be mapped to localized labels via i18n keys for multi-language support</li>
     *     <li>Consider adding action category (e.g., "Security", "Configuration") for grouped display</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Define action codes as constants in {@code AuditActionConstants} to avoid typos</li>
     *     <li>Document each action code's business meaning and audit requirements</li>
     *     <li>Use uppercase with underscores for consistency and easy filtering</li>
     * </ul>
     *
     */
    private String action;

    /**
     * The unique identifier of the business entity affected by this audit operation.
     * <p>
     * Interpreted in conjunction with {@link #targetType}:
     * <ul>
     *     <li>If {@code targetType = USER}, then {@code targetId} references {@code sys_user.id}</li>
     *     <li>If {@code targetType = ROLE}, then {@code targetId} references {@code sys_role.id}</li>
     *     <li>If {@code targetType = CONFIG}, then {@code targetId} may be {@code null} for global settings</li>
     * </ul>
     * <p>
     * <strong>Serialization Strategy:</strong>
     * <p>
     * Annotated with {@code @JsonSerialize(using = ToStringSerializer.class)} to
     * convert the {@link Long} value to a {@code String} in JSON output. This prevents
     * precision loss when the API is consumed by JavaScript/TypeScript clients, which
     * represent integers as 64-bit floats with only 53 bits of precision.
     * <pre>
     * {@code
     * // Without ToStringSerializer:
     * { "targetId": 9007199254740993 }  // May be truncated in JS
     *
     * // With ToStringSerializer:
     * { "targetId": "9007199254740993" }  // Safe string representation
     * }
     * </pre>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * // Find all operations on a specific user
     * GET /api/v1/audit-logs?targetType=USER&targetId=12345
     *
     * // Reverse lookup: what happened to this resource?
     * SELECT action, created_at, created_by FROM sys_audit_log
     * WHERE target_type = 'CONFIG' AND target_id = 789
     * ORDER BY created_at DESC;
     * }
     * </pre>
     * <p>
     * <strong>Nullability:</strong>
     * <ul>
     *     <li>May be {@code null} for operations on collections or global resources</li>
     *     <li>When {@code null}, the {@code value} field should provide additional context</li>
     * </ul>
     *
     * @see TargetType
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /**
     * The categorized type of the target entity being operated on.
     * <p>
     * Uses the {@link TargetType} enum to ensure type safety and enable efficient
     * filtering without string matching. Common values include:
     * <ul>
     *     <li>{@code USER} — User account operations (create, update, delete, login)</li>
     *     <li>{@code ROLE} — Role/permission management actions</li>
     *     <li>{@code MENU} — Navigation configuration changes</li>
     *     <li>{@code BUTTON} — UI permission modifications</li>
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
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue/React: Map enum to display label
     * const targetTypeLabels = {
     *   USER: 'User Account',
     *   ROLE: 'Role/Permission',
     *   MENU: 'Navigation Menu',
     *   // ...
     * };
     *
     * // Display in audit log table
     * <td>{{ targetTypeLabels[log.targetType] }}</td>
     * }
     * </pre>
     *
     * @see TargetType
     * @see com.github.starhq.template.common.enums.TargetType
     */
    private TargetType targetType;

    /**
     * Serialized JSON representation of operation details or state changes.
     * <p>
     * Typical structures:
     * <pre>
     * {@code
     * // For CREATE operations
     * {
     *   "operation": "CREATE",
     *   "newState": { "username": "alice", "email": "a@example.com" }
     * }
     *
     * // For UPDATE operations
     * {
     *   "operation": "UPDATE",
     *   "changes": {
     *     "email": { "old": "a@old.com", "new": "a@new.com" }
     *   }
     * }
     *
     * // For DELETE operations
     * {
     *   "operation": "DELETE",
     *   "deletedState": { "id": 123, "username": "alice" }
     * }
     * }
     * </pre>
     * <p>
     * <strong>Privacy & Security Requirements:</strong>
     * <ul>
     *     <li><strong>Mask PII</strong>: Ensure fields like {@code password}, {@code idCard}, {@code phone} are redacted before persistence</li>
     *     <li><strong>Limit Size</strong>: Truncate to max length (e.g., 8KB) to prevent storage abuse or DoS</li>
     *     <li><strong>Avoid Secrets</strong>: Never log authentication tokens, API keys, or encryption secrets</li>
     *     <li><strong>Structured Format</strong>: Use consistent JSON schema for easy parsing and analysis</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>For simple values: Display as plain text in audit log table</li>
     *     <li>For complex JSON: Provide "View Details" modal with syntax-highlighted JSON viewer</li>
     *     <li>For sensitive data: Show masked placeholder with "Authorized users only" tooltip</li>
     * </ul>
     * <p>
     * <strong>Query Tips:</strong>
     * <ul>
     *     <li>MySQL 5.7+: Use {@code JSON_EXTRACT(value, '$.changes.email.new')} for targeted searches</li>
     *     <li>Elasticsearch: Index {@code value} as nested object for full-text search across operation details</li>
     * </ul>
     *
     * @see <a href="https://baomidou.com/pages/223848/">MyBatis-Plus JSON TypeHandler Guide</a>
     */
    private String value;

}