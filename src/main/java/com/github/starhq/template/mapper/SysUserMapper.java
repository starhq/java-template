package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.github.starhq.template.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis-Plus mapper interface for {@link SysUser} entity with role-joined queries.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for user
 * management, and provides a specialized method {@link #selectUserWithRole} for fetching
 * users with their assigned roles in a single query for authentication and authorization.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Authentication</strong>: Fetch user with roles during login for Spring Security integration</li>
 *     <li><strong>User Management Console</strong>: Display user-role relationships in admin panel</li>
 *     <li><strong>Permission Resolution</strong>: Resolve effective permissions via user's role assignments</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysUser}) vs. permission resolution</li>
 *     <li><strong>Query Efficiency</strong>: Single query with join avoids N+1 problem</li>
 *     <li><strong>Dynamic Filtering</strong>: Leverage {@link Wrapper} for flexible, type-safe conditions</li>
 *     <li><strong>Cache Friendliness</strong>: User-role data suitable for Redis caching with TTL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class UserService {
 *     @Autowired private SysUserMapper userMapper;
 *
 *     // Load user with roles for authentication
 *     public SysUser loadUserWithRoles(String username) {
 *         LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
 *             .eq(SysUser::getUsername, username)
 *             .eq(SysUser::getStatus, UserStatus.ENABLED);
 *         return userMapper.selectUserWithRole(wrapper);
 *     }
 *
 *     // Resolve effective role codes for authorization
 *     public Set<String> getUserRoleCodes(Long userId) {
 *         LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
 *             .eq(SysUser::getId, userId);
 *         SysUser user = userMapper.selectUserWithRole(wrapper);
 *         return user.getRoles().stream()
 *             .map(SysRole::getCode)
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
 * @see SysUser
 * @see com.github.starhq.template.service.UserService
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * Fetches a user entity with their assigned roles populated.
     * <p>
     * This method performs a multi-table join to load a user along with their role assignments
     * in a single query. Designed for authentication flows where the complete user-context
     * (including roles) is needed to build {@code UserDetails} for Spring Security.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>Performs {@code LEFT JOIN} between {@code sys_user} and {@code sys_user_role} → {@code sys_role}</li>
     *     <li>Populates {@code SysUser.roles} collection with assigned role entities</li>
     *     <li>Only includes active users ({@code status = ENABLED}) and active roles</li>
     *     <li>Results are distinct to avoid duplicates from multiple role assignments</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code wrapper}: Query conditions wrapper; must include user identifier conditions (e.g., username, email, or id)</li>
     *     <li>Type: {@link Wrapper}{@code <SysUser>} for compile-time field safety</li>
     * </ul>
     * <p>
     * <strong>MyBatis-Plus Integration:</strong>
     * <p>
     * The {@code @Param(Constants.WRAPPER)} annotation enables MyBatis-Plus to inject
     * {@link Wrapper} conditions into the SQL dynamically. When using XML mapping,
     * reference {@code ${ew.sqlSegment}} or {@code <where>${ew.sqlSegment}</where>}
     * for proper condition injection.
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysUserMapper.xml -->
     * <resultMap id="UserWithRoleMap" type="com.github.starhq.template.entity.SysUser">
     *     <id property="id" column="user_id"/>
     *     <result property="username" column="username"/>
     *     <result property="email" column="email"/>
     *     <result property="status" column="status"/>
     *     <collection property="roles" ofType="com.github.starhq.template.entity.SysRole">
     *         <id property="id" column="role_id"/>
     *         <result property="code" column="role_code"/>
     *         <result property="name" column="role_name"/>
     *         <result property="description" column="role_description"/>
     *     </collection>
     * </resultMap>
     *
     * <select id="selectUserWithRole" resultMap="UserWithRoleMap">
     *     SELECT
     *         u.id as user_id, u.username, u.email, u.password, u.status, u.created_at,
     *         r.id as role_id, r.code as role_code, r.name as role_name, r.description as role_description
     *     FROM sys_user u
     *     LEFT JOIN sys_user_role ur ON u.id = ur.user_id
     *     LEFT JOIN sys_role r ON ur.role_id = r.id AND r.status = 1  -- Only active roles
     *     <where>
     *         ${ew.sqlSegment}  <!-- Dynamic conditions from Wrapper -->
     *     </where>
     * </select>
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add composite indexes: {@code sys_user_role(user_id, role_id)}, {@code sys_role(status)}</li>
     *     <li>Cache user-role mappings keyed by {@code userId} with TTL matching session duration</li>
     *     <li>For high-traffic auth paths, consider pre-computing user permissions during login</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Ensure password field is properly masked or excluded from result mapping</li>
     *     <li>Log access to this method for audit trails of user permission queries</li>
     *     <li>Consider rate-limiting authentication queries to prevent brute-force attacks</li>
     * </ul>
     *
     * @param wrapper the query conditions wrapper for filtering users; must include user identifier conditions
     * @return a {@link SysUser} entity with populated {@code roles} collection; {@code null} if user not found
     * @see SysUser#getAuthorities()
     * @see com.github.starhq.template.common.enums.UserStatus
     */
    SysUser selectUserWithRole(@Param(Constants.WRAPPER) Wrapper<SysUser> wrapper);

    // ========== Inherited Methods from BaseMapper<SysUser> ==========
    //
    // The following standard CRUD methods are automatically provided by MyBatis-Plus:
    //
    // // Insert
    // int insert(SysUser entity);
    //
    // // Select
    // SysUser selectById(Serializable id);
    // List<SysUser> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysUser> selectByMap(Map<String, Object> columnMap);
    // SysUser selectOne(LambdaQueryWrapper<SysUser> queryWrapper);
    // List<SysUser> selectList(LambdaQueryWrapper<SysUser> queryWrapper);
    // <E extends IPage<SysUser>> E selectPage(E page, LambdaQueryWrapper<SysUser> queryWrapper);
    //
    // // Update
    // int updateById(SysUser entity);
    // int update(SysUser entity, LambdaUpdateWrapper<SysUser> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysUser> queryWrapper);
    //
    // For detailed usage and advanced features, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}
