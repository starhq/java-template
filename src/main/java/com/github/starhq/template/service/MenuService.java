package com.github.starhq.template.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.dto.menu.MenuDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.menu.MenuSimpleVO;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.menu.tree.MenuCheckVO;
import com.github.starhq.template.model.vo.menu.tree.MenuListVO;

import java.io.Serializable;
import java.util.List;

/**
 * Service interface for menu management with hierarchical tree operations and role-based access control.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysMenu} entities, while adding business-level methods for menu tree queries,
 * sidebar navigation generation, role-based menu assignment, and menu lifecycle management.
 * Designed to centralize menu logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Menu Management</strong>: CRUD operations for defining navigation hierarchy in admin console</li>
 *     <li><strong>Sidebar Generation</strong>: Build user-specific navigation trees based on assigned roles and permissions</li>
 *     <li><strong>Role Configuration</strong>: List menus with checked state for role-based permission assignment</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured menu metadata for dynamic router generation and UI rendering</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service layer handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Type Safety</strong>: Use typed DTOs/VOs instead of generic maps for compile-time validation</li>
 *     <li><strong>Cache-Aware</strong>: Frequent read operations should leverage caching for performance</li>
 *     <li><strong>Access-Controlled</strong>: All write operations should enforce role-based permissions (ADMIN-only)</li>
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
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysMenu
 * @see MenuDTO
 * @see MenuListVO
 * @see LeftNavVO
 * @see MenuCheckVO
 * @see MenuSimpleVO
 */
public interface MenuService extends IService<SysMenu> {

    /**
     * Retrieves a flat or hierarchical list of menu definitions for admin console management.
     * <p>
     * This method supports multi-dimensional filtering for efficient menu management:
     * <ul>
     *     <li><strong>Parent Filter</strong>: Filter menus by parent ID to scope queries to specific subtree</li>
     *     <li><strong>Name Filter</strong>: Fuzzy search on menu title for admin convenience</li>
     *     <li><strong>Type Filter</strong>: Filter by menu type (directory, menu, button) for bulk operations</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for visibility control</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageRequest}: Must not be {@code null}; provides pagination params and base filters</li>
     *     <li>{@code pageRequest.getParentId()}: Optional; filters menus by parent menu ID</li>
     *     <li>{@code pageRequest.getName()}: Optional; performs right-fuzzy match on menu title</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Hierarchical Result</strong>: Returns list of {@link MenuListVO} with nested {@code children} for tree rendering</li>
     *     <li><strong>Empty Result</strong>: Returns empty list if no menus match criteria</li>
     *     <li><strong>Ordering</strong>: Results sorted by {@code sort_order} for consistent UI display</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code selectList()} calls for compliance tracking</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration attacks on menu definitions</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure composite indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_menu_parent_sort ON sys_menu(parent_id, sort_order);
     *         CREATE INDEX idx_menu_name_status ON sys_menu(title, status);
     *         }</pre>
     *     </li>
     *     <li><strong>Tree Building</strong>: Use single query with recursive CTE or application-level tree building for efficiency</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching menu tree by root ID for repeated admin console access</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service call: Query menus with filters
     * PageRequest request = new PageRequest();
     * request.setParentId(0L); // Root level menus
     * request.setName("User");
     *
     * List<MenuListVO> result = menuService.selectList(request);
     *
     * // Process results for admin table
     * result.forEach(menu -> {
     *     System.out.println(menu.getTitle() + " -> " + menu.getPath());
     *     // Output: "User Management -> /system/user"
     * });
     * }
     * </pre>
     *
     * @param pageRequest the pagination and filter criteria; must not be {@code null}
     * @return list of {@link MenuListVO} with nested children for tree rendering; never {@code null}
     * @throws IllegalArgumentException if {@code pageRequest} is {@code null}
     * @see PageRequest
     * @see MenuListVO
     */
    List<MenuListVO> selectList(PageRequest pageRequest);

