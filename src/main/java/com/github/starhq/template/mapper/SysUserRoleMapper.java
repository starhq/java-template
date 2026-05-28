package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysUserRole} junction entity with bulk upsert support.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for role-user
 * assignment mappings, and provides a specialized method {@link #upsertUserRole} for efficient
 * batch insertion or replacement of user-role associations in RBAC (Role-Based Access Control)
 * scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Role Management</strong>: Bulk assign/revoke roles to users in admin console</li>
 *     <li><strong>Batch Data Migration</strong>: Import user-role mappings from external systems or legacy data</li>
 *     <li><strong>Cache Warm-up</strong>: Preload user-role associations into application cache at startup</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Atomic Batch Operations</strong>: Use single SQL statement for efficient bulk upsert with transaction safety</li>
 *     <li><strong>Idempotency</strong>: Method should be safe to call multiple times with same input (replace existing mappings)</li>
 *     <li><strong>Performance First</strong>: Minimize database round-trips by processing all mappings in one query</li>
 *     <li><strong>Error Isolation</strong>: Fail fast on constraint violations; caller handles retry or rollback logic</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class UserRoleService {
 *     @Autowired private SysUserRoleMapper userRoleMapper;
 *
 *     @Transactional(rollbackFor = Exception.class)
 *     public void assignRolesToUser(Long userId, Set<Long> roleIds) {
 *         // 1. Delete existing mappings for this user
 *         userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>()
 *             .eq(SysUserRole::getUserId, userId));
 *
 *         // 2. Build new mappings and bulk insert
 *         if (!CollectionUtils.isEmpty(roleIds)) {
 *             List<SysUserRole> mappings = roleIds.stream()
 *                 .map(roleId -> new SysUserRole(userId, roleId))
 *                 .collect(Collectors.toList());
 *             userRoleMapper.upsertUserRole(mappings);
 *         }
 *
 *         // 3. Invalidate related caches
 *         cacheHelper.evict(List.of(userId), List.of(CacheConstant.USER_ROLES));
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysUserRole
 * @see com.github.starhq.template.service.UserService
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * Performs bulk insert or replace of user-role assignment mappings.
     * <p>
     * This method uses database-specific upsert syntax (e.g., MySQL {@code INSERT ... ON DUPLICATE KEY UPDATE},
     * PostgreSQL {@code INSERT ... ON CONFLICT DO UPDATE}) to efficiently handle both new assignments
     * and updates to existing user-role associations in a single operation.
     * <p>
     * <strong>Operation Semantics:</strong>
     * <ul>
     *     <li><strong>Insert</strong>: If {@code (user_id, role_id)} pair doesn't exist, create new mapping</li>
     *     <li><strong>Replace</strong>: If pair already exists, update timestamp or other mutable fields (if any)</li>
     *     <li><strong>Atomic</strong>: All mappings in the list are processed in a single transaction</li>
     *     <li><strong>Fail-Fast</strong>: Any constraint violation (e.g., foreign key) aborts the entire batch</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userRoles}: List of {@link SysUserRole} entities to upsert; must not be {@code null} or empty</li>
     *     <li>Each entity must have valid {@code userId} and {@code roleId} referencing existing records</li>
     *     <li>Duplicate {@code (userId, roleId)} pairs in the input list may cause undefined behavior</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Callers should wrap this method in {@code @Transactional} to ensure atomicity with related operations:
     * <pre>
     * {@code
     * @Transactional
     * public void updateUserRoles(Long userId, Set<Long> newRoleIds) {
     *     // 1. Delete old mappings
     *     userRoleMapper.deleteByUserId(userId);
     *
     *     // 2. Bulk insert new mappings (this method)
     *     List<SysUserRole> mappings = buildMappings(userId, newRoleIds);
     *     userRoleMapper.upsertUserRole(mappings);
     *
     *     // 3. Other business logic...
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large batches (>1000 items), consider chunking to avoid SQL parameter limits or memory pressure</li>
     *     <li>Ensure composite unique key {@code UNIQUE KEY uk_user_role (user_id, role_id)} exists for efficient upsert</li>
     *     <li>Add index on {@code (role_id, user_id)} if reverse lookups (role → users) are frequent</li>
     *     <li>Monitor execution time; optimize with database-specific batch syntax if needed</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example (MySQL):</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysUserRoleMapper.xml -->
     * <insert id="upsertUserRole" parameterType="java.util.List">
     *     INSERT INTO sys_user_role (user_id, role_id, created_at, created_by)
     *     VALUES
     *     <foreach collection="list" item="item" separator=",">
     *         (#{item.userId}, #{item.roleId}, #{item.createdAt}, #{item.createdBy})
     *     </foreach>
     *     ON DUPLICATE KEY UPDATE
     *         created_at = VALUES(created_at),
     *         created_by = VALUES(created_by)
     * </insert>
     * }
     * </pre>
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li>{@code DataIntegrityViolationException}: Foreign key constraint failure (invalid userId/roleId)</li>
     *     <li>{@code DuplicateKeyException}: Should not occur if unique key is properly defined; indicates logic error</li>
     *     <li>{@code BatchUpdateException}: Partial failure in batch; entire transaction rolls back if wrapped in {@code @Transactional}</li>
     * </ul>
     *
     * @param userRoles the list of user-role mappings to insert or update; must not be {@code null} or empty
     * @throws com.baomidou.mybatisplus.core.exceptions.MybatisPlusException if batch operation fails
     * @throws org.springframework.dao.DataIntegrityViolationException       if foreign key constraints are violated
     * @see SysUserRole
     * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html">MySQL INSERT ... ON DUPLICATE KEY UPDATE</a>
     * @see <a href="https://www.postgresql.org/docs/current/sql-insert.html#SQL-ON-CONFLICT">PostgreSQL INSERT ... ON CONFLICT</a>
     */
    void upsertUserRole(List<SysUserRole> userRoles);
}
