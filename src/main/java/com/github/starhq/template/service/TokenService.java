package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.dto.KeyWordPageRequest;
import com.github.starhq.template.model.dto.TokenSimpleDTO;
import com.github.starhq.template.model.vo.TokenPageVO;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Service interface for JWT token management with lifecycle control and permission integration.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysToken} entities, while adding business-level methods for token issuance,
 * refresh, user-based queries, and token lifecycle management. Designed to centralize
 * token logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Token Issuance</strong>: Generate JWT access/refresh tokens after successful authentication</li>
 *     <li><strong>Token Refresh</strong>: Support token renewal workflow for extended sessions without re-authentication</li>
 *     <li><strong>User Token Management</strong>: Query/revoke tokens by user ID for session control</li>
 *     <li><strong>Admin Token Audit</strong>: Paginated token queries for security monitoring and compliance</li>
 * </ul>
 * <p>
 * <strong>Security Design Principles:</strong>
 * <ul>
 *     <li><strong>Stateless Authentication</strong>: JWT tokens contain all necessary claims; no server-side session storage required</li>
 *     <li><strong>Token Hygiene</strong>: Short-lived access tokens (15-30 min) + long-lived refresh tokens (7-30 days) for balanced security/UX</li>
 *     <li><strong>Revocation Support</strong>: Server-side token tracking enables forced logout and session termination</li>
 *     <li><strong>Access Control</strong>: All token operations enforce role-based permissions (ADMIN for audit, self for user ops)</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Handle login and token issuance
 * @RestController
 * @RequestMapping("/api/v1/auth")
 * @RequiredArgsConstructor
 * public class AuthController {
 *
 *     private final TokenService tokenService;
 *
 *     @PostMapping("/login")
 *     public Result<JwtToken> login(@RequestBody LoginDTO dto) {
 *         // Authenticate user via Spring Security
 *         UserDetails userDetails = authenticationManager.authenticate(dto);
 *
 *         // Issue tokens
 *         JwtToken token = tokenService.build(userDetails);
 *         return Result.success(token);
 *     }
 *
 *     @PostMapping("/refresh")
 *     public Result<JwtToken> refresh() {
 *         // Refresh tokens using refresh token from request
 *         JwtToken newToken = tokenService.refresh();
 *         return Result.success(newToken);
 *     }
 * }
 *
 * // Frontend: Store tokens and use for authenticated requests
 * const authStore = {
 *   accessToken: null,
 *   refreshToken: null,
 *
 *   setTokens: (access, refresh) => {
 *     authStore.accessToken = access;
 *     // Refresh token should be stored in httpOnly cookie by backend
 *   },
 *
 *   // Include access token in API requests
 *   apiClient: axios.create({
 *     headers: { 'Authorization': `Bearer ${authStore.accessToken}` }
 *   })
 * };
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysToken
 * @see JwtToken
 * @see TokenSimpleDTO
 * @see TokenPageVO
 * @see KeyWordPageRequest
 */
public interface TokenService extends IService<SysToken> {

    /**
     * Revokes all active tokens for a specific user, effectively forcing logout.
     * <p>
     * This method is typically used for:
     * <ul>
     *     <li><strong>Admin-Initiated Logout</strong>: Force logout a user for security or policy reasons</li>
     *     <li><strong>Password Change</strong>: Revoke existing tokens when user changes password</li>
     *     <li><strong>Account Suspension</strong>: Immediately terminate all active sessions for disabled accounts</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must not be {@code null}; the primary key of the user whose tokens to revoke</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if tokens were successfully revoked (or no tokens existed)</li>
     *     <li><strong>Failure</strong>: Returns {@code false} only if database operation fails</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Immediate Effect</strong>: Revoked tokens should be rejected on next API call via token validation filter</li>
     *     <li><strong>Audit Logging</strong>: Log token revocation events for compliance tracking and security analysis</li>
     *     <li><strong>Cache Invalidation</strong>: Invalidate any cached token validation results for the user</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Token revocation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Admin endpoint to force logout a user
     * @PostMapping("/users/{userId}/logout")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Void> forceLogout(@PathVariable Long userId) {
     *     boolean success = tokenService.removeByUserId(userId);
     *
     *     if (success) {
     *         // Optional: Notify user via WebSocket/push notification
     *         return Result.success("User logged out successfully");
     *     } else {
     *         return Result.fail(ErrorCode.TOKEN_REVOKE_FAILED);
     *     }
     * }
     *
     * // Service: Revoke tokens on password change
     * @Transactional
     * public void changePassword(Long userId, String newPassword) {
     *     // Update password
     *     userMapper.updatePassword(userId, passwordEncoder.encode(newPassword));
     *
     *     // Revoke all existing tokens to force re-authentication
     *     tokenService.removeByUserId(userId);
     *
     *     // Log audit event
     *     auditLogService.record("PASSWORD_CHANGED", TargetType.USER, userId, ...);
     * }
     * }
     * </pre>
     *
     * @param userId the primary key of the user whose tokens to revoke; must not be {@code null}
     * @return {@code true} if tokens were successfully revoked; {@code false} if database operation fails
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @throws AccessDeniedException    if caller lacks ADMIN permission
     * @see SysToken
     */
    boolean removeByUserId(Long userId);