    /**
     * Retrieves the sidebar navigation tree for a specific user based on assigned roles and permissions.
     * <p>
     * This method builds a user-specific menu hierarchy by:
     * <ol>
     *     <li>Fetching all roles assigned to the user</li>
     *     <li>Collecting all menus accessible through those roles</li>
     *     <li>Filtering by menu status (enabled) and type (directory/menu only, excluding buttons)</li>
     *     <li>Building hierarchical tree structure with nested {@code children} for frontend rendering</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must not be {@code null}; the primary key of the user to generate sidebar for</li>
     *     <li>Lookup strategy: Typically joins {@code sys_user} → {@code sys_user_role} → {@code sys_role_menu} → {@code sys_menu}</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link LeftNavVO} representing root-level menus with nested children</li>
     *     <li><strong>No Permissions</strong>: Returns empty list if user has no roles or roles have no accessible menus</li>
     *     <li><strong>Null Safety</strong>: Never returns {@code null}; empty list indicates no accessible menus</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: Sidebar is loaded on every page view or app initialization</li>
     *     <li><strong>Low Write Frequency</strong>: Menu assignments change infrequently</li>
     *     <li><strong>User-Specific</strong>: Cache key can be {@code "sidebar:" + userId} for precise invalidation</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "sidebar", key = "#userId", unless = "#result.isEmpty()")
     * public List<LeftNavVO> selectSidebar(Serializable userId) { ... }
     *
     * // Cache invalidation on menu/role changes
     * @CacheEvict(value = "sidebar", key = "#userId")
     * public boolean updateMenu(Serializable id, MenuDTO dto) { ... }
     * }
     * </pre>
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
     * <strong>Performance Optimization:</strong>
     * <ul>
     *     <li><strong>Batch Query</strong>: Use single JOIN query or batch loading to fetch user menus efficiently</li>
     *     <li><strong>Index Strategy</strong>: Ensure indexes on junction tables ({@code user_role}, {@code role_menu}) for efficient joins</li>
     *     <li><strong>Result Deduplication</strong>: Use {@code DISTINCT} or {@code Set} to eliminate duplicate menus from multiple roles</li>
     * </ul>
     *
     * @param userId the primary key of the user to generate sidebar for; must not be {@code null}
     * @return list of {@link LeftNavVO} representing user-accessible navigation tree; never {@code null}
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @see LeftNavVO
     */
    List<LeftNavVO> selectSidebar(Serializable userId);

