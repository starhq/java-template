package com.github.starhq.template.model.vo;

import com.github.starhq.template.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Lightweight view object for user metadata in dropdowns, selectors, and internal service communication.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the user's unique identifier
 * and provides minimal fields required for user reference and display. Designed
 * for scenarios where full user details are unnecessary, such as:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, multi-selects, or assignee lists with user options</li>
 *     <li><strong>Permission Hints</strong>: Providing safe metadata for frontend route guards without exposing sensitive fields</li>
 *     <li><strong>Internal Service Communication</strong>: Passing user references between microservices for distributed auth</li>
 *     <li><strong>Cache Optimization</strong>: Storing user metadata in Redis/Caffeine with minimal memory footprint</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for user identification and display (id, username, status)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond enum; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see BaseIdVO
 * @see UserStatus
 * @see com.github.starhq.template.entity.SysUser
 * @see com.github.starhq.template.service.UserService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserSimpleVO extends BaseIdVO {

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
    private static final long serialVersionUID = -4740787591715630663L;

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
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep usernames concise (≤ 50 characters) for consistent UI layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "user.alice"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     *     <li>Use consistent case handling: lowercase for comparison, preserve original for display</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Populate a select dropdown
     * <a-select v-model="form.assigneeId" :options="userOptions">
     *   <a-select-option v-for="user in userOptions" :key="user.id" :value="user.id">
     *     {{ user.username }}
     *     <a-tag v-if="user.status !== 'ENABLED'" color="orange" size="small">
     *       {{ $t(`user.status.${user.status}`) }}
     *     </a-tag>
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render user list with status hint
     * {users.map(user => (
     *   <Select.Option key={user.id} value={user.id} disabled={!user.status.canLogin()}>
     *     {user.username}
     *     {user.status !== 'ENABLED' && <Tag color="orange">{t(`user.status.${user.status}`)}</Tag>}
     *   </Select.Option>
     * ))}
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
     * // Backend: Check if user can be assigned to a task
     * if (user.getStatus() != UserStatus.ENABLED) {
     *     throw new BusinessException(ErrorCode.USER_NOT_AVAILABLE);
     * }
     *
     * // Frontend: Display status badge with color coding
     * <a-tag :color="getStatusColor(user.status)">
     *   {{ $t(`user.status.${user.status}`) }}
     * </a-tag>
     * }
     * </pre>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Color coding: <a-tag color="green">ENABLED</a-tag>, <a-tag color="red">DISABLED</a-tag>, <a-tag color="orange">LOCKED</a-tag></li>
     *     <li>Tooltip: Show status description on hover for admin clarity</li>
     *     <li>Disable selection: Gray out disabled/locked users in assignee dropdowns</li>
     * </ul>
     * <p>
     * <strong>State Transition Awareness:</strong>
     * <ul>
     *     <li><strong>ENABLED → DISABLED</strong>: Immediately revokes active sessions; user cannot login</li>
     *     <li><strong>DISABLED → ENABLED</strong>: Requires admin approval; restores login capability</li>
     *     <li><strong>* → LOCKED</strong>: Auto-triggered after N failed login attempts; auto-unlock after M minutes</li>
     *     <li><strong>* → DELETED</strong>: Soft-delete only; requires explicit restore operation to reactivate</li>
     * </ul>
     *
     * @see UserStatus
     * @see com.github.starhq.template.entity.SysUser#getStatus()
     */
    private UserStatus status;

}