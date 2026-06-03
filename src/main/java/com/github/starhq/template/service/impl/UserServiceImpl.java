package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.converter.UserConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.entity.SysUserRole;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.RelationHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.mapper.SysUserRoleMapper;
import com.github.starhq.template.model.dto.KeyWordPageRequest;
import com.github.starhq.template.model.dto.UserDTO;
import com.github.starhq.template.model.vo.UserPageVO;
import com.github.starhq.template.model.vo.UserSimpleVO;
import com.github.starhq.template.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.function.LongConsumer;

/**
 * Service implementation for user management with CRUD operations, role associations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link UserService} for user-specific business operations.
 * Designed to centralize user management logic with consistent validation, cache integration, and
 * distributed audit logging for compliance tracking.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>User CRUD</strong>: Create, read, update, delete user accounts with username uniqueness validation</li>
 *     <li><strong>Role Associations</strong>: Manage user-role relationships for RBAC permission control</li>
 *     <li><strong>Cache Management</strong>: Invalidate related caches (user, token, menu, resource) on user changes</li>
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
 * @see UserService
 * @see AuditBaseServiceImpl
 * @see SysUser
 * @see RelationHelper
 */
@Service("userService")
public class UserServiceImpl extends AuditBaseServiceImpl<SysUserMapper, SysUser> implements UserService {

    /**
     * Mapper for {@link SysUserRole} junction table operations (user-role assignments).
     * <p>Used for CRUD operations on user-role relationships.</p>
     *
     * @see SysUserRoleMapper
     */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * Mapper for {@link SysToken} database operations (session tokens).
     * <p>Used for cascading delete: removing user tokens when user is deleted.</p>
     *
     * @see SysTokenMapper
     */
    private final SysTokenMapper tokenMapper;

    /**
     * Mapper for {@link SysRole} database operations (role definitions).
     * <p>Used for validating role IDs when assigning roles to users.</p>
     *
     * @see SysRoleMapper
     */
    private final SysRoleMapper roleMapper;

    /**
     * Helper for efficient batch username resolution in audit field population.
     * <p>Injected into {@link AuditBaseServiceImpl#pageVO} to resolve {@code createdBy}/{@code updatedBy}
     * IDs to human-readable usernames via cached batch lookup.</p>
     *
     * @see SysUserMapperHelper
     */
    private final SysUserMapperHelper userMapperHelper;

    /**
     * Converter for transforming between {@link UserDTO}, {@link SysUser}, and various VO types.
     * <p>Ensures consistent field mapping and avoids boilerplate conversion code across service methods.</p>
     *
     * @see UserConverter
     */
    private final UserConverter userConverter;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>Ensures cache consistency when user data changes by notifying all nodes to evict
     * related cache entries for users, tokens, menus, buttons, and resources.</p>
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Helper for managing entity associations with validation, upsert, and cleanup logic.
     * <p>Provides reusable methods for assigning roles to users with existence validation
     * and atomic upsert operations.</p>
     *
     * @see RelationHelper#assignRelations
     */
    private final RelationHelper relationHelper;

    /**
     * Constructs a new {@code UserServiceImpl} with the required dependencies.
     * <p>
     * This constructor injects mappers for user state management, token lifecycle,
     * and role assignments.
     *
     * @param cacheHelper      the cache utility for batch username resolution (inherited from base class)
     * @param userRoleMapper   the mapper for managing user-role relational bindings during assignment/removal
     * @param tokenMapper      the mapper for managing user session tokens (e.g., invalidating tokens on logout/password change)
     * @param roleMapper       the mapper for querying role details during user role assignment
     * @param userMapperHelper the helper for resolving user IDs to usernames during audit field population
     * @param userConverter    the converter for transforming between user entities, DTOs, and VOs
     * @param eventService     the service for publishing domain events (e.g., user creation, cache invalidation)
     * @param relationHelper   the utility helper for abstracting complex relational batch operations
     */
    public UserServiceImpl(CacheHelper cacheHelper,
                           SysUserRoleMapper userRoleMapper,
                           SysTokenMapper tokenMapper,
                           SysRoleMapper roleMapper,
                           SysUserMapperHelper userMapperHelper,
                           UserConverter userConverter,
                           EventService eventService,
                           RelationHelper relationHelper) {
        super(cacheHelper);
        this.userRoleMapper = userRoleMapper;
        this.tokenMapper = tokenMapper;
        this.roleMapper = roleMapper;
        this.userMapperHelper = userMapperHelper;
        this.userConverter = userConverter;
        this.eventService = eventService;
        this.relationHelper = relationHelper;
    }


