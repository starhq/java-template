package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.converter.RoleConverter;
import com.github.starhq.template.entity.*;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.RelationHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.*;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.role.RoleDTO;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import com.github.starhq.template.model.vo.role.RolePageVO;
import com.github.starhq.template.model.vo.role.RoleSimpleVO;
import com.github.starhq.template.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.LongConsumer;

/**
 * Service implementation for role management with CRUD operations, permission associations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link RoleService} for role-specific business operations.
 * Designed to centralize role management logic with consistent validation, cache integration, and
 * distributed audit logging for compliance tracking.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>Role CRUD</strong>: Create, read, update, delete role definitions with code uniqueness validation</li>
 *     <li><strong>Permission Associations</strong>: Manage role-menu, role-button, role-resource, and user-role relationships</li>
 *     <li><strong>Cache Management</strong>: Invalidate related caches (user, menu, button, resource) on role changes</li>
 *     <li><strong>Audit Integration</strong>: Record all write operations via {@code @AuditLoggable} for compliance</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Transaction-Safe</strong>: All write operations use {@code @Transactional} for atomicity</li>
 *     <li><strong>Cache-Consistent</strong>: Changes trigger distributed cache invalidation via {@link EventService}</li>
 *     <li><strong>Reusable Helpers</strong>: Leverage {@link RelationHelper} for consistent association management</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see RoleService
 * @see AuditBaseServiceImpl
 * @see SysRole
 * @see RelationHelper
 */
