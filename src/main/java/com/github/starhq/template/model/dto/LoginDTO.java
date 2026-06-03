package com.github.starhq.template.model.dto;

import com.github.starhq.template.common.validation.StrongPassword;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Data transfer object for user authentication (login) requests.
 * <p>
 * This class encapsulates the credentials and verification data required for
 * user login, including username, password, and CAPTCHA challenge-response.
 * It extends {@link SensitiveDTO} to ensure automatic masking of password
 * fields in logs and API responses, preventing credential leakage.
 * <p>
 * <strong>Authentication Flow:</strong>
 * <ol>
 *     <li>Client requests CAPTCHA image with unique {@code uuid}</li>
 *     <li>User enters username, password, and CAPTCHA code</li>
 *     <li>Server validates CAPTCHA ({@code uuid} + {@code captcha}) before credential check</li>
 *     <li>Server verifies username/password against stored credentials</li>
 *     <li>On success: Issue JWT tokens; On failure: Return generic error (prevent user enumeration)</li>
 * </ol>
 * <p>
 * <strong>Security Requirements:</strong>
 * <ul>
 *     <li><strong>CAPTCHA Verification</strong>: Must be validated server-side before password check to prevent brute-force attacks</li>
 *     <li><strong>Password Strength</strong>: Enforced via {@link StrongPassword} annotation (uppercase, lowercase, digit, special char)</li>
 *     <li><strong>Transmission Security</strong>: Always sent over HTTPS; credentials never logged in plaintext</li>
 *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-user login attempt limits to mitigate credential stuffing</li>
 *     <li><strong>Generic Error Messages</strong>: Return identical error format for invalid username vs. wrong password to prevent user enumeration</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * // Controller endpoint
 * @PostMapping("/auth/login")
 * public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
 *     // 1. Validate CAPTCHA first (fail fast)
 *     if (!captchaService.validate(dto.getUuid(), dto.getCaptcha())) {
 *         return Result.fail(ErrorCode.CAPTCHA_INCORRECT);
 *     }
 *
 *     // 2. Authenticate credentials (generic error to prevent enumeration)
 *     LoginVO vo = authService.authenticate(dto.getUsername(), dto.getPassword());
 *
 *     // 3. Clear used CAPTCHA to prevent replay
 *     captchaService.invalidate(dto.getUuid());
 *
 *     return Result.success(vo);
 * }
 *
 * // Service layer authentication
 * @Service
 * public class AuthService {
 *     public LoginVO authenticate(String username, String password) {
 *         SysUser user = userMapper.selectByUsername(username);
 *         if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
 *             // Generic error: don't reveal whether username exists
 *             throw new BusinessException(ErrorCode.CREDENTIALS_INCORRECT);
 *         }
 *
 *         if (user.getStatus() != UserStatus.ENABLED) {
 *             throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
 *         }
 *
 *         // Generate JWT tokens
 *         String accessToken = jwtService.generateAccessToken(user);
 *         String refreshToken = jwtService.generateRefreshToken(user);
 *
 *         // Record login audit (password auto-masked by SensitiveDTO)
 *         auditLogService.record("USER_LOGIN", TargetType.USER, user.getId(),
 *             Map.of("ip", RequestUtils.getClientIp()), user.getId());
 *
 *         return new LoginVO(accessToken, refreshToken, convertToUserVO(user));
 *     }
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see SensitiveDTO
 * @see StrongPassword
 * @see com.github.starhq.template.service.AuthService
 * @see com.github.starhq.template.common.validation.PasswordStrengthValidator
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LoginDTO extends SensitiveDTO {

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
    private static final long serialVersionUID = -5231672189461799210L;

    /**
     * The username or unique identifier for authentication.
     * <p>
     * Supports multiple login identifier strategies:
     * <ul>
     *     <li>Simple username: {@code "alice"}</li>
     *     <li>Email-style: {@code "alice@example.com"}</li>
     *     <li>Phone number: {@code "+8613800138000"} (if enabled in configuration)</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null or empty; whitespace-only values are rejected</li>
     *     <li>{@code @Size(min=3, max=20)}: Enforces reasonable length for usability and security</li>
     * </ul>
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li>Case-insensitive comparison recommended for username lookup</li>
     *     <li>Trim leading/trailing whitespace before database query</li>
     *     <li>Use parameterized queries to prevent SQL injection (MyBatis handles this)</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li>Never reveal whether a username exists in error messages (prevent enumeration)</li>
     *     <li>Implement account lockout after N failed attempts to mitigate brute-force attacks</li>
     *     <li>Consider requiring MFA for high-risk login scenarios (new device, unusual location)</li>
     * </ul>
     * <p>
     * <strong>Error Messages:</strong>
     * <ul>
     *     <li>{@code {error.param.blank}}: "Username cannot be empty"</li>
     *     <li>{@code {error.param.range}}: "Username must be 3-20 characters"</li>
     * </ul>
     *
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 3, max = 20, message = "{error.param.range}")
    private String username;

    /**
     * The user's password credential for authentication.
     * <p>
     * This field is compared against the stored bcrypt hash using
     * {@code PasswordEncoder.matches()}. The plaintext value is never
     * persisted, logged, or returned in API responses.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null or empty</li>
     *     <li>{@code @Size(min=8, max=20)}: Enforces length constraints for security and memorability</li>
     *     <li>{@code @StrongPassword}: Requires complexity: uppercase, lowercase, digit, and special character</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li><strong>Transmission</strong>: Always sent over HTTPS; never in URL query parameters</li>
     *     <li><strong>Storage</strong>: Hashed with bcrypt (strength ≥ 12) before persistence</li>
     *     <li><strong>Logging</strong>: Automatically masked by {@link SensitiveDTO} parent class</li>
     *     <li><strong>Response</strong>: Never included in API responses, even on authentication failure</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-IP/per-user attempt limits to prevent brute-force</li>
     * </ul>
     * <p>
     * <strong>Password Policy Enforcement:</strong>
     * <p>
     * The {@code @StrongPassword} annotation delegates to {@link com.github.starhq.template.common.validation.PasswordStrengthValidator}
     * which typically enforces:
     * <ul>
     *     <li>At least one uppercase letter (A-Z)</li>
     *     <li>At least one lowercase letter (a-z)</li>
     *     <li>At least one digit (0-9)</li>
     *     <li>At least one special character (!@#$%^&* etc.)</li>
     *     <li>Minimum length of 8 characters (enforced by {@code @Size})</li>
     * </ul>
     * <p>
     * <strong>Error Messages:</strong>
     * <ul>
     *     <li>{@code {error.param.blank}}: "Password cannot be empty"</li>
     *     <li>{@code {error.param.range}}: "Password must be 8-20 characters"</li>
     *     <li>{@code @StrongPassword}: "Password must contain uppercase, lowercase, digit, and special character"</li>
     * </ul>
     *
     * @see StrongPassword
     * @see org.springframework.security.crypto.password.PasswordEncoder#matches(CharSequence, String)
     * @see SensitiveDTO
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 8, max = 20, message = "{error.param.range}")
    @StrongPassword
    private String password;

    /**
     * The CAPTCHA code entered by the user for bot prevention.
     * <p>
     * This value is compared against the server-side stored CAPTCHA solution
     * associated with the {@code uuid}. CAPTCHA validation should occur
     * <strong>before</strong> credential verification to prevent brute-force
     * attacks on the password field.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null or empty</li>
     *     <li>Case-insensitive comparison recommended (convert both to lowercase before match)</li>
     *     <li>Trim whitespace to handle user input variations</li>
     * </ul>
     * <p>
     * <strong>CAPTCHA Lifecycle:</strong>
     * <ol>
     *     <li>Client requests CAPTCHA: {@code GET /captcha?uuid=xxx} → Server returns image + stores solution in Redis</li>
     *     <li>User submits login: {@code POST /auth/login} with {@code uuid} + {@code captcha}</li>
     *     <li>Server validates: {@code redis.get("captcha:" + uuid) == captcha.toLowerCase()}</li>
     *     <li>On success: Invalidate CAPTCHA key to prevent replay; On failure: Return generic error</li>
     * </ol>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li><strong>Short TTL</strong>: CAPTCHA solutions should expire in 2-5 minutes to limit attack window</li>
     *     <li><strong>Single Use</strong>: Invalidate CAPTCHA after first validation attempt (success or failure)</li>
     *     <li><strong>Rate Limiting</strong>: Limit CAPTCHA generation requests per IP to prevent resource exhaustion</li>
     *     <li><strong>Accessibility</strong>: Provide audio CAPTCHA alternative for visually impaired users</li>
     * </ul>
     * <p>
     * <strong>Error Messages:</strong>
     * <ul>
     *     <li>{@code {error.param.blank}}: "CAPTCHA cannot be empty"</li>
     *     <li>Validation failure: Return generic {@code ErrorCode.CAPTCHA_INCORRECT} (don't reveal if uuid expired)</li>
     * </ul>
     *
     * @see com.github.starhq.template.service.CaptchaService#verify(String, String)
     */
    @NotBlank(message = "{error.param.blank}")
    private String captcha;

    /**
     * The unique identifier for the CAPTCHA challenge.
     * <p>
     * This UUID is generated by the server when issuing a CAPTCHA image and
     * is used to retrieve the corresponding solution from server-side storage
     * (typically Redis) during validation. It ensures each CAPTCHA is
     * single-use and time-limited.
     * <p>
     * <strong>Format Requirements:</strong>
     * <ul>
     *     <li>Standard UUID format: {@code xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}</li>
     *     <li>Or base64-encoded random string (if using custom generator)</li>
     *     <li>Must match the {@code uuid} returned with the CAPTCHA image</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null or empty</li>
     *     <li>Format validation: Should match UUID pattern or custom format regex</li>
     *     <li>Existence check: Must correspond to a valid, unexpired CAPTCHA session</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li><strong>Unpredictability</strong>: UUIDs must be cryptographically random to prevent guessing</li>
     *     <li><strong>Short TTL</strong>: Associated CAPTCHA solution should expire in 2-5 minutes</li>
     *     <li><strong>Binding</strong>: Optionally bind UUID to client IP to prevent CAPTCHA farming attacks</li>
     *     <li><strong>Logging</strong>: Safe to log UUID (not sensitive); use for audit trail of login attempts</li>
     * </ul>
     * <p>
     * <strong>Usage Pattern:</strong>
     * <pre>
     * {@code
     * // 1. Client requests CAPTCHA
     * GET /api/v1/captcha
     * Response: { "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890", "image": "data:image/png;base64,..." }
     *
     * // 2. Client submits login with CAPTCHA
     * POST /api/v1/auth/login
     * {
     *   "username": "alice",
     *   "password": "Str0ngP@ss!",
     *   "captcha": "X7k9",
     *   "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
     * }
     * }
     * </pre>
     *
     * @see java.util.UUID
     * @see com.github.starhq.template.service.CaptchaService#generateCode(String, HttpServletResponse)
     */
    @NotBlank(message = "{error.param.blank}")
    private String uuid;

}