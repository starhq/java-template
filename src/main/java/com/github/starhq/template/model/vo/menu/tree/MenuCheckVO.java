package com.github.starhq.template.model.vo.menu.tree;

import com.github.starhq.template.model.vo.tree.BaseIdTreeVO;
import com.github.starhq.template.model.vo.tree.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * View object for menu permission configuration in role management UI with tree structure support.
 * <p>
 * This class extends {@link BaseIdTreeVO} to inherit tree hierarchy capabilities
 * ({@code id}, {@code parentId}, {@code children}) and implements {@link Tree}
 * for recursive menu traversal. Designed for rendering permission checkboxes in
 * admin console role configuration pages where administrators select which menus
 * a role can access.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Permission UI</strong>: Render tree of menus with checked state for role assignment</li>
 *     <li><strong>Batch Operations</strong>: Support select-all/deselect-all functionality with parent-child linkage</li>
 *     <li><strong>Audit Trail</strong>: Track which menus were granted/revoked during role updates</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React tree checkbox components</li>
 * </ul>
 * <p>
 * <strong>Checked State Semantics:</strong>
 * <p>
 * The {@code checked} field indicates whether the menu is currently assigned to
 * the target role. It is computed server-side via SQL {@code LEFT JOIN} + {@code CASE WHEN}:
 * <pre>
 * {@code
 * SELECT
 *     m.id, m.name, m.parent_id,
 *     CASE WHEN rm.role_id IS NOT NULL THEN true ELSE false END as checked
 * FROM sys_menu m
 * LEFT JOIN sys_role_menu rm
 *     ON m.id = rm.menu_id AND rm.role_id = :roleId
 * WHERE m.status = 1
 * ORDER BY m.parent_id, m.sort_order;
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysMenu}) vs. UI presentation ({@code MenuCheckVO})</li>
 *     <li><strong>Immutable for Rendering</strong>: This VO is read-only; updates are handled via separate DTOs</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 *     <li><strong>Tree-Recursive</strong>: Supports nested children for hierarchical permission display</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-04
 * @see BaseIdTreeVO
 * @see Tree
 * @see com.github.starhq.template.entity.SysMenu
 * @see com.github.starhq.template.mapper.SysMenuMapper#selectMenusByRoleId(Serializable)
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuCheckVO extends BaseIdTreeVO<MenuCheckVO> implements Tree<MenuCheckVO> {

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
    private static final long serialVersionUID = -6263663296433871145L;

    /**
     * The human-readable display name of the menu item for UI presentation.
     * <p>
     * This field is shown in permission configuration dialogs to help
     * administrators identify the purpose of each menu node. Examples:
     * <ul>
     *     <li>{@code "Dashboard"} — Main overview page</li>
     *     <li>{@code "User Management"} — Parent menu for user-related operations</li>
     *     <li>{@code "Create User"} — Leaf menu for user creation form</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent tree layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "menu.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Tree checkbox component
     * <a-tree
     *   :tree-data="menuTree"
     *   :checked-keys="checkedKeys"
     *   :checkable="true"
     *   @check="onCheck"
     *   check-strictly
     * >
     *   <template #title="{ title }">
     *     {{ $t(title) }} <!-- i18n resolution -->
     *   </template>
     * </a-tree>
     *
     * // React: Tree with checkboxes
     * <Tree
     *   treeData={menuTree}
     *   checkedKeys={checkedKeys}
     *   checkable
     *   onCheck={onCheck}
     *   checkStrictly
     * />
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu#getName()
     */
    private String name;

    /**
     * Flag indicating whether this menu is assigned to the target role.
     * <p>
     * This field is computed server-side during query execution and reflects
     * the current permission state. It is used to pre-check checkboxes in the
     * role configuration UI.
     * <p>
     * <strong>Computation Logic:</strong>
     * <pre>
     * {@code
     * // SQL: LEFT JOIN + CASE WHEN
     * CASE WHEN sys_role_menu.role_id IS NOT NULL THEN true ELSE false END
     *
     * // Java: After query, map to Boolean
     * menuCheckVO.setChecked(rm.getRoleId() != null);
     * }
     * </pre>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Initial Render</strong>: Pre-check boxes for already-assigned menus</li>
     *     <li><strong>Batch Selection</strong>: Implement "Select All" by filtering {@code checked = false} items</li>
     *     <li><strong>Parent-Child Linkage</strong>: Support cascaded check/uncheck (optional via frontend logic)</li>
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
     * // Vue 3: Handle checkbox changes with parent-child linkage
     * const onCheck = (checkedKeys, { checkedNodes, halfCheckedKeys }) => {
     *   // Option 1: Strict mode (no parent-child linkage)
     *   selectedMenuIds.value = checkedKeys;
     *
     *   // Option 2: Cascaded mode (include parent when all children checked)
     *   // selectedMenuIds.value = [...checkedKeys, ...halfCheckedKeys];
     * };
     *
     * // Submit: Send only changed menu IDs to backend
     * const payload = {
     *   roleId: 1001,
     *   addedMenuIds: [2001, 2002],   // checked: false → true
     *   removedMenuIds: [2003]         // checked: true → false
     * };
     * await api.updateRoleMenus(payload);
     * }
     * </pre>
     *
     * @see com.github.starhq.template.mapper.SysMenuMapper#selectMenusByRoleId(Serializable)
     */
    private Boolean checked;

}