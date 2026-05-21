package com.github.starhq.template.model.vo.role;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * View object for role permission configuration in user management UI.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the role's unique identifier
 * and adds presentation-specific fields for rendering permission checkboxes in
 * admin console user configuration pages. Designed for the "assign roles to user"
 * workflow where administrators select which roles a user can have.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Role Management</strong>: Render list of roles with checked state for user assignment</li>
 *     <li><strong>Batch Operations</strong>: Support select-all/deselect-all functionality in role management</li>
 *     <li><strong>Audit Trail</strong>: Track which roles were granted/revoked during user updates</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React checkbox components</li>
 * </ul>
 * <p>
 * <strong>Checked State Semantics:</strong>
 * <p>
 * The {@code checked} field indicates whether the role is currently assigned to
 * the target user. It is computed server-side via SQL {@code LEFT JOIN} + {@code CASE WHEN}:
 * <pre>
 * {@code
 * SELECT
 *     r.id, r.name,
 *     CASE WHEN ur.user_id IS NOT NULL THEN true ELSE false END as checked
 * FROM sys_role r
 * LEFT JOIN sys_user_role ur
 *     ON r.id = ur.role_id AND ur.user_id = :userId
 * WHERE r.status = 1
 * ORDER BY r.sort_order, r.name;
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysRole}) vs. UI presentation ({@code RoleCheckVO})</li>
 *     <li><strong>Immutable for Rendering</strong>: This VO is read-only; updates are handled via separate DTOs</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-25
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysRole
 * @see com.github.starhq.template.mapper.SysRoleMapper#selectRolesByUserId(Serializable)
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleCheckVO extends BaseIdVO {

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
    private static final long serialVersionUID = 4185116018187356828L;

    /**
     * The human-readable display name of the role for UI presentation.
     * <p>
     * This field is shown in permission configuration dialogs to help
     * administrators identify the purpose of each role. Examples:
     * <ul>
     *     <li>{@code "Administrator"} — Full system access role</li>
     *     <li>{@code "User Manager"} — Role for managing user accounts</li>
     *     <li>{@code "Report Viewer"} — Read-only access to reports</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 30 characters) for consistent checkbox layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "role.admin"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     *     <li>Use title case for readability: {@code "User Manager"} not {@code "user manager"}</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 checkbox list
     * <a-checkbox-group v-model="checkedRoles">
     *   <a-checkbox
     *     v-for="role in roles"
     *     :key="role.id"
     *     :value="role.id"
     *   >
     *     {{ role.name }}
     *   </a-checkbox>
     * </a-checkbox-group>
     *
     * // React: Checkbox list
     * {roles.map(role => (
     *   <Checkbox key={role.id} value={role.id}>
     *     {role.name}
     *   </Checkbox>
     * ))}
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysRole#getName()
     */
    private String name;

    /**
     * Flag indicating whether this role is assigned to the target user.
     * <p>
     * This field is computed server-side during query execution and reflects
     * the current permission state. It is used to pre-check checkboxes in the
     * user configuration UI.
     * <p>
     * <strong>Computation Logic:</strong>
     * <pre>
     * {@code
     * // SQL: LEFT JOIN + CASE WHEN
     * CASE WHEN sys_user_role.user_id IS NOT NULL THEN true ELSE false END
     *
     * // Java: After query, map to Boolean
     * roleCheckVO.setChecked(ur.getUserId() != null);
     * }
     * </pre>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Initial Render</strong>: Pre-check boxes for already-assigned roles</li>
     *     <li><strong>Batch Selection</strong>: Implement "Select All" by filtering {@code checked = false} items</li>
     *     <li><strong>Change Detection</strong>: Compare initial vs. final {@code checked} states to compute added/removed roles</li>
     * </ul>
     * <p>
     * <strong>Null Handling:</strong>
     * <ul>
     *     <li>Should never be {@code null} in well-formed responses; default to {@code false} if computation fails</li>
     *     <li>Frontend should treat {@code null} as {@code false} for defensive rendering</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Handle checkbox changes
     * const onCheck = (checkedValues) => {
     *   // Store selected role IDs
     *   selectedRoleIds.value = checkedValues;
     * };
     *
     * // Submit: Send only changed role IDs to backend
     * const payload = {
     *   userId: 2001,
     *   addedRoleIds: [1001, 1002],   // checked: false → true
     *   removedRoleIds: [1003]         // checked: true → false
     * };
     * await api.updateUserRoles(payload);
     * }
     * </pre>
     *
     * @see com.github.starhq.template.mapper.SysRoleMapper#selectRolesByUserId(Serializable)
     */
    private Boolean checked;

}