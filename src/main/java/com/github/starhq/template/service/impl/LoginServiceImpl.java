package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.model.dto.user.LoginDTO;
import com.github.starhq.template.service.CaptchaService;
import com.github.starhq.template.service.LoginService;
import com.github.starhq.template.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service implementation for user authentication with CAPTCHA verification, credential validation, and JWT token issuance.
 * <p>
 * This class implements {@link LoginService} to provide the complete authentication workflow:
 * <ol>
 *     <li>CAPTCHA verification to prevent automated brute-force attacks</li>
 *     <li>Credential authentication via Spring Security {@link AuthenticationManager}</li>
 *     <li>Exception translation to business-friendly error codes for consistent API responses</li>
 *     <li>JWT token generation and session tracking via {@link TokenService}</li>
 * </ol>
 * Designed to centralize login logic with defense-in-depth security policies, audit trail support,
 * and integration with stateless JWT-based authentication.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>CAPTCHA Enforcement</strong>: Require valid CAPTCHA before authentication to block bots</li>
 *     <li><strong>Credential Validation</strong>: Delegate to Spring Security for password verification and account status checks</li>
 *     <li><strong>Exception Translation</strong>: Convert low-level Spring Security exceptions to business error codes for frontend handling</li>
 *     <li><strong>Token Issuance</strong>: Generate access/refresh tokens and record session for subsequent authenticated requests</li>
 * </ul>
 * <p>
 * <strong>Security Design Principles:</strong>
 * <ul>
 *     <li><strong>Fail-Secure</strong>: Any authentication failure returns generic error to prevent username enumeration</li>
 *     <li><strong>Layered Defense</strong>: CAPTCHA + credential validation + account status checks + rate limiting</li>
 *     <li><strong>Stateless Authentication</strong>: JWT tokens contain all necessary claims; no server-side session storage required</li>
 *     <li><strong>Audit-Ready</strong>: All login attempts (success/failure) logged for compliance tracking and anomaly detection</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Handle login request with error handling
 * @RestController
 * @RequestMapping("/api/v1/auth")
 * @RequiredArgsConstructor
 * public class AuthController {
 *
 *     private final LoginService loginService;
 *
 *     @PostMapping("/login")
 *     public Result<JwtToken> login(@Valid @RequestBody LoginDTO loginDTO) {
 *         try {
 *             // Authenticate and issue tokens
 *             JwtToken token = loginService.login(loginDTO);
 *             return Result.success(token);
 *         } catch (CustomException e) {
 *             // Return business error code for frontend handling
 *             return Result.fail(e.getErrorCode());
 *         }
 *     }
 * }
 *
 * // Frontend: Handle login response and store tokens
 * const handleLogin = async (credentials) => {
 *   try {
 *     const { data } = await api.login(credentials);
 *     authStore.setTokens(data.accessToken, data.refreshToken);
 *     router.push('/dashboard');
 *   } catch (error) {
 *     if (error.code === ErrorCode.CREDENTIALS.code) {
 *       message.error('Invalid username or password');
 *     } else if (error.code === ErrorCode.DISABLED.code) {
 *       message.error('Account is disabled, please contact support');
 *     }
 *   }
 * };
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-23
 * @see LoginService
 * @see AuthenticationManager
 * @see JwtToken
 * @see LoginDTO
 */
