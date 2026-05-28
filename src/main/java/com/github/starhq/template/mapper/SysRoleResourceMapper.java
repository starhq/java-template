package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysRoleResource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysRoleResource} junction entity with bulk upsert support.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for role-API resource
 * permission mappings, and provides a specialized method {@link #upsertRoleResource} for efficient
 * batch insertion or replacement of role-resource associations in RBAC (Role-Based Access Control)
 * scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Permission Configuration</strong>: Bulk assign/revoke API resource permissions to roles in admin console</li>
 *     <li><strong>Batch Data Migration</strong>: Import role-resource mappings from external systems or legacy data</li>
 *     <li><strong>Cache Warm-up</strong>: Preload role-resource associations into application cache at startup</li>
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
 * public class RoleResourceService {
 *     @Autowired private SysRoleResourceMapper roleResourceMapper;
 *
 *     @Transactional(rollbackFor = Exception.class)
 *     public void assignResourcesToRole(Long roleId, Set<Long> resourceIds) {
 *         // 1. Delete existing mappings for this role
 *         roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>()
 *             .eq(SysRoleResource::getRoleId, roleId));
 *
 *         // 2. Build new mappings and bulk insert
 *         if (!CollectionUtils.isEmpty(resourceIds)) {
 *             List<SysRoleResource> mappings = resourceIds.stream()
 *                 .map(resId -> new SysRoleResource(roleId, resId))
 *                 .collect(Collectors.toList());
 *             roleResourceMapper.upsertRoleResource(mappings);
 *         }
 *
 *         // 3. Invalidate related caches
 *         cacheHelper.evict(List.of(roleId), List.of(CacheConstant.ROLE_RESOURCES));
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysRoleResource
 * @see com.github.starhq.template.service.RoleService
 */
@Mapper
public interface SysRoleResourceMapper extends BaseMapper<SysRoleResource> {

    /**
     * Performs bulk insert or replace of role-API resource permission mappings.
     * <p>
     * This method uses database-specific upsert syntax (e.g., MySQL {@code INSERT ... ON DUPLICATE KEY UPDATE},
     * PostgreSQL {@code INSERT ... ON CONFLICT DO UPDATE}) to efficiently handle both new assignments
     * and updates to existing role-resource associations in a single operation.
     * <p>
     * <strong>Operation Semantics:</strong>
     * <ul>
     *     <li><strong>Insert</strong>: If {@code (role_id, resource_id)} pair doesn't exist, create new mapping</li>
     *     <li><strong>Replace</strong>: If pair already exists, update timestamp or other mutable fields (if any)</li>
     *     <li><strong>Atomic</strong>: All mappings in the list are processed in a single transaction</li>
     *     <li><strong>Fail-Fast</strong>: Any constraint violation (e.g., foreign key) aborts the entire batch</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleResources}: List of {@link SysRoleResource} entities to upsert; must not be {@code null} or empty</li>
     *     <li>Each entity must have valid {@code roleId} and {@code resourceId} referencing existing records</li>
     *     <li>Duplicate {@code (roleId, resourceId)} pairs in the input list may cause undefined behavior</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Callers should wrap this method in {@code @Transactional} to ensure atomicity with related operations:
     * <pre>
     * {@code
     * @Transactional
     * public void updateRolePermissions(Long roleId, Set<Long> newResourceIds) {
     *     // 1. Delete old mappings
     *     roleResourceMapper.deleteByRoleId(roleId);
     *
     *     // 2. Bulk insert new mappings (this method)
     *     List<SysRoleResource> mappings = buildMappings(roleId, newResourceIds);
     *     roleResourceMapper.upsertRoleResource(mappings);
     *
     *     // 3. Other business logic...
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large batches (>1000 items), consider chunking to avoid SQL parameter limits or memory pressure</li>
     *     <li>Ensure composite unique key {@code UNIQUE KEY uk_role_resource (role_id, resource_id)} exists for efficient upsert</li>
     *     <li>Add index on {@code (resource_id, role_id)} if reverse lookups (resource → roles) are frequent</li>
     *     <li>Monitor execution time; optimize with database-specific batch syntax if needed</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example (MySQL):</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysRoleResourceMapper.xml -->
     * <insert id="upsertRoleResource" parameterType="java.util.List">
     *     INSERT INTO sys_role_resource (role_id, resource_id, created_at, created_by)
     *     VALUES
     *     <foreach collection="list" item="item" separator=",">
     *         (#{item.roleId}, #{item.resourceId}, #{item.createdAt}, #{item.createdBy})
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
     *     <li>{@code DataIntegrityViolationException}: Foreign key constraint failure (invalid roleId/resourceId)</li>
     *     <li>{@code DuplicateKeyException}: Should not occur if unique key is properly defined; indicates logic error</li>
     *     <li>{@code BatchUpdateException}: Partial failure in batch; entire transaction rolls back if wrapped in {@code @Transactional}</li>
     * </ul>
     *
     * @param roleResources the list of role-resource mappings to insert or update; must not be {@code null} or empty
     * @throws com.baomidou.mybatisplus.core.exceptions.MybatisPlusException if batch operation fails
     * @throws org.springframework.dao.DataIntegrityViolationException       if foreign key constraints are violated
     * @see SysRoleResource
     * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html">MySQL INSERT ... ON DUPLICATE KEY UPDATE</a>
     * @see <a href="https://www.postgresql.org/docs/current/sql-insert.html#SQL-ON-CONFLICT">PostgreSQL INSERT ... ON CONFLICT</a>
     */
    void upsertRoleResource(List<SysRoleResource> roleResources);
}
