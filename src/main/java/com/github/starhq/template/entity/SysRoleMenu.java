package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Junction entity representing the many-to-many relationship between roles and menu permissions.
 * <p>
 * This class maps to the {@code sys_role_menu} table and serves as a bridge table
 * linking {@link SysRole} and {@link SysMenu} entities. Each record grants a specific
 * menu-level permission to a role, enabling dynamic sidebar navigation rendering and
 * route-level access control based on user role assignments.
 * <p>
 * <strong>Relationship Model:</strong>
 * <ul>
 *     <li>{@code SysRole 1..* SysRoleMenu *..1 SysMenu}</li>
 *     <li>Composite primary key: {@code (role_id, menu_id)} ensures uniqueness and prevents duplicate grants</li>
 *     <li>Deletion is cascade-safe: removing a role or menu automatically cleans up associated mappings</li>
 * </ul>
 * <p>
 * <strong>Batch Operations & Caching:</strong>
 * <p>
 * Role-menu assignments are typically updated in bulk (e.g., admin configures all menus
 * for a role at once). Always use batch insert/update methods to minimize database round-trips.
 * Cache the role → menu mapping in Redis/Caffeine keyed by {@code roleId} to achieve O(1)
 * permission checks during sidebar rendering or route guard evaluation.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see SysRole
 * @see SysMenu
 * @see TableName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_menu")
public class SysRoleMenu {

    /**
     * The unique identifier of the role being granted menu access.
     * <p>
     * References {@link SysRole#getId()} to establish the "who" side of the permission grant.
     * Combined with {@link #menuId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid role</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_role_menu (role_id, menu_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all menu IDs granted to a specific role
     * SELECT menu_id FROM sys_role_menu WHERE role_id = 1001;
     *
     * -- Check if a role has access to a specific menu
     * SELECT COUNT(*) FROM sys_role_menu WHERE role_id = 1001 AND menu_id = 2002;
     * }
     * </pre>
     *
     * @see SysRole
     */
    private Long roleId;

    /**
     * The unique identifier of the menu being granted to the role.
     * <p>
     * References {@link SysMenu#getId()} to establish the "what" side of the permission grant.
     * Combined with {@link #roleId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid menu</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_role_menu (role_id, menu_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all roles that have access to a specific menu
     * SELECT role_id FROM sys_role_menu WHERE menu_id = 2002;
     *
     * -- Build role-based navigation tree
     * SELECT m.* FROM sys_menu m
     * JOIN sys_role_menu rm ON m.id = rm.menu_id
     * WHERE rm.role_id = 1001 AND m.status = 1
     * ORDER BY m.parent_id, m.sort_order;
     * }
     * </pre>
     *
     * @see SysMenu
     */
    private Long menuId;

}