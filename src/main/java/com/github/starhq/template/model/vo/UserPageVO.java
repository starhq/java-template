package com.github.starhq.template.model.vo;

import com.github.starhq.template.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * View object for paginated user responses in admin console or API clients.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit common audit trail fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and adds user-specific business fields for comprehensive user management.
 * Designed for rendering user lists in management interfaces with filtering, sorting, and audit context.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Management</strong>: Display paginated user definitions with filtering by username, status</li>
 *     <li><strong>Account Administration</strong>: Enable bulk operations (enable/disable, delete) with audit trail</li>
 *     <li><strong>Compliance Reporting</strong>: Track user lifecycle events via inherited audit fields for regulatory audits</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React table components with sorting/pagination</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Audit Integration</strong>: Inherits audit fields from {@link BaseAuditVO} for compliance tracking</li>
 *     <li><strong>Type Safety</strong>: Uses {@link UserStatus} enum instead of raw strings for status validation</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond enum; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see BaseAuditVO
 * @see UserStatus
 * @see com.github.starhq.template.entity.SysUser
 * @see com.github.starhq.template.service.UserService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserPageVO extends BaseAuditVO {

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
    private static final long serialVersionUID = -5245307163348676076L;

    /**
     * The unique username identifier for authentication and display.
     * <p>
     * This field serves as the primary login credential and display name
     * in admin interfaces. Typically follows naming conventions:
     * <ul>
     *     <li>Alphanumeric with underscores: {@code "alice_admin"}, {@code "bob_123"}</li>
     *     <li>Email-style: {@code "alice@example.com"} (if email is used as username)</li>
     *     <li>Phone-style: {@code "+8613800138000"} (if phone is used as username)</li>
     * </ul>
     * <p>
     * <strong>Uniqueness & Validation:</strong>
     * <ul>
     *     <li><strong>Global Uniqueness</strong>: Must be unique across all users; enforce via database {@code UNIQUE INDEX} and application-level pre-check</li>
     *     <li><strong>Format Validation</strong>: Typically validated via regex: {@code ^[a-zA-Z0-9_@.+\\-]{3,50}$}</li>
     *     <li><strong>Case Sensitivity</strong>: Recommended case-insensitive comparison for login; preserve original case for display</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 table column with copy-to-clipboard
     * <a-table-column title="Username" data-index="username" width="150px">
     *   <template #bodyCell="{ text }">
     *     <a-space>
     *       <span class="font-mono">{{ text }}</span>
     *       <a-tooltip title="Copy username">
     *         <a-button size="small" type="text" @click="copyToClipboard(text)">
     *           <a-icon type="copy" />
     *         </a-button>
     *       </a-tooltip>
     *     </a-space>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Never expose password hints or authentication details via this field</li>
     *     <li>Consider masking usernames in logs for privacy compliance (GDPR, PIPL)</li>
     *     <li>Implement rate limiting on username-based searches to prevent enumeration attacks</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysUser#getUsername()
     */
    private String username;

    /**
     * The account status controlling login permissions and system access.
     * <p>
     * Uses the {@link UserStatus} enum to ensure type safety and enable efficient
     * permission checks without string matching. Common values include:
     * <ul>
     *     <li>{@code ENABLED} — Account is active; user can login and access authorized resources</li>
     *     <li>{@code DISABLED} — Account is suspended; login attempts are rejected with generic error</li>
     *     <li>{@code LOCKED} — Account is temporarily locked due to failed login attempts; auto-unlock after cooldown</li>
     *     <li>{@code DELETED} — Account is soft-deleted; retained for audit but excluded from active queries</li>
     * </ul>
     * <p>
     * <strong>Serialization Behavior:</strong>
     * <p>
     * Jackson automatically serializes enum values to their {@code name()} (uppercase string):
     * <pre>
     * {@code
     * // Java: UserStatus status = UserStatus.ENABLED;
     * // JSON: { "status": "ENABLED" }
     * }
     * </pre>
     * <p>
     * <strong>Permission Check Strategy:</strong>
     * <pre>
     * {@code
     * // Backend: Check if user can login
     * if (user.getStatus() != UserStatus.ENABLED) {
     *     throw new BusinessException(ErrorCode.ACCOUNT_NOT_ACTIVE);
     * }
     *
     * // Frontend: Display status badge with color coding
     * <a-tag :color="getStatusColor(user.status)">
     *   {{ $t(`user.status.${user.status}`) }}
     * </a-tag>
     * }
     * </pre>
     * <p>
     * <strong>State Transition Rules:</strong>
     * <ul>
     *     <li><strong>ENABLED → DISABLED</strong>: Allowed; immediately revokes active sessions</li>
     *     <li><strong>DISABLED → ENABLED</strong>: Allowed; requires admin approval workflow</li>
     *     <li><strong>* → LOCKED</strong>: Auto-triggered after N failed login attempts; auto-unlock after M minutes</li>
     *     <li><strong>* → DELETED</strong>: Soft-delete only; requires explicit restore operation to reactivate</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Color coding: <a-tag color="green">ENABLED</a-tag>, <a-tag color="red">DISABLED</a-tag>, <a-tag color="orange">LOCKED</a-tag></li>
     *     <li>Tooltip: Show status description and last change time on hover</li>
     *     <li>Action buttons: Disable "Enable" button for already-enabled users, etc.</li>
     * </ul>
     *
     * @see UserStatus
     * @see com.github.starhq.template.entity.SysUser#getStatus()
     */
    private UserStatus status;

}