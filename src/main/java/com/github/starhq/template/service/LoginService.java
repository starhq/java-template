package com.github.starhq.template.service;

import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.model.dto.user.LoginDTO;

/**
 * Service interface for user authentication and JWT token issuance.
 * <p>
 * This interface provides the core authentication workflow for the application,
 * handling credential validation, account status checks, and secure token generation.
 * Designed to centralize login logic with consistent security policies, audit trail support,
 * and integration with Spring Security and JWT-based stateless authentication.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Login</strong>: Authenticate users via username/password and issue JWT access/refresh tokens</li>
 *     <li><strong>Token Refresh</strong>: Support token renewal workflow for extended sessions without re-authentication</li>
 *     <li><strong>Security Enforcement</strong>: Enforce account status checks (enabled, not locked, not expired) during authentication</li>
 *     <li><strong>Audit Integration</strong>: Record login attempts (success/failure) for compliance tracking and anomaly detection</li>
 * </ul>
 * <p>
 * <strong>Security Design Principles:</strong>
 * <ul>
 *     <li><strong>Fail-Secure</strong>: Any authentication failure returns generic error to prevent username enumeration</li>
 *     <li><strong>Stateless Authentication</strong>: JWT tokens contain all necessary claims; no server-side session storage required</li>
 *     <li><strong>Token Hygiene</strong>: Short-lived access tokens (15-30 min) + long-lived refresh tokens (7-30 days) for balanced security/UX</li>
 *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-user login attempt limits to prevent brute-force attacks</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Handle login request
 * @RestController
 * @RequestMapping("/api/v1/auth")
 * @RequiredArgsConstructor
 * public class AuthController {
 *
 *     private final LoginService loginService;
 *
 *     @PostMapping("/login")
 *     public Result<JwtToken> login(@Valid @RequestBody LoginDTO loginDTO) {
 *         // Authenticate and issue tokens
 *         JwtToken token = loginService.login(loginDTO);
 *
 *         // Return tokens to client for subsequent authenticated requests
 *         return Result.success(token);
 *     }
 * }
 *
 * // Frontend: Store tokens and use for authenticated API calls
 * const handleLogin = async (credentials) => {
 *   const { data } = await api.login(credentials);
 *
 *   // Store tokens securely (httpOnly cookie or secure storage)
 *   authStore.setTokens(data.accessToken, data.refreshToken);
 *
 *   // Redirect to dashboard
 *   router.push('/dashboard');
 * };
 *
 * // Subsequent API calls: Include access token in Authorization header
 * const apiClient = axios.create({
 *   baseURL: '/api',
 *   headers: {
 *     'Authorization': `Bearer ${authStore.accessToken}`
 *   }
 * });
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-23
 * @see JwtToken
 * @see LoginDTO
 * @see org.springframework.security.authentication.AuthenticationManager
 */
public interface LoginService {

