package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;

/**
 * Entity class representing a role in the Role-Based Access Control (RBAC) system.
 * <p>
 * This class maps to the {@code sys_role} table, extends {@link BaseEntity} for audit
 * trail, and implements {@link GrantedAuthority} for seamless integration with
 * Spring Security's authorization framework. Roles serve as logical permission
 * containers that can be assigned to users, enabling flexible and scalable access
 * control without modifying individual user records.
 * <p>
 * <strong>Spring Security Integration:</strong>
 * <p>
 * By implementing {@link GrantedAuthority}, this entity can be directly used in
 * {@link org.springframework.security.core.Authentication} objects. The {@code code}
 * field is returned by {@link #getAuthority()} and evaluated by security expressions
 * like {@code @PreAuthorize("hasRole('ADMIN')")} or {@code hasAuthority('user:manage')}.
 * <p>
 * <strong>Caching & Performance:</strong>
 * <p>
 * Role assignments are evaluated on every authenticated request. Always cache the
 * user-role-permission matrix in Redis/Caffeine keyed by {@code userId} to achieve
 * O(1) authorization checks without repetitive database queries.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see GrantedAuthority
 * @see com.github.starhq.template.service.RoleService
 * @see TableName
 */
@Data
@Alias("role")
@TableName("sys_role")
@EqualsAndHashCode(callSuper = false)
public class SysRole extends BaseEntity implements GrantedAuthority {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when entities are stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value if the class
     * structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -4242770371889322444L;

    /**
     * The unique technical identifier/code for this role, used in security expressions.
     * <p>
     * Typically follows {@code module:role} or {@code scope:level} conventions:
     * <ul>
     *     <li>{@code "ROLE_ADMIN"} — System administrator with full access</li>
     *     <li>{@code "user:manager"} — User management permissions</li>
     *     <li>{@code "report:viewer"} — Read-only report access</li>
     * </ul>
     * <p>
     * <strong>Spring Security Convention:</strong>
     * <p>
     * When using {@code hasRole('XXX')} expressions, Spring Security automatically
     * prefixes the authority with {@code "ROLE_"}. To match:
     * <ul>
     *     <li>Store codes with {@code ROLE_} prefix: {@code "ROLE_ADMIN"}</li>
     *     <li>Or use {@code hasAuthority('user:manager')} for prefix-free matching</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link String} — alphanumeric with underscores/colons</li>
     *     <li>Uniqueness: Must be globally unique across all roles</li>
     *     <li>Index Recommendation: {@code CREATE UNIQUE INDEX uk_role_code ON sys_role(code)}</li>
     *     <li>Nullability: {@code NOT NULL} — required for security evaluation</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^(ROLE_)?[a-z][a-z0-9_:-]*$")} for standardized format</li>
     *     <li>Length: {@code @Size(min = 3, max = 64)}</li>
     * </ul>
     *
     * @see #getAuthority()
     * @see org.springframework.security.access.expression.SecurityExpressionRoot#hasRole
     */
    private String code;

    /**
     * The human-readable display name for administrative interfaces.
     * <p>
     * Used in role assignment dialogs, user management consoles, and audit reports.
     * Examples: {@code "System Administrator"}, {@code "Content Editor"},
     * {@code "Read-Only Auditor"}.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 50 characters) for consistent UI layout</li>
     *     <li>For multi-language systems, store i18n keys (e.g., {@code "role.admin.title"})
     *         and resolve translations at the frontend or gateway layer</li>
     *     <li>Avoid technical jargon; target non-technical system administrators</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank}</li>
     *     <li>Uniqueness: Recommended globally unique to avoid admin UI confusion</li>
     *     <li>Pattern: Safe characters only to prevent XSS or rendering issues</li>
     * </ul>
     */
    private String name;

    /**
     * Optional explanatory text detailing the role's purpose, scope, and usage guidelines.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what permissions this role grants</li>
     *     <li>Documenting compliance requirements (e.g., {@code "Required for SOX audit access"})</li>
     *     <li>Recording deprecation notices or migration paths for legacy roles</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators and auditors</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     *     <li>Avoid exposing internal implementation details or sensitive permission logic</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong>
     * <ul>
     *     <li>Use {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} if detailed documentation or markdown formatting is required</li>
     * </ul>
     */
    private String description;

    /**
     * Flag indicating whether this role should be automatically assigned to new users.
     * <p>
     * When {@code true}, the registration or user creation workflow should automatically
     * grant this role to newly created accounts. Typically used for baseline permissions
     * like {@code "ROLE_USER"} or {@code "tenant:member"}.
     * <p>
     * <strong>Business Rules:</strong>
     * <ul>
     *     <li>Only one role should be marked as default per tenant/organization to avoid
     *         ambiguous permission grants</li>
     *     <li>Default roles should grant minimal, safe permissions following the
     *         principle of least privilege</li>
     *     <li>Changing this flag should trigger re-evaluation of existing user assignments</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Boolean} — use wrapper class to distinguish unset ({@code null})
     *         from explicit {@code false}</li>
     *     <li>Default Value: {@code false} — roles are opt-in unless explicitly marked</li>
     *     <li>Index Recommendation: {@code INDEX idx_is_default (is_default)} for efficient
     *         default role lookup during user registration</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Optional: Allow {@code null} for roles not intended as defaults</li>
     *     <li>Business Constraint: At most one {@code is_default = true} per scope</li>
     * </ul>
     */
    private Boolean isDefault;

    /**
     * Returns the role code as the granted authority for Spring Security evaluation.
     * <p>
     * This method enables direct use of {@link SysRole} instances in Spring Security's
     * {@link org.springframework.security.core.Authentication#getAuthorities()} collection.
     * The returned value is evaluated by security expressions:
     * <pre>
     * {@code
     * // Method-level security
     * @PreAuthorize("hasAuthority('user:manager')")
     * public void manageUsers() { ... }
     *
     * // Template-level security (Thymeleaf)
     * <div sec:authorize="hasAuthority('report:viewer')">...</div>
     * }
     * </pre>
     * <p>
     * <strong>Return Contract:</strong>
     * <ul>
     *     <li>Returns {@code code} if non-null — the technical role identifier</li>
     *     <li>Returns {@code null} if {@code code} is unset — treated as "no authority" by Spring Security</li>
     *     <li>Never throws exceptions — safe for use in security filter chains</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Ensure {@code code} is validated and sanitized before persistence to prevent
     *         injection attacks in security expressions</li>
     *     <li>Never expose raw role codes in public APIs without authorization checks</li>
     *     <li>Use constant definitions for critical roles to avoid typos:
     *         {@code public static final String ADMIN = "ROLE_ADMIN";}</li>
     * </ul>
     *
     * @return the role code as a {@link String} authority, or {@code null} if unset
     * @see GrantedAuthority#getAuthority()
     * @see org.springframework.security.access.expression.SecurityExpressionRoot
     */
    @Override
    public @Nullable String getAuthority() {
        return this.code;
    }

}