    /**
     * Retrieves a paginated list of user definitions with dynamic filtering and audit field resolution.
     * <p>
     * <strong>Filter Logic:</strong>
     * <ul>
     *     <li>Delegates to {@link AuditBaseServiceImpl#pageVO} for base query building</li>
     *     <li>Adds right-fuzzy match on {@code username} if {@code keyword} is present</li>
     *     <li>Batch username resolution for {@code creator}/{@code updater} fields via {@code userMapperHelper}</li>
     * </ul>
     *
     * @param pageRequest the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link UserPageVO}; never {@code null}
     * @see KeyWordPageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see UserConverter#toPageVO(SysUser)
     */
    @Override
    public IPage<UserPageVO> page(KeyWordPageRequest pageRequest) {
        return pageVO(pageRequest,
                wrapper -> {
                    // Add keyword filter on username (right-fuzzy match)
                    if (StringUtils.hasText(pageRequest.getKeyword())) {
                        wrapper.likeRight(QueryConstant.USERNAME, pageRequest.getKeyword());
                    }
                },
                userMapperHelper, // Batch username loader
                userConverter::toPageVO); // Entity to VO converter
    }

    /**
     * Retrieves simplified user metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * <strong>ID Resolution:</strong>
     * <ul>
     *     <li>If {@code id} is provided, fetches that specific user</li>
     *     <li>If {@code id} is {@code null}, defaults to current authenticated user via {@link SecurityContextUtils}</li>
     * </ul>
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>Uses {@link #getAndCheckById} to throw {@link com.github.starhq.template.common.exception.NotFoundException} if user not found</li>
     *     <li>Ensures caller receives valid {@link UserSimpleVO} or handles exception</li>
     * </ul>
     *
     * @param id the primary key of the user to retrieve; if {@code null}, uses current user ID
     * @return {@link UserSimpleVO} if found
     * @throws com.github.starhq.template.common.exception.NotFoundException if no user exists with given {@code id}
     * @see UserSimpleVO
     * @see UserConverter#toSimpleVO(SysUser)
     * @see SecurityContextUtils#getRequiredUserId()
     */
    @Override
    public UserSimpleVO getUserById(Serializable id) {
        // Default to current user if ID is not provided
        id = null != id ? id : SecurityContextUtils.getRequiredUserId();

        // Fetch user with not-found check
        SysUser user = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);
        return userConverter.toSimpleVO(user);
    }

    /**
     * Updates an existing user profile with validation, association management, and cache invalidation.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of user update + association updates</li>
     *     <li>If any step fails, entire operation rolls back to maintain data consistency</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link UserConverter#updateEntity}</li>
     *     <li>Update with error handling via {@link #update}</li>
     *     <li>Update user associations (roles) via {@link #updateUserAssociations}</li>
     *     <li>Cache invalidation via {@link #clearUserCache}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param id            the primary key of the user to update; must not be {@code null}
     * @param updateUserDto the update data; must not be {@code null}
     * @return {@code true} if successfully updated
     * @throws com.github.starhq.template.common.exception.NotFoundException  if user not found
     * @throws com.github.starhq.template.common.exception.DuplicateException if username conflicts
     * @see UserDTO
     * @see #updateUserAssociations(Long, UserDTO)
     * @see #clearUserCache(Serializable)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.USER, action = AuditLogConstant.USER_UPDATE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateUser(Serializable id, UserDTO updateUserDto) {
        // Fetch existing user with not-found check
        SysUser existingUser = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);

        // Apply updates from DTO to entity
        userConverter.updateEntity(updateUserDto, existingUser);

        // Update with error handling (duplicate username, not-found, general)
        update(existingUser, ErrorCode.USER_DUPLICATE_USERNAME, ErrorCode.USER_UPDATE_FAILED, ErrorCode.USER_NOT_FOUND);

        // Update user associations (roles)
        updateUserAssociations(existingUser.getId(), updateUserDto);

        // Invalidate related caches to ensure consistency
        clearUserCache(id);

        return true;
    }

    /**
     * Creates a new user account with validation, association management, and audit logging.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of user creation + association setup</li>
     *     <li>If association setup fails, user creation rolls back to prevent orphaned data</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>DTO-to-entity conversion via {@link UserConverter}</li>
     *     <li>Insertion with duplicate username handling via {@link #insert}</li>
     *     <li>Setup user associations (roles) via {@link #updateUserAssociations}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param userDTO the user creation data; must not be {@code null}
     * @return {@code true} if successfully created
     * @throws com.github.starhq.template.common.exception.DuplicateException if username already exists
     * @see UserDTO
     * @see #updateUserAssociations(Long, UserDTO)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.USER, action = AuditLogConstant.USER_INSERT)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createUser(UserDTO userDTO) {
        // Convert DTO to entity
        SysUser user = userConverter.toEntity(userDTO);

        // Insert with error handling (duplicate username, general insert failure)
        insert(user, ErrorCode.USER_DUPLICATE_USERNAME, ErrorCode.USER_INSERT_FAILED);

        // Setup user associations (roles)
        updateUserAssociations(user.getId(), userDTO);

        return true;
    }

    /**
     * Deletes a user account with cascading relation cleanup and distributed cache invalidation.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of cascade delete + user delete</li>
     *     <li>If user deletion fails, relation cleanup is rolled back</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Cascading delete: Remove all user-role assignments and user tokens</li>
     *     <li>User deletion via {@link #delete}</li>
     *     <li>Distributed cache invalidation via {@link #clearUserCache}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param id the primary key of the user to delete; must not be {@code null}
     * @return {@code true} if successfully deleted
     * @see #deleteUserRelations(Serializable)
     * @see #clearUserCache(Serializable)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.USER, action = AuditLogConstant.USER_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        // 1. Cascading delete: Remove all user associations and tokens
        deleteUserRelations(id);

        // 2. Delete user with error handling
        delete(id, ErrorCode.USER_NOT_FOUND, ErrorCode.USER_DELETE_FAILED);

        // 3. Invalidate related caches
        clearUserCache(id);

        return true;
    }

    /**
     * Updates all role associations for a user.
     * <p>
     * This method delegates to type-specific assignment methods to ensure consistent
     * validation and upsert logic across all association types.
     *
     * @param userId  the user ID to update associations for
     * @param userDTO the DTO containing new role IDs
     * @see #assignRolesToUser(Long, Set)
     */
    private void updateUserAssociations(Long userId, UserDTO userDTO) {
        assignRolesToUser(userId, userDTO.getRoleIds());
    }

    // ==================== Association Assignment Methods ====================

    /**
     * Assigns roles to a user with validation and atomic upsert.
     * <p>
     * Uses {@link RelationHelper#assignRelations} for consistent handling:
     * <ul>
     *     <li>Validates all role IDs exist before assignment</li>
     *     <li>Deletes existing assignments for the user</li>
     *     <li>Inserts new assignments via upsert to avoid duplicates</li>
     * </ul>
     *
     * @param userId  the user ID
     * @param roleIds the set of role IDs to assign
     * @see RelationHelper#assignRelations
     * @see #validateRoleIds(Set)
     */
    private void assignRolesToUser(Long userId, Set<Long> roleIds) {
        LongConsumer delete = id -> userRoleMapper.delete(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)
        );

        relationHelper.assignRelations(
                userId,
                roleIds,
                delete,
                SysUserRole::new,
                userRoleMapper::upsertUserRole,
                this::validateRoleIds,
                RelationHelper.AssociationType.USER
        );
    }

    // ==================== Validation Methods ====================

    /**
     * Validates that all role IDs in the set exist in the database.
     * <p>
     * Delegates to {@link RelationHelper#validateEntityExists} for consistent error handling.
     *
     * @param roleIds the set of role IDs to validate
     * @throws com.github.starhq.template.common.exception.NotFoundException if any role ID does not exist
     * @see RelationHelper#validateEntityExists
     */
    private void validateRoleIds(Set<Long> roleIds) {
        relationHelper.validateEntityExists(
                roleIds,
                roleMapper,
                SysRole::getId,
                ErrorCode.ROLE_NOT_FOUND
        );
    }

    // ==================== Delete Helper Methods ====================

    /**
     * Deletes all associations and tokens for a user.
     * <p>
     * This method ensures clean cascade deletion by removing all junction table entries
     * and session tokens before deleting the user itself, preventing orphaned records.
     *
     * @param userId the user ID to clean associations for
     */
    private void deleteUserRelations(Serializable userId) {
        // Remove user-role assignments
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        // Remove user session tokens
        tokenMapper.delete(new LambdaQueryWrapper<SysToken>().eq(SysToken::getUserId, userId));
    }

    /**
     * Invalidates all caches affected by user changes.
     * <p>
     * This method publishes distributed cache eviction events for:
     * <ul>
     *     <li>{@link CacheConstant#ADMIN_NAME}: Username resolution cache</li>
     *     <li>{@link CacheConstant#TOKEN}: User session token cache</li>
     *     <li>{@link CacheConstant#USER}: User metadata cache</li>
     *     <li>{@link CacheConstant#BUTTON}: Button permission cache (affected by role changes)</li>
     *     <li>{@link CacheConstant#RESOURCE}: Resource permission cache (affected by role changes)</li>
     *     <li>{@link CacheConstant#MENU}: Menu permission cache (affected by role changes)</li>
     * </ul>
     * <p>
     * Uses {@link EventService#notifyCacheEvict} for multi-node cache consistency.
     *
     * @param userId the user ID whose caches should be invalidated
     * @see EventService#notifyCacheEvict(List, List)
     * @see CacheConstant
     */
    private void clearUserCache(Serializable userId) {
        List<Serializable> userIds = List.of(userId);
        List<String> cacheNames = List.of(
                CacheConstant.ADMIN_NAME,
                CacheConstant.TOKEN,
                CacheConstant.USER,
                CacheConstant.BUTTON,
                CacheConstant.RESOURCE,
                CacheConstant.MENU
        );
        eventService.notifyCacheEvict(userIds, cacheNames);
    }

}