    /**
     * Retrieves simplified token metadata for a specific user.
     * <p>
     * This method provides lightweight token information for:
     * <ul>
     *     <li><strong>Session Display</strong>: Show active session info in user profile UI</li>
     *     <li><strong>Token Validation</strong>: Quick lookup to verify token existence without full entity load</li>
     *     <li><strong>Debugging</strong>: Admin tools to inspect user's current token state</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must not be {@code null}; the primary key of the user to query</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link TokenSimpleDTO} with token metadata (id, issuedAt, expiresAt, etc.)</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if user has no active tokens</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes sensitive data for security</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Self-Access Only</strong>: Users should only query their own tokens; admin can query any user</li>
     *     <li><strong>No Token Exposure</strong>: Never return actual token strings via this method; use for metadata only</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration attacks</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a candidate for caching due to:
     * <ul>
     *     <li><strong>Read Frequency</strong>: Token metadata may be queried frequently for session UI</li>
     *     <li><strong>Short TTL</strong>: Cache with short TTL (e.g., 1-5 min) to reflect revocations promptly</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Get current user's token info for session display
     * @GetMapping("/session")
     * public Result<TokenSimpleDTO> getSession() {
     *     Long userId = SecurityContextUtils.getRequiredUserId();
     *     TokenSimpleDTO token = tokenService.getByUserId(userId);
     *
     *     if (token != null) {
     *         return Result.success(token);
     *     } else {
     *         return Result.fail(ErrorCode.SESSION_NOT_FOUND);
     *     }
     * }
     *
     * // Frontend: Display session expiry countdown
     * const SessionInfo = () => {
     *   const { data: session } = useRequest(() => api.getSession());
     *
     *   return (
     *     <div>
     *       <p>Session expires: {formatExpiry(session.expiresAt)}</p>
     *       <a-button danger onClick={handleLogout}>Logout</a-button>
     *     </div>
     *   );
     * };
     * }
     * </pre>
     *
     * @param userId the primary key of the user to query; must not be {@code null}
     * @return {@link TokenSimpleDTO} if token exists; {@code null} if no active tokens
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @throws AccessDeniedException    if caller lacks permission to query target user's tokens
     * @see TokenSimpleDTO
     */
    TokenSimpleDTO getByUserId(Long userId);

    /**
     * Retrieves a paginated list of token records for admin audit and monitoring.
     * <p>
     * This method supports multi-dimensional filtering for security analysis:
     * <ul>
     *     <li><strong>User Filter</strong>: Query tokens by user ID for targeted investigation</li>
     *     <li><strong>Time Range</strong>: Filter by token issuance/expiry time for temporal analysis</li>
     *     <li><strong>Keyword Search</strong>: Fuzzy search on username, IP, or user agent for forensic queries</li>
     *     <li><strong>Status Filter</strong>: Filter by active/expired/revoked status for lifecycle management</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params and base filters</li>
     *     <li>{@code pageInfo.getKeyword()}: Optional; performs fuzzy match on username/IP/userAgent</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page if no tokens match criteria</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysToken} entity is converted to {@link TokenPageVO} with audit fields</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method must be protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Logging</strong>: Log all {@code page()} calls for compliance tracking</li>
     *     <li><strong>Data Masking</strong>: Mask sensitive fields (e.g., full IP, detailed user agent) in returned VOs</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_token_user_time ON sys_token(user_id, issued_at DESC);
     *         CREATE INDEX idx_token_keyword ON sys_token(username, ip_address);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Query Optimization</strong>: Avoid SELECT *; fetch only required fields for pagination</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Admin endpoint for token audit
     * @GetMapping("/audit/tokens")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<IPage<TokenPageVO>> listTokens(KeyWordPageRequest request) {
     *     IPage<TokenPageVO> page = tokenService.page(request);
     *     return Result.success(page.getRecords(), page.getTotal());
     * }
     *
     * // Frontend: Admin console token table with filters
     * const TokenAuditTable = () => {
     *   const [filters, setFilters] = useState({ keyword: '', userId: null });
     *   const { data: tokenPage, loading } = useRequest(() =>
     *     api.listTokens({ ...filters, page: 1, size: 20 })
     *   );
     *
     *   return (
     *     <a-table :data-source="tokenPage.records" :loading={loading}>
     *       <a-table-column title="User" dataIndex="username" />
     *       <a-table-column title="IP" dataIndex="ipAddress" />
     *       <a-table-column title="Issued At" dataIndex="issuedAt" />
     *       <a-table-column title="Expires At" dataIndex="expiresAt" />
     *       <a-table-column title="Status" dataIndex="status">
     *         <template #bodyCell="{ text }">
     *           <a-tag :color="getStatusColor(text)">{text}</a-tag>
     *         </template>
     *       </a-table-column>
     *     </a-table>
     *   );
     * };
     * }
     * </pre>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link TokenPageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException if {@code pageInfo} is {@code null}
     * @throws AccessDeniedException    if caller lacks ADMIN permission
     * @see KeyWordPageRequest
     * @see TokenPageVO
     * @see IPage
     */
    IPage<TokenPageVO> page(KeyWordPageRequest pageInfo);

