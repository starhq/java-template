package com.github.starhq.template.model.vo.token;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.starhq.template.common.util.RequestContextUtil;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * View object for paginated token session responses in admin console or API clients.
 * <p>
 * This class provides comprehensive token metadata for session management, security auditing,
 * and user activity tracking. Designed for rendering token lists in management interfaces
 * with filtering by user, expiration status, revocation state, and login location.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Session Management</strong>: Display active/expired tokens with revocation controls for user security</li>
 *     <li><strong>Security Auditing</strong>: Track login locations, token lifecycles, and suspicious activity patterns</li>
 *     <li><strong>Compliance Reporting</strong>: Export token audit trails for regulatory requirements (GDPR, SOX, PCI-DSS)</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React table components with sorting/pagination</li>
 * </ul>
 * <p>
 * <strong>Serialization Strategy:</strong>
 * <p>
 * Long ID fields use {@code @JsonSerialize(using = ToStringSerializer.class)} to convert
 * {@link Long} to {@code String} in JSON output. This prevents precision loss when consuming
 * APIs from JavaScript/TypeScript clients (which use 53-bit integers).
 * <p>
 * Time fields use {@code @JsonFormat} with explicit pattern and timezone to ensure consistent
 * datetime representation across different server/client timezones.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see java.io.Serializable
 * @see com.github.starhq.template.entity.SysToken
 * @see com.github.starhq.template.service.TokenService
 */
@Data
public class TokenPageVO implements Serializable {

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
    private static final long serialVersionUID = -3183460973926186125L;

    /**
     * The unique identifier of this token session record.
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
     * { "id": 9007199254740993 }  // May be truncated in JS
     *
     * // With ToStringSerializer:
     * { "id": "9007199254740993" }  // Safe string representation
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — auto-generated primary key from database sequence</li>
     *     <li>Uniqueness: Globally unique across all token records</li>
     *     <li>Usage: Reference for token revocation, audit lookup, and session management</li>
     * </ul>
     *
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * The unique identifier of the user who owns this token session.
     * <p>
     * Establishes a many-to-one relationship: {@code User 1..* Token}.
     * Used for filtering tokens by user in management UIs and for permission
     * resolution in authentication flows.
     * <p>
     * <strong>Serialization Strategy:</strong>
     * <p>
     * Annotated with {@code @JsonSerialize(using = ToStringSerializer.class)} to
     * convert the {@link Long} value to a {@code String} in JSON output for JavaScript
     * client compatibility (see {@link #id} for details).
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * // Fetch all active tokens for a specific user
     * GET /api/v1/tokens?userId=12345&revoked=false
     *
     * // Frontend: Display user's active sessions
     * const userSessions = tokens.filter(t => t.userId === currentUserId && !t.revoked);
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_user.id} for foreign key integrity</li>
     *     <li>Nullability: Should not be {@code null} — every token must belong to a user</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_user_id ON sys_token(user_id)}</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysUser
     * @see ToStringSerializer
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * The absolute timestamp when this token becomes invalid and must be rejected.
     * <p>
     * This field indicates the expiration time for the token. After this time,
     * the token must be rejected during authentication checks regardless of revocation status.
     * <p>
     * <strong>Serialization Format:</strong>
     * <p>
     * Annotated with {@code @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")}
     * to ensure consistent datetime representation:
     * <ul>
     *     <li>Pattern: {@code yyyy-MM-dd HH:mm:ss} — human-readable format for admin UIs</li>
     *     <li>Timezone: {@code GMT+8} — China Standard Time (adjust per deployment region)</li>
     *     <li>Example output: {@code "2026-05-21 14:30:00"}</li>
     * </ul>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Client-side</strong>: Check {@code expiredAt} to proactively refresh tokens before expiry</li>
     *     <li><strong>Server-side</strong>: Reject requests with {@code now() > expiredAt}</li>
     *     <li><strong>Admin UI</strong>: Display remaining time or expiration status for session management</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use short-lived tokens (2-12 hours) for access tokens; longer (7-30 days) for refresh tokens</li>
     *     <li>Implement sliding expiration: extend {@code expiredAt} on successful refresh</li>
     *     <li>Log token expiration events for security monitoring and anomaly detection</li>
     * </ul>
     *
     * @see java.time.OffsetDateTime
     * @see JsonFormat
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime expiredAt;

    /**
     * The timestamp when this token session was created.
     * <p>
     * Used for audit trails, session duration analysis, and identifying stale tokens
     * that may indicate compromised accounts or forgotten logins.
     * <p>
     * <strong>Serialization Format:</strong>
     * <p>
     * Annotated with {@code @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")}
     * for consistent datetime representation (see {@link #expiredAt} for details).
     * <p>
     * <strong>Audit Use Cases:</strong>
     * <ul>
     *     <li><strong>Session Duration</strong>: Calculate {@code expiredAt - createdAt} for usage analytics</li>
     *     <li><strong>Security Analysis</strong>: Detect unusually long-lived sessions that may indicate token theft</li>
     *     <li><strong>Compliance</strong>: Provide creation timestamps for regulatory audit trails</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Show creation time with relative formatting
     * <a-table-column title="Created At" data-index="createdAt">
     *   <template #bodyCell="{ text }">
     *     <a-tooltip :title="text">
     *       {{ formatRelativeTime(text) }} <!-- e.g., "2 hours ago" -->
     *     </a-tooltip>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     *
     * @see java.time.OffsetDateTime
     * @see JsonFormat
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime createdAt;

    /**
     * Flag indicating whether this token has been explicitly invalidated.
     * <p>
     * When {@code true}, the token is immediately rejected during authentication,
     * regardless of {@code expiredAt}. Typical triggers for revocation:
     * <ul>
     *     <li>User-initiated logout</li>
     *     <li>Password or MFA credential reset</li>
     *     <li>Suspicious activity detection (impossible travel, brute force)</li>
     *     <li>Admin-enforced session termination</li>
     *     <li>Token rotation (old refresh token revoked when new one issued)</li>
     * </ul>
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li>{@code revoked = true}: Token is permanently invalid; cannot be reactivated</li>
     *     <li>{@code revoked = false}: Token validity depends on {@code expiredAt} and other checks</li>
     *     <li>{@code null}: Treat as {@code false} for backward compatibility (unrevoked by default)</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show status badge: <a-tag color="red">Revoked</a-tag> or <a-tag color="green">Active</a-tag></li>
     *     <li>Disable "Revoke" button for already-revoked tokens</li>
     *     <li>Provide confirmation dialog before revoking active tokens</li>
     * </ul>
     *
     */
    private Boolean revoked;

