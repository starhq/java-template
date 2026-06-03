package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.vo.MenuCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysMenu} entity with permission-specific queries.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for menu
 * definitions, and provides specialized methods for role-based menu assignment and
 * user permission resolution in RBAC (Role-Based Access Control) scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Configuration</strong>: Fetch menus with checked status for admin permission UI</li>
 *     <li><strong>User Authorization</strong>: Resolve effective menus for a user via role inheritance</li>
 *     <li><strong>Impact Analysis</strong>: Identify users affected by menu permission changes</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysMenu}) vs. permission VO ({@code MenuCheckVO})</li>
 *     <li><strong>Dynamic Query Support</strong>: Leverage {@link QueryWrapper} for flexible, type-safe conditions</li>
 *     <li><strong>Query Efficiency</strong>: Use database joins and indexes for fast permission resolution</li>
 *     <li><strong>Cache Friendliness</strong>: Results are suitable for Redis/Caffeine caching with TTL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class MenuPermissionService {
 *     @Autowired private SysMenuMapper menuMapper;
 *
 *     // Fetch checked menus for role configuration UI
 *     public List<MenuCheckVO> getRoleMenus(Long roleId) {
 *         return menuMapper.selectMenusByRoleId(roleId);
 *     }
 *
 *     // Resolve effective menus for user authorization
 *     public List<SysMenu> getUserMenus(Long userId) {
 *         QueryWrapper<SysMenu> wrapper = new QueryWrapper<SysMenu>()
 *             .inSql("id", "SELECT menu_id FROM sys_role_menu WHERE role_id IN " +
 *                        "(SELECT role_id FROM sys_user_role WHERE user_id = " + userId + ")")
 *             .eq("status", 1)
 *             .orderByAsc("sort_order");
 *         return menuMapper.selectAssignedMenus(wrapper);
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysMenu
 * @see MenuCheckVO
 * @see com.github.starhq.template.service.RoleService
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * Fetches menu entities matching dynamic query conditions for permission resolution.
     * <p>
     * This method leverages MyBatis-Plus's {@link QueryWrapper} to support flexible,
     * type-safe filtering for scenarios such as:
     * <ul>
     *     <li>User menu authorization: Filter menus by role inheritance chain</li>
     *     <li>Admin menu management: Filter by status, type, or parent hierarchy</li>
     *     <li>Dynamic sidebar rendering: Apply business rules for menu visibility</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code wrapper}: Query conditions wrapper; may be {@code null} for unfiltered queries (returns all active menus)</li>
     *     <li>Type: {@link QueryWrapper}{@code <SysMenu>} for compile-time field safety</li>
     * </ul>
     * <p>
     * <strong>MyBatis-Plus Integration:</strong>
     * <p>
     * The {@code @Param(Constants.WRAPPER)} annotation enables MyBatis-Plus to inject
     * {@link QueryWrapper} conditions into the SQL dynamically. When using XML mapping,
     * reference {@code ${ew.sqlSegment}} or {@code <where>${ew.sqlSegment}</where>}
     * for proper condition injection.
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Fetch menus for a specific user via role inheritance
     * QueryWrapper<SysMenu> wrapper = new QueryWrapper<SysMenu>()
     *     .inSql("id", "SELECT menu_id FROM sys_role_menu WHERE role_id IN " +
     *                "(SELECT role_id FROM sys_user_role WHERE user_id = #{userId})")
     *     .eq("status", 1)  // Only active menus
     *     .orderByAsc("sort_order");
     *
     * List<SysMenu> menus = menuMapper.selectAssignedMenus(wrapper);
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add index on {@code sys_menu(status, parent_id, sort_order)} for efficient filtering</li>
     *     <li>Avoid complex {@code inSql} subqueries in high-frequency paths; prefer pre-computed permission caches</li>
     *     <li>For large menu trees (>200 nodes), consider client-side lazy-loading or pagination</li>
     * </ul>
     *
     * @param wrapper the query conditions wrapper for dynamic filtering; may be {@code null} for unfiltered queries
     * @return a list of {@link SysMenu} entities matching the conditions; empty list if none; never {@code null}
     * @see QueryWrapper
     * @see Constants#WRAPPER
     * @see BaseMapper#selectList(Wrapper)
     */
    List<SysMenu> selectAssignedMenus(@Param(Constants.WRAPPER) QueryWrapper<SysMenu> wrapper);

    /**
     * Fetches menus with checked status for a specific role in permission configuration UI.
     * <p>
     * This method returns all menus in the system with a {@code checked} flag indicating
     * whether each menu is assigned to the specified role. Designed for rendering
     * permission checkboxes or tree nodes in admin console role management pages.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs a {@code LEFT JOIN} between {@code sys_menu} and {@code sys_role_menu}</li>
     *     <li>Returns all menus; {@code checked = true} if assigned to the role, {@code false} otherwise</li>
     *     <li>Results are typically sorted by {@code parent_id}, {@code sort_order} for intuitive tree rendering</li>
     *     <li>Only includes active menus ({@code status = 1}) to avoid assigning disabled items</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleId}: Must be a valid role ID; if {@code null} or non-existent, returns all menus with {@code checked = false}</li>
     *     <li>Type: {@link Serializable} to support {@code Long}, {@code String}, or custom ID types</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add index on {@code sys_role_menu(role_id, menu_id)} for efficient join</li>
     *     <li>Cache results with TTL (e.g., 10min) since menu definitions change infrequently</li>
     *     <li>For large menu sets (>200), consider lazy-loading child nodes in UI</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysMenuMapper.xml -->
     * <select id="selectMenusByRoleId" resultType="com.github.starhq.template.model.vo.MenuCheckVO">
     *     SELECT
     *         m.id, m.parent_id, m.name, m.url, m.icon, m.sort_order, m.status,
     *         CASE WHEN rm.role_id IS NOT NULL THEN true ELSE false END as checked
     *     FROM sys_menu m
     *     LEFT JOIN sys_role_menu rm ON m.id = rm.menu_id AND rm.role_id = #{roleId}
     *     WHERE m.status = 1  -- Only active menus
     *     ORDER BY m.parent_id, m.sort_order
     * </select>
     * }
     * </pre>
     *
     * @param roleId the unique identifier of the role to check menu assignments for; may be {@code null}
     * @return a list of {@link MenuCheckVO} with {@code checked} flag indicating role assignment; never {@code null}
     * @see MenuCheckVO
     * @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus XML Mapping Guide</a>
     */
    List<MenuCheckVO> selectMenusByRoleId(@Param("roleId") Serializable roleId);

    /**
     * Fetches user IDs that have access to a specific menu permission.
     * <p>
     * This method performs a reverse lookup to identify all users who can access a given
     * menu, useful for impact analysis when modifying or deprecating menu permissions.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Traverses: {@code sys_menu} → {@code sys_role_menu} → {@code sys_user_role} → {@code sys_user}</li>
     *     <li>Returns distinct user IDs to avoid duplicates from multiple role paths</li>
     *     <li>Only includes active users ({@code status = ENABLED}) and active roles</li>
     * </ul>
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *     <li><strong>Change Impact Analysis</strong>: Notify affected users before removing a menu permission</li>
     *     <li><strong>Audit Trail</strong>: Log which users had access to sensitive menu items</li>
     *     <li><strong>Compliance Reporting</strong>: Generate access reports for regulatory audits</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code menuId}: Must be a valid menu ID; if {@code null} or non-existent, returns empty list</li>
     *     <li>Type: {@link Serializable} to support flexible ID strategies</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Ensure caller has administrative privileges before exposing user lists</li>
     *     <li>Consider masking or aggregating results for privacy compliance (GDPR, PIPL)</li>
     *     <li>Log access to this method for audit trails of permission impact queries</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large user bases, consider pagination or streaming results to avoid memory pressure</li>
     *     <li>Add indexes on join keys: {@code sys_role_menu(menu_id)}, {@code sys_user_role(role_id)}</li>
     *     <li>Cache results with short TTL (e.g., 5min) if used for real-time impact analysis</li>
     * </ul>
     *
     * @param menuId the unique identifier of the menu to check user access for; may be {@code null}
     * @return a list of user IDs with access to the menu; empty list if none; never {@code null}
     * @see com.github.starhq.template.entity.SysUser
     * @see com.github.starhq.template.common.enums.UserStatus
     */
    List<Long> selectUserIdsByMenuId(@Param("menuId") Serializable menuId);
}