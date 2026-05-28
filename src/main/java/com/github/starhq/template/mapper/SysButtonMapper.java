package com.github.starhq.template.mapper;

import java.io.Serializable;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;

/**
 * MyBatis-Plus mapper interface for {@link SysButton} entity with permission-specific queries.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for button
 * permissions, and provides specialized methods for role-based and user-based permission
 * resolution in RBAC (Role-Based Access Control) scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Configuration</strong>: Fetch buttons assigned to a role for admin permission UI</li>
 *     <li><strong>User Authorization</strong>: Resolve effective buttons for a user via role inheritance</li>
 *     <li><strong>Impact Analysis</strong>: Identify users affected by button permission changes</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysButton}) vs. permission VO ({@code ButtonCheckVO})</li>
 *     <li><strong>Type Safety</strong>: Use {@link Serializable} for ID parameters to support multiple ID types</li>
 *     <li><strong>Query Efficiency</strong>: Leverage database joins and indexes for fast permission resolution</li>
 *     <li><strong>Cache Friendliness</strong>: Results are suitable for Redis/Caffeine caching with TTL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class ButtonPermissionService {
 *     @Autowired private SysButtonMapper buttonMapper;
 *
 *     // Fetch checked buttons for role configuration UI
 *     public List<ButtonCheckVO> getRoleButtons(Long roleId) {
 *         return buttonMapper.selectButtonsByRoleId(roleId);
 *     }
 *
 *     // Resolve effective buttons for user authorization
 *     public Set<String> getUserButtonCodes(Long userId) {
 *         return buttonMapper.selectAssignedButtonsByUserId(userId).stream()
 *             .map(SysButton::getCode)
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
 * @see SysButton
 * @see ButtonCheckVO
 * @see com.github.starhq.template.service.RoleService
 */
@Mapper
public interface SysButtonMapper extends BaseMapper<SysButton> {

    /**
     * Fetches buttons with checked status for a specific role in permission configuration UI.
     * <p>
     * This method returns all buttons in the system with a {@code checked} flag indicating
     * whether each button is assigned to the specified role. Designed for rendering
     * permission checkboxes in admin console role management pages.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs a {@code LEFT JOIN} between {@code sys_button} and {@code sys_role_button}</li>
     *     <li>Returns all buttons; {@code checked = true} if assigned to the role, {@code false} otherwise</li>
     *     <li>Results are typically sorted by {@code menu_id}, {@code sort_order} for intuitive UI grouping</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleId}: Must be a valid role ID; if {@code null} or non-existent, returns all buttons with {@code checked = false}</li>
     *     <li>Type: {@link Serializable} to support {@code Long}, {@code String}, or custom ID types</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add index on {@code sys_role_button(role_id, button_id)} for efficient join</li>
     *     <li>Cache results with TTL (e.g., 10min) since button definitions change infrequently</li>
     *     <li>For large button sets (>500), consider lazy-loading or pagination in UI</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysButtonMapper.xml -->
     * <select id="selectButtonsByRoleId" resultType="com.github.starhq.template.model.vo.button.ButtonCheckVO">
     *     SELECT
     *         b.id, b.menu_id, b.name, b.code, b.description,
     *         CASE WHEN rb.role_id IS NOT NULL THEN true ELSE false END as checked
     *     FROM sys_button b
     *     LEFT JOIN sys_role_button rb ON b.id = rb.button_id AND rb.role_id = #{roleId}
     *     ORDER BY b.menu_id, b.sort_order
     * </select>
     * }
     * </pre>
     *
     * @param roleId the unique identifier of the role to check button assignments for; may be {@code null}
     * @return a list of {@link ButtonCheckVO} with {@code checked} flag indicating role assignment; never {@code null}
     * @see ButtonCheckVO
     * @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus XML Mapping Guide</a>
     */
    List<ButtonCheckVO> selectButtonsByRoleId(@Param("roleId") Serializable roleId);