@Service("loginService")
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    /**
     * Spring Security authentication manager for credential validation.
     * <p>
     * This component handles:
     * <ul>
     *     <li>Loading user details via configured {@link org.springframework.security.core.userdetails.UserDetailsService}</li>
     *     <li>Verifying password match using configured {@link org.springframework.security.crypto.password.PasswordEncoder}</li>
     *     <li>Checking account status flags (enabled, locked, expired, credentialsExpired)</li>
     *     <li>Throwing typed {@link AuthenticationException} subclasses for specific failure scenarios</li>
     * </ul>
     * <p>
     * <strong>Exception Mapping:</strong>
     * <ul>
     *     <li>{@link BadCredentialsException}: Username/password mismatch</li>
     *     <li>{@link DisabledException}: Account is disabled by administrator</li>
     *     <li>{@link LockedException}: Account is temporarily locked due to failed attempts</li>
     *     <li>{@link AccountExpiredException}: Account has passed its expiration date</li>
     *     <li>{@link CredentialsExpiredException}: Password has expired and requires change</li>
     * </ul>
     *
     * @see AuthenticationManager#authenticate(Authentication)
     * @see org.springframework.security.authentication.dao.DaoAuthenticationProvider
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Service for JWT token generation and session management.
     * <p>
     * Used to:
     * <ul>
     *     <li>Generate access token (short-lived, included in Authorization header)</li>
     *     <li>Generate refresh token (long-lived, stored securely for token renewal)</li>
     *     <li>Record session metadata (IP, user agent, login time) for audit and token revocation</li>
     *     <li>Support token refresh workflow without re-authentication</li>
     * </ul>
     *
     * @see TokenService#build(UserDetails)
     * @see JwtToken
     */
    private final TokenService tokenService;

    /**
     * Service for CAPTCHA generation and verification.
     * <p>
     * Used to enforce visual challenge before authentication to:
     * <ul>
     *     <li>Prevent automated brute-force attacks on login endpoint</li>
     *     <li>Block credential stuffing attempts from compromised credential lists</li>
     *     <li>Add friction for malicious bots while maintaining UX for legitimate users</li>
     * </ul>
     * <p>
     * <strong>Verification Behavior:</strong>
     * <ul>
     *     <li>Throws {@link com.github.starhq.template.common.exception.BusinessException} on CAPTCHA mismatch/expiry</li>
     *     <li>Invalidates CAPTCHA after single use to prevent replay attacks</li>
     *     <li>Tracks per-IP failure attempts for progressive rate limiting</li>
     * </ul>
     *
     * @see CaptchaService#verify(String, String)
     */
    private final CaptchaService captchaService;

    /**
     * Authenticates a user with provided credentials and issues JWT tokens for subsequent API access.
     * <p>
     * This method implements the complete authentication workflow:
     * <ol>
     *     <li><strong>CAPTCHA Verification</strong>: Validate user-provided CAPTCHA via {@link CaptchaService#verify}; throws business exception on failure</li>
     *     <li><strong>Token Preparation</strong>: Wrap credentials in {@link UsernamePasswordAuthenticationToken} for Spring Security</li>
     *     <li><strong>Authentication Execution</strong>: Delegate to {@link AuthenticationManager#authenticate} for credential validation and account status checks</li>
     *     <li><strong>Exception Translation</strong>: Convert Spring Security exceptions to business-friendly {@link CustomException} with appropriate error codes</li>
     *     <li><strong>Token Generation</strong>: On success, extract {@link UserDetails} and generate JWT tokens via {@link TokenService#build}</li>
     *     <li><strong>Audit Logging</strong>: Record successful login attempt with IP, user agent, and timestamp (handled by TokenService or AOP)</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code loginDTO}: Must not be {@code null}; should include {@code username}, {@code password}, {@code uuid}, {@code captcha}</li>
     *     <li>{@code loginDTO.getUsername()}: Must not be {@code null} or empty; case-insensitive lookup recommended</li>
     *     <li>{@code loginDTO.getPassword()}: Must not be {@code null} or empty; plain text password (will be encoded for comparison)</li>
     *     <li>{@code loginDTO.getUuid()}/{@code getCaptcha()}: Must match valid, unexpired CAPTCHA challenge</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@link JwtToken} with {@code accessToken}, {@code refreshToken}, {@code expiresIn}, and user metadata</li>
     *     <li><strong>CAPTCHA Failure</strong>: Throws {@link com.github.starhq.template.common.exception.BusinessException} with {@link ErrorCode#CAPTCHA_VERIFY} or {@link ErrorCode#CAPTCHA_EXPIRED}</li>
     *     <li><strong>Credential Failure</strong>: Throws {@link CustomException} with {@link ErrorCode#CREDENTIALS} (generic to prevent username enumeration)</li>
     *     <li><strong>Account Status Issues</strong>: Throws {@link CustomException} with {@link ErrorCode#DISABLED} for disabled/locked accounts</li>
     *     <li><strong>System Errors</strong>: Throws {@link CustomException} with {@link ErrorCode#UNAUTHORIZED} for unexpected authentication failures</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>CAPTCHA First</strong>: Verify CAPTCHA before expensive authentication to block bots early</li>
     *     <li><strong>Generic Errors</strong>: Return same error message for invalid username OR password to prevent username enumeration attacks</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-user login attempt limits (e.g., max 5 attempts per 10 minutes) to prevent brute-force attacks</li>
     *     <li><strong>Token Storage</strong>: Recommend storing refresh tokens in httpOnly, secure cookies; access tokens in memory or secure storage</li>
     *     <li><strong>Audit Logging</strong>: Log all login attempts (success/failure) with IP, user agent, and timestamp for security monitoring</li>
     * </ul>
     * <p>
     * <strong>Exception Handling Strategy:</strong>
     * <pre>
     * {@code
     * // CAPTCHA failure: Business exception with specific code
     * captchaService.verify(uuid, captcha); // Throws BusinessException on failure
     *
     * // Credential failure: Generic error to prevent enumeration
     * catch (BadCredentialsException e) {
     *     throw new CustomException(ErrorCode.CREDENTIALS, HttpStatus.UNAUTHORIZED);
     * }
     *
     * // Account status failure: Specific error for UX clarity
     * catch (DisabledException | LockedException e) {
     *     throw new CustomException(ErrorCode.DISABLED, HttpStatus.UNAUTHORIZED);
     * }
     *
     * // Unexpected authentication failure: Generic fallback
     * catch (AuthenticationException e) {
     *     throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
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
     *         return Result.success(token);
     *     } catch (CustomException e) {
     *         // Return business error code for frontend handling
     *         return Result.fail(e.getErrorCode());
     *     }
     * }
     *
     * // Frontend: Handle login response and store tokens
     * const handleLogin = async (credentials) => {
     *   try {
     *     const { data } = await api.login(credentials);
     *     authStore.setTokens(data.accessToken, data.refreshToken);
     *     router.push('/dashboard');
     *   } catch (error) {
     *     if (error.code === ErrorCode.CREDENTIALS.code) {
     *       message.error('Invalid username or password');
     *       refreshCaptcha(); // Auto-refresh CAPTCHA on failure
     *     } else if (error.code === ErrorCode.DISABLED.code) {
     *       message.error('Account is disabled, please contact support');
     *     }
     *   }
     * };
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>CAPTCHA Cache</strong>: Ensure CAPTCHA verification leverages Redis cache for low-latency validation</li>
     *     <li><strong>Authentication Caching</strong>: Consider caching UserDetails by username for repeated auth attempts (with short TTL)</li>
     *     <li><strong>Token Generation</strong>: JWT signing is CPU-intensive; consider async generation or CDN caching for high-traffic scenarios</li>
     * </ul>
     *
     * @param loginDTO the login credentials with username, password, uuid, and captcha; must not be {@code null}
     * @return {@link JwtToken} containing access token, refresh token, expiry, and user metadata
     * @throws IllegalArgumentException                                      if {@code loginDTO} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.BusinessException if CAPTCHA verification fails
     * @throws CustomException                                               with {@link ErrorCode#CREDENTIALS} if credentials are invalid (generic to prevent enumeration)
     * @throws CustomException                                               with {@link ErrorCode#DISABLED} if account is disabled or locked
     * @throws CustomException                                               with {@link ErrorCode#UNAUTHORIZED} for unexpected authentication failures
     * @see LoginDTO
     * @see JwtToken
     * @see CaptchaService#verify(String, String)
     * @see AuthenticationManager#authenticate(Authentication)
     * @see TokenService#build(UserDetails)
     */
    @Override
    public JwtToken login(LoginDTO loginDTO) {
        // 1. Verify CAPTCHA first (fail fast to block bots before expensive auth)
        // Throws BusinessException with CAPTCHA_VERIFY or CAPTCHA_EXPIRED on failure
        captchaService.verify(loginDTO.getUuid(), loginDTO.getCaptcha());

        // 2. Prepare authentication token for Spring Security
        // UsernamePasswordAuthenticationToken triggers DaoAuthenticationProvider workflow
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());

        Authentication authentication;
        try {
            // 3. Execute authentication (delegates to configured UserDetailsService + PasswordEncoder)
            // Throws typed AuthenticationException subclasses for specific failure scenarios
            authentication = authenticationManager.authenticate(authenticationToken);

        } catch (BadCredentialsException _) {
            // ✅ Translate to business exception: generic error to prevent username enumeration
            // Frontend shows "Invalid username or password" without revealing which field is wrong
            throw new CustomException(ErrorCode.CREDENTIALS, HttpStatus.UNAUTHORIZED);

        } catch (DisabledException | LockedException _) {
            // ✅ Translate to business exception: specific error for account status issues
            // Frontend can show "Account is disabled" for better UX while maintaining security
            throw new CustomException(ErrorCode.DISABLED, HttpStatus.UNAUTHORIZED);

        } catch (AuthenticationException _) {
            // ✅ Fallback: catch any other authentication exception with generic error
            // Prevents information leakage from unexpected auth provider errors
            throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        // 4. Authentication successful: extract authenticated user details
        // Principal is guaranteed to be UserDetails after successful authentication
        Object principal = authentication.getPrincipal();
        UserDetails userDetails = (UserDetails) principal;

        // 5. Generate JWT tokens and record session metadata
        // Returns JwtToken with accessToken, refreshToken, expiresIn, and user metadata
        return tokenService.build(userDetails);
    }

}