    /**
     * Issues new JWT access and refresh tokens for an authenticated user.
     * <p>
     * This method implements the complete token issuance workflow:
     * <ol>
     *     <li>Extract user metadata from {@link UserDetails} (username, roles, permissions)</li>
     *     <li>Generate short-lived access token (15-30 min) for API authorization</li>
     *     <li>Generate long-lived refresh token (7-30 days) for session renewal</li>
     *     <li>Persist token metadata to {@link SysToken} for revocation support</li>
     *     <li>Return {@link JwtToken} containing both tokens and expiry metadata</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code user}: Must not be {@code null}; authenticated {@link UserDetails} with authorities</li>
     *     <li>{@code user.getUsername()}: Used as token subject claim</li>
     *     <li>{@code user.getAuthorities()}: Included as roles/permissions claims in access token</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@link JwtToken} with {@code accessToken}, {@code refreshToken}, {@code expiresIn}</li>
     *     <li><strong>Failure</strong>: Throws {@link BusinessException} if token generation or persistence fails</li>
     * </ul>
     * <p>
     * <strong>JWT Token Structure:</strong>
     * <pre>
     * {@code
     * // Access token claims (short-lived, included in Authorization header)
     * {
     *   "sub": "alice",              // Subject (username)
     *   "userId": 1001,              // Custom claim: user ID
     *   "roles": ["USER", "ADMIN"],  // Custom claim: authorities
     *   "permissions": ["user:read", "user:write"], // Custom claim: fine-grained permissions
     *   "iat": 1714000000,           // Issued at (epoch seconds)
     *   "exp": 1714001800            // Expiry (15 minutes later)
     * }
     *
     * // Refresh token claims (long-lived, stored securely for renewal)
     * {
     *   "sub": "alice",
     *   "userId": 1001,
     *   "type": "refresh",           // Custom claim: token type
     *   "iat": 1714000000,
     *   "exp": 1714604800            // Expiry (7 days later)
     * }
     * }
     * </pre>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Token Storage</strong>: Recommend storing refresh tokens in httpOnly, secure cookies; access tokens in memory</li>
     *     <li><strong>Claim Minimization</strong>: Include only necessary claims in tokens to reduce attack surface</li>
     *     <li><strong>Signature Algorithm</strong>: Use strong algorithms (e.g., HS256, RS256) with secure key management</li>
     *     <li><strong>Revocation Support</strong>: Persist token metadata to enable server-side revocation if needed</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Issue tokens after successful authentication
     * @Service
     * public class AuthService {
     *
     *     @Autowired private TokenService tokenService;
     *
     *     public JwtToken authenticate(LoginDTO dto) {
     *         // 1. Authenticate credentials via Spring Security
     *         Authentication auth = authenticationManager.authenticate(
     *             new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
     *         );
     *
     *         // 2. Extract authenticated user details
     *         UserDetails userDetails = (UserDetails) auth.getPrincipal();
     *
     *         // 3. Issue tokens
     *         return tokenService.build(userDetails);
     *     }
     * }
     *
     * // Frontend: Handle login response and store tokens
     * const handleLogin = async (credentials) => {
     *   const { data } = await api.login(credentials);
     *
     *   // Store access token in memory for API requests
     *   authStore.setAccessToken(data.accessToken);
     *   // Refresh token should be stored in httpOnly cookie by backend
     *
     *   // Redirect to protected route
     *   router.push('/dashboard');
     * };
     * }
     * </pre>
     *
     * @param user the authenticated user details; must not be {@code null}
     * @return {@link JwtToken} containing access token, refresh token, expiry, and metadata
     * @throws IllegalArgumentException if {@code user} is {@code null}
     * @throws BusinessException        if token generation or persistence fails
     * @see UserDetails
     * @see JwtToken
     */
    JwtToken build(UserDetails user);