    /**
     * Fetches all buttons effectively assigned to a specific user via role inheritance.
     * <p>
     * This method resolves the complete set of button permissions a user has by traversing
     * the role assignment chain: {@code User → Roles → Role-Button Mappings → Buttons}.
     * Results are used for frontend UI rendering (show/hide buttons) and backend API authorization.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs multi-table joins: {@code sys_user_role} → {@code sys_role_button} → {@code sys_button}</li>
     *     <li>Returns distinct buttons to avoid duplicates from multiple role assignments</li>
     *     <li>Only includes active buttons ({@code status = 1}) and active roles ({@code status = 1})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must be a valid user ID; if {@code null} or non-existent, returns empty list</li>
     *     <li>Type: {@link Serializable} to support flexible ID strategies</li>
     * </ul>
     * <p>
     * <strong>Integration with Spring Security:</strong>
     * <pre>
     * {@code
     * // In custom UserDetailsService
     * public UserDetails loadUserByUsername(String username) {
     *     SysUser user = userMapper.selectByUsername(username);
     *     List<SysButton> buttons = buttonMapper.selectAssignedButtonsByUserId(user.getId());
     *
     *     List<GrantedAuthority> authorities = buttons.stream()
     *         .map(b -> new SimpleGrantedAuthority(b.getCode()))
     *         .collect(Collectors.toList());
     *
     *     user.setAuthorities(authorities);
     *     return user;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add composite indexes: {@code sys_user_role(user_id, role_id)}, {@code sys_role_button(role_id, button_id)}</li>
     *     <li>Cache results keyed by {@code userId} with TTL matching session duration</li>
     *     <li>For high-traffic systems, consider pre-computing user permissions during login</li>
     * </ul>
     *
     * @param userId the unique identifier of the user to resolve permissions for; may be {@code null}
     * @return a list of {@link SysButton} entities representing effective permissions; empty list if none; never {@code null}
     * @see SysButton#getCode()
     * @see org.springframework.security.core.GrantedAuthority
     */
    List<SysButton> selectAssignedButtonsByUserId(@Param("userId") Serializable userId);

    /**
     * Fetches user IDs that have access to a specific button permission.
     * <p>
     * This method performs a reverse lookup to identify all users who can access a given
     * button, useful for impact analysis when modifying or deprecating button permissions.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Traverses: {@code sys_button} → {@code sys_role_button} → {@code sys_user_role} → {@code sys_user}</li>
     *     <li>Returns distinct user IDs to avoid duplicates from multiple role paths</li>
     *     <li>Only includes active users ({@code status = ENABLED}) and active roles</li>
     * </ul>
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *     <li><strong>Change Impact Analysis</strong>: Notify affected users before removing a button permission</li>
     *     <li><strong>Audit Trail</strong>: Log which users had access to sensitive operations</li>
     *     <li><strong>Compliance Reporting</strong>: Generate access reports for regulatory audits</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code buttonId}: Must be a valid button ID; if {@code null} or non-existent, returns empty list</li>
     *     <li>Type: {@link Serializable} to support flexible ID strategies</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large user bases, consider pagination or streaming results to avoid memory pressure</li>
     *     <li>Add indexes on join keys: {@code sys_role_button(button_id)}, {@code sys_user_role(role_id)}</li>
     *     <li>Cache results with short TTL (e.g., 5min) if used for real-time impact analysis</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Ensure caller has administrative privileges before exposing user lists</li>
     *     <li>Consider masking or aggregating results for privacy compliance (GDPR, PIPL)</li>
     * </ul>
     *
     * @param buttonId the unique identifier of the button to check user access for; may be {@code null}
     * @return a list of user IDs with access to the button; empty list if none; never {@code null}
     * @see com.github.starhq.template.entity.SysUser
     * @see com.github.starhq.template.common.enums.UserStatus
     */
    List<Long> selectUserIdsByButtonId(@Param("buttonId") Serializable buttonId);
}