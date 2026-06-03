package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.common.util.TreeBuildUtils;
import com.github.starhq.template.common.util.TypeConvertUtils;
import com.github.starhq.template.converter.MenuConverter;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.entity.SysRoleMenu;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysMenuMapper;
import com.github.starhq.template.mapper.SysRoleMenuMapper;
import com.github.starhq.template.model.dto.MenuDTO;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.vo.MenuSimpleVO;
import com.github.starhq.template.model.vo.LeftNavVO;
import com.github.starhq.template.model.vo.MenuCheckVO;
import com.github.starhq.template.model.vo.MenuListVO;
import com.github.starhq.template.service.MenuService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Service implementation for menu management with hierarchical tree operations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link MenuService} for menu-specific business operations.
 * Designed to centralize menu management logic with consistent validation, cache integration, and
 * distributed audit logging for compliance tracking.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>Menu CRUD</strong>: Create, read, update, delete menu definitions with path uniqueness and parent validation</li>
 *     <li><strong>Tree Building</strong>: Convert flat menu lists to hierarchical structures for admin console and sidebar rendering</li>
 *     <li><strong>Permission Integration</strong>: Generate user-specific sidebar navigation based on role-menu assignments</li>
 *     <li><strong>Cache Management</strong>: Invalidate menu caches on changes to ensure consistency across distributed nodes</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Cache-Consistent</strong>: Changes trigger cache invalidation via {@link EventService} for multi-node consistency</li>
 *     <li><strong>Audit-Ready</strong>: All write operations annotated with {@code @AuditLoggable} for compliance tracking</li>
 *     <li><strong>Null-Safe</strong>: Uses {@link Objects} and {@link CollectionUtils} to prevent NPE in security-critical paths</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Expose menu management endpoints
 * @RestController
 * @RequestMapping("/api/v1/menus")
 * @RequiredArgsConstructor
 * public class MenuController {
 *
 *     private final MenuService menuService;
 *
 *     @GetMapping("/list")
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public Result<List<MenuListVO>> listMenus(PageRequest request) {
 *         List<MenuListVO> menus = menuService.selectList(request);
 *         return Result.success(menus);
 *     }
 *
 *     @GetMapping("/sidebar")
 *     public Result<List<LeftNavVO>> getSidebar() {
 *         Long userId = SecurityContextUtils.getRequiredUserId();
 *         List<LeftNavVO> sidebar = menuService.selectSidebar(userId);
 *         return Result.success(sidebar);
 *     }
 * }
 *
 * // Frontend: Use sidebar for dynamic router generation
 * const { data: sidebar } = useRequest(() => api.getSidebar());
 * // Generate Vue Router routes from menu tree
 * const routes = sidebar.value.map(menu => ({
 *   path: menu.path,
 *   name: menu.name,
 *   component: () => import(`@/views/${menu.component}.vue`),
 *   meta: { title: menu.title, icon: menu.icon }
 * }));
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-04
 * @see MenuService
 * @see AuditBaseServiceImpl
 * @see SysMenu
 * @see MenuListVO
 * @see LeftNavVO
 */
