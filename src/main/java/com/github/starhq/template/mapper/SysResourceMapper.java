package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysResource} entity with permission-specific queries.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for API resource
 * definitions, and provides specialized methods for role-based resource assignment and
 * user permission resolution in RBAC (Role-Based Access Control) scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Configuration</strong>: Fetch resources with checked status for admin permission UI</li>
 *     <li><strong>API Authorization</strong>: Resolve effective API resources for a user via role inheritance</li>
 *     <li><strong>Impact Analysis</strong>: Identify users affected by resource permission changes</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysResource}) vs. permission VO ({@code ResourceCheckVO})</li>
 *     <li><strong>Type Safety</strong>: Use {@link Serializable} for ID parameters to support multiple ID types</li>
 *     <li><strong>Query Efficiency</strong>: Leverage database joins and indexes for fast permission resolution</li>
 *     <li><strong>Cache Friendliness</strong>: Results are suitable for Redis/Caffeine caching with TTL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class ResourcePermissionService {
 *     @Autowired private SysResourceMapper resourceMapper;
 *
 *     // Fetch checked resources for role configuration UI
 *     public List<ResourceCheckVO> getRoleResources(Long roleId) {
 *         return resourceMapper.selectResourcesByRoleId(roleId);
 *     }
 *
 *     // Resolve effective resources for user API authorization
 *     public boolean hasResourceAccess(Long userId, String url, String method) {
 *         List<SysResource> resources = resourceMapper.selectAssignedResourceByUserId(userId);
 *         return resources.stream().anyMatch(r ->
 *             pathMatcher.match(r.getUrl(), url) &&
 *             HttpMethodBitmaskUtils.isAllowed(r.getMethods(), HttpMethodUtils.toFlag(method))
 *         );
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysResource
 * @see ResourceCheckVO
 * @see com.github.starhq.template.service.RoleService
 */
@Mapper
public interface SysResourceMapper extends BaseMapper<SysResource> {

    /**
     * Fetches resources with checked status for a specific role in permission configuration UI.
     * <p>
     * This method returns all API resources in the system with a {@code checked} flag indicating
     * whether each resource is assigned to the specified role. Designed for rendering
     * permission checkboxes in admin console role management pages.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs a {@code LEFT JOIN} between {@code sys_resource} and {@code sys_role_resource}</li>
     *     <li>Returns all resources; {@code checked = true} if assigned to the role, {@code false} otherwise</li>
     *     <li>Results are typically sorted by {@code url}, {@code name} for intuitive UI grouping</li>
     *     <li>Only includes active resources ({@code status = 1}) to avoid assigning disabled endpoints</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleId}: Must be a valid role ID; if {@code null} or non-existent, returns all resources with {@code checked = false}</li>
     *     <li>Type: {@link Serializable} to support {@code Long}, {@code String}, or custom ID types</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add index on {@code sys_role_resource(role_id, resource_id)} for efficient join</li>
     *     <li>Cache results with TTL (e.g., 10min) since resource definitions change infrequently</li>
     *     <li>For large resource sets (>500), consider pagination or search filtering in UI</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysResourceMapper.xml -->
     * <select id="selectResourcesByRoleId" resultType="com.github.starhq.template.model.vo.resource.ResourceCheckVO">
     *     SELECT
     *         r.id, r.name, r.url, r.methods, r.description, r.status,
     *         CASE WHEN rr.role_id IS NOT NULL THEN true ELSE false END as checked
     *     FROM sys_resource r
     *     LEFT JOIN sys_role_resource rr ON r.id = rr.resource_id AND rr.role_id = #{roleId}
     *     WHERE r.status = 1  -- Only active resources
     *     ORDER BY r.url, r.name
     * </select>
     * }
     * </pre>
     *
     * @param roleId the unique identifier of the role to check resource assignments for; may be {@code null}
     * @return a list of {@link ResourceCheckVO} with {@code checked} flag indicating role assignment; never {@code null}
     * @see ResourceCheckVO
     * @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus XML Mapping Guide</a>
     */
    List<ResourceCheckVO> selectResourcesByRoleId(@Param("roleId") Serializable roleId);

    /**
     * Fetches all API resources effectively assigned to a specific user via role inheritance.
     * <p>
     * This method resolves the complete set of API resource permissions a user has by traversing
     * the role assignment chain: {@code User → Roles → Role-Resource Mappings → Resources}.
     * Results are used for backend API authorization checks (URL + HTTP method matching).
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs multi-table joins: {@code sys_user_role} → {@code sys_role_resource} → {@code sys_resource}</li>
     *     <li>Returns distinct resources to avoid duplicates from multiple role assignments</li>
     *     <li>Only includes active resources ({@code status = 1}) and active roles ({@code status = 1})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must be a valid user ID; if {@code null} or non-existent, returns empty list</li>
     *     <li>Type: {@link Serializable} to support flexible ID strategies</li>
     * </ul>
     * <p>
     * <strong>Integration with API Gateway:</strong>
     * <pre>
     * {@code
     * // In API authorization filter
     * public boolean checkAccess(HttpServletRequest request, Long userId) {
     *     String url = request.getRequestURI();
     *     String method = request.getMethod();
     *
     *     List<SysResource> resources = resourceMapper.selectAssignedResourceByUserId(userId);
     *
     *     return resources.stream().anyMatch(r ->
     *         pathMatcher.match(r.getUrl(), url) &&
     *         HttpMethodBitmaskUtils.isAllowed(r.getMethods(), HttpMethodUtils.toFlag(method))
     *     );
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add composite indexes: {@code sys_user_role(user_id, role_id)}, {@code sys_role_resource(role_id, resource_id)}</li>
     *     <li>Cache results keyed by {@code userId} with TTL matching session duration</li>
     *     <li>For high-traffic systems, consider pre-computing user permissions during login</li>
     *     <li>Consider storing resource permissions as {@code Set<String>} (url:method) for O(1) lookup</li>
     * </ul>
     *
     * @param userId the unique identifier of the user to resolve permissions for; may be {@code null}
     * @return a list of {@link SysResource} entities representing effective API permissions; empty list if none; never {@code null}
     * @see SysResource#getUrl()
     * @see SysResource#getMethods()
     * @see com.github.starhq.template.common.enums.HttpMethod
     */
    List<SysResource> selectAssignedResourceByUserId(@Param("userId") Serializable userId);

    /**
     * Fetches user IDs that have access to a specific API resource permission.
     * <p>
     * This method performs a reverse lookup to identify all users who can access a given
     * API endpoint, useful for impact analysis when modifying or deprecating resource permissions.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Traverses: {@code sys_resource} → {@code sys_role_resource} → {@code sys_user_role} → {@code sys_user}</li>
     *     <li>Returns distinct user IDs to avoid duplicates from multiple role paths</li>
     *     <li>Only includes active users ({@code status = ENABLED}) and active roles</li>
     * </ul>
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *     <li><strong>Change Impact Analysis</strong>: Notify affected users before removing an API permission</li>
     *     <li><strong>Audit Trail</strong>: Log which users had access to sensitive API endpoints</li>
     *     <li><strong>Compliance Reporting</strong>: Generate access reports for regulatory audits (e.g., PCI-DSS, GDPR)</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code resourceId}: Must be a valid resource ID; if {@code null} or non-existent, returns empty list</li>
     *     <li>Type: {@link Serializable} to support flexible ID strategies</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Ensure caller has administrative privileges before exposing user lists</li>
     *     <li>Consider masking or aggregating results for privacy compliance (GDPR, PIPL)</li>
     *     <li>Log access to this method for audit trails of permission impact queries</li>
     *     <li>Rate-limit this endpoint to prevent enumeration attacks</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large user bases, consider pagination or streaming results to avoid memory pressure</li>
     *     <li>Add indexes on join keys: {@code sys_role_resource(resource_id)}, {@code sys_user_role(role_id)}</li>
     *     <li>Cache results with short TTL (e.g., 5min) if used for real-time impact analysis</li>
     * </ul>
     *
     * @param resourceId the unique identifier of the resource to check user access for; may be {@code null}
     * @return a list of user IDs with access to the resource; empty list if none; never {@code null}
     * @see com.github.starhq.template.entity.SysUser
     * @see com.github.starhq.template.common.enums.UserStatus
     */
    List<Long> selectUserIdsByResourceId(@Param("resourceId") Serializable resourceId);

    // ========== Inherited Methods from BaseMapper<SysResource> ==========
    //
    // The following standard CRUD methods are automatically provided by MyBatis-Plus:
    //
    // // Insert
    // int insert(SysResource entity);
    //
    // // Select
    // SysResource selectById(Serializable id);
    // List<SysResource> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysResource> selectByMap(Map<String, Object> columnMap);
    // SysResource selectOne(LambdaQueryWrapper<SysResource> queryWrapper);
    // List<SysResource> selectList(LambdaQueryWrapper<SysResource> queryWrapper);
    // <E extends IPage<SysResource>> E selectPage(E page, LambdaQueryWrapper<SysResource> queryWrapper);
    //
    // // Update
    // int updateById(SysResource entity);
    // int update(SysResource entity, LambdaUpdateWrapper<SysResource> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysResource> queryWrapper);
    //
    // For detailed usage and advanced features, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}