package com.github.starhq.template.model.dto;

import com.github.starhq.template.event.domain.CacheEvictEvent;
import com.github.starhq.template.service.TokenService;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.OffsetDateTime;

/**
 * Lightweight data transfer object for token session metadata in internal or admin contexts.
 * <p>
 * This class extends {@link SensitiveDTO} to ensure automatic masking of sensitive fields
 * (e.g., {@code accessToken}, {@code refreshToken}) in logs and API responses. It is designed
 * for scenarios where token metadata needs to be transmitted or displayed without exposing
 * actual credential values, such as:
 * <ul>
 *     <li><strong>Admin Console</strong>: Listing active user sessions with device/IP info for management</li>
 *     <li><strong>Audit Logging</strong>: Recording token lifecycle events (issue, refresh, revoke) without credential leakage</li>
 *     <li><strong>Internal Service Communication</strong>: Passing token references between microservices for session validation</li>
 *     <li><strong>Security Monitoring</strong>: Analyzing login patterns by IP/device without storing raw tokens</li>
 * </ul>
 * <p>
 * <strong>Security Design:</strong>
 * <p>
 * By inheriting from {@link SensitiveDTO}, this class leverages custom JSON serialization
 * to automatically mask sensitive string fields. When serialized to JSON (e.g., in API responses
 * or logs), fields like {@code accessToken} and {@code refreshToken} are replaced with
 * {@code "***"} to prevent credential exposure.
 * <p>
 * <strong>Field Sensitivity Classification:</strong>
 * <ul>
 *     <li><strong>Highly Sensitive</strong> (auto-masked): {@code accessToken}, {@code refreshToken}</li>
 *     <li><strong>Medium Sensitivity</strong> (context-dependent): {@code deviceFingerprint} (may reveal user device)</li>
 *     <li><strong>Low Sensitivity</strong> (safe to expose): {@code expiredAt}, {@code revoked}, {@code loginIp} (with privacy considerations)</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * // Service: Convert entity to safe DTO for admin UI
 * public TokenSimpleDTO convertToSafeDTO(SysToken entity) {
 *     TokenSimpleDTO dto = new TokenSimpleDTO();
 *     dto.setAccessToken(entity.getAccessToken()); // Will be masked in JSON output
 *     dto.setRefreshToken(entity.getRefreshToken()); // Will be masked
 *     dto.setExpiredAt(entity.getExpiredAt());
 *     dto.setRevoked(entity.getRevoked());
 *     dto.setLoginIp(entity.getLoginIp());
 *     dto.setDeviceFingerprint(entity.getDeviceFingerprint());
 *     return dto;
 * }
 *
 * // Controller: Return safe token metadata to admin frontend
 * @GetMapping("/sessions")
 * public Result<List<TokenSimpleDTO>> getUserSessions(@AuthenticationPrincipal SysUser user) {
 *     List<SysToken> tokens = tokenService.getActiveTokensByUserId(user.getId());
 *     List<TokenSimpleDTO> safeTokens = tokens.stream()
 *         .map(this::convertToSafeDTO)
 *         .collect(Collectors.toList());
 *     return Result.success(safeTokens);
 *     // JSON output: { "accessToken": "***", "refreshToken": "***", "expiredAt": "...", ... }
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see SensitiveDTO
 * @see com.github.starhq.template.entity.SysToken
 * @see com.github.starhq.template.service.TokenService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TokenSimpleDTO extends SensitiveDTO {

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
    private static final long serialVersionUID = 933255569L;

    /**
     * The access token credential for API authentication.
     * <p>
     * <strong>Sensitivity: HIGH</strong> — This field is automatically masked by
     * {@link SensitiveDTO} when serialized to JSON. The actual token value should
     * <em>never</em> be exposed in logs, API responses, or client-side code.
     * <p>
     * <strong>Usage Context:</strong>
     * <ul>
     *     <li>Internal service-to-service communication where token reference is needed</li>
     *     <li>Admin audit views showing token metadata (value masked for security)</li>
     *     <li>Debugging sessions where token existence matters but value does not</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li>Never log this field in plaintext; rely on {@code SensitiveDTO} masking</li>
     *     <li>Never include in client-side JavaScript or mobile app storage</li>
     *     <li>Transmit only over HTTPS with proper authentication/authorization checks</li>
     *     <li>Consider using token references (e.g., token ID) instead of raw values when possible</li>
     * </ul>
     * <p>
     * <strong>Serialization Behavior:</strong>
     * <pre>
     * {@code
     * // Input: accessToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * // JSON Output: { "accessToken": "***" }  // Auto-masked by SensitiveDTO
     * }
     * </pre>
     *
     * @see SensitiveDTO
     * @see SensitiveDTO
     */
    private String accessToken;

    /**
     * The refresh token credential for obtaining new access tokens.
     * <p>
     * <strong>Sensitivity: HIGH</strong> — This field is automatically masked by
     * {@link SensitiveDTO} when serialized to JSON. Refresh tokens have longer
     * lifetimes than access tokens, making their exposure even more critical.
     * <p>
     * <strong>Usage Context:</strong>
     * <ul>
     *     <li>Token management views showing refresh token metadata (value masked)</li>
     *     <li>Audit trails recording token refresh events without credential leakage</li>
     *     <li>Internal diagnostics where token existence matters but value does not</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li><strong>Never expose</strong> refresh tokens in any client-facing context</li>
     *     <li>Implement token rotation: issuing a new refresh token invalidates the previous one</li>
     *     <li>Bind refresh tokens to device fingerprint to prevent cross-device theft</li>
     *     <li>Revoke immediately if suspicious refresh patterns are detected</li>
     * </ul>
     * <p>
     * <strong>Serialization Behavior:</strong>
     * <pre>
     * {@code
     * // Input: refreshToken = "dGhpcyBpcyBhIHJlZnJlc2ggdG9rZW4..."
     * // JSON Output: { "refreshToken": "***" }  // Auto-masked by SensitiveDTO
     * }
     * </pre>
     *
     * @see SensitiveDTO
     * @see TokenService#refresh()
     */
    private String refreshToken;

    /**
     * The absolute timestamp when the token pair becomes invalid.
     * <p>
     * This field indicates the expiration time for both access and refresh tokens.
     * After this time, the tokens must be rejected during authentication checks.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link OffsetDateTime} — preserves timezone information for global deployments</li>
     *     <li>Timezone: Recommend storing and comparing in {@code UTC} for consistency</li>
     *     <li>Precision: Typically millisecond precision; sufficient for token expiration checks</li>
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
     *     <li>Use short-lived access tokens (2-12 hours) + longer refresh tokens (7-30 days)</li>
     *     <li>Implement sliding expiration: extend {@code expiredAt} on successful refresh</li>
     *     <li>Log token expiration events for security monitoring and anomaly detection</li>
     * </ul>
     *
     * @see java.time.OffsetDateTime
     */
    private OffsetDateTime expiredAt;

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
     * <strong>Implementation Guidance:</strong>
     * <ul>
     *     <li>Store revocation state in fast cache (Redis) for O(1) checks during authentication</li>
     *     <li>Propagate revocation events across service instances in distributed systems</li>
     *     <li>Log revocation events with context (who, when, why) for audit trails</li>
     * </ul>
     *
     * @see CacheEvictEvent
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
     *
     */
    private String loginIp;

    /**
     * A cryptographic hash or stable identifier representing the client device/browser.
     * <p>
     * Enables device-aware session management, concurrent login limits, and bot/fraud
     * detection. Typically derived from User-Agent, screen resolution, timezone, canvas
     * fingerprinting, and other browser characteristics, then hashed with SHA-256.
     * <p>
     * <strong>Generation Strategy:</strong>
     * <pre>
     * {@code
     * // Example: Combine multiple signals and hash
     * String raw = userAgent + screenRes + timezone + canvasHash + ...;
     * String fingerprint = DigestUtils.sha256Hex(raw);
     * }
     * </pre>
     * <p>
     * <strong>Privacy & Security:</strong>
     * <ul>
     *     <li><strong>Never store raw fingerprints</strong> — always hash before persistence</li>
     *     <li><strong>Bind refresh tokens</strong> to fingerprint to prevent cross-device replay attacks</li>
     *     <li><strong>Allow user visibility</strong> — let users view and revoke sessions by device for transparency</li>
     *     <li><strong>Compliance</strong>: Document fingerprinting practices in privacy policy per GDPR/CCPA</li>
     * </ul>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Concurrent session limits</strong>: Max 3 devices per user; revoke oldest on new login</li>
     *     <li><strong>Anomaly detection</strong>: Alert on new device login for high-value accounts</li>
     *     <li><strong>Session management UI</strong>: Show device type, last active time, revoke button</li>
     * </ul>
     *
     */
    private String deviceFingerprint;

}