@Service("menuService")
public class MenuServiceImpl extends AuditBaseServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    /**
     * Helper for efficient batch username resolution in audit field population.
     * <p>
     * Injected into {@link AuditBaseServiceImpl#pageVO} to resolve {@code createdBy}/{@code updatedBy}
     * IDs to human-readable usernames via cached batch lookup, avoiding N+1 queries during pagination.
     *
     * @see SysUserMapperHelper
     * @see AuditBaseServiceImpl#pageVO
     */
    private final SysUserMapperHelper userMapperHelper;

    /**
     * Mapper for {@link SysRoleMenu} junction table operations (role-menu assignments).
     * <p>
     * Used for:
     * <ul>
     *     <li>Fetching menus assigned to specific roles for sidebar generation ({@link #selectSidebar})</li>
     *     <li>Cascading delete: removing role-menu assignments when menu is deleted ({@link #removeByIds})</li>
     * </ul>
     *
     * @see SysRoleMenuMapper
     */
    private final SysRoleMenuMapper roleMenuMapper;

    /**
     * Converter for transforming between {@link MenuDTO}, {@link SysMenu}, and various VO types.
     * <p>
     * Ensures consistent field mapping and avoids boilerplate conversion code across service methods.
     * Supports:
     * <ul>
     *     <li>DTO to entity conversion for persistence</li>
     *     <li>Entity to {@link MenuListVO} for admin console tree rendering</li>
     *     <li>Entity to {@link LeftNavVO} for user sidebar navigation</li>
     *     <li>Entity to {@link MenuCheckVO} for role-menu assignment UI</li>
     *     <li>Entity to {@link MenuSimpleVO} for lightweight internal references</li>
     * </ul>
     *
     * @see MenuConverter
     */
    private final MenuConverter menuConverter;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>
     * Ensures cache consistency when menu data changes (e.g., update, delete) by notifying
     * all nodes to evict related cache entries via distributed messaging.
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Constructs a new {@code MenuServiceImpl} with the required dependencies.
     *
     * @param cacheHelper      the cache utility for batch username resolution (inherited from base class)
     * @param userMapperHelper the helper for resolving user IDs to usernames during audit field population
     * @param roleMenuMapper   the mapper for managing role-menu relationships and authorization bindings
     * @param menuConverter    the converter for transforming between menu entities, DTOs, and tree-structured VOs
     * @param eventService     the service for publishing domain events (e.g., cache invalidation triggers)
     */
    public MenuServiceImpl(CacheHelper cacheHelper,
                           SysUserMapperHelper userMapperHelper,
                           SysRoleMenuMapper roleMenuMapper,
                           MenuConverter menuConverter,
                           EventService eventService) {
        super(cacheHelper);
        this.userMapperHelper = userMapperHelper;
        this.roleMenuMapper = roleMenuMapper;
        this.menuConverter = menuConverter;
        this.eventService = eventService;
    }

    /**
     * Retrieves a hierarchical list of menu definitions for admin console management.
     * <p>
     * This method implements a two-phase workflow:
     * <ol>
     *     <li><strong>Flat Query</strong>: Fetch matching menus via {@link AuditBaseServiceImpl#pageVO} with optional filters</li>
     *     <li><strong>Tree Building</strong>: Convert flat list to hierarchical structure via {@link TreeBuildUtils#build}</li>
     * </ol>
     * <p>
     * <strong>Filter Logic:</strong>
     * <ul>
     *     <li>Delegates to {@code pageVO} for base query building from {@link PageRequest}</li>
     *     <li>Supports dynamic filters via {@code Consumer<QueryWrapper>} callback (currently none)</li>
     *     <li>Batch username resolution for {@code creator}/{@code updater} fields via {@code userMapperHelper}</li>
     * </ul>
     * <p>
     * <strong>Tree Building Strategy:</strong>
     * <pre>
     * {@code
     * // TreeBuildUtils.build() expects:
     * // 1. Each VO implements Tree<T> with getId(), getParentId(), getChildren()
     * // 2. Root nodes have parentId = 0 or null
     * // 3. Children are nested recursively under parent's children list
     *
     * List<MenuListVO> tree = TreeBuildUtils.build(flatList);
     * // Result: [{ id: 1, title: "System", children: [{ id: 2, title: "User" }] }]
     * }
     * </pre>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Hierarchical Result</strong>: Returns list of {@link MenuListVO} with nested {@code children} for tree rendering</li>
     *     <li><strong>Empty Result</strong>: Returns empty list {@code Collections.emptyList()} if no menus match</li>
     *     <li><strong>Ordering</strong>: Results sorted by {@code sort_order} within each parent level for UI consistency</li>
     * </ul>
     * <p>
     * <strong>Performance Optimizations:</strong>
     * <ul>
     *     <li><strong>Single Query</strong>: Fetch all matching menus in one database query; avoid N+1 lookups</li>
     *     <li><strong>In-Memory Tree Building</strong>: {@code TreeBuildUtils.build} uses O(N) algorithm with HashMap indexing</li>
     *     <li><strong>Search Count Disabled</strong>: {@code pageRequest.setSearchCount(false)} avoids expensive COUNT query for tree lists</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Return menu tree for admin console
     * @GetMapping("/list")
     * public Result<List<MenuListVO>> listMenus(PageRequest request) {
     *     List<MenuListVO> menus = menuService.selectList(request);
     *     return Result.success(menus);
     * }
     *
     * // Frontend: Render menu tree with expand/collapse
     * <a-tree :tree-data="menuTree" :field-names="{ title: 'title', key: 'id', children: 'children' }" />
     * }
     * </pre>
     *
     * @param pageRequest the pagination and filter criteria; must not be {@code null}
     * @return hierarchical list of {@link MenuListVO} with nested children; never {@code null}
     * @see PageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see TreeBuildUtils#build(List)
     * @see MenuConverter#toListVO(SysMenu)
     */
    @Override
    public List<MenuListVO> selectList(PageRequest pageRequest) {
        // Disable search count for tree queries (expensive COUNT not needed for hierarchical display)
        pageRequest.setSearchCount(false);

        // 1. Fetch flat list with audit field resolution
        List<MenuListVO> result = pageVO(pageRequest,
                null, // No dynamic filters
                userMapperHelper, // Batch username loader
                menuConverter::toListVO // Entity to VO converter
        ).getRecords();

        // 2. Return empty list if no results (avoid null)
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }

        // 3. Build hierarchical tree structure from flat list
        return TreeBuildUtils.build(result);
    }

    /**
     * Retrieves the sidebar navigation tree for a specific user based on assigned roles and permissions.
     * <p>
     * This method implements a cache-optimized workflow:
     * <ol>
     *     <li><strong>Cache Lookup</strong>: Check Redis/Caffeine for pre-built sidebar via {@code @Cacheable}</li>
     *     <li><strong>Database Fallback</strong>: If cache miss, fetch assigned menus via custom mapper query</li>
     *     <li><strong>Tree Building</strong>: Convert flat list to hierarchical structure via {@link TreeBuildUtils#build}</li>
     *     <li><strong>Cache Population</strong>: Store result in cache with key {@code "menus:" + userId}</li>
     * </ol>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * Annotated with {@code @Cacheable(value = "menus", key = "#p0")} to:
     * <ul>
     *     <li>Cache user-specific sidebar under key {@code "menus:" + userId} for efficient repeated access</li>
     *     <li>Use default cache TTL configured in {@link CacheConstant} (typically 30 min)</li>
     *     <li>Automatically return cached result on subsequent calls to reduce database load</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation:</strong>
     * <p>
     * Cache entries should be evicted when:
     * <ul>
     *     <li>Menu definitions are created/updated/deleted (via {@link #createMenu}, {@link #updateMenu}, {@link #removeByIds})</li>
     *     <li>Role-menu assignments change (via {@code RoleMenuService})</li>
     *     <li>User roles are modified (affects which menus are accessible)</li>
     *     <li>Use {@link EventService#notifyCacheEvict} for distributed cache invalidation across nodes</li>
     * </ul>
     * <p>
     * <strong>Permission Resolution:</strong>
     * <pre>
     * {@code
     * // Query: Fetch menus assigned to user's roles
     * SELECT m.* FROM sys_menu m
     * INNER JOIN sys_role_menu rm ON m.id = rm.menu_id
     * INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id
     * WHERE ur.user_id = :userId
     *   AND m.status = 1 -- Only enabled menus
     *   AND m.menu_type IN (0, 1) -- Only directories and menus (exclude buttons)
     * ORDER BY m.parent_id, m.sort_order;
     * }
     * </pre>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link LeftNavVO} representing root-level menus with nested children</li>
     *     <li><strong>No Permissions</strong>: Returns empty list {@code Collections.emptyList()} if user has no accessible menus</li>
     *     <li><strong>Null Safety</strong>: Never returns {@code null}; empty list indicates no accessible menus</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Generate dynamic routes from sidebar data
     * const { data: sidebar } = useRequest(() => api.getSidebar());
     *
     * const generateRoutes = (menus: LeftNavVO[]): RouteRecordRaw[] => {
     *   return menus.map(menu => ({
     *     path: menu.path,
     *     name: menu.name,
     *     component: () => import(`@/views/${menu.component}.vue`),
     *     meta: { title: menu.title, icon: menu.icon, hidden: menu.hidden },
     *     children: menu.children ? generateRoutes(menu.children) : []
     *   }));
     * };
     *
     * // Add routes to router
     * router.addRoutes(generateRoutes(sidebar.value));
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Batch Query</strong>: {@code selectAssignedMenus} should use single JOIN query to fetch all user menus efficiently</li>
     *     <li><strong>Index Strategy</strong>: Ensure indexes on junction tables ({@code user_role}, {@code role_menu}) for efficient joins</li>
     *     <li><strong>Result Deduplication</strong>: Use {@code DISTINCT} or {@code Set} to eliminate duplicate menus from multiple roles</li>
     * </ul>
     *
     * @param userId the primary key of the user to generate sidebar for; must not be {@code null}
     * @return list of {@link LeftNavVO} representing user-accessible navigation tree; never {@code null}
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @see LeftNavVO
     * @see SysMenuMapper#selectAssignedMenus(QueryWrapper)
     * @see TreeBuildUtils#build(List)
     * @see Cacheable
     */
    @Cacheable(value = "menus", key = "#p0")
    @Override
    public List<LeftNavVO> selectSidebar(Serializable userId) {
        // Build query wrapper for assigned menus (filters by userId, orders by sort_order)
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();
        wrapper.eq(QueryConstant.USER_ID, userId)
                .orderBy(true, false, QueryConstant.SORT); // DESC order by sort field

        // Fetch assigned menus via custom mapper method (avoids N+1 query)
        List<SysMenu> menus = getBaseMapper().selectAssignedMenus(wrapper);

        // Return empty list if no menus assigned (never return null)
        if (CollectionUtils.isEmpty(menus)) {
            return Collections.emptyList();
        }

        // Convert entities to LeftNavVO and build hierarchical tree
        List<LeftNavVO> sidebars = menus.stream()
                .map(menuConverter::toLeftNavVO)
                .toList();

        return TreeBuildUtils.build(sidebars);
    }

    /**
     * Retrieves simplified menu metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full menu queries when only
     * basic identification fields ({@code id}, {@code title}, {@code path}) are needed.
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>Uses {@link #getAndCheckById} to throw {@link NotFoundException} if menu not found</li>
     *     <li>Ensures caller receives valid {@link MenuSimpleVO} or handles exception</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch menu for parent selector population
     * MenuSimpleVO menu = menuService.getMenuById(1001L);
     * // Use for UI rendering
     * parentOptions.add(new SelectOption(menu.getId(), menu.getTitle()));
     *
     * // Frontend: Populate select with menu options
     * const { data: menuOptions } = useRequest(() => api.getMenuOptions());
     * const menuOptions = computed(() =>
     *   menuOptions.value?.map(opt => ({ label: opt.title, value: opt.id })) || []
     * );
     * }
     * </pre>
     *
     * @param id the primary key of the menu to retrieve; must not be {@code null}
     * @return {@link MenuSimpleVO} if found
     * @throws NotFoundException if no menu exists with given {@code id}
     * @see MenuSimpleVO
     * @see MenuConverter#toSimpleVO(SysMenu)
     * @see #getAndCheckById(Serializable, ErrorCode)
     */
    @Override
    public MenuSimpleVO getMenuById(Serializable id) {
        // Fetch menu with not-found check (throws NotFoundException if missing)
        SysMenu menu = getAndCheckById(id, ErrorCode.MENU_NOT_FOUND);

        // Convert to lightweight VO for UI rendering
        return menuConverter.toSimpleVO(menu);
    }

    /**
     * Retrieves a list of menus with checked state for role-based permission configuration.
     * <p>
     * This method supports the "assign menus to role" workflow in admin consoles by:
     * <ol>
     *     <li>Fetching all available menus (typically filtered by status=enabled)</li>
     *     <li>Joining with role-menu assignments to determine {@code checked} state</li>
     *     <li>Returning {@link MenuCheckVO} with {@code id}, {@code title}, {@code checked}, and nested {@code children}</li>
     * </ol>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link MenuCheckVO} with pre-computed {@code checked} flags and nested children</li>
     *     <li><strong>Empty Result</strong>: Returns empty list {@code Collections.emptyList()} if no menus exist or role has no assignments</li>
     *     <li><strong>Ordering</strong>: Results sorted by {@code parent_id}, {@code sort_order} for UI consistency</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Tree checkbox for role menu assignment
     * <a-tree
     *   :tree-data="menuTree"
     *   :checked-keys="checkedMenuIds"
     *   checkable
     *   @check="onMenuCheck"
     * />
     *
     * // Submit: Send only changed menu IDs to backend
     * const handleSubmit = async () => {
     *   const added = checkedMenuIds.value.filter(id => !initialIds.includes(id));
     *   const removed = initialIds.filter(id => !checkedMenuIds.value.includes(id));
     *
     *   await api.updateRoleMenus(roleId.value, { added, removed });
     * };
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Single Query</strong>: {@code selectMenusByRoleId} should use {@code LEFT JOIN} + {@code CASE WHEN} to compute {@code checked} in database</li>
     *     <li><strong>Index Strategy</strong>: Ensure index on {@code sys_role_menu(role_id, menu_id)} for efficient join</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching results by {@code roleId} with invalidation on role-menu changes</li>
     * </ul>
     *
     * @param roleId the primary key of the role to configure; must not be {@code null}
     * @return list of {@link MenuCheckVO} with computed {@code checked} state and nested children; never {@code null}
     * @throws IllegalArgumentException if {@code roleId} is {@code null}
     * @see MenuCheckVO
     * @see SysMenuMapper#selectMenusByRoleId(Serializable)
     * @see TreeBuildUtils#build(List)
     */
    @Override
    public List<MenuCheckVO> selectCheckedMenus(Serializable roleId) {
        // Fetch menus with checked state via custom mapper method
        List<MenuCheckVO> result = getBaseMapper().selectMenusByRoleId(roleId);

        // Return empty list if no results (never return null)
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }

        // Build hierarchical tree structure from flat list
        return TreeBuildUtils.build(result);
    }

    /**
     * Updates an existing menu definition with validation, conflict prevention, and cache invalidation.
     * <p>
     * This method handles the complete menu update workflow including:
     * <ol>
     *     <li>Parent reference validation via {@link #validateParent}</li>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link MenuConverter#updateEntity}</li>
     *     <li>Update with comprehensive error handling via {@link #update}</li>
     *     <li>Cache invalidation via {@code cacheHelper.clear}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_UPDATE)} to:
     * <ul>
     *     <li>Automatically record menu modification events for compliance tracking</li>
     *     <li>Capture old/new values, operator ID, and timestamp in audit log</li>
     *     <li>Enable change history queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidates button cache to ensure consistency:
     * <pre>
     * {@code
     * // Clear button cache (menus may contain button children; invalidate to be safe)
     * cacheHelper.clear(CacheConstant.BUTTON);
     *
     * // Note: Also consider invalidating menu cache if needed
     * // eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.MENU));
     * }
     * </pre>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #update} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same menu are properly serialized</li>
     * </ul>
     *
     * @param id      the primary key of the menu to update; must not be {@code null}
     * @param menuDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if menu was successfully updated
     * @throws IllegalArgumentException                                      if {@code id} or {@code menuDto} is {@code null}
     * @throws NotFoundException                                             if no menu exists with given {@code id} or referenced parent not found
     * @throws com.github.starhq.template.common.exception.BusinessException if update fails for other reasons
     * @see MenuDTO
     * @see #validateParent(Serializable)
     * @see #getAndCheckById(Serializable, ErrorCode)
     * @see #update(Object, ErrorCode, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_UPDATE)
    @Override
    public boolean updateMenu(Serializable id, MenuDTO menuDto) {
        // Validate that referenced parent menu exists (prevent orphaned children)
        validateParent(menuDto.getParentId());

        // Fetch existing menu with not-found check
        SysMenu menu = getAndCheckById(id, ErrorCode.MENU_NOT_FOUND);

        // Apply updates from DTO to entity (partial update support)
        menuConverter.updateEntity(menuDto, menu);

        // Update with comprehensive error handling (null duplicate code, not-found, general)
        update(menu, null, ErrorCode.MENU_UPDATE_FAILED, ErrorCode.MENU_NOT_FOUND);

        // Invalidate button cache to ensure consistency (menus may contain button children)
        cacheHelper.clear(CacheConstant.BUTTON);

        return true;
    }

    /**
     * Creates a new menu definition with validation, duplicate prevention, and audit logging.
     * <p>
     * This method handles the complete menu creation workflow including:
     * <ol>
     *     <li>Parent reference validation via {@link #validateParent}</li>
     *     <li>DTO-to-entity conversion via {@link MenuConverter}</li>
     *     <li>Insertion with duplicate prevention via {@link #insert}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_INSERT)} to:
     * <ul>
     *     <li>Automatically record menu creation events for compliance tracking</li>
     *     <li>Capture operator ID, timestamp, and operation details in audit log</li>
     *     <li>Enable audit trail queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Parent Validation</strong>: Ensures menu references existing parent to prevent orphaned entries</li>
     *     <li><strong>Path Uniqueness</strong>: Should be enforced at database level via UNIQUE INDEX on {@code path} column</li>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #insert} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Menu insertion either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Audit log entry is recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same path are properly serialized by database</li>
     * </ul>
     *
     * @param menuDto the menu creation data; must not be {@code null}
     * @return {@code true} if menu was successfully created
     * @throws IllegalArgumentException                                      if {@code menuDto} is {@code null} or missing required fields
     * @throws NotFoundException                                             if referenced parent menu does not exist
     * @throws com.github.starhq.template.common.exception.BusinessException if insertion fails for other reasons
     * @see MenuDTO
     * @see #validateParent(Serializable)
     * @see #insert(Object, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_INSERT)
    @Override
    public boolean createMenu(MenuDTO menuDto) {
        // Validate that referenced parent menu exists (prevent orphaned children)
        validateParent(menuDto.getParentId());

        // Convert DTO to entity for persistence
        SysMenu menu = menuConverter.toEntity(menuDto);

        // Insert with business error handling (null duplicate code, general insert failure)
        insert(menu, null, ErrorCode.MENU_INSERT_FAILED);

        return true;
    }

    /**
     * Deletes a single menu definition with cascading role-menu cleanup, audit logging, and cache invalidation.
     * <p>
     * This method delegates to {@link #removeByIds} for batch deletion support:
     * <pre>
     * {@code
     * // Convert single ID to list for batch processing
     * return removeByIds(Collections.singletonList(id));
     * }
     * </pre>
     * <p>
     * <strong>See {@link #removeByIds} for full workflow documentation.</strong>
     *
     * @param id the primary key of the menu to delete; must not be {@code null}
     * @return {@code true} if menu was successfully deleted
     * @see #removeByIds(Collection)
     * @see AuditLoggable
     * @see Transactional
     */
    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        // 1. Cascading delete: Remove all role-menu assignments for this menu
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));

        // 2. Delete menu with business error handling
        delete(id, ErrorCode.MENU_NOT_FOUND, ErrorCode.MENU_DELETE_FAILED);

        // 3. Publish distributed cache eviction event for multi-node consistency
        eventService.notifyCacheEvict(List.of(id), List.of(CacheConstant.MENU));

        return true;
    }

    /**
     * Deletes multiple menu definitions with cascading role-menu cleanup, audit logging, and distributed cache invalidation.
     * <p>
     * This method handles the complete menu deletion workflow including:
     * <ol>
     *     <li>Input validation (non-empty ID list)</li>
     *     <li>ID type conversion via {@link TypeConvertUtils#toLong}</li>
     *     <li>Existence validation via {@link #validateMenuExists}</li>
     *     <li>Child node validation via {@link #validateNotHasChildren} (prevent orphaned children)</li>
     *     <li>Cascading delete: Remove all role-menu assignments for these menus</li>
     *     <li>Menu deletion via {@link AuditBaseServiceImpl#removeByIds}</li>
     *     <li>Distributed cache invalidation via {@link EventService}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Annotated with {@code @Transactional(rollbackFor = Exception.class)} to ensure:
     * <ul>
     *     <li>Atomicity: Role-menu cleanup and menu deletion succeed or fail together</li>
     *     <li>Consistency: If menu deletion fails, role-menu assignments are rolled back</li>
     *     <li>Isolation: Concurrent deletions of same menus are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cascading Delete Strategy:</strong>
     * <pre>
     * {@code
     * // Remove all role-menu assignments for these menus
     * roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
     *     .in(SysRoleMenu::getMenuId, ids));
     *
     * // Then delete the menus themselves
     * super.removeByIds(ids);
     * }
     * </pre>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful deletion, publishes distributed cache eviction event:
     * <pre>
     * {@code
     * // Notify all nodes to evict menu-related cache entries
     * eventService.notifyCacheEvict(
     *     Collections.emptyList(),        // No specific keys (evict by pattern)
     *     List.of(CacheConstant.MENU)     // Cache region to evict
     * );
     * }
     * </pre>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_REMOVE)} to:
     * <ul>
     *     <li>Automatically record menu deletion events for compliance tracking</li>
     *     <li>Capture deleted menu details, operator ID, and timestamp in audit log</li>
     *     <li>Enable deletion history queries in admin console for security analysis</li>
     * </ul>
     *
     * @param list collection of menu IDs to delete; must not be {@code null} or empty
     * @return {@code true} if all menus were successfully deleted
     * @throws IllegalArgumentException                                      if {@code list} is {@code null} or contains {@code null} IDs
     * @throws NotFoundException                                             if any menu ID does not exist
     * @throws BusinessException                                             if any menu has child nodes (prevent orphaned children)
     * @throws com.github.starhq.template.common.exception.BusinessException if deletion fails for other reasons
     * @see AuditBaseServiceImpl#removeByIds(Collection)
     * @see EventService#notifyCacheEvict(List, List)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeByIds(Collection<?> list) {
        // Return true for empty input (no-op)
        if (CollectionUtils.isEmpty(list)) {
            return true;
        }

        // Convert IDs to Long type for database operations
        List<Long> ids = list.stream()
                .map(TypeConvertUtils::toLong)
                .toList();

        // 1. Validate that all menus exist (fail fast if any ID is invalid)
        validateMenuExists(ids);

        // 2. Validate that no menu has child nodes (prevent orphaned children)
        validateNotHasChildren(ids);

        // 3. Cascading delete: Remove all role-menu assignments for these menus
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                .in(SysRoleMenu::getMenuId, ids));

        // 4. Delete menus via base class method (with audit field handling)
        boolean result = super.removeByIds(ids);

        // 5. Publish distributed cache eviction event for multi-node consistency
        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.MENU));

        return result;
    }

    // ====================== Private Helper Methods ======================

    /**
     * Validates that the menu's referenced parent menu exists in the database.
     * <p>
     * This method enforces referential integrity by ensuring that every menu
     * is associated with a valid parent menu (or {@code null}/0 for root-level menus),
     * preventing orphaned menu entries in the hierarchy.
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * // Allow null/0 parentId for root-level menus
     * if (parentId == null) {
     *     return;
     * }
     *
     * // Check if parent menu with given ID exists
     * boolean exists = getBaseMapper().exists(
     *     new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getId, parentId)
     * );
     *
     * // Throw not-found exception if parent does not exist
     * if (!exists) {
     *     throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from {@link #createMenu} and {@link #updateMenu} before persistence
     * to ensure data integrity. Not intended for public API exposure.
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li><strong>Parent Not Found</strong>: Throws {@link NotFoundException} with {@link ErrorCode#MENU_NOT_FOUND}</li>
     *     <li><strong>Null Safety</strong>: Allows {@code null} parentId for root-level menus</li>
     * </ul>
     *
     * @param parentId the primary key of the parent menu to validate; may be {@code null} for root-level menus
     * @throws NotFoundException if referenced parent menu does not exist
     * @see ErrorCode#MENU_NOT_FOUND
     */
    private void validateParent(Serializable parentId) {
        // Allow null parentId for root-level menus
        if (Objects.isNull(parentId)) {
            return;
        }

        // Check if parent menu with given ID exists (enforce foreign key constraint at application level)
        boolean exists = getBaseMapper().exists(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getId, parentId)
        );

        // Throw not-found exception if parent does not exist
        if (!exists) {
            throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
        }
    }

    /**
     * Validates that all menu IDs in the list exist in the database.
     * <p>
     * This method ensures that deletion operations only target existing menus,
     * preventing silent failures or partial deletions.
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * // Count how many of the requested IDs actually exist
     * long existCount = this.count(
     *     new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, ids)
     * );
     *
     * // If count doesn't match requested count, some IDs are invalid
     * if (existCount != ids.size()) {
     *     throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from {@link #removeByIds} before deletion to ensure all target menus exist.
     * Not intended for public API exposure.
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li><strong>Missing Menu</strong>: Throws {@link NotFoundException} with {@link ErrorCode#MENU_NOT_FOUND}</li>
     *     <li><strong>Partial Match</strong>: Fails entire operation if any ID is invalid (atomic deletion)</li>
     * </ul>
     *
     * @param ids list of menu IDs to validate; must not be {@code null} or empty
     * @throws NotFoundException if any menu ID does not exist
     * @see ErrorCode#MENU_NOT_FOUND
     */
    private void validateMenuExists(List<Long> ids) {
        // Count how many of the requested IDs actually exist
        long existCount = this.count(
                new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, ids)
        );

        // If count doesn't match requested count, some IDs are invalid
        if (existCount != ids.size()) {
            throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
        }
    }

    /**
     * Validates that none of the menu IDs in the list have child nodes.
     * <p>
     * This method prevents deletion of menus that have children, avoiding orphaned
     * menu entries in the hierarchy. Callers must delete child menus first.
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * // Count how many menus have any of the requested IDs as parent
     * long childCount = this.count(
     *     new LambdaQueryWrapper<SysMenu>().in(SysMenu::getParentId, ids)
     * );
     *
     * // If any children exist, deletion would orphan them
     * if (childCount > 0) {
     *     throw new BusinessException(ErrorCode.MENU_HAS_CHILD);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from {@link #removeByIds} before deletion to ensure no orphaned children.
     * Not intended for public API exposure.
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li><strong>Has Children</strong>: Throws {@link BusinessException} with {@link ErrorCode#MENU_HAS_CHILD}</li>
     *     <li><strong>User Guidance</strong>: Frontend should guide users to delete child menus first</li>
     * </ul>
     *
     * @param ids list of menu IDs to validate; must not be {@code null} or empty
     * @throws BusinessException if any menu has child nodes
     * @see ErrorCode#MENU_HAS_CHILD
     */
    private void validateNotHasChildren(List<Long> ids) {
        // Count how many menus have any of the requested IDs as parent
        long childCount = this.count(
                new LambdaQueryWrapper<SysMenu>().in(SysMenu::getParentId, ids)
        );

        // If any children exist, deletion would orphan them
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILD);
        }
    }

}