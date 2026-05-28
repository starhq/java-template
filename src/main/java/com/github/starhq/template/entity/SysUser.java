package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;


/**
 * Entity class representing a system user account for authentication and authorization.
 * <p>
 * This class maps to the {@code sys_user} table, extends {@link BaseEntity} for audit
 * trail, and implements {@link UserDetails} for seamless integration with Spring
 * Security's authentication framework. It serves as the core identity principal in
 * the RBAC system, linking users to roles, permissions, and session management.
 * <p>
 * <strong>Spring Security Integration:</strong>
 * <p>
 * By implementing {@link UserDetails}, this entity can be directly returned by
 * {@link org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(String)}.
 * Key methods delegating to this entity:
 * <ul>
 *     <li>{@link #getAuthorities()}: Returns granted permissions for access control evaluation</li>
 *     <li>{@link #getPassword()}: Returns bcrypt-hashed password for credential matching</li>
 *     <li>{@link #getUsername()}: Returns unique login identifier for authentication lookup</li>
 *     <li>{@link #isEnabled()}: Delegates to {@code status == UserStatus.ENABLED} for account status checks</li>
 * </ul>
 * <p>
 * <strong>Security & Privacy:</strong>
 * <p>
 * User entities contain sensitive PII and credentials. Always:
 * <ul>
 *     <li>Hash passwords with {@code BCrypt} (strength ≥ 12) before persistence</li>
 *     <li>Never expose raw passwords, salts, or security codes in API responses</li>
 *     <li>Apply field-level masking for PII (email, phone) in logs and audit trails</li>
 *     <li>Enforce account lockout policies after repeated failed login attempts</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see UserDetails
 * @see com.github.starhq.template.service.UserService
 * @see TableName
 */
@Data
@Alias("user")
@TableName("sys_user")
@EqualsAndHashCode(callSuper = false)
public class SysUser extends BaseEntity implements UserDetails {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when entities are stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., session replication).
     * Update this value only if the class structure changes in a
     * backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -6558931765964042708L;
    /**
     * The unique login identifier for authentication.
     * <p>
     * Used as the primary lookup key in {@code UserDetailsService.loadUserByUsername()}.
     * Typically follows email-style ({@code "alice@example.com"}) or simple username
     * ({@code "alice"}) conventions. Must be globally unique across all users.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link String} — case-insensitive comparison recommended for login</li>
     *     <li>Uniqueness: Must be globally unique; enforce via {@code UNIQUE INDEX uk_username}</li>
     *     <li>Nullability: {@code NOT NULL} — required for authentication</li>
     *     <li>Length: {@code @Size(min = 3, max = 64)} to balance usability and storage</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[\\w.@+-]+$")} for safe identifier format</li>
     *     <li>Case Handling: Store lowercase, compare case-insensitively during login</li>
     * </ul>
     * <p>
     * <strong>Spring Security Contract:</strong>
     * <p>
     * This field is returned by {@link #getUsername()} and used as the principal name
     * in {@link org.springframework.security.core.Authentication} objects.
     *
     * @see #getUsername()
     * @see org.springframework.security.core.userdetails.UserDetailsService
     */
    private String username;

    /**
     * The bcrypt-hashed password credential for authentication.
     * <p>
     * <strong>Storage Format:</strong>
     * <p>
     * Always store passwords using {@code BCrypt} with strength ≥ 12:
     * <pre>
     * {@code
     * // Encoding (during registration/password change)
     * String hashed = new BCryptPasswordEncoder(12).encode(rawPassword);
     *
     * // Verification (during login)
     * boolean matches = new BCryptPasswordEncoder().matches(rawPassword, hashed);
     * }
     * </pre>
     * <p>
     * <strong>Security Requirements:</strong>
     * <ul>
     *     <li>NEVER store or log plaintext passwords</li>
     *     <li>NEVER expose this field in API responses, logs, or error messages</li>
     *     <li>Rotate password hashing algorithm periodically (e.g., from BCrypt to Argon2)</li>
     *     <li>Enforce password complexity rules at registration/change endpoints</li>
     * </ul>
     * <p>
     * <strong>Spring Security Contract:</strong>
     * <p>
     * This field is returned by {@link #getPassword()} and passed to
     * {@link org.springframework.security.crypto.password.PasswordEncoder#matches(CharSequence, String)}
     * during authentication.
     *
     * @see #getPassword()
     * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
     */
    private String password;

    /**
     * The account status controlling login eligibility and system access.
     * <p>
     * Managed via the {@link UserStatus} enum, typical values include:
     * <ul>
     *     <li>{@code ENABLED}: Account is active and can authenticate</li>
     *     <li>{@code DISABLED}: Account is administratively suspended (e.g., policy violation)</li>
     *     <li>{@code LOCKED}: Account is temporarily locked due to failed login attempts</li>
     *     <li>{@code EXPIRED}: Account has passed its validity period (e.g., contractor access)</li>
     * </ul>
     * <p>
     * <strong>Spring Security Integration:</strong>
     * <p>
     * The {@link #isEnabled()} method delegates to {@code status == UserStatus.ENABLED}.
     * Additional status checks (locked, expired) can be implemented via:
     * <ul>
     *     <li>{@link #isAccountNonLocked()}: Returns {@code status != UserStatus.LOCKED}</li>
     *     <li>{@link #isAccountNonExpired()}: Returns {@code status != UserStatus.EXPIRED}</li>
     * </ul>
     * <p>
     * <strong>Business Rules:</strong>
     * <ul>
     *     <li>Default to {@code ENABLED} for new registrations (unless admin approval required)</li>
     *     <li>Automatically set {@code LOCKED} after N consecutive failed login attempts</li>
     *     <li>Allow admins to manually toggle status with audit logging</li>
     * </ul>
     *
     * @see UserStatus
     * @see #isEnabled()
     * @see #isAccountNonLocked()
     * @see #isAccountNonExpired()
     */
    private UserStatus status;

    /**
     * The collection of granted authorities (roles/permissions) for this user.
     * <p>
     * <strong>Transient Field:</strong>
     * <p>
     * Annotated with {@code @TableField(exist = false)} because this data is not
     * stored in the {@code sys_user} table. Instead, it is dynamically populated
     * by {@code UserService.loadUserByUsername()} via JOIN queries to role/permission
     * mapping tables ({@code sys_user_role}, {@code sys_role_button}, etc.).
     * <p>
     * <strong>Population Strategy:</strong>
     * <pre>
     * {@code
     * // In UserDetailsService implementation
     * public UserDetails loadUserByUsername(String username) {
     *     SysUser user = userMapper.selectByUsername(username);
     *
     *     // Load authorities from mapping tables
     *     List<GrantedAuthority> authorities = roleMapper.selectAuthoritiesByUserId(user.getId());
     *     user.setAuthorities(authorities);
     *
     *     return user;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Caching Recommendation:</strong>
     * <p>
     * Authority loading involves multiple JOINs. Cache the result in Redis/Caffeine
     * keyed by {@code userId} with TTL matching session duration to avoid repetitive
     * database queries on every request.
     * <p>
     * <strong>Spring Security Contract:</strong>
     * <p>
     * This field is returned by {@link #getAuthorities()} and evaluated by security
     * expressions like {@code @PreAuthorize("hasAuthority('user:manage')")}.
     *
     * @see #getAuthorities()
     * @see TableField#exist()
     * @see GrantedAuthority
     */
    @TableField(exist = false)
    private Collection<? extends GrantedAuthority> authorities;

}