    /**
     * The IP address of the client at the time of token issuance.
     * <p>
     * Captured from proxy headers ({@code X-Forwarded-For}, {@code X-Real-IP}) or
     * direct connection metadata. Used for security auditing, geo-location analysis,
     * and anomaly detection (e.g., rapid location changes indicating account takeover).
     * <p>
     * <strong>Format:</strong> IPv4 ({@code "192.168.1.1"}) or IPv6 ({@code "2001:db8::1"}).
     * <p>
     * <strong>Privacy Considerations:</strong>
     * <ul>
     *     <li>IP addresses may be considered personal data under GDPR/PIPL; handle with care</li>
     *     <li>Consider anonymizing or aggregating IPs in reports for privacy compliance</li>
     *     <li>Implement IP-based rate limiting and geofencing for additional security</li>
     * </ul>
     * <p>
     * <strong>Security Use Cases:</strong>
     * <ul>
     *     <li>Detect impossible travel: login from New York → London within 1 hour</li>
     *     <li>Identify proxy/VPN usage patterns for fraud detection</li>
     *     <li>Enforce IP allowlists/denylists for high-security accounts</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Show IP with geo-location tooltip
     * <a-table-column title="Login IP" data-index="loginIp">
     *   <template #bodyCell="{ text }">
     *     <a-tooltip :title="getGeoLocation(text)">
     *       <code>{{ text }}</code>
     *     </a-tooltip>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     *
     * @see RequestContextUtil#getContext()#getLoginIp()
     */
    private String loginIp;

    /**
     * The username of the account that owns this token session.
     * <p>
     * Denormalized field for convenient display in admin UIs without requiring
     * additional JOIN queries to fetch user details. Should be kept in sync with
     * the referenced {@code SysUser.username} via application logic or database triggers.
     * <p>
     * <strong>Data Consistency:</strong>
     * <ul>
     *     <li>Updated automatically when user changes username (via application event or trigger)</li>
     *     <li>For audit integrity, consider storing historical username snapshots if username changes are frequent</li>
     *     <li>Never use this field for authentication; always reference {@code userId} for security-critical operations</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Show username with link to user profile
     * <a-table-column title="Username" data-index="username">
     *   <template #bodyCell="{ text, record }">
     *     <router-link :to="`/users/${record.userId}`">
     *       {{ text }}
     *     </router-link>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysUser#getUsername()
     */
    private String username;

}