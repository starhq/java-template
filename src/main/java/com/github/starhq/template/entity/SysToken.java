package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.OffsetDateTime;

/**
 * Entity class representing an authentication token record for session management and security auditing.
 * <p>
 * This class maps to the {@code sys_token} table and tracks the lifecycle of issued JWT/OAuth2 tokens,
 * including access/refresh token pairs, expiration times, revocation status, and client metadata (IP, device).
 * It enables secure session control, multi-device login management, and forced logout capabilities.
 * <p>
 * <strong>Security & Lifecycle Management:</strong>
 * <ul>
 *     <li><strong>Access Token</strong>: Short-lived credential for API authentication (typically 2-12 hours)</li>
 *     <li><strong>Refresh Token</strong>: Long-lived credential for silent re-authentication (typically 7-30 days)</li>
 *     <li><strong>Revocation</strong>: Immediate invalidation via {@code revoked} flag (logout, password reset, breach)</li>
 *     <li><strong>Expiration</strong>: Automatic rejection after {@code expiredAt}; cleanup via scheduled jobs</li>
 * </ul>
 * <p>
 * <strong>Storage & Performance:</strong>
 * <p>
 * Token validation is high-frequency. Always index {@code userId}, {@code revoked}, and {@code expiredAt}.
 * Consider caching active tokens in Redis with TTL matching {@code expiredAt}, using the database as the
 * source of truth for revocation status, audit trails, and concurrent session limits.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see com.github.starhq.template.service.TokenService
 * @see TableName
 */
@Data
@Alias("token")
@TableName("sys_token")
public class SysToken {

    /**
     * The unique identifier for this token record.
     * <p>
     * Mapped to the primary key column via {@link TableId}. Uses MyBatis-Plus
     * default {@code ASSIGN_ID} strategy (snowflake algorithm) for globally
     * unique, time-ordered 64-bit numeric IDs.
     *
     * @see TableId
     * @see com.baomidou.mybatisplus.annotation.IdType#ASSIGN_ID
     */
    @TableId
    private Long id;

    /**
     * The unique identifier of the user who owns this token session.
     * <p>
     * Establishes a many-to-one relationship: {@code SysUser 1..* SysToken}.
     * Enables multi-device login management, session tracking, and targeted token revocation.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_user.id}</li>
     *     <li>Nullability: {@code NOT NULL} — every token must belong to an authenticated user</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_user_id ON sys_token(user_id)}</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysUser
     */
    private Long userId;

    /**
     * The short-lived access token used for authenticating API requests.
     * <p>
     * Typically transmitted via the {@code Authorization: Bearer <token>} HTTP header.
     * Validated by gateway/interceptor against signature, expiration, and revocation status.
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Never log, cache in plaintext, or expose in URL/query parameters</li>
     *     <li>Consider AES-256 encryption at rest if compliance (PCI-DSS, HIPAA) requires it</li>
     *     <li>Rotate on each refresh to mitigate replay attacks</li>
     * </ul>
     */
    private String accessToken;

    /**
     * The long-lived refresh token used to obtain new access token pairs without re-authentication.
     * <p>
     * Supports secure token rotation: issuing a new refresh token invalidates the previous one,
     * limiting the window of exposure if a refresh token is compromised.
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Store in secure, httpOnly cookies or encrypted storage on client</li>
     *     <li>Bind to {@code deviceFingerprint} to prevent cross-device theft</li>
     *     <li>Revoke immediately if abnormal refresh patterns are detected</li>
     * </ul>
     */
    private String refreshToken;

    /**
     * The absolute timestamp when this token pair becomes invalid.
     * <p>
     * Used by the authentication filter to reject expired requests before signature validation.
     * Typically set to {@code currentTime + accessTTL} during issuance.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link OffsetDateTime} — preserves timezone information for global deployments</li>
     *     <li>Timezone: Recommend storing in {@code UTC} for consistency across distributed nodes</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_expired_at ON sys_token(expired_at)} for TTL cleanup jobs</li>
     * </ul>
     */
    private OffsetDateTime expiredAt;

    /**
     * The timestamp when this token record was initially persisted.
     * <p>
     * Automatically populated by MyBatis-Plus {@code MetaObjectHandler} during {@code INSERT} operations.
     * Immutable after creation and serves as an audit trail for session issuance and forensics.
     * <p>
     * <strong>Fill Strategy:</strong> {@link FieldFill#INSERT} — set once, never modified.
     *
     * @see FieldFill#INSERT
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#insertFill
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * Flag indicating whether this token has been explicitly invalidated.
     * <p>
     * When {@code true}, the token is immediately rejected during authentication,
     * regardless of {@code expiredAt}. Typical triggers:
     * <ul>
     *     <li>User-initiated logout</li>
     *     <li>Password or MFA credential reset</li>
     *     <li>Suspicious activity detection (impossible travel, brute force)</li>
     *     <li>Admin-enforced session termination</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Default: {@code false} — tokens are valid until explicitly revoked or expired</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_revoked ON sys_token(revoked)} for fast blacklist lookups</li>
     *     <li>Cache Strategy: Replicate revocation state to Redis with short TTL (e.g., 5min) for O(1) gateway checks</li>
     * </ul>
     */
    private Boolean revoked;

    /**
     * The IP address of the client at the time of authentication.
     * <p>
     * Captured from proxy headers ({@code X-Forwarded-For}, {@code X-Real-IP}) or direct connection.
     * Used for security auditing, geo-location analysis, and anomaly detection (e.g., rapid location changes).
     * <p>
     * <strong>Format:</strong> IPv4 ({@code "192.168.1.1"}) or IPv6 ({@code "2001:db8::1"}).
     * Validate and sanitize to prevent header spoofing in trusted proxy environments.
     */
    private String loginIp;

    /**
     * A cryptographic hash or stable identifier representing the client device/browser.
     * <p>
     * Enables device-aware session management, concurrent login limits (e.g., max 3 devices),
     * and bot/fraud detection. Typically derived from User-Agent, screen resolution, timezone,
     * and canvas fingerprinting, then hashed with SHA-256.
     * <p>
     * <strong>Privacy & Security:</strong>
     * <ul>
     *     <li>Never store raw client fingerprints — always hash before persistence</li>
     *     <li>Bind refresh tokens to this value to prevent cross-device replay attacks</li>
     *     <li>Allow users to view and revoke sessions by device for transparency</li>
     * </ul>
     */
    private String deviceFingerprint;

}