    /**
     * Retrieves simplified menu metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full menu queries when only
     * basic identification fields ({@code id}, {@code title}, {@code path}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the menu to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link MenuSimpleVO} with {@code id}, {@code title}, {@code path}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no menu exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes audit metadata for minimal payload</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: Menu metadata is referenced frequently in UI rendering</li>
     *     <li><strong>Low Write Frequency</strong>: Menu definitions change infrequently</li>
     *     <li><strong>Small Payload</strong>: {@link MenuSimpleVO} contains minimal fields for efficient cache storage</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "menus", key = "#id", unless = "#result == null")
     * public MenuSimpleVO getMenuById(Serializable id) { ... }
     *
     * // Cache invalidation on menu update/delete
     * @CacheEvict(value = "menus", key = "#id")
     * public boolean updateMenu(Serializable id, MenuDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch menu for parent selector population
     * MenuSimpleVO menu = menuService.getMenuById(1001L);
     * if (menu != null) {
     *     // Use for UI rendering
     *     parentOptions.add(new SelectOption(menu.getId(), menu.getTitle()));
     * }
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
     * @return {@link MenuSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see MenuSimpleVO
     */
    MenuSimpleVO getMenuById(Serializable id);

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
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleId}: Must not be {@code null}; the primary key of the role to configure</li>
     *     <li>Checked state logic: {@code checked = true} if menu is assigned to role; {@code false} otherwise</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link MenuCheckVO} with pre-computed {@code checked} flags and nested children</li>
     *     <li><strong>Empty Result</strong>: Returns empty list if no menus exist or role has no assignments</li>
     *     <li><strong>Ordering</strong>: Results typically sorted by {@code parent_id}, {@code sort_order} for UI consistency</li>
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
     *     <li><strong>Single Query</strong>: Use {@code LEFT JOIN} + {@code CASE WHEN} to compute {@code checked} in database</li>
     *     <li><strong>Index Strategy</strong>: Ensure index on {@code sys_role_menu(role_id, menu_id)} for efficient join</li>
     *     <li><strong>Cache Strategy</strong>: Cache results by {@code roleId} with invalidation on role-menu changes</li>
     * </ul>
     *
     * @param roleId the primary key of the role to configure; must not be {@code null}
     * @return list of {@link MenuCheckVO} with computed {@code checked} state and nested children; never {@code null}
     * @throws IllegalArgumentException if {@code roleId} is {@code null}
     * @see MenuCheckVO
     */
    List<MenuCheckVO> selectCheckedMenus(Serializable roleId);

    /**
     * Updates an existing menu definition with validation and conflict prevention.
     * <p>
     * This method handles the complete menu update workflow including:
     * <ul>
     *     <li>Existence check (ensure menu with given {@code id} exists)</li>
     *     <li>Parent reference validation (if {@code parentId} is being changed)</li>
     *     <li>Path uniqueness validation (if {@code path} is being changed)</li>
     *     <li>Field-level updates (title, path, component, icon, sort, status, etc.)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected menu metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the menu to update</li>
     *     <li>{@code menuDto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code menuDto.getPath()}: If changed, must be unique across all menus (excluding current entry)</li>
     *     <li>{@code menuDto.getParentId()}: If changed, must reference existing menu or {@code 0L} for root</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if menu was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no menu exists with given {@code id}</li>
     *     <li><strong>Duplicate Path</strong>: Returns {@code false} or throws exception if new {@code path} conflicts</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same menu are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidate related caches to ensure consistency:
     * <pre>
     * {@code
     * // Invalidate menu metadata cache by ID
     * cacheHelper.evict(List.of(id), List.of(CacheConstant.MENU));
     *
     * // Invalidate sidebar cache for all users if menu structure changed
     * if (pathOrParentChanged) {
     *     cacheHelper.evictByPattern(CacheConstant.SIDEBAR, "*");
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle menu update request
     * @PutMapping("/{id}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Void> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuDTO dto) {
     *     boolean success = menuService.updateMenu(id, dto);
     *
     *     if (success) {
     *         return Result.success("Menu updated successfully");
     *     } else {
     *         return Result.fail(ErrorCode.MENU_UPDATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Update logic
     * @Transactional
     * @Override
     * public boolean updateMenu(Serializable id, MenuDTO dto) {
     *     // 1. Check existence
     *     SysMenu existing = getById(id);
     *     if (existing == null) {
     *         return false;
     *     }
     *
     *     // 2. Validate path uniqueness if changing
     *     if (!existing.getPath().equals(dto.getPath()) &&
     *         lambdaQuery().eq(SysMenu::getPath, dto.getPath()).ne(SysMenu::getId, id).exists()) {
     *         throw new DuplicateException(ErrorCode.MENU_PATH_EXISTS);
     *     }
     *
     *     // 3. Apply updates
     *     converter.updateEntity(existing, dto);
     *     existing.setUpdatedBy(SecurityContextUtils.getUserId());
     *     existing.setUpdatedAt(OffsetDateTime.now());
     *
     *     // 4. Persist changes
     *     boolean success = updateById(existing);
     *
     *     // 5. Invalidate caches
     *     if (success) {
     *         cacheHelper.evict(List.of(id), List.of(CacheConstant.MENU));
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param id      the primary key of the menu to update; must not be {@code null}
     * @param menuDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if menu was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException                                       if {@code id} or {@code menuDto} is {@code null}
     * @throws com.github.starhq.template.common.exception.DuplicateException if new {@code path} conflicts with existing menu
     * @see MenuDTO
     */
    boolean updateMenu(Serializable id, MenuDTO menuDto);

    /**
     * Creates a new menu definition with validation and duplicate prevention.
     * <p>
     * This method handles the complete menu creation workflow including:
     * <ul>
     *     <li>Input validation (path uniqueness, parent reference, component format)</li>
     *     <li>Path format enforcement (e.g., {@code /module/resource} for frontend router compatibility)</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code menuDto}: Must not be {@code null}; should include {@code title}, {@code path}, {@code parentId}</li>
     *     <li>{@code menuDto.getPath()}: Must be unique across all menus; format: {@code ^/[a-z][a-z0-9/]*$}</li>
     *     <li>{@code menuDto.getParentId()}: Must reference existing menu or {@code 0L} for root-level menu</li>
     *     <li>{@code menuDto.getComponent()}: If type is "menu", must reference valid Vue component path</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if menu was successfully created</li>
     *     <li><strong>Duplicate Path</strong>: Returns {@code false} or throws exception if {@code path} already exists</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Menu creation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same path are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Path Validation</strong>: Enforce format to prevent path traversal or router injection attacks</li>
     *     <li><strong>Component Validation</strong>: Ensure component path references whitelisted directories to prevent arbitrary code execution</li>
     *     <li><strong>Audit Logging</strong>: Log menu creation events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle menu creation request
     * @PostMapping
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Long> createMenu(@Valid @RequestBody MenuDTO dto) {
     *     boolean success = menuService.createMenu(dto);
     *
     *     if (success) {
     *         // Optional: Return new menu ID for frontend reference
     *         return Result.success(dto.getId());
     *     } else {
     *         return Result.fail(ErrorCode.MENU_CREATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Creation logic
     * @Transactional
     * @Override
     * public boolean createMenu(MenuDTO dto) {
     *     // 1. Validate input
     *     validateMenuDto(dto);
     *
     *     // 2. Check path uniqueness
     *     if (lambdaQuery().eq(SysMenu::getPath, dto.getPath()).exists()) {
     *         throw new DuplicateException(ErrorCode.MENU_PATH_EXISTS);
     *     }
     *
     *     // 3. Convert DTO to entity
     *     SysMenu entity = converter.toEntity(dto);
     *     entity.setStatus(1); // Default enabled
     *     entity.setCreatedBy(SecurityContextUtils.getUserId());
     *     entity.setCreatedAt(OffsetDateTime.now());
     *
     *     // 4. Persist entity
     *     return save(entity);
     * }
     * }
     * </pre>
     *
     * @param menuDto the menu creation data; must not be {@code null}
     * @return {@code true} if menu was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException                                       if {@code menuDto} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.DuplicateException if {@code path} already exists
     * @see MenuDTO
     */
    boolean createMenu(MenuDTO menuDto);

}