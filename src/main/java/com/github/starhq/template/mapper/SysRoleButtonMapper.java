package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysRoleButton;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis-Plus mapper interface for {@link SysRoleButton} junction entity with bulk upsert support.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for role-button
 * permission mappings, and provides a specialized method {@link #upsertRoleButton} for efficient
 * batch insertion or replacement of role-button associations in RBAC (Role-Based Access Control)
 * scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Permission Configuration</strong>: Bulk assign/revoke button permissions to roles in admin console</li>
 *     <li><strong>Batch Data Migration</strong>: Import role-button mappings from external systems or legacy data</li>
 *     <li><strong>Cache Warm-up</strong>: Preload role-button associations into application cache at startup</li>
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
 * public class RoleButtonService {
 *     @Autowired private SysRoleButtonMapper roleButtonMapper;
 *
 *     @Transactional(rollbackFor = Exception.class)
 *     public void assignButtonsToRole(Long roleId, Set<Long> buttonIds) {
 *         // 1. Delete existing mappings for this role
 *         roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>()
 *             .eq(SysRoleButton::getRoleId, roleId));
 *
 *         // 2. Build new mappings and bulk insert
 *         if (!CollectionUtils.isEmpty(buttonIds)) {
 *             List<SysRoleButton> mappings = buttonIds.stream()
 *                 .map(btnId -> new SysRoleButton(roleId, btnId))
 *                 .collect(Collectors.toList());
 *             roleButtonMapper.upsertRoleButton(mappings);
 *         }
 *
 *         // 3. Invalidate related caches
 *         cacheHelper.evict(List.of(roleId), List.of(CacheConstant.ROLE_BUTTONS));
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysRoleButton
 * @see com.github.starhq.template.service.RoleService
 */
@Mapper
public interface SysRoleButtonMapper extends BaseMapper<SysRoleButton> {

    /**
     * Performs bulk insert or replace of role-button permission mappings.
     * <p>
     * This method uses database-specific upsert syntax (e.g., MySQL {@code INSERT ... ON DUPLICATE KEY UPDATE},
     * PostgreSQL {@code INSERT ... ON CONFLICT DO UPDATE}) to efficiently handle both new assignments
     * and updates to existing role-button associations in a single operation.
     * <p>
     * <strong>Operation Semantics:</strong>
     * <ul>
     *     <li><strong>Insert</strong>: If {@code (role_id, button_id)} pair doesn't exist, create new mapping</li>
     *     <li><strong>Replace</strong>: If pair already exists, update timestamp or other mutable fields (if any)</li>
     *     <li><strong>Atomic</strong>: All mappings in the list are processed in a single transaction</li>
     *     <li><strong>Fail-Fast</strong>: Any constraint violation (e.g., foreign key) aborts the entire batch</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleButtons}: List of {@link SysRoleButton} entities to upsert; must not be {@code null} or empty</li>
     *     <li>Each entity must have valid {@code roleId} and {@code buttonId} referencing existing records</li>
     *     <li>Duplicate {@code (roleId, buttonId)} pairs in the input list may cause undefined behavior</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Callers should wrap this method in {@code @Transactional} to ensure atomicity with related operations:
     * <pre>
     * {@code
     * @Transactional
     * public void updateRolePermissions(Long roleId, Set<Long> newButtonIds) {
     *     // 1. Delete old mappings
     *     roleButtonMapper.deleteByRoleId(roleId);
     *
     *     // 2. Bulk insert new mappings (this method)
     *     List<SysRoleButton> mappings = buildMappings(roleId, newButtonIds);
     *     roleButtonMapper.upsertRoleButton(mappings);
     *
     *     // 3. Other business logic...
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large batches (>1000 items), consider chunking to avoid SQL parameter limits or memory pressure</li>
     *     <li>Ensure composite unique key {@code UNIQUE KEY uk_role_button (role_id, button_id)} exists for efficient upsert</li>
     *     <li>Add index on {@code (button_id, role_id)} if reverse lookups (button → roles) are frequent</li>
     *     <li>Monitor execution time; optimize with database-specific batch syntax if needed</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example (MySQL):</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysRoleButtonMapper.xml -->
     * <insert id="upsertRoleButton" parameterType="java.util.List">
     *     INSERT INTO sys_role_button (role_id, button_id, created_at, created_by)
     *     VALUES
     *     <foreach collection="list" item="item" separator=",">
     *         (#{item.roleId}, #{item.buttonId}, #{item.createdAt}, #{item.createdBy})
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
     *     <li>{@code DataIntegrityViolationException}: Foreign key constraint failure (invalid roleId/buttonId)</li>
     *     <li>{@code DuplicateKeyException}: Should not occur if unique key is properly defined; indicates logic error</li>
     *     <li>{@code BatchUpdateException}: Partial failure in batch; entire transaction rolls back if wrapped in {@code @Transactional}</li>
     * </ul>
     *
     * @param roleButtons the list of role-button mappings to insert or update; must not be {@code null} or empty
     * @throws com.baomidou.mybatisplus.core.exceptions.MybatisPlusException if batch operation fails
     * @throws org.springframework.dao.DataIntegrityViolationException       if foreign key constraints are violated
     * @see SysRoleButton
     * @see <a href="https://dev.mysql.com/doc/refman/8.0/en/insert-on-duplicate.html">MySQL INSERT ... ON DUPLICATE KEY UPDATE</a>
     * @see <a href="https://www.postgresql.org/docs/current/sql-insert.html#SQL-ON-CONFLICT">PostgreSQL INSERT ... ON CONFLICT</a>
     */
    void upsertRoleButton(@Param("list") List<SysRoleButton> roleButtons);

    // ========== Inherited Methods from BaseMapper<SysRoleButton> ==========
    //
    // The following standard CRUD methods are automatically provided by MyBatis-Plus:
    //
    // // Insert (single)
    // int insert(SysRoleButton entity);
    //
    // // Select
    // SysRoleButton selectById(Serializable id);  // Note: composite key may require custom query
    // List<SysRoleButton> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysRoleButton> selectByMap(Map<String, Object> columnMap);
    // SysRoleButton selectOne(LambdaQueryWrapper<SysRoleButton> queryWrapper);
    // List<SysRoleButton> selectList(LambdaQueryWrapper<SysRoleButton> queryWrapper);
    // <E extends IPage<SysRoleButton>> E selectPage(E page, LambdaQueryWrapper<SysRoleButton> queryWrapper);
    //
    // // Update (single)
    // int updateById(SysRoleButton entity);
    // int update(SysRoleButton entity, LambdaUpdateWrapper<SysRoleButton> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysRoleButton> queryWrapper);
    //
    // For junction tables with composite keys, consider adding custom methods:
    // - deleteByRoleId(Long roleId)
    // - deleteByButtonId(Long buttonId)
    // - selectByRoleId(Long roleId)
    //
    // For detailed usage and advanced features, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}