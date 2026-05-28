package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.converter.ButtonConverter;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.entity.SysRoleButton;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysButtonMapper;
import com.github.starhq.template.mapper.SysMenuMapper;
import com.github.starhq.template.mapper.SysRoleButtonMapper;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;
import com.github.starhq.template.service.ButtonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service implementation for button/permission management with CRUD operations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link ButtonService} for button-specific business operations.
 * Designed to centralize button management logic with consistent validation, cache integration, and
 * distributed event handling for multi-node consistency.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>Button CRUD</strong>: Create, read, update, delete button definitions with duplicate prevention</li>
 *     <li><strong>Permission Queries</strong>: Resolve user permissions by fetching assigned button codes</li>
 *     <li><strong>Role Configuration</strong>: Provide buttons with checked state for role-based permission assignment</li>
 *     <li><strong>Cache Management</strong>: Integrate with Spring Cache and distributed events for consistent button metadata</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Cache-First</strong>: Frequent read operations leverage {@code @Cacheable} for performance</li>
 *     <li><strong>Audit-Ready</strong>: All write operations annotated with {@code @AuditLoggable} for compliance tracking</li>
 *     <li><strong>Transaction-Safe</strong>: Critical operations use {@code @Transactional} for atomicity</li>
 *     <li><strong>Null-Safe</strong>: Uses {@link CollectionUtils} and {@link Objects} to prevent NPE in security-critical paths</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Expose button management endpoints
 * @RestController
 * @RequestMapping("/api/v1/buttons")
 * @RequiredArgsConstructor
 * public class ButtonController {
 *
 *     private final ButtonService buttonService;
 *
 *     @GetMapping
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public Result<IPage<ButtonPageVO>> listButtons(ButtonPageRequest request) {
 *         IPage<ButtonPageVO> page = buttonService.page(request);
 *         return Result.success(page.getRecords(), page.getTotal());
 *     }
 *
 *     @GetMapping("/user/{userId}")
 *     public Result<List<String>> getUserButtons(@PathVariable Long userId) {
 *         List<String> codes = buttonService.select(userId);
 *         return Result.success(codes);
 *     }
 * }
 *
 * // Frontend: Use button codes for dynamic UI rendering
 * const hasPermission = (code: string) => userPermissions.value.includes(code);
 * <a-button v-if="hasPermission('user:create')">Create User</a-button>
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-03
 * @see ButtonService
 * @see AuditBaseServiceImpl
 * @see SysButton
 * @see ButtonPageVO
 * @see ButtonCheckVO
 */
@Slf4j
@Service("buttonService")
public class ButtonServiceImpl extends AuditBaseServiceImpl<SysButtonMapper, SysButton> implements ButtonService {

    /**
     * Mapper for {@link SysMenu} database operations (menu hierarchy validation).
     * <p>
     * Used to validate that button's {@code menuId} references an existing menu
     * before insertion or update, ensuring referential integrity.
     *
     * @see SysMenuMapper
     */
    private final SysMenuMapper menuMapper;

    /**
     * Mapper for {@link SysRoleButton} junction table operations (role-button assignments).
     * <p>
     * Used for:
     * <ul>
     *     <li>Fetching buttons with checked state for role configuration ({@link #selectCheckedButtons})</li>
     *     <li>Cascading delete: removing role-button assignments when button is deleted ({@link #removeById})</li>
     * </ul>
     *
     * @see SysRoleButtonMapper
     */
    private final SysRoleButtonMapper roleButtonMapper;

    /**
     * Helper for efficient batch username resolution in audit field population.
     * <p>
     * Injected into {@link AuditBaseServiceImpl#pageVO} to resolve {@code createdBy}/{@code updatedBy}
     * IDs to human-readable usernames via cached batch lookup, avoiding N+1 queries.
     *
     * @see SysUserMapperHelper
     * @see AuditBaseServiceImpl#pageVO
     */
    private final SysUserMapperHelper userMapperHelper;