@Service("roleService")
public class RoleServiceImpl extends AuditBaseServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    /**
     * Mapper for {@link SysMenu} database operations (menu hierarchy).
     * <p>Used for validating menu IDs when assigning menus to roles.</p>
     *
     * @see SysMenuMapper
     */
    private final SysMenuMapper menuMapper;

    /**
     * Mapper for {@link SysButton} database operations (button permissions).
     * <p>Used for validating button IDs when assigning buttons to roles.</p>
     *
     * @see SysButtonMapper
     */
    private final SysButtonMapper buttonMapper;

    /**
     * Mapper for {@link SysResource} database operations (API/data resources).
     * <p>Used for validating resource IDs when assigning resources to roles.</p>
     *
     * @see SysResourceMapper
     */
    private final SysResourceMapper resourceMapper;

    /**
     * Helper for efficient batch username resolution in audit field population.
     * <p>Injected into {@link AuditBaseServiceImpl#pageVO} to resolve {@code createdBy}/{@code updatedBy}
     * IDs to human-readable usernames via cached batch lookup.</p>
     *
     * @see SysUserMapperHelper
     */
    private final SysUserMapperHelper userMapperHelper;

    /**
     * Mapper for {@link SysRoleResource} junction table operations (role-resource assignments).
     * <p>Used for CRUD operations on role-resource relationships.</p>
     *
     * @see SysRoleResourceMapper
     */
    private final SysRoleResourceMapper roleResourceMapper;

    /**
     * Mapper for {@link SysRoleMenu} junction table operations (role-menu assignments).
     * <p>Used for CRUD operations on role-menu relationships.</p>
     *
     * @see SysRoleMenuMapper
     */
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * Mapper for {@link SysRoleButton} junction table operations (role-button assignments).
     * <p>Used for CRUD operations on role-button relationships.</p>
     *
     * @see SysRoleButtonMapper
     */
    private final SysRoleButtonMapper roleButtonMapper;

    /**
     * Mapper for {@link SysUserRole} junction table operations (user-role assignments).
     * <p>Used for cascading delete: removing user-role assignments when role is deleted.</p>
     *
     * @see SysUserRoleMapper
     */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * Helper for managing entity associations with validation, upsert, and cleanup logic.
     * <p>Provides reusable methods for assigning resources/menus/buttons to roles with
     * existence validation and atomic upsert operations.</p>
     *
     * @see RelationHelper#assignRelations
     */
    private final RelationHelper relationHelper;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>Ensures cache consistency when role data changes by notifying all nodes to evict
     * related cache entries for users, menus, buttons, and resources.</p>
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Converter for transforming between {@link RoleDTO}, {@link SysRole}, and various VO types.
     * <p>Ensures consistent field mapping and avoids boilerplate conversion code across service methods.</p>
     *
     * @see RoleConverter
     */
    private final RoleConverter roleConverter;

    /**
     * Constructs a new {@code RoleServiceImpl} with all required dependencies for RBAC management.
     * <p>
     * This constructor requires a comprehensive set of mappers to handle complex many-to-many
     * relationships between roles, users, menus, buttons, and resources.
     *
     * @param cacheHelper        the cache utility for batch username resolution (inherited from base class)
     * @param userMapperHelper   the helper for resolving user IDs to usernames during audit field population
     * @param menuMapper         the mapper for querying menu details during role-menu assignment
     * @param buttonMapper       the mapper for querying button details during role-button assignment
     * @param resourceMapper     the mapper for querying resource details during role-resource assignment
     * @param roleResourceMapper the mapper for managing role-resource relationships in the database
     * @param roleMenuMapper     the mapper for managing role-menu relationships in the database
     * @param roleButtonMapper   the mapper for managing role-button relationships in the database
     * @param userRoleMapper     the mapper for managing user-role relationships in the database
     * @param relationHelper     the utility helper for abstracting complex relational batch operations (e.g., delete and re-insert)
     * @param eventService       the service for publishing domain events (e.g., clearing authorization caches)
     * @param roleConverter      the converter for transforming between role entities, DTOs, and VOs
     */
    public RoleServiceImpl(CacheHelper cacheHelper,
                           SysUserMapperHelper userMapperHelper,
                           SysMenuMapper menuMapper,
                           SysButtonMapper buttonMapper,
                           SysResourceMapper resourceMapper,
                           SysRoleResourceMapper roleResourceMapper,
                           SysRoleMenuMapper roleMenuMapper,
                           SysRoleButtonMapper roleButtonMapper,
                           SysUserRoleMapper userRoleMapper,
                           RelationHelper relationHelper,
                           EventService eventService,
                           RoleConverter roleConverter) {
        super(cacheHelper);
        this.menuMapper = menuMapper;
        this.buttonMapper = buttonMapper;
        this.resourceMapper = resourceMapper;
        this.userMapperHelper = userMapperHelper;
        this.roleResourceMapper = roleResourceMapper;
        this.roleMenuMapper = roleMenuMapper;
        this.roleButtonMapper = roleButtonMapper;
        this.userRoleMapper = userRoleMapper;
        this.relationHelper = relationHelper;
        this.eventService = eventService;
        this.roleConverter = roleConverter;
    }

    /**
     * Retrieves a paginated list of role definitions with audit field resolution.
     * <p>Delegates to {@link AuditBaseServiceImpl#pageVO} for base query building, batch username resolution,
     * and entity-to-VO conversion.</p>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link RolePageVO}; never {@code null}
     * @see PageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see RoleConverter#toPageVO(SysRole)
     */
    @Override
    public IPage<RolePageVO> page(PageRequest pageInfo) {
        return pageVO(pageInfo,
                null, // No dynamic filters
                userMapperHelper, // Batch username loader
                roleConverter::toPageVO); // Entity to VO converter
    }

    /**
     * Retrieves simplified role metadata by ID for dropdowns, selectors, or internal references.
     * <p>Uses {@link #getAndCheckById} to throw {@link com.github.starhq.template.common.exception.NotFoundException}
     * if role not found, ensuring caller receives valid VO or handles exception.</p>
     *
     * @param id the primary key of the role to retrieve; must not be {@code null}
     * @return {@link RoleSimpleVO} if found
     * @throws com.github.starhq.template.common.exception.NotFoundException if no role exists with given {@code id}
     * @see RoleSimpleVO
     * @see RoleConverter#toSimpleVO(SysRole)
     */
    @Override
    public RoleSimpleVO getRoleById(Serializable id) {
        // Fetch role with not-found check
        SysRole sysRole = getAndCheckById(id, ErrorCode.ROLE_NOT_FOUND);
        // Convert to lightweight VO
        return roleConverter.toSimpleVO(sysRole);
    }

    /**
     * Retrieves a list of roles with checked state for user-based role assignment.
     * <p>Delegates to custom mapper method that computes {@code checked} state via {@code LEFT JOIN}
     * with user-role assignments.</p>
     *
     * @param userId the primary key of the user to configure; must not be {@code null}
     * @return list of {@link RoleCheckVO} with computed {@code checked} state; never {@code null}
     * @see SysRoleMapper#selectRolesByUserId(Serializable)
     */
    @Override
    public List<RoleCheckVO> selectCheckedRoles(Serializable userId) {
        return getBaseMapper().selectRolesByUserId(userId);
    }

    /**
     * Updates an existing role definition with validation, association management, and cache invalidation.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of role update + association updates</li>
     *     <li>If any step fails, entire operation rolls back to maintain data consistency</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link RoleConverter#updateEntity}</li>
     *     <li>Update with error handling via {@link #update}</li>
     *     <li>Update role associations (resources/menus/buttons) via {@link #updateRoleAssociations}</li>
     *     <li>Cache invalidation via {@link #clearRoleCache}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param id      the primary key of the role to update; must not be {@code null}
     * @param roleDto the update data; must not be {@code null}
     * @return {@code true} if successfully updated
     * @throws com.github.starhq.template.common.exception.NotFoundException  if role not found
     * @throws com.github.starhq.template.common.exception.DuplicateException if role code conflicts
     * @see RoleDTO
     * @see #updateRoleAssociations(Long, RoleDTO)
     * @see #clearRoleCache()
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.ROLE, action = AuditLogConstant.ROLE_UPDATE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateRole(Serializable id, RoleDTO roleDto) {
        // Fetch existing role with not-found check
        SysRole role = getAndCheckById(id, ErrorCode.ROLE_NOT_FOUND);

        // Apply updates from DTO to entity
        roleConverter.updateEntity(roleDto, role);

        // Update with error handling (duplicate code, not-found, general)
        update(role, ErrorCode.ROLE_DUPLICATE_CODE, ErrorCode.ROLE_UPDATE_FAILED, ErrorCode.ROLE_NOT_FOUND);

        // Update role associations (resources/menus/buttons)
        updateRoleAssociations(role.getId(), roleDto);

        // Invalidate related caches to ensure consistency
        clearRoleCache();

        return true;
    }

    /**
     * Creates a new role definition with validation, association management, and audit logging.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of role creation + association setup</li>
     *     <li>If association setup fails, role creation rolls back to prevent orphaned data</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>DTO-to-entity conversion via {@link RoleConverter}</li>
     *     <li>Insertion with duplicate code handling via {@link #insert}</li>
     *     <li>Setup role associations (resources/menus/buttons) via {@link #updateRoleAssociations}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param roleDto the role creation data; must not be {@code null}
     * @return {@code true} if successfully created
     * @throws com.github.starhq.template.common.exception.DuplicateException if role code already exists
     * @see RoleDTO
     * @see #updateRoleAssociations(Long, RoleDTO)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.ROLE, action = AuditLogConstant.ROLE_INSERT)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createRole(RoleDTO roleDto) {
        // Convert DTO to entity
        SysRole role = roleConverter.toEntity(roleDto);

        // Insert with error handling (duplicate code, general insert failure)
        insert(role, ErrorCode.ROLE_DUPLICATE_CODE, ErrorCode.ROLE_INSERT_FAILED);

        // Setup role associations (resources/menus/buttons)
        updateRoleAssociations(role.getId(), roleDto);

        return true;
    }

    /**
     * Deletes a role definition with cascading relation cleanup and distributed cache invalidation.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of cascade delete + role delete</li>
     *     <li>If role deletion fails, relation cleanup is rolled back</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Cascading delete: Remove all role-resource, role-menu, role-button, and user-role assignments</li>
     *     <li>Role deletion via {@link #delete}</li>
     *     <li>Distributed cache invalidation via {@link #clearRoleCache}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param id the primary key of the role to delete; must not be {@code null}
     * @return {@code true} if successfully deleted
     * @see #deleteRoleRelations(Serializable)
     * @see #clearRoleCache()
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.ROLE, action = AuditLogConstant.ROLE_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        // 1. Cascading delete: Remove all role associations
        deleteRoleRelations(id);

        // 2. Delete role with error handling
        delete(id, ErrorCode.ROLE_NOT_FOUND, ErrorCode.ROLE_DELETE_FAILED);

        // 3. Invalidate related caches
        clearRoleCache();

        return true;
    }

    /**
     * Updates all permission associations for a role (resources, menus, buttons).
     * <p>
     * This method delegates to type-specific assignment methods to ensure consistent
     * validation and upsert logic across all association types.
     *
     * @param roleId  the role ID to update associations for
     * @param roleDto the DTO containing new association IDs
     * @see #assignResourcesToRole(Long, Set)
     * @see #assignMenusToRole(Long, Set)
     * @see #assignButtonsToRole(Long, Set)
     */
    private void updateRoleAssociations(Long roleId, RoleDTO roleDto) {
        assignResourcesToRole(roleId, roleDto.getResourceIds());
        assignMenusToRole(roleId, roleDto.getMenuIds());
        assignButtonsToRole(roleId, roleDto.getButtonIds());
    }

    // ==================== Association Assignment Methods ====================

    /**
     * Assigns resources to a role with validation and atomic upsert.
     * <p>
     * Uses {@link RelationHelper#assignRelations} for consistent handling:
     * <ul>
     *     <li>Validates all resource IDs exist before assignment</li>
     *     <li>Deletes existing assignments for the role</li>
     *     <li>Inserts new assignments via upsert to avoid duplicates</li>
     * </ul>
     *
     * @param roleId      the role ID
     * @param resourceIds the set of resource IDs to assign
     * @see RelationHelper#assignRelations
     * @see #validateResourceIds(Set)
     */
    private void assignResourcesToRole(Long roleId, Set<Long> resourceIds) {
        LongConsumer delete = id -> roleResourceMapper.delete(
                new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getRoleId, id)
        );

        relationHelper.assignRelations(
                roleId,
                resourceIds,
                delete,
                SysRoleResource::new,
                roleResourceMapper::upsertRoleResource,
                this::validateResourceIds,
                RelationHelper.AssociationType.RESOURCE
        );
    }

    /**
     * Assigns menus to a role with validation and atomic upsert.
     * <p>
     * Uses {@link RelationHelper#assignRelations} for consistent handling:
     * <ul>
     *     <li>Validates all menu IDs exist before assignment</li>
     *     <li>Deletes existing assignments for the role</li>
     *     <li>Inserts new assignments via upsert to avoid duplicates</li>
     * </ul>
     *
     * @param roleId  the role ID
     * @param menuIds the set of menu IDs to assign
     * @see RelationHelper#assignRelations
     * @see #validateMenuIds(Set)
     */
    private void assignMenusToRole(Long roleId, Set<Long> menuIds) {
        LongConsumer delete = id -> roleMenuMapper.delete(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id)
        );

        relationHelper.assignRelations(
                roleId,
                menuIds,
                delete,
                SysRoleMenu::new,
                roleMenuMapper::upsertRoleMenu,
                this::validateMenuIds,
                RelationHelper.AssociationType.MENU
        );
    }

    /**
     * Assigns buttons to a role with validation and atomic upsert.
     * <p>
     * Uses {@link RelationHelper#assignRelations} for consistent handling:
     * <ul>
     *     <li>Validates all button IDs exist before assignment</li>
     *     <li>Deletes existing assignments for the role</li>
     *     <li>Inserts new assignments via upsert to avoid duplicates</li>
     * </ul>
     *
     * @param roleId    the role ID
     * @param buttonIds the set of button IDs to assign
     * @see RelationHelper#assignRelations
     * @see #validateButtonIds(Set)
     */
    private void assignButtonsToRole(Long roleId, Set<Long> buttonIds) {
        LongConsumer delete = id -> roleButtonMapper.delete(
                new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, id)
        );

        relationHelper.assignRelations(
                roleId,
                buttonIds,
                delete,
                SysRoleButton::new,
                roleButtonMapper::upsertRoleButton,
                this::validateButtonIds,
                RelationHelper.AssociationType.BUTTON
        );
    }

    // ==================== Validation Methods ====================

    /**
     * Validates that all resource IDs in the set exist in the database.
     * <p>
     * Delegates to {@link RelationHelper#validateEntityExists} for consistent error handling.
     *
     * @param resourceIds the set of resource IDs to validate
     * @throws com.github.starhq.template.common.exception.NotFoundException if any resource ID does not exist
     * @see RelationHelper#validateEntityExists
     */
    private void validateResourceIds(Set<Long> resourceIds) {
        relationHelper.validateEntityExists(
                resourceIds,
                resourceMapper,
                SysResource::getId,
                RelationHelper.AssociationType.RESOURCE.getNotFoundError()
        );
    }

    /**
     * Validates that all menu IDs in the set exist in the database.
     * <p>
     * Delegates to {@link RelationHelper#validateEntityExists} for consistent error handling.
     *
     * @param menuIds the set of menu IDs to validate
     * @throws com.github.starhq.template.common.exception.NotFoundException if any menu ID does not exist
     * @see RelationHelper#validateEntityExists
     */
    private void validateMenuIds(Set<Long> menuIds) {
        relationHelper.validateEntityExists(
                menuIds,
                menuMapper,
                SysMenu::getId,
                RelationHelper.AssociationType.MENU.getNotFoundError()
        );
    }

    /**
     * Validates that all button IDs in the set exist in the database.
     * <p>
     * Delegates to {@link RelationHelper#validateEntityExists} for consistent error handling.
     *
     * @param buttonIds the set of button IDs to validate
     * @throws com.github.starhq.template.common.exception.NotFoundException if any button ID does not exist
     * @see RelationHelper#validateEntityExists
     */
    private void validateButtonIds(Set<Long> buttonIds) {
        relationHelper.validateEntityExists(
                buttonIds,
                buttonMapper,
                SysButton::getId,
                RelationHelper.AssociationType.BUTTON.getNotFoundError()
        );
    }

    // ==================== Delete Helper Methods ====================

    /**
     * Deletes all associations for a role (resources, menus, buttons, users).
     * <p>
     * This method ensures clean cascade deletion by removing all junction table entries
     * before deleting the role itself, preventing orphaned relationship records.
     *
     * @param roleId the role ID to clean associations for
     */
    private void deleteRoleRelations(Serializable roleId) {
        roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getRoleId, roleId));
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, roleId));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
    }

    /**
     * Invalidates all caches affected by role changes.
     * <p>
     * This method publishes distributed cache eviction events for:
     * <ul>
     *     <li>{@link CacheConstant#USER}: User permission caches (roles affect user permissions)</li>
     *     <li>{@link CacheConstant#BUTTON}: Button permission caches (role-button assignments changed)</li>
     *     <li>{@link CacheConstant#RESOURCE}: Resource permission caches (role-resource assignments changed)</li>
     *     <li>{@link CacheConstant#MENU}: Menu permission caches (role-menu assignments changed)</li>
     * </ul>
     * <p>
     * Uses {@link EventService#notifyCacheEvict} for multi-node cache consistency.
     *
     * @see EventService#notifyCacheEvict(List, List)
     * @see CacheConstant
     */
    private void clearRoleCache() {
        List<Serializable> keys = Collections.emptyList(); // Evict by pattern
        List<String> cacheNames = List.of(
                CacheConstant.USER,
                CacheConstant.BUTTON,
                CacheConstant.RESOURCE,
                CacheConstant.MENU
        );
        eventService.notifyCacheEvict(keys, cacheNames);
    }

}