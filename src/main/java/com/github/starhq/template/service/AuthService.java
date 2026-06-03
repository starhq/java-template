package com.github.starhq.template.service;

import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.model.dto.ResetPasswordDTO;
import com.github.starhq.template.model.dto.UserDTO;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Authentication and authorization service interface integrating Spring Security.
 * <p>
 * This interface extends {@link UserDetailsService} to provide standardized user loading
 * for Spring Security authentication flow, while adding business-level operations for
 * user registration and password reset. Designed to centralize authentication logic
 * with consistent security policies and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Authentication</strong>: Load user details by username for Spring Security login flow</li>
 *     <li><strong>User Registration</strong>: Create new user accounts with validation and duplicate prevention</li>
 *     <li><strong>Password Reset</strong>: Secure password reset workflow with token validation and audit logging</li>
 *     <li><strong>Security Integration</strong>: Provide {@link UserDetails} for role/permission-based access control</li>
 * </ul>
 * <p>
 * <strong>Spring Security Integration:</strong>
 * <p>
 * By implementing {@link UserDetailsService}, this service integrates with Spring Security's
 * authentication mechanism:
 * <pre>
 * {@code
 * // Security configuration: Wire AuthService as UserDetailsService
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig {
 *
 *     @Autowired private AuthService authService;
 *
 *     @Bean
 *     public AuthenticationManager authenticationManager(
 *             AuthenticationConfiguration config) throws Exception {
 *         return config.getAuthenticationManager();
 *     }
 *
 *     @Bean
 *     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *         http
 *             .authorizeHttpRequests(auth -> auth
 *                 .requestMatchers("/api/v1/auth/**").permitAll()
 *                 .anyRequest().authenticated()
 *             )
 *             .formLogin(form -> form
 *                 .usernameParameter("username")
 *                 .passwordParameter("password")
 *             )
 *             .userDetailsService(authService); // Use AuthService for user loading
 *         return http.build();
 *     }
 * }
 *
 * // Authentication flow: Spring Security calls loadUserByUsername()
 * UserDetails userDetails = authService.loadUserByUsername("alice");
 * // Returns UserDetails with username, password (encoded), authorities, enabled status
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Security-First</strong>: All methods enforce input validation, password encoding, and audit logging</li>
 *     <li><strong>Stateless Authentication</strong>: Designed for JWT/token-based auth; no HTTP session dependency</li>
 *     <li><strong>Extensibility</strong>: Interface allows multiple implementations (DB, LDAP, OAuth2) via strategy pattern</li>
 *     <li><strong>i18n Ready</strong>: Error messages use {@link com.github.starhq.template.common.enums.ErrorCode} for multi-language support</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see UserDetailsService
 * @see UserDetails
 * @see UserDTO
 * @see ResetPasswordDTO
 * @see com.github.starhq.template.common.enums.ErrorCode
 */
public interface AuthService extends UserDetailsService {

    /**
     * Resets a user's password via secure token-based workflow.
     * <p>
     * This method implements a secure password reset flow that typically involves:
     * <ol>
     *     <li>User requests reset via email/phone (handled outside this method)</li>
     *     <li>System generates time-limited reset token and sends to user</li>
     *     <li>User submits new password with token via this method</li>
     *     <li>System validates token, encodes new password, and updates user record</li>
     *     <li>System invalidates used token and logs audit trail</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code resetPasswordDto}: Must not be {@code null}; should include {@code token}, {@code newPassword}</li>
     *     <li>{@code resetPasswordDto.getToken()}: Must be valid, unexpired, and unused reset token</li>
     *     <li>{@code resetPasswordDto.getNewPassword()}: Must meet password policy; will be encoded before storage</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if password was successfully reset</li>
     *     <li><strong>Invalid Token</strong>: Returns {@code false} or throws {@link BusinessException} with token error code</li>
     *     <li><strong>Expired Token</strong>: Returns {@code false} or throws {@link BusinessException} with expiry error code</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Token Security</strong>: Tokens must be cryptographically random, single-use, and time-limited (e.g., 15-60 min)</li>
     *     <li><strong>Rate Limiting</strong>: Limit reset attempts per user/IP to prevent enumeration attacks</li>
     *     <li><strong>Timing Attack Prevention</strong>: Use constant-time comparison for token validation</li>
     *     <li><strong>Audit Logging</strong>: Log all reset attempts (success/failure) with IP and timestamp</li>
     *     <li><strong>Session Invalidation</strong>: Invalidate all active sessions for the user after password change</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Password update and token invalidation succeed or fail together</li>
     *     <li>Consistency: Audit log entry is recorded in same transaction</li>
     *     <li>Isolation: Concurrent reset attempts for same user are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle password reset submission
     * @PostMapping("/reset-password")
     * public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
     *     boolean success = authService.resetPassword(dto);
     *
     *     if (success) {
     *         // Optional: Auto-login user after successful reset
     *         // UserDetails userDetails = authService.loadUserByUsername(dto.getUsername());
     *         // String token = jwtTokenProvider.createToken(userDetails);
     *
     *         return Result.success("Password reset successfully");
     *     } else {
     *         return Result.fail(ErrorCode.PASSWORD_RESET_INVALID_TOKEN);
     *     }
     * }
     *
     * // Service implementation: Reset logic
     * @Transactional
     * @Override
     * public boolean resetPassword(ResetPasswordDTO dto) {
     *     // 1. Validate and decode token
     *     ResetToken token = resetTokenService.validateToken(dto.getToken());
     *     if (token == null || token.isExpired() || token.isUsed()) {
     *         return false;
     *     }
     *
     *     // 2. Lookup user by token's user ID
     *     SysUser user = getAndCheckById(token.getUserId(), ErrorCode.USER_NOT_FOUND);
     *
     *     // 3. Encode and update password
     *     user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
     *     user.setUpdatedAt(OffsetDateTime.now());
     *     userMapper.updateById(user);
     *
     *     // 4. Invalidate used token
     *     resetTokenService.markTokenUsed(dto.getToken());
     *
     *     // 5. Invalidate all active sessions for user
     *     sessionService.invalidateUserSessions(user.getId());
     *
     *     // 6. Log audit trail
     *     auditLogService.record("PASSWORD_RESET", TargetType.USER, user.getId(), ...);
     *
     *     return true;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Token Storage Strategy:</strong>
     * <ul>
     *     <li><strong>Database</strong>: Store token hash (not plain token) with expiry and used flag</li>
     *     <li><strong>Redis</strong>: Use Redis with TTL for automatic expiry; store {@code token_hash -> user_id} mapping</li>
     *     <li><strong>Signed JWT</strong>: Embed user ID and expiry in signed JWT token; no server-side storage needed</li>
     * </ul>
     *
     * @param resetPasswordDto the reset request with token and new password; must not be {@code null}
     * @return {@code true} if password was successfully reset; {@code false} if token invalid/expired
     * @throws IllegalArgumentException                                      if {@code resetPasswordDto} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.BusinessException if validation fails
     * @see ResetPasswordDTO
     * @see org.springframework.security.crypto.password.PasswordEncoder
     */
    boolean resetPassword(ResetPasswordDTO resetPasswordDto);

}