package com.github.starhq.template.model.vo.resource;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * View object for API resource permission configuration in role management UI.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the resource's unique identifier
 * and adds presentation-specific fields for rendering permission checkboxes in
 * admin console role configuration pages. Designed for the "assign API resources to role"
 * workflow where administrators select which API endpoints a role can access.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Permission UI</strong>: Render list/table of API resources with checked state for role assignment</li>
 *     <li><strong>Batch Operations</strong>: Support select-all/deselect-all functionality in permission management</li>
 *     <li><strong>Audit Trail</strong>: Track which API resources were granted/revoked during role updates</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React checkbox components</li>
 * </ul>
 * <p>
 * <strong>Checked State Semantics:</strong>
 * <p>
 * The {@code checked} field indicates whether the API resource is currently assigned to
 * the target role. It is computed server-side via SQL {@code LEFT JOIN} + {@code CASE WHEN}:
 * <pre>
 * {@code
 * SELECT
 *     r.id, r.name,
 *     CASE WHEN rr.role_id IS NOT NULL THEN true ELSE false END as checked
 * FROM sys_resource r
 * LEFT JOIN sys_role_resource rr
 *     ON r.id = rr.resource_id AND rr.role_id = :roleId
 * WHERE r.status = 1
 * ORDER BY r.url, r.name;
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysResource}) vs. UI presentation ({@code ResourceCheckVO})</li>
 *     <li><strong>Immutable for Rendering</strong>: This VO is read-only; updates are handled via separate DTOs</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-01
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysResource
 * @see com.github.starhq.template.mapper.SysResourceMapper#selectResourcesByRoleId(Serializable)
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResourceCheckVO extends BaseIdVO {

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
    private static final long serialVersionUID = -8709045478621472232L;

    /**
     * The human-readable display name of the API resource for UI presentation.
     * <p>
     * This field is shown in permission configuration interfaces to help
     * administrators identify the purpose of each API resource. Examples:
     * <ul>
     *     <li>{@code "Create User API"} — Endpoint for user creation</li>
     *     <li>{@code "Export Data API"} — Endpoint for data export functionality</li>
     *     <li>{@code "Delete Order API"} — Endpoint for order deletion operation</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 50 characters) for consistent checkbox layout</li>
     *     <li>Include HTTP method prefix for clarity: {@code "[POST] Create User"}</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "resource.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 checkbox list
     * <a-checkbox-group v-model="checkedResources">
     *   <a-checkbox
     *     v-for="res in resources"
     *     :key="res.id"
     *     :value="res.id"
     *   >
     *     {{ res.name }} <small class="text-gray-500">({{ res.url }})</small>
     *   </a-checkbox>
     * </a-checkbox-group>
     *
     * // React: Checkbox list with URL hint
     * {resources.map(res => (
     *   <Checkbox key={res.id} value={res.id}>
     *     {res.name} <span className="text-muted">({res.url})</span>
     *   </Checkbox>
     * ))}
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysResource#getName()
     */
    private String name;

    /**
     * Flag indicating whether this API resource is assigned to the target role.
     * <p>
     * This field is computed server-side during query execution and reflects
     * the current permission state. It is used to pre-check checkboxes in the
     * role configuration UI.
     * <p>
     * <strong>Computation Logic:</strong>
     * <pre>
     * {@code
     * // SQL: LEFT JOIN + CASE WHEN
     * CASE WHEN sys_role_resource.role_id IS NOT NULL THEN true ELSE false END
     *
     * // Java: After query, map to Boolean
     * resourceCheckVO.setChecked(rr.getRoleId() != null);
     * }
     * </pre>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Initial Render</strong>: Pre-check boxes for already-assigned resources</li>
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
     * // Vue 3: Handle checkbox changes
     * const onCheck = (checkedValues) => {
     *   // Store selected resource IDs
     *   selectedResourceIds.value = checkedValues;
     * };
     *
     * // Submit: Send only changed resource IDs to backend
     * const payload = {
     *   roleId: 1001,
     *   addedResourceIds: [2001, 2002],   // checked: false → true
     *   removedResourceIds: [2003]         // checked: true → false
     * };
     * await api.updateRoleResources(payload);
     * }
     * </pre>
     *
     * @see com.github.starhq.template.mapper.SysResourceMapper#selectResourcesByRoleId(Serializable)
     */
    private Boolean checked;

}