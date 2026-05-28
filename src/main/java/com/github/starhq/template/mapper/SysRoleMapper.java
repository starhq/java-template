package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysRole} entity with user-centric role queries.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for role
 * definitions, and provides specialized methods for resolving role assignments in
 * user management and permission configuration scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Role Management</strong>: Fetch roles with checked status for user assignment UI</li>
 *     <li><strong>Permission Resolution</strong>: Resolve effective roles for a user during authentication</li>
 *     <li><strong>Audit & Reporting</strong>: Generate role assignment reports for compliance reviews</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysRole}) vs. UI VO ({@code RoleCheckVO})</li>
 *     <li><strong>Type Safety</strong>: Use {@link Serializable} for ID parameters to support multiple ID strategies</li>
 *     <li><strong>Query Efficiency</strong>: Leverage indexed joins for fast role resolution</li>
 *     <li><strong>Cache Friendliness</strong>: Results are suitable for Redis/Caffeine caching with TTL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class UserRoleService {
 *     @Autowired private SysRoleMapper roleMapper;
 *
 *     // Fetch roles with checked status for user assignment UI
 *     public List<RoleCheckVO> getUserRoles(Long userId) {
 *         return roleMapper.selectRolesByUserId(userId);
 *     }
 *
 *     // Resolve effective role codes for Spring Security authorization
 *     public Set<String> getUserRoleCodes(Long userId) {
 *         return roleMapper.selectRolesByUserId(userId).stream()
 *             .filter(RoleCheckVO::getChecked)
 *             .map(RoleCheckVO::getCode)
 *             .collect(Collectors.toSet());
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysRole
 * @see RoleCheckVO
 * @see com.github.starhq.template.service.UserService
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * Fetches roles with checked status for a specific user in role assignment UI.
     * <p>
     * This method returns all roles in the system with a {@code checked} flag indicating
     * whether each role is assigned to the specified user. Designed for rendering
     * role checkboxes or multi-select components in admin console user management pages.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs a {@code LEFT JOIN} between {@code sys_role} and {@code sys_user_role}</li>
     *     <li>Returns all roles; {@code checked = true} if assigned to the user, {@code false} otherwise</li>
     *     <li>Results are typically sorted by {@code sort_order}, {@code name} for intuitive UI display</li>
     *     <li>Only includes active roles ({@code status = 1}) to avoid assigning disabled roles</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must be a valid user ID; if {@code null} or non-existent, returns all roles with {@code checked = false}</li>
     *     <li>Type: {@link Serializable} to support {@code Long}, {@code String}, or custom ID types</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add index on {@code sys_user_role(user_id, role_id)} for efficient join</li>
     *     <li>Cache results with TTL (e.g., 10min) since role assignments change infrequently</li>
     *     <li>For large role sets (>100), consider pagination or search filtering in UI</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysRoleMapper.xml -->
     * <select id="selectRolesByUserId" resultType="com.github.starhq.template.model.vo.role.RoleCheckVO">
     *     SELECT
     *         r.id, r.code, r.name, r.description, r.is_default, r.status, r.sort_order,
     *         CASE WHEN ur.user_id IS NOT NULL THEN true ELSE false END as checked
     *     FROM sys_role r
     *     LEFT JOIN sys_user_role ur ON r.id = ur.role_id AND ur.user_id = #{userId}
     *     WHERE r.status = 1  -- Only active roles
     *     ORDER BY r.sort_order ASC, r.name ASC
     * </select>
     * }
     * </pre>
     *
     * @param userId the unique identifier of the user to check role assignments for; may be {@code null}
     * @return a list of {@link RoleCheckVO} with {@code checked} flag indicating user assignment; never {@code null}
     * @see RoleCheckVO
     * @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus XML Mapping Guide</a>
     */
    List<RoleCheckVO> selectRolesByUserId(@Param("userId") Serializable userId);
}