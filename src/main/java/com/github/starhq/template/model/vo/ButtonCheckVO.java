package com.github.starhq.template.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * View object for button permission configuration in role management UI.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the button's unique identifier
 * and adds presentation-specific fields for rendering permission checkboxes in
 * admin console role configuration pages. Designed for the "assign buttons to role"
 * workflow where administrators select which UI buttons a role can access.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Permission UI</strong>: Render tree/table of buttons with checked state for role assignment</li>
 *     <li><strong>Batch Operations</strong>: Support select-all/deselect-all functionality in permission management</li>
 *     <li><strong>Audit Trail</strong>: Track which buttons were granted/revoked during role updates</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React checkbox components</li>
 * </ul>
 * <p>
 * <strong>Checked State Semantics:</strong>
 * <p>
 * The {@code checked} field indicates whether the button is currently assigned to
 * the target role. It is computed server-side via SQL {@code LEFT JOIN} + {@code CASE WHEN}:
 * <pre>
 * {@code
 * SELECT
 *     b.id, b.name,
 *     CASE WHEN rb.role_id IS NOT NULL THEN true ELSE false END as checked
 * FROM sys_button b
 * LEFT JOIN sys_role_button rb
 *     ON b.id = rb.button_id AND rb.role_id = :roleId
 * WHERE b.status = 1
 * ORDER BY b.menu_id, b.sort_order;
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysButton}) vs. UI presentation ({@code ButtonCheckVO})</li>
 *     <li><strong>Immutable for Rendering</strong>: This VO is read-only; updates are handled via separate DTOs</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-03
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysButton
 * @see com.github.starhq.template.mapper.SysButtonMapper#selectButtonsByRoleId(Serializable)
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonCheckVO extends BaseIdVO {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., via RPC or messaging).
     * Update this value only if the class structure changes in a
     * backward-incompatible way (e.g., removing fields, changing types).
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 8927044383616401568L;

    /**
     * The human-readable display name of the button for UI presentation.
     * <p>
     * This field is shown in permission configuration interfaces to help
     * administrators identify the purpose of each button. Examples:
     * <ul>
     *     <li>{@code "Create User"} — Button for user creation</li>
     *     <li>{@code "Export Data"} — Button for data export functionality</li>
     *     <li>{@code "Assign Role"} — Button for role assignment operation</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent checkbox layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "button.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 checkbox group
     * <a-checkbox-group v-model="checkedButtons">
     *   <a-checkbox
     *     v-for="btn in buttons"
     *     :key="btn.id"
     *     :value="btn.id"
     *   >
     *     {{ btn.name }}
     *   </a-checkbox>
     * </a-checkbox-group>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysButton#getName()
     */
    private String name;

    /**
     * Flag indicating whether this button is assigned to the target role.
     * <p>
     * This field is computed server-side during query execution and reflects
     * the current permission state. It is used to pre-check checkboxes in the
     * role configuration UI.
     * <p>
     * <strong>Computation Logic:</strong>
     * <pre>
     * {@code
     * // SQL: LEFT JOIN + CASE WHEN
     * CASE WHEN sys_role_button.role_id IS NOT NULL THEN true ELSE false END
     *
     * // Java: After query, map to Boolean
     * buttonCheckVO.setChecked(rb.getRoleId() != null);
     * }
     * </pre>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Initial Render</strong>: Pre-check boxes for already-assigned buttons</li>
     *     <li><strong>Batch Selection</strong>: Implement "Select All" by filtering {@code checked = false} items</li>
     *     <li><strong>Change Detection</strong>: Compare initial vs. final {@code checked} states to compute added/removed permissions</li>
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
     * // React: Controlled checkbox component
     * <Checkbox
     *   checked={button.checked}
     *   onChange={(e) => handleToggle(button.id, e.target.checked)}
     * >
     *   {button.name}
     * </Checkbox>
     *
     * // Submit: Send only changed button IDs to backend
     * const payload = {
     *   roleId: 1001,
     *   addedButtonIds: [2001, 2002],   // checked: false → true
     *   removedButtonIds: [2003]         // checked: true → false
     * };
     * }
     * </pre>
     *
     * @see com.github.starhq.template.mapper.SysButtonMapper#selectButtonsByRoleId(Serializable)
     */
    private Boolean checked;

}