    /**
     * Converter for transforming between {@link ButtonDTO}, {@link SysButton}, and various VO types.
     * <p>
     * Ensures consistent field mapping and avoids boilerplate conversion code across service methods.
     *
     * @see ButtonConverter
     */
    private final ButtonConverter buttonConverter;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>
     * Ensures cache consistency when button data changes (e.g., update, delete) by notifying
     * all nodes to evict related cache entries.
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Constructs a new {@code ButtonServiceImpl} with the required dependencies.
     *
     * @param cacheHelper      the cache utility for batch username resolution (inherited from base class)
     * @param menuMapper       the mapper for querying menu associations to validate button existence
     * @param roleButtonMapper the mapper for managing role-button relationships and authorization rules
     * @param userMapperHelper the helper for resolving user IDs to usernames during audit field population
     * @param buttonConverter  the converter for transforming between entities, DTOs, and VOs
     * @param eventService     the service for publishing domain events (e.g., cache invalidation triggers)
     */
    public ButtonServiceImpl(CacheHelper cacheHelper,
                             SysMenuMapper menuMapper,
                             SysRoleButtonMapper roleButtonMapper,
                             SysUserMapperHelper userMapperHelper,
                             ButtonConverter buttonConverter,
                             EventService eventService) {
        super(cacheHelper);
        this.menuMapper = menuMapper;
        this.roleButtonMapper = roleButtonMapper;
        this.userMapperHelper = userMapperHelper;
        this.buttonConverter = buttonConverter;
        this.eventService = eventService;
    }

    /**
     * Retrieves a paginated list of button definitions with dynamic filtering and audit field resolution.
     * <p>
     * This method delegates to {@link AuditBaseServiceImpl#pageVO} to provide:
     * <ul>
     *     <li>Base query building from {@link ButtonPageRequest}</li>
     *     <li>Dynamic menu ID filter via {@code Consumer<QueryWrapper>} callback</li>
     *     <li>Batch username resolution for {@code creator}/{@code updater} fields</li>
     *     <li>Entity-to-VO conversion via {@link ButtonConverter#toPageVO}</li>
     * </ul>
     * <p>
     * <strong>Filter Logic:</strong>
     * <pre>
     * {@code
     * // Apply menu ID filter if specified
     * if (pageInfo.getMenuId() != null) {
     *     wrapper.eq("menu_id", pageInfo.getMenuId());
     *     // SQL: AND menu_id = 1001
     * }
     * }
     * </pre>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} with current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches</li>
     *     <li><strong>Audit Fields</strong>: All returned VOs have {@code creator}/{@code updater} populated with usernames</li>
     * </ul>
     * <p>
     * <strong>Performance Optimizations:</strong>
     * <ul>
     *     <li><strong>Batch Resolution</strong>: Collect all audit IDs first, then single batch lookup via {@code userMapperHelper}</li>
     *     <li><strong>Cache-First</strong>: Username resolution leverages {@link CacheHelper} for efficient caching</li>
     *     <li><strong>Early Return</strong>: Skip username resolution if page is empty to avoid unnecessary work</li>
     * </ul>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link ButtonPageVO} matching the criteria; never {@code null}
     * @see ButtonPageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see ButtonConverter#toPageVO(SysButton)
     */
    @Override
    public IPage<ButtonPageVO> page(ButtonPageRequest pageInfo) {
        return pageVO(pageInfo,
                // Dynamic filter: add menu ID condition if specified
                wrapper -> {
                    if (!Objects.isNull(pageInfo.getMenuId())) {
                        wrapper.eq(QueryConstant.MENU_ID, pageInfo.getMenuId());
                    }
                },
                // Batch username loader for audit field resolution
                userMapperHelper,
                // Entity to VO converter
                buttonConverter::toPageVO);
    }

    /**
     * Retrieves the list of permission codes (button codes) accessible to a specific user.
     * <p>
     * This method resolves a user's effective permissions by:
     * <ol>
     *     <li>Fetching all buttons assigned to the user's roles via {@code selectAssignedButtonsByUserId}</li>
     *     <li>Extracting unique {@code code} values for frontend permission checks</li>
     *     <li>Caching results by {@code userId} for repeated permission checks</li>
     * </ol>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * Annotated with {@code @Cacheable(value = "buttons", key = "#p0")} to:
     * <ul>
     *     <li>Cache permission codes by {@code userId} to reduce database load during repeated auth checks</li>
     *     <li>Use {@code userId} as cache key for precise invalidation when user roles change</li>
     *     <li>Return empty list for cache misses to avoid caching null results</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of unique permission codes (e.g., {@code ["user:create", "user:delete"]})</li>
     *     <li><strong>No Permissions</strong>: Returns empty list {@code Collections.emptyList()} if user has no assigned buttons</li>
     *     <li><strong>Null Safety</strong>: Never returns {@code null}; empty list indicates no permissions</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Store permissions in Pinia store
     * const userStore = useUserStore();
     * const permissions = ref<string[]>([]);
     *
     * // Load permissions on login
     * const loadPermissions = async (userId: number) => {
     *   const { data } = await api.getUserButtons(userId);
     *   permissions.value = data;
     *   userStore.setPermissions(data);
     * };
     *
     * // Usage in template: Dynamic button rendering
     * <a-button v-if="permissions.includes('user:create')">
     *   Create User
     * </a-button>
     *
     * // Usage in logic: Programmatic permission check
     * const canDelete = () => permissions.value.includes('user:delete');
     * }
     * </pre>
     * <p>
     * <strong>Cache Invalidation:</strong>
     * <p>
     * Cache entries should be evicted when:
     * <ul>
     *     <li>User roles are modified (via {@code RoleService})</li>
     *     <li>Role-button assignments change (via {@code RoleButtonService})</li>
     *     <li>Button definitions are updated/deleted (via {@code EventService.notifyCacheEvict})</li>
     * </ul>
     *
     * @param userId the primary key of the user to query; must not be {@code null}
     * @return list of unique permission codes accessible to the user; never {@code null}
     * @see SysButton#getCode()
     * @see Cacheable
     * @see CacheConstant#BUTTON
     */
    @Cacheable(value = "buttons", key = "#p0")
    @Override
    public List<String> select(Serializable userId) {
        // Fetch assigned buttons via custom mapper method (avoids N+1 query)
        List<SysButton> buttons = getBaseMapper().selectAssignedButtonsByUserId(userId);

        // Return empty list if no buttons assigned (never return null)
        if (CollectionUtils.isEmpty(buttons)) {
            return Collections.emptyList();
        }

        // Extract unique permission codes for frontend checks
        return buttons.stream().map(SysButton::getCode).toList();
    }

    /**
     * Retrieves simplified button metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full button queries when only
     * basic identification fields ({@code id}, {@code name}, {@code code}) are needed.
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>Uses {@link #getAndCheckById} to throw {@link NotFoundException} if button not found</li>
     *     <li>Ensures caller receives valid {@link ButtonSimpleVO} or handles exception</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch button for dropdown population
     * ButtonSimpleVO button = buttonService.getButtonById(2001L);
     * // Use for UI rendering
     * dropdownOptions.add(new SelectOption(button.getId(), button.getName()));
     *
     * // Frontend: Populate select with button options
     * const { data: buttons } = useRequest(() => api.getButtonOptions());
     * const buttonOptions = computed(() =>
     *   buttons.value?.map(btn => ({ label: btn.name, value: btn.id })) || []
     * );
     * }
     * </pre>
     *
     * @param id the primary key of the button to retrieve; must not be {@code null}
     * @return {@link ButtonSimpleVO} if found
     * @throws NotFoundException if no button exists with given {@code id}
     * @see ButtonSimpleVO
     * @see ButtonConverter#toSimpleVO(SysButton)
     * @see #getAndCheckById(Serializable, ErrorCode)
     */
    @Override
    public ButtonSimpleVO getButtonById(Serializable id) {
        // Fetch button with not-found check (throws NotFoundException if missing)
        SysButton button = getAndCheckById(id, ErrorCode.BUTTON_NOT_FOUND);

        // Convert to lightweight VO for UI rendering
        return buttonConverter.toSimpleVO(button);
    }

    /**
     * Retrieves a list of buttons with checked state for role-based permission configuration.
     * <p>
     * This method supports the "assign buttons to role" workflow in admin consoles by:
     * <ol>
     *     <li>Fetching all available buttons (typically filtered by status=enabled)</li>
     *     <li>Joining with role-button assignments to determine {@code checked} state</li>
     *     <li>Returning {@link ButtonCheckVO} with {@code id}, {@code name}, {@code checked} fields</li>
     * </ol>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link ButtonCheckVO} with pre-computed {@code checked} flags</li>
     *     <li><strong>Empty Result</strong>: Returns empty list if no buttons exist or role has no assignments</li>
     *     <li><strong>Ordering</strong>: Results typically sorted by {@code menu_id}, {@code sort_order} for UI consistency</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Checkbox group for role permission assignment
     * <a-checkbox-group v-model="checkedButtonIds">
     *   <a-checkbox
     *     v-for="btn in buttons"
     *     :key="btn.id"
     *     :value="btn.id"
     *     :checked="btn.checked"
     *   >
     *     {{ btn.name }} <small class="text-gray-500">({{ btn.code }})</small>
     *   </a-checkbox>
     * </a-checkbox-group>
     *
     * // Submit: Send only changed button IDs to backend
     * const handleSubmit = async () => {
     *   const added = checkedButtonIds.value.filter(id => !initialIds.includes(id));
     *   const removed = initialIds.filter(id => !checkedButtonIds.value.includes(id));
     *
     *   await api.updateRoleButtons(roleId.value, { added, removed });
     * };
     * }
     * </pre>
     *
     * @param roleId the primary key of the role to configure; must not be {@code null}
     * @return list of {@link ButtonCheckVO} with computed {@code checked} state; never {@code null}
     * @see ButtonCheckVO
     * @see SysButtonMapper#selectButtonsByRoleId(Serializable)
     */
    @Override
    public List<ButtonCheckVO> selectCheckedButtons(Serializable roleId) {
        // Delegate to custom mapper method that computes checked state via LEFT JOIN + CASE WHEN
        return getBaseMapper().selectButtonsByRoleId(roleId);
    }

    /**
     * Creates a new button definition with validation, duplicate prevention, and audit logging.
     * <p>
     * This method handles the complete button creation workflow including:
     * <ol>
     *     <li>Menu reference validation via {@link #validateMenu}</li>
     *     <li>DTO-to-entity conversion via {@link ButtonConverter}</li>
     *     <li>Insertion with duplicate key handling via {@link #insert}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_INSERT)} to:
     * <ul>
     *     <li>Automatically record button creation events for compliance tracking</li>
     *     <li>Capture operator ID, timestamp, and operation details in audit log</li>
     *     <li>Enable audit trail queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Menu Validation</strong>: Ensures button references existing menu to prevent orphaned permissions</li>
     *     <li><strong>Duplicate Prevention</strong>: {@link #insert} catches {@code DuplicateKeyException} and throws business-friendly error</li>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #insert} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Button insertion either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Audit log entry is recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same code are properly serialized by database</li>
     * </ul>
     *
     * @param buttonDto the button creation data; must not be {@code null}
     * @return {@code true} if button was successfully created
     * @throws IllegalArgumentException                                       if {@code buttonDto} is {@code null} or missing required fields
     * @throws NotFoundException                                              if referenced menu does not exist
     * @throws com.github.starhq.template.common.exception.DuplicateException if {@code code} already exists
     * @see ButtonDTO
     * @see #validateMenu(ButtonDTO)
     * @see #insert(Object, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_INSERT)
    @Override
    public boolean createButton(ButtonDTO buttonDto) {
        // Validate that referenced menu exists (prevent orphaned buttons)
        validateMenu(buttonDto);

        // Convert DTO to entity for persistence
        SysButton button = buttonConverter.toEntity(buttonDto);

        // Insert with business error handling (duplicate code → BUTTON_DUPLICATE_CODE)
        insert(button, ErrorCode.BUTTON_DUPLICATE_CODE, ErrorCode.BUTTON_INSERT_FAILED);

        return true;
    }

    /**
     * Updates an existing button definition with validation, conflict prevention, and cache invalidation.
     * <p>
     * This method handles the complete button update workflow including:
     * <ol>
     *     <li>Menu reference validation via {@link #validateMenu}</li>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link ButtonConverter#updateEntity}</li>
     *     <li>Update with comprehensive error handling via {@link #update}</li>
     *     <li>Cache invalidation via {@code cacheHelper.clear}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidates button metadata cache to ensure consistency:
     * <pre>
     * {@code
     * // Clear all button-related cache entries (by pattern)
     * cacheHelper.clear(CacheConstant.BUTTON);
     *
     * // Alternative: Precise invalidation by button ID
     * // cacheHelper.evict(List.of(id), List.of(CacheConstant.BUTTON));
     * }
     * </pre>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_UPDATE)} to:
     * <ul>
     *     <li>Automatically record button modification events for compliance tracking</li>
     *     <li>Capture old/new values, operator ID, and timestamp in audit log</li>
     *     <li>Enable change history queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #update} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same button are properly serialized</li>
     * </ul>
     *
     * @param id        the primary key of the button to update; must not be {@code null}
     * @param buttonDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if button was successfully updated
     * @throws IllegalArgumentException                                       if {@code id} or {@code buttonDto} is {@code null}
     * @throws NotFoundException                                              if no button exists with given {@code id} or referenced menu not found
     * @throws com.github.starhq.template.common.exception.DuplicateException if new {@code code} conflicts with existing button
     * @see ButtonDTO
     * @see #validateMenu(ButtonDTO)
     * @see #getAndCheckById(Serializable, ErrorCode)
     * @see #update(Object, ErrorCode, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_UPDATE)
    @Override
    public boolean updateButton(Serializable id, ButtonDTO buttonDto) {
        // Validate that referenced menu exists
        validateMenu(buttonDto);

        // Fetch existing button with not-found check
        SysButton button = getAndCheckById(id, ErrorCode.BUTTON_NOT_FOUND);

        // Apply updates from DTO to entity (partial update support)
        buttonConverter.updateEntity(buttonDto, button);

        // Update with comprehensive error handling (duplicate/not-found/general)
        update(button, ErrorCode.BUTTON_DUPLICATE_CODE, ErrorCode.BUTTON_UPDATE_FAILED, ErrorCode.BUTTON_NOT_FOUND);

        // Invalidate button metadata cache to ensure consistency across nodes
        cacheHelper.clear(CacheConstant.BUTTON);

        return true;
    }

    /**
     * Deletes a button definition with cascading role-button cleanup and distributed cache invalidation.
     * <p>
     * This method handles the complete button deletion workflow including:
     * <ol>
     *     <li>Cascading delete: Remove all role-button assignments for this button</li>
     *     <li>Button deletion with not-found handling via {@link #delete}</li>
     *     <li>Distributed cache invalidation via {@link EventService}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Annotated with {@code @Transactional(rollbackFor = Exception.class)} to ensure:
     * <ul>
     *     <li>Atomicity: Role-button cleanup and button deletion succeed or fail together</li>
     *     <li>Consistency: If button deletion fails, role-button assignments are rolled back</li>
     *     <li>Isolation: Concurrent deletions of same button are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cascading Delete Strategy:</strong>
     * <pre>
     * {@code
     * // Remove all role-button assignments for this button
     * roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>()
     *     .eq(SysRoleButton::getButtonId, id));
     *
     * // Then delete the button itself
     * delete(id, ErrorCode.BUTTON_NOT_FOUND, ErrorCode.BUTTON_DELETE_FAILED);
     * }
     * </pre>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful deletion, publishes distributed cache eviction event:
     * <pre>
     * {@code
     * // Notify all nodes to evict button-related cache entries
     * eventService.notifyCacheEvict(
     *     Collections.emptyList(),        // No specific keys (evict by pattern)
     *     List.of(CacheConstant.BUTTON)   // Cache region to evict
     * );
     * }
     * </pre>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_REMOVE)} to:
     * <ul>
     *     <li>Automatically record button deletion events for compliance tracking</li>
     *     <li>Capture deleted button details, operator ID, and timestamp in audit log</li>
     *     <li>Enable deletion history queries in admin console for security analysis</li>
     * </ul>
     *
     * @param id the primary key of the button to delete; must not be {@code null}
     * @return {@code true} if button was successfully deleted
     * @throws IllegalArgumentException                                      if {@code id} is {@code null}
     * @throws com.github.starhq.template.common.exception.BusinessException if deletion fails due to foreign key constraints
     * @see #delete(Serializable, ErrorCode, ErrorCode)
     * @see EventService#notifyCacheEvict(List, List)
     * @see AuditLoggable
     */
    @Transactional(rollbackFor = Exception.class)
    @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_REMOVE)
    @Override
    public boolean removeById(Serializable id) {
        // 1. Cascading delete: Remove all role-button assignments for this button
        roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getButtonId, id));

        // 2. Delete button with business error handling
        delete(id, ErrorCode.BUTTON_NOT_FOUND, ErrorCode.BUTTON_DELETE_FAILED);

        // 3. Publish distributed cache eviction event for multi-node consistency
        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.BUTTON));

        return true;
    }

    // ====================== Private Helper Methods ======================

    /**
     * Validates that the button's referenced menu exists in the database.
     * <p>
     * This method enforces referential integrity by ensuring that every button
     * is associated with a valid parent menu, preventing orphaned permission definitions.
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * // Check if menu with given ID exists
     * boolean exists = menuMapper.exists(
     *     new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getId, buttonDto.getMenuId())
     * );
     *
     * // Throw not-found exception if menu does not exist
     * if (!exists) {
     *     throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from {@link #createButton} and {@link #updateButton} before persistence
     * to ensure data integrity. Not intended for public API exposure.
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li><strong>Menu Not Found</strong>: Throws {@link NotFoundException} with {@link ErrorCode#MENU_NOT_FOUND}</li>
     *     <li><strong>Null Safety</strong>: Assumes {@code buttonDto.getMenuId()} is non-null (validated at DTO layer)</li>
     * </ul>
     *
     * @param buttonDto the button DTO containing {@code menuId} to validate; must not be {@code null}
     * @throws NotFoundException if referenced menu does not exist
     * @see ErrorCode#MENU_NOT_FOUND
     */
    private void validateMenu(ButtonDTO buttonDto) {
        // Check if menu with given ID exists (enforce foreign key constraint at application level)
        boolean exists = menuMapper.exists(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getId, buttonDto.getMenuId()));

        // Throw not-found exception if menu does not exist
        if (!exists) {
            throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
        }
    }

}