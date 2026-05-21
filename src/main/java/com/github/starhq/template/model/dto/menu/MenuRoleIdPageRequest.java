package com.github.starhq.template.model.dto.menu;

import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Pagination request parameters for querying menu entries with role assignment status.
 * <p>
 * This class extends {@link PageRequest} to inherit standard pagination fields
 * ({@code page}, {@code size}, {@code sort}) and adds a role identifier for fetching
 * menus with their checked status for a specific role in permission configuration UI.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Permission Configuration</strong>: Display all menus with checked status for role assignment</li>
 *     <li><strong>Permission Impact Analysis</strong>: Review which menus are accessible to a specific role</li>
 *     <li><strong>Admin Console</strong>: Menu tree rendering with role-based selection in admin panel</li>
 * </ul>
 * <p>
 * <strong>Query Behavior:</strong>
 * <p>
 * When used with {@link com.github.starhq.template.mapper.SysMenuMapper#selectMenusByRoleId},
 * the filters are applied as follows:
 * <ul>
 *     <li>{@code roleId}: Used to determine which menus are assigned to the role</li>
 *     <li>Results include all menus with a {@code checked} flag indicating role assignment</li>
 *     <li>Only active menus ({@code status = 1}) are included to avoid assigning disabled items</li>
 * </ul>
 * <p>
 * <strong>Serialization:</strong>
 * <p>
 * This class implements {@link java.io.Serializable} with a fixed {@code serialVersionUID}
 * to ensure compatibility when caching request objects or transmitting across service boundaries.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see PageRequest
 * @see com.github.starhq.template.service.RoleService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuRoleIdPageRequest extends PageRequest {

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
    private static final long serialVersionUID = 5293766598391302696L;

    /**
     * The unique identifier of the role for which to fetch menu assignments.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Optional: May be {@code null} for unfiltered queries (returns all menus with {@code checked = false})</li>
     *     <li>Business Constraint: If provided, must reference an existing {@code SysRole} record</li>
     * </ul>
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li>Used in {@code LEFT JOIN} to determine {@code checked} status for role assignment</li>
     *     <li>If {@code null}: Returns all menus with {@code checked = false}</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Fetch all menus with checked status for "Admin" role (roleId=1)
     * MenuRoleIdPageRequest request = new MenuRoleIdPageRequest();
     * request.setRoleId(1L);
     * request.setPage(1);
     * request.setSize(100);
     *
     * IPage<MenuCheckVO> result = menuService.getRoleMenus(request);
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysRole
     * @see com.github.starhq.template.model.vo.menu.tree.MenuCheckVO
     */
    private Long roleId;

}