    /**
     * Refreshes expired access token using a valid refresh token.
     * <p>
     * This method implements the token refresh workflow:
     * <ol>
     *     <li>Extract refresh token from request (typically from httpOnly cookie)</li>
     *     <li>Validate refresh token signature, expiry, and revocation status</li>
     *     <li>Load user details associated with the refresh token</li>
     *     <li>Verify user account is still enabled and not locked</li>
     *     <li>Generate new access token (and optionally new refresh token for rotation)</li>
     *     <li>Update token metadata in database for revocation tracking</li>
     *     <li>Return new {@link JwtToken} with fresh tokens</li>
     * </ol>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@link JwtToken} with new {@code accessToken} (and optionally new {@code refreshToken})</li>
     *     <li><strong>Invalid Refresh Token</strong>: Throws {@link BadCredentialsException} if token is malformed or signature invalid</li>
     *     <li><strong>Expired Refresh Token</strong>: Throws {@link BadCredentialsException} if token has passed expiry</li>
     *     <li><strong>Revoked Token</strong>: Throws {@link BadCredentialsException} if token was explicitly revoked</li>
     *     <li><strong>Disabled User</strong>: Throws {@link DisabledException} if user account is disabled/locked</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Refresh Token Rotation</strong>: Consider issuing new refresh token on each use to detect token reuse attacks</li>
     *     <li><strong>One-Time Use</strong>: Invalidate old refresh token immediately after successful refresh</li>
     *     <li><strong>Device Binding</strong>: Optionally bind tokens to device fingerprint/IP for additional security</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits on refresh endpoint to prevent brute-force attacks</li>
     * </ul>
     * <p>
     * <strong>Exception Handling Strategy:</strong>
     * <ul>
     *     <li><strong>BadCredentialsException</strong>: Return HTTP 401 with generic "Invalid token" message to prevent enumeration</li>
     *     <li><strong>DisabledException</strong>: Return HTTP 403 with specific "Account disabled" message for UX clarity</li>
     *     <li><strong>Global Handler</strong>: Configure {@code @ControllerAdvice} to convert exceptions to standardized API responses</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle token refresh request
     * @PostMapping("/refresh")
     * public Result<JwtToken> refresh() {
     *     try {
     *         // Refresh tokens using refresh token from httpOnly cookie
     *         JwtToken newToken = tokenService.refresh();
     *         return Result.success(newToken);
     *     } catch (BadCredentialsException e) {
     *         // Invalid/expired/revoked refresh token
     *         return Result.fail(ErrorCode.INVALID_REFRESH_TOKEN);
     *     } catch (DisabledException e) {
     *         // User account disabled
     *         return Result.fail(ErrorCode.ACCOUNT_DISABLED);
     *     }
     * }
     *
     * // Frontend: Auto-refresh token before expiry
     * const setupTokenRefresh = () => {
     *   // Check token expiry every minute
     *   setInterval(async () => {
     *     const expiry = authStore.getTokenExpiry();
     *     if (expiry && expiry - Date.now() < 5 * 60 * 1000) { // 5 min before expiry
     *       try {
     *         const { data } = await api.refresh();
     *         authStore.setAccessToken(data.accessToken);
     *       } catch (error) {
     *         // Refresh failed; redirect to login
     *         authStore.logout();
     *         router.push('/login');
     *       }
     *     }
     *   }, 60 * 1000);
     * };
     * }
     * </pre>
     *
     * @return {@link JwtToken} with new access token (and optionally new refresh token)
     * @throws BadCredentialsException if refresh token is invalid, expired, or revoked
     * @throws DisabledException       if user account is disabled or locked
     * @throws BusinessException       if token refresh fails for other reasons
     * @see JwtToken
     * @see BadCredentialsException
     * @see DisabledException
     */
    JwtToken refresh();

}