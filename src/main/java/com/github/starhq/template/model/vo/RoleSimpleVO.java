package com.github.starhq.template.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Lightweight view object for role metadata in dropdowns, selectors, and internal service communication.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the role's unique identifier
 * and provides minimal fields required for role reference and display. Designed
 * for scenarios where full role details are unnecessary, such as:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, multi-selects, or permission lists with role options</li>
 *     <li><strong>Permission Hints</strong>: Providing safe metadata for frontend route guards without exposing audit fields</li>
 *     <li><strong>Internal Service Communication</strong>: Passing role references between microservices for distributed auth</li>
 *     <li><strong>Cache Optimization</strong>: Storing role metadata in Redis/Caffeine with minimal memory footprint</li>
 * </ul>
 * <p>
 * <strong>Update Scenario Usage:</strong>
 * <p>
 * This VO is specifically designed for role update scenarios where only basic
 * metadata is needed to populate forms or validate changes, without exposing
 * sensitive audit fields or complex permission structures.
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for role identification and display (id, code, name, description, isDefault)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-25
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysRole
 * @see com.github.starhq.template.service.RoleService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleSimpleVO extends BaseIdVO {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -4170990859019139982L;

    /**
     * The unique technical identifier/code for this role.
     * <p>
     * This field serves as the primary key for business logic to reference roles
     * in permission checks and API authorization. Typically follows {@code snake_case}
     * or {@code camelCase} conventions for readability and IDE auto-completion:
     * <ul>
     *     <li>{@code "admin"} — System administrator role</li>
     *     <li>{@code "user_manager"} — Role for managing user accounts</li>
     *     <li>{@code "report_viewer"} — Read-only access to reports</li>
     * </ul>
     * <p>
     * <strong>Uniqueness & Immutability:</strong>
     * <ul>
     *     <li><strong>Global Uniqueness</strong>: Must be unique across all roles; enforce via database {@code UNIQUE INDEX} and application-level pre-check</li>
     *     <li><strong>Immutability After Creation</strong>: Once a role is referenced in business logic or assigned to users, changing its {@code code} may break existing references. Consider making {@code code} immutable after creation</li>
     * </ul>
     * <p>
     * <strong>Format Recommendations:</strong>
     * <ul>
     *     <li>Use lowercase with underscores ({@code snake_case}) for consistency: {@code "user_manager"}</li>
     *     <li>Avoid special characters except underscores; restrict to {@code [a-z0-9_]} via regex validation if needed</li>
     *     <li>Prefix with module name for large systems: {@code "sys_admin"}, {@code "biz_order_manager"}</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Backend: Check role by code in permission logic
     * if (user.getRoles().stream().anyMatch(r -> "admin".equals(r.getCode()))) {
     *     // Grant admin privileges
     * }
     *
     * // Frontend: Populate form with role code
     * <a-form-item label="Role Code">
     *   <a-input v-model="form.code" disabled /> <!-- Code is immutable -->
     * </a-form-item>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysRole#getCode()
     */
    private String code;

    /**
     * The human-readable display name for administrative interfaces.
     * <p>
     * This field is used in admin consoles, dropdown selectors, and documentation
     * to help system administrators identify the purpose of the role.
     * Examples: {@code "Administrator"}, {@code "User Manager"}, {@code "Report Viewer"}.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 30 characters) for consistent UI layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "role.admin"})
     *         and resolving translations at the frontend or gateway layer</li>
     *     <li>Avoid technical jargon; target non-technical system administrators</li>
     *     <li>Use title case for readability: {@code "User Manager"} not {@code "user manager"}</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Populate a select dropdown
     * <a-select v-model="form.roleId" :options="roleOptions">
     *   <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
     *     {{ role.name }} <small class="text-gray-500">({{ role.code }})</small>
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render role list with code hint
     * {roles.map(role => (
     *   <Select.Option key={role.id} value={role.id}>
     *     {role.name} <span className="text-muted">({role.code})</span>
     *   </Select.Option>
     * ))}
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysRole#getName()
     */
    private String name;

    /**
     * Optional explanatory text describing the purpose and usage rules of this role.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what permissions the role grants</li>
     *     <li>Documenting deprecated roles or migration notes</li>
     *     <li>Clarifying complex business rules (e.g., {@code "Requires MFA for sensitive operations"})</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Avoid exposing internal implementation details or sensitive business logic</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show as tooltip on hover: {@code <a-tooltip :title="item.description">}</li>
     *     <li>Truncate long descriptions with ellipsis for dropdown layout</li>
     *     <li>Support markdown formatting if rich text descriptions are enabled</li>
     * </ul>
     * <p>
     * <strong>Storage Recommendation:</strong>
     * <ul>
     *     <li>Database column: {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} type if detailed documentation or markdown formatting is required</li>
     *     <li>Add full-text index if descriptions are frequently searched in admin UI</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysRole#getDescription()
     */
    private String description;

    /**
     * Flag indicating whether this role is the system default role for new users.
     * <p>
     * When {@code true}, this role is automatically assigned to newly registered users
     * unless explicitly overridden. Typically used for baseline permissions that all
     * users should have (e.g., login access, basic profile management).
     * <p>
     * <strong>Business Rules:</strong>
     * <ul>
     *     <li><strong>Single Default</strong>: Only one role should have {@code isDefault = true} at any time</li>
     *     <li><strong>Protected Deletion</strong>: Default roles should not be deletable without explicit override</li>
     *     <li><strong>Audit Trail</strong>: Changes to default role status should be logged for compliance</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show visual indicator (badge/icon) for default roles in management UI</li>
     *     <li>Disable delete button for default roles with explanatory tooltip</li>
     *     <li>Require confirmation dialog when changing default role assignment</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Backend: Assign default role to new user
     * if (user.getRoles().isEmpty()) {
     *     SysRole defaultRole = roleMapper.selectOne(
     *         new LambdaQueryWrapper<SysRole>().eq(SysRole::getIsDefault, true)
     *     );
     *     if (defaultRole != null) {
     *         userRoleMapper.insert(new SysUserRole(user.getId(), defaultRole.getId()));
     *     }
     * }
     *
     * // Frontend: Show default badge in role selector
     * <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
     *   {{ role.name }}
     *   <a-tag v-if="role.isDefault" color="blue" size="small">Default</a-tag>
     * </a-select-option>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysRole#getIsDefault()
     */
    private Boolean isDefault;

}