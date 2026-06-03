package com.github.starhq.template.model.dto;

import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Set;

/**
 * Data Transfer Object for creating or updating user accounts in the RBAC system.
 * <p>
 * This class extends {@link SensitiveDTO} to inherit automatic serialization filtering
 * for sensitive fields (e.g., passwords) and encapsulates user-submitted form data for
 * user management operations with built-in validation constraints.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Management Console</strong>: User creation/edit forms in admin panel</li>
 *     <li><strong>API Endpoints</strong>: {@code POST /api/users} and {@code PUT /api/users/{id}} request bodies</li>
 *     <li><strong>Service Layer</strong>: Type-safe parameter passing with compile-time validation hints</li>
 * </ul>
 * <p>
 * <strong>Validation Strategy:</strong>
 * <p>
 * All constraints use internationalized message keys (e.g., {@code "{error.param.blank}"})
 * configured in {@code ValidationMessages.properties} for multi-language support.
 * Validation is triggered automatically by Spring's {@code @Valid} annotation in controllers:
 * <pre>
 * {@code
 * @PostMapping("/users")
 * public Result<Void> createUser(@Valid @RequestBody UserDTO dto) {
 *     // dto is guaranteed to pass validation constraints here
 *     userService.create(dto);
 *     return Result.success();
 * }
 * }
 * </pre>
 * <p>
 * <strong>Serialization:</strong>
 * <p>
 * Implements {@link java.io.Serializable} with a fixed {@code serialVersionUID} to support:
 * <ul>
 *     <li>Caching DTO instances in distributed caches (Redis)</li>
 *     <li>Transmitting across service boundaries in microservice architectures</li>
 *     <li>Session replication in clustered deployments</li>
 * </ul>
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 *     <li>Password field is automatically excluded from JSON serialization via {@link SensitiveDTO}</li>
 *     <li>Password strength is validated via custom {@link StrongPassword} annotation</li>
 *     <li>Role assignments are validated for existence before persistence</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see SensitiveDTO
 * @see StrongPassword
 * @see jakarta.validation.Valid
 * @see com.github.starhq.template.service.UserService
 * @see <a href="https://beanvalidation.org/2.0/">Jakarta Bean Validation 2.0 Specification</a>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserDTO extends SensitiveDTO {

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
    private static final long serialVersionUID = 60288L;

    /**
     * The unique username for user login and identification.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=3, max=20)}: Length must be between 3 and 20 characters (inclusive)</li>
     *     <li>Uniqueness: Must be globally unique across all users (enforced at service/database layer)</li>
     * </ul>
     * <p>
     * <strong>Format Convention:</strong>
     * <ul>
     *     <li>Use lowercase letters, numbers, underscores, dots, and @ symbols</li>
     *     <li>Common formats: {@code "john.doe"}, {@code "johndoe"}, {@code "john_doe"}</li>
     *     <li>May include email addresses for email-based login systems</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Validate username uniqueness to prevent account confusion</li>
     *     <li>Consider implementing username enumeration prevention (same error for existing/non-existing users)</li>
     *     <li>Log login attempts for security audit trails</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is typically bound to form input fields with real-time validation:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-form-item label="Username" prop="username">
     *   <el-input v-model="form.username" :maxlength="20" :minlength="3" />
     * </el-form-item>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 3, max = 20, message = "{error.param.range}")
    private String username;

    /**
     * The password for user authentication.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=8, max=20)}: Length must be between 8 and 20 characters (inclusive)</li>
     *     <li>{@code @StrongPassword}: Custom validation for password strength (uppercase, lowercase, digits, special chars)</li>
     *     <li>Message Keys: {@code "{error.param.blank}"} / {@code "{error.param.range}"} for i18n support</li>
     * </ul>
     * <p>
     * <strong>Password Strength Requirements:</strong>
     * <p>
     * The {@code @StrongPassword} annotation enforces:
     * <ul>
     *     <li>At least 8 characters</li>
     *     <li>At least one uppercase letter (A-Z)</li>
     *     <li>At least one lowercase letter (a-z)</li>
     *     <li>At least one digit (0-9)</li>
     *     <li>At least one special character (!@#$%^&*(),.?":{}|&lt;&gt;)</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Password is automatically excluded from JSON serialization via {@link SensitiveDTO}</li>
     *     <li>Password should be hashed (e.g., BCrypt) before storage</li>
     *     <li>Implement rate-limiting on login attempts to prevent brute-force attacks</li>
     *     <li>Consider implementing password history to prevent reuse</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * Password fields should use masked input and strength indicators:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-form-item label="Password" prop="password">
     *   <el-input v-model="form.password" type="password" :maxlength="20" :minlength="8" show-password />
     *   <el-progress
     *     v-if="form.password"
     *     :percentage="calculatePasswordStrength(form.password)"
     *     :color="passwordStrengthColor" />
     * </el-form-item>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 8, max = 20, message = "{error.param.range}")
    @StrongPassword // Custom annotation for validating password strength
    private String password;

    /**
     * The active status of the user account.
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>{@code ENABLED}: User can log in and access the system</li>
     *     <li>{@code DISABLED}: User cannot log in; account is locked</li>
     *     <li>{@code EXPIRED}: User account has expired; requires admin intervention</li>
     *     <li>{@code LOCKED}: User account is locked due to security reasons</li>
     * </ul>
     * <p>
     * <strong>Default Value:</strong>
     * <ul>
     *     <li>New users are typically created with {@code UserStatus.ENABLED}</li>
     *     <li>Admins may change status to {@code DISABLED} for policy violations or inactive accounts</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Create active user
     * UserDTO dto = new UserDTO();
     * dto.setUsername("john.doe");
     * dto.setPassword("StrongPass123!");
     * dto.setStatus(UserStatus.ENABLED);
     *
     * // Lock user account
     * dto.setStatus(UserStatus.DISABLED);
     * }
     * </pre>
     *
     * @see UserStatus
     */
    private UserStatus status;

    /**
     * The set of role identifiers assigned to this user.
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Establishes many-to-many relationship: {@code User 1..* Role}</li>
     *     <li>Used for role-based access control (RBAC) and permission resolution</li>
     *     <li>Users inherit permissions from all assigned roles</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotEmpty}: Must not be {@code null} or empty for create operations</li>
     *     <li>Each role ID must reference an existing, active {@code SysRole} record</li>
     *     <li>At least one role is typically required for user activation</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Assign multiple roles to user
     * UserDTO dto = new UserDTO();
     * dto.setUsername("jane.doe");
     * dto.setPassword("StrongPass123!");
     * dto.setRoleIds(Set.of(1L, 2L)); // Admin and User Manager roles
     * }
     * </pre>
     * <p>
     * <strong>Permission Resolution:</strong>
     * <p>
     * User permissions are computed as the union of all assigned role permissions:
     * <pre>
     * {@code
     * // Pseudo-code for permission resolution
     * Set<String> userPermissions = user.getRoles().stream()
     *     .flatMap(role -> role.getPermissions().stream())
     *     .collect(Collectors.toSet());
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Large role sets may impact query performance; consider pagination for assignment UI</li>
     *     <li>Cache user-role mappings with TTL to reduce database queries</li>
     *     <li>Validate role IDs exist before assignment to prevent orphaned references</li>
     * </ul>
     *
     * @see UserStatus
     * @see com.github.starhq.template.entity.SysRole
     */
    private Set<Long> roleIds;

}