    /**
     * Authenticates a user with provided credentials and issues JWT tokens for subsequent API access.
     * <p>
     * This method implements the complete authentication workflow:
     * <ol>
     *     <li>Validate input credentials (non-empty username/password)</li>
     *     <li>Load user details via {@link org.springframework.security.core.userdetails.UserDetailsService}</li>
     *     <li>Verify password match using {@link org.springframework.security.crypto.password.PasswordEncoder}</li>
     *     <li>Check account status (enabled, not locked, not expired) via {@link com.github.starhq.template.common.util.SecurityUserUtils}</li>
     *     <li>Record successful login attempt in audit log for compliance tracking</li>
     *     <li>Return {@link JwtToken} containing tokens, expiry, and user metadata</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code loginDTO}: Must not be {@code null}; should include {@code username} and {@code password}</li>
     *     <li>{@code loginDTO.getUsername()}: Must not be {@code null} or empty; case-insensitive lookup recommended</li>
     *     <li>{@code loginDTO.getPassword()}: Must not be {@code null} or empty; plain text password (will be encoded for comparison)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@link JwtToken} with {@code accessToken}, {@code refreshToken}, {@code expiresIn}, and user metadata</li>
     *     <li><strong>Account Status Issues</strong>: Throws typed exceptions for disabled/locked/expired accounts with specific error codes</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Password Handling</strong>: Never log or expose plain text passwords; use {@code PasswordEncoder.matches()} for constant-time comparison</li>
     *     <li><strong>Generic Errors</strong>: Return same error message for invalid username OR password to prevent username enumeration attacks</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-user login attempt limits (e.g., max 5 attempts per 10 minutes) to prevent brute-force attacks</li>
     *     <li><strong>Token Storage</strong>: Recommend storing refresh tokens in httpOnly, secure cookies; access tokens in memory or secure storage</li>
     *     <li><strong>Audit Logging</strong>: Log all login attempts (success/failure) with IP, user agent, and timestamp for security monitoring</li>
     * </ul>
     * <p>
     * <strong>JWT Token Structure:</strong>
     * <pre>
     * {@code
     * // Access token claims (short-lived, included in Authorization header)
     * {
     *   "sub": "alice",           // Subject (username)
     *   "userId": 1001,           // Custom claim: user ID
     *   "roles": ["USER", "ADMIN"], // Custom claim: authorities
     *   "iat": 1714000000,        // Issued at (epoch seconds)
     *   "exp": 1714001800         // Expiry (15 minutes later)
     * }
     *
     * // Refresh token claims (long-lived, stored securely for token renewal)
     * {
     *   "sub": "alice",
     *   "userId": 1001,
     *   "type": "refresh",        // Custom claim: token type
     *   "iat": 1714000000,
     *   "exp": 1714604800         // Expiry (7 days later)
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle login request with error handling
     * @PostMapping("/login")
     * public Result<JwtToken> login(@Valid @RequestBody LoginDTO loginDTO) {
     *     try {
     *         // Authenticate and issue tokens
     *         JwtToken token = loginService.login(loginDTO);
     *
     *         // Return tokens to client
     *         return Result.success(token);
     *
     *     } catch (AuthenticationException e) {
     *         // Return generic auth error to prevent username enumeration
     *         return Result.fail(ErrorCode.AUTH_FAILED);
     *
     *     } catch (AccountStatusException e) {
     *         // Return specific account status error for UX
     *         return Result.fail(e.getErrorCode());
     *     }
     * }
     *
     * // Frontend: Handle login response and store tokens
     * const handleLogin = async (credentials) => {
     *   try {
     *     const { data } = await api.login(credentials);
     *
     *     // Store tokens securely
     *     authStore.setAccessToken(data.accessToken);
     *     // Refresh token should be stored in httpOnly cookie by backend
     *
     *     // Redirect to protected route
     *     router.push('/dashboard');
     *
     *   } catch (error) {
     *     if (error.code === ErrorCode.AUTH_FAILED.code) {
     *       message.error('Invalid username or password');
     *     } else if (error.code === ErrorCode.ACCOUNT_LOCKED.code) {
     *       message.error('Account is locked, please contact support');
     *     }
     *   }
     * };
     * }
     * </pre>
     * <p>
     * <strong>Exception Handling Strategy:</strong>
     * <ul>
     *     <li><strong>Authentication Failures</strong>: Use generic {@link com.github.starhq.template.common.enums.ErrorCode#UNAUTHORIZED} to prevent username
     *     enumeration</li>
     *     <li><strong>Account Status Issues</strong>: Use specific error codes ({@code ACCOUNT_DISABLED}, {@code ACCOUNT_LOCKED}, {@code ACCOUNT_EXPIRED}) for clear UX feedback</li>
     *     <li><strong>System Errors</strong>: Log internally but return generic {@code INTERNAL_ERROR} to avoid information leakage</li>
     *     <li><strong>Global Handler</strong>: Configure {@code @ControllerAdvice} to convert exceptions to standardized API responses</li>
     * </ul>
     *
     * @param loginDTO the login credentials with username and password; must not be {@code null}
     * @return {@link JwtToken} containing access token, refresh token, expiry, and user metadata
     * @throws IllegalArgumentException                                      if {@code loginDTO} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.BusinessException if token generation fails or system error occurs
     * @see LoginDTO
     * @see JwtToken
     * @see org.springframework.security.crypto.password.PasswordEncoder#matches(CharSequence, String)
     */
    JwtToken login(LoginDTO loginDTO);

}