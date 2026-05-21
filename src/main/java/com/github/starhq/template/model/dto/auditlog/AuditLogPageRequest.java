package com.github.starhq.template.model.dto.auditlog;

import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Pagination request parameters for querying system audit logs.
 * <p>
 * This class extends {@link PageRequest} to inherit standard pagination fields
 * ({@code page}, {@code size}, {@code sort}) and adds audit-specific filters
 * for targeted log retrieval in admin console or compliance reporting scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Audit Console</strong>: Filter logs by target type (USER/ROLE/MENU) and operator username</li>
 *     <li><strong>Security Analysis</strong>: Investigate suspicious activities by narrowing search scope</li>
 *     <li><strong>Compliance Reporting</strong>: Export filtered audit trails for regulatory audits (GDPR, SOX, etc.)</li>
 * </ul>
 * <p>
 * <strong>Query Behavior:</strong>
 * <p>
 * When used with {@link com.github.starhq.template.mapper.SysAuditLogMapper#selectAuditLogPage},
 * the filters are applied as follows:
 * <ul>
 *     <li>{@code targetType}: Exact match on {@code sys_audit_log.target_type} enum column</li>
 *     <li>{@code username}: Fuzzy match (LIKE) on {@code sys_user.username} via JOIN, or exact match on {@code audit_log.created_by} if pre-resolved</li>
 * </ul>
 * <p>
 * <strong>Serialization:</strong>
 * <p>
 * This class implements {@link java.io.Serializable} with a fixed {@code serialVersionUID}
 * to ensure compatibility when caching request objects or transmitting across service boundaries.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-10
 * @see PageRequest
 * @see TargetType
 * @see com.github.starhq.template.service.AuditLogService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AuditLogPageRequest extends PageRequest {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 5444876205894752120L;

    /**
     * Filter audit logs by the type of target entity being operated on.
     * <p>
     * Typical values from {@link TargetType}:
     * <ul>
     *     <li>{@code USER}: Operations on user accounts (create, update, delete, login)</li>
     *     <li>{@code ROLE}: Role management actions (assign permissions, enable/disable)</li>
     *     <li>{@code MENU}: Navigation configuration changes</li>
     *     <li>{@code BUTTON}: UI permission modifications</li>
     *     <li>{@code DICT}: Dictionary data maintenance</li>
     *     <li>{@code CONFIG}: System parameter updates</li>
     * </ul>
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li>Exact match: {@code WHERE target_type = :targetType}</li>
     *     <li>If {@code null}: No filtering by target type (returns all types)</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Fetch all user-related audit logs
     * AuditLogPageRequest request = new AuditLogPageRequest();
     * request.setTargetType(TargetType.USER);
     * request.setPage(1);
     * request.setSize(20);
     *
     * IPage<AuditLogPageVO> result = auditLogService.page(request);
     * }
     * </pre>
     *
     * @see TargetType
     */
    private TargetType targetType;

    /**
     * Filter audit logs by the username of the operator who performed the action.
     * <p>
     * Supports fuzzy matching for flexible search in admin UIs:
     * <ul>
     *     <li>Input {@code "alice"} matches usernames like {@code "alice"}, {@code "alice_admin"}, {@code "alice_test"}</li>
     *     <li>Case-insensitive matching recommended at database or application layer</li>
     * </ul>
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li>Fuzzy match: {@code WHERE sys_user.username LIKE CONCAT('%', :username, '%')} via JOIN</li>
     *     <li>If {@code null} or empty: No filtering by username (returns all operators)</li>
     *     <li>Trimming: Leading/trailing whitespace should be trimmed before query execution</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Validate input to prevent SQL injection; use parameterized queries (MyBatis handles this)</li>
     *     <li>Consider rate-limiting fuzzy searches to prevent enumeration attacks</li>
     *     <li>Log search queries for audit trails of audit log access (meta-auditing)</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Optional: Allow {@code null} or empty for unfiltered queries</li>
     *     <li>Length: {@code @Size(max = 64)} to prevent excessive wildcard expansion</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[\\w.@+-]*$")} for safe identifier characters</li>
     * </ul>
     */
    private String username;

}