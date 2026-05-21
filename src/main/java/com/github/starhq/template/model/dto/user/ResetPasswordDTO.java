package com.github.starhq.template.model.dto.user;

import com.github.starhq.template.common.validation.StrongPassword;
import com.github.starhq.template.model.dto.SensitiveDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Data transfer object for user password reset requests.
 * <p>
 * This class encapsulates the parameters required for securely changing a user's
 * password, including verification of the current password and enforcement of
 * strength policies for the new password. It extends {@link SensitiveDTO} to
 * ensure automatic masking of password fields in logs and API responses.
 * <p>
 * <strong>Security Requirements:</strong>
 * <ul>
 *     <li><strong>Old Password Verification</strong>: Must match the user's current hashed password to prevent unauthorized changes</li>
 *     <li><strong>New Password Strength</strong>: Must satisfy complexity rules (uppercase, lowercase, digit, special char) via {@link StrongPassword}</li>
 *     <li><strong>Length Constraints</strong>: 8-20 characters to balance security and usability</li>
 *     <li><strong>Transmission Security</strong>: Always sent over HTTPS; never logged in plaintext</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * // Controller endpoint
 * @PutMapping("/password/reset")
 * public Result<Void> resetPassword(@RequestBody @Valid ResetPasswordDTO dto) {
 *     userService.resetPassword(SecurityContextUtils.getUserId(), dto);
 *     return Result.success();
 * }
 *
 * // Service layer validation and update
 * @Service
 * public class UserService {
 *     public void resetPassword(Long userId, ResetPasswordDTO dto) {
 *         // 1. Verify old password matches stored hash
 *         SysUser user = userMapper.selectById(userId);
 *         if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
 *             throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
 *         }
 *
 *         // 2. Prevent reuse of recent passwords (optional policy)
 *         if (isRecentPassword(userId, dto.getNewPassword())) {
 *             throw new BusinessException(ErrorCode.PASSWORD_REUSE_NOT_ALLOWED);
 *         }
 *
 *         // 3. Update with new hashed password
 *         user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
 *         user.setUpdateTime(LocalDateTime.now());
 *         userMapper.updateById(user);
 *
 *         // 4. Invalidate all active sessions for this user
 *         tokenService.revokeAllUserTokens(userId);
 *
 *         // 5. Record audit log (password field auto-masked by SensitiveDTO)
 *         auditLogService.record("PASSWORD_RESET", TargetType.USER, userId, null, userId);
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
 * @see com.github.starhq.template.service.UserService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResetPasswordDTO extends SensitiveDTO {

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
    private static final long serialVersionUID = 149698384L;

    /**
     * The user's current password for identity verification.
     * <p>
     * This field is required to confirm that the password change request is
     * initiated by the legitimate account owner. The value is compared against
     * the stored bcrypt hash using {@code PasswordEncoder.matches()}.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null or empty; whitespace-only values are rejected</li>
     *     <li>{@code @Size(min=8, max=20)}: Enforces length constraints consistent with registration policy</li>
     *     <li>{@code @StrongPassword}: Applies custom strength rules (see annotation definition)</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li>Never log this field in plaintext; {@link SensitiveDTO} ensures automatic masking</li>
     *     <li>Implement rate limiting on reset endpoints to prevent brute-force attacks</li>
     *     <li>Consider requiring MFA for password reset in high-security environments</li>
     * </ul>
     * <p>
     * <strong>Error Messages:</strong>
     * <ul>
     *     <li>{@code {error.param.blank}}: "Password cannot be empty"</li>
     *     <li>{@code {error.param.range}}: "Password must be 8-20 characters"</li>
     *     <li>{@code @StrongPassword}: Custom message defined in annotation (e.g., "Password must contain uppercase, lowercase, digit, and special character")</li>
     * </ul>
     *
     * @see StrongPassword
     * @see org.springframework.security.crypto.password.PasswordEncoder#matches(CharSequence, String)
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 8, max = 20, message = "{error.param.range}")
    @StrongPassword
    private String oldPassword;

    /**
     * The new password to set for the user account.
     * <p>
     * This field must satisfy the system's password strength policy to ensure
     * account security. After validation, it is hashed with bcrypt before
     * persistence; the plaintext value is never stored or logged.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null or empty</li>
     *     <li>{@code @Size(min=8, max=20)}: Enforces length constraints for memorability and security</li>
     *     <li>{@code @StrongPassword}: Requires at least one uppercase, one lowercase, one digit, and one special character</li>
     * </ul>
     * <p>
     * <strong>Additional Policy Checks (Service Layer):</strong>
     * <ul>
     *     <li><strong>Password History</strong>: Prevent reuse of last N passwords (configurable)</li>
     *     <li><strong>Similarity Check</strong>: Reject new passwords too similar to old password</li>
     *     <li><strong>Dictionary Check</strong>: Block common passwords (e.g., "password123", "admin")</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li>Always hash with bcrypt (strength ≥ 12) before persistence</li>
     *     <li>Never include password in API response, even on error</li>
     *     <li>Invalidate all active sessions after successful password change</li>
     *     <li>Send confirmation email/notification to user after reset</li>
     * </ul>
     * <p>
     * <strong>Error Messages:</strong>
     * <ul>
     *     <li>{@code {error.param.blank}}: "New password cannot be empty"</li>
     *     <li>{@code {error.param.range}}: "New password must be 8-20 characters"</li>
     *     <li>{@code @StrongPassword}: Custom message defined in annotation</li>
     * </ul>
     *
     * @see StrongPassword
     * @see com.github.starhq.template.common.validation.PasswordStrengthValidator
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 8, max = 20, message = "{error.param.range}")
    @StrongPassword
    private String newPassword;

}