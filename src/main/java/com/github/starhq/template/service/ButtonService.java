package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;

import java.io.Serializable;
import java.util.List;

/**
 * Service interface for button/permission management with CRUD operations and access control.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysButton} entities, while adding business-level methods for permission queries,
 * role-based button assignment, and button metadata retrieval. Designed to centralize
 * button management logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Button Management</strong>: CRUD operations for defining UI button permissions in admin console</li>
 *     <li><strong>Permission Queries</strong>: Retrieve available buttons for a user based on assigned roles</li>
 *     <li><strong>Role Configuration</strong>: List buttons with checked state for role-based permission assignment</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured button metadata for dynamic UI rendering</li>
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
 *         List<String> buttonCodes = buttonService.select(userId);
 *         return Result.success(buttonCodes);
 *     }
 * }
 *
 * // Frontend: Use button codes for dynamic UI rendering
 * const hasPermission = (buttonCode: string) => {
 *   return userPermissions.value.includes(buttonCode);
 * };
 *
 * // Usage in template
 * <a-button v-if="hasPermission('user:create')">Create User</a-button>
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysButton
 * @see ButtonPageRequest
 * @see ButtonPageVO
 * @see ButtonCheckVO
 * @see ButtonSimpleVO
 */
public interface ButtonService extends IService<SysButton> {

    /**
     * Retrieves a paginated list of button definitions matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient button management:
     * <ul>
     *     <li><strong>Name Filter</strong>: Fuzzy search on button display name</li>
     *     <li><strong>Code Filter</strong>: Exact match on permission code (e.g., {@code "user:create"})</li>
     *     <li><strong>Menu Filter</strong>: Filter buttons by parent menu ID for hierarchical organization</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for bulk operations</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageInfo.getName()}: Optional; performs right-fuzzy match on button name</li>
     *     <li>{@code pageInfo.getCode()}: Optional; performs exact match on permission code</li>
     *     <li>{@code pageInfo.getMenuId()}: Optional; filters buttons under specific menu</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysButton} entity is converted to {@link ButtonPageVO} with audit fields</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code page()} calls for compliance tracking</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration attacks on permission definitions</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure composite indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_button_menu_code ON sys_button(menu_id, code);
     *         CREATE INDEX idx_button_name_status ON sys_button(name, status);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching frequent queries with short TTL for admin console responsiveness</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service call: Query buttons with filters
     * ButtonPageRequest request = new ButtonPageRequest();
     * request.setPage(1);
     * request.setSize(20);
     * request.setMenuId(1001L);
     * request.setName("Create");
     *
     * IPage<ButtonPageVO> result = buttonService.page(request);
     *
     * // Process results
     * result.getRecords().forEach(button -> {
     *     System.out.println(button.getCode() + ": " + button.getName());
     *     // Output: "user:create: Create User Button"
     * });
     * }
     * </pre>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link ButtonPageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException if {@code pageInfo} is {@code null}
     * @see ButtonPageRequest
     * @see ButtonPageVO
     * @see IPage
     */
    IPage<ButtonPageVO> page(ButtonPageRequest pageInfo);

    /**
     * Retrieves the list of permission codes (button codes) accessible to a specific user.
     * <p>
     * This method resolves a user's effective permissions by:
     * <ol>
     *     <li>Fetching all roles assigned to the user</li>
     *     <li>Collecting all buttons assigned to those roles</li>
     *     <li>Extracting unique {@code code} values for frontend permission checks</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must not be {@code null}; the primary key of the user to query</li>
     *     <li>Lookup strategy: Typically joins {@code sys_user} → {@code sys_user_role} → {@code sys_role_button} → {@code sys_button}</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of unique permission codes (e.g., {@code ["user:create", "user:delete"]})</li>
     *     <li><strong>No Permissions</strong>: Returns empty list {@code []} if user has no roles or roles have no buttons</li>
     *     <li><strong>Null Safety</strong>: Never returns {@code null}; empty list indicates no permissions</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Authorization</strong>: Caller should have permission to query target user's permissions (self or admin)</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching results by {@code userId} with short TTL for repeated permission checks</li>
     *     <li><strong>Real-time Updates</strong>: Cache should be invalidated when user roles or button assignments change</li>
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
     * <strong>Performance Optimization:</strong>
     * <ul>
     *     <li><strong>Batch Query</strong>: Use single JOIN query instead of N+1 lookups for role/button resolution</li>
     *     <li><strong>Index Strategy</strong>: Ensure indexes on junction tables ({@code user_role}, {@code role_button}) for efficient joins</li>
     *     <li><strong>Result Deduplication</strong>: Use {@code DISTINCT} or {@code Set} to eliminate duplicate codes from multiple roles</li>
     * </ul>
     *
     * @param userId the primary key of the user to query; must not be {@code null}
     * @return list of unique permission codes accessible to the user; never {@code null}
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @see SysButton#getCode()
     */
    List<String> select(Serializable userId);

    /**
     * Retrieves simplified button metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full button queries when only
     * basic identification fields ({@code id}, {@code name}, {@code code}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the button to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link ButtonSimpleVO} with {@code id}, {@code name}, {@code code}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no button exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes audit metadata for minimal payload</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: Button metadata is referenced frequently in UI rendering</li>
     *     <li><strong>Low Write Frequency</strong>: Button definitions change infrequently</li>
     *     <li><strong>Small Payload</strong>: {@link ButtonSimpleVO} contains minimal fields for efficient cache storage</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "buttons", key = "#id", unless = "#result == null")
     * public ButtonSimpleVO getButtonById(Serializable id) { ... }
     *
     * // Cache invalidation on button update/delete
     * @CacheEvict(value = "buttons", key = "#id")
     * public boolean updateButton(Serializable id, ButtonDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch button for dropdown population
     * ButtonSimpleVO button = buttonService.getButtonById(2001L);
     * if (button != null) {
     *     // Use for UI rendering
     *     dropdownOptions.add(new SelectOption(button.getId(), button.getName()));
     * }
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
     * @return {@link ButtonSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see ButtonSimpleVO
     */
    ButtonSimpleVO getButtonById(Serializable id);

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
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleId}: Must not be {@code null}; the primary key of the role to configure</li>
     *     <li>Checked state logic: {@code checked = true} if button is assigned to role; {@code false} otherwise</li>
     * </ul>
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
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Single Query</strong>: Use {@code LEFT JOIN} + {@code CASE WHEN} to compute {@code checked} in database</li>
     *     <li><strong>Index Strategy</strong>: Ensure index on {@code sys_role_button(role_id, button_id)} for efficient join</li>
     *     <li><strong>Cache Strategy</strong>: Cache results by {@code roleId} with invalidation on role-button changes</li>
     * </ul>
     *
     * @param roleId the primary key of the role to configure; must not be {@code null}
     * @return list of {@link ButtonCheckVO} with computed {@code checked} state; never {@code null}
     * @throws IllegalArgumentException if {@code roleId} is {@code null}
     * @see ButtonCheckVO
     */
    List<ButtonCheckVO> selectCheckedButtons(Serializable roleId);

    /**
     * Creates a new button definition with validation and duplicate prevention.
     * <p>
     * This method handles the complete button creation workflow including:
     * <ul>
     *     <li>Input validation (code uniqueness, name format, menu reference)</li>
     *     <li>Permission code format enforcement (e.g., {@code module:resource:action})</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code buttonDto}: Must not be {@code null}; should include {@code name}, {@code code}, {@code menuId}</li>
     *     <li>{@code buttonDto.getCode()}: Must be unique across all buttons; format: {@code [a-z][a-z0-9]*(:[a-z][a-z0-9]*){1,3}}</li>
     *     <li>{@code buttonDto.getMenuId()}: Must reference existing menu; foreign key constraint enforced</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if button was successfully created</li>
     *     <li><strong>Duplicate Code</strong>: Returns {@code false} or throws exception if {@code code} already exists</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Button creation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same code are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Code Validation</strong>: Enforce permission code format to prevent injection or privilege escalation</li>
     *     <li><strong>Audit Logging</strong>: Log button creation events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle button creation request
     * @PostMapping
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Long> createButton(@Valid @RequestBody ButtonDTO dto) {
     *     boolean success = buttonService.createButton(dto);
     *
     *     if (success) {
     *         // Optional: Return new button ID for frontend reference
     *         return Result.success(dto.getId());
     *     } else {
     *         return Result.fail(ErrorCode.BUTTON_CREATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Creation logic
     * @Transactional
     * @Override
     * public boolean createButton(ButtonDTO dto) {
     *     // 1. Validate input
     *     validateButtonDto(dto);
     *
     *     // 2. Check code uniqueness
     *     if (lambdaQuery().eq(SysButton::getCode, dto.getCode()).exists()) {
     *         throw new DuplicateException(ErrorCode.BUTTON_CODE_EXISTS);
     *     }
     *
     *     // 3. Convert DTO to entity
     *     SysButton entity = converter.toEntity(dto);
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
     * @param buttonDto the button creation data; must not be {@code null}
     * @return {@code true} if button was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException                                       if {@code buttonDto} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.DuplicateException if {@code code} already exists
     * @see ButtonDTO
     */
    boolean createButton(ButtonDTO buttonDto);

    /**
     * Updates an existing button definition with validation and conflict prevention.
     * <p>
     * This method handles the complete button update workflow including:
     * <ul>
     *     <li>Existence check (ensure button with given {@code id} exists)</li>
     *     <li>Code uniqueness validation (if {@code code} is being changed)</li>
     *     <li>Field-level updates (name, code, menu, sort, description, etc.)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected button metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the button to update</li>
     *     <li>{@code buttonDto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code buttonDto.getCode()}: If changed, must be unique across all buttons (excluding current button)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if button was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no button exists with given {@code id}</li>
     *     <li><strong>Duplicate Code</strong>: Returns {@code false} or throws exception if new {@code code} conflicts</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same button are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidate related caches to ensure consistency:
     * <pre>
     * {@code
     * // Invalidate button metadata cache by ID
     * cacheHelper.evict(List.of(id), List.of(CacheConstant.BUTTON));
     *
     * // Invalidate user permission cache if button code changed
     * if (codeChanged) {
     *     cacheHelper.evictByPattern(CacheConstant.USER_PERMISSIONS, "*");
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle button update request
     * @PutMapping("/{id}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Void> updateButton(@PathVariable Long id, @Valid @RequestBody ButtonDTO dto) {
     *     boolean success = buttonService.updateButton(id, dto);
     *
     *     if (success) {
     *         return Result.success("Button updated successfully");
     *     } else {
     *         return Result.fail(ErrorCode.BUTTON_UPDATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Update logic
     * @Transactional
     * @Override
     * public boolean updateButton(Serializable id, ButtonDTO dto) {
     *     // 1. Check existence
     *     SysButton existing = getById(id);
     *     if (existing == null) {
     *         return false;
     *     }
     *
     *     // 2. Validate code uniqueness if changing
     *     if (!existing.getCode().equals(dto.getCode()) &&
     *         lambdaQuery().eq(SysButton::getCode, dto.getCode()).exists()) {
     *         throw new DuplicateException(ErrorCode.BUTTON_CODE_EXISTS);
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
     *         cacheHelper.evict(List.of(id), List.of(CacheConstant.BUTTON));
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param id        the primary key of the button to update; must not be {@code null}
     * @param buttonDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if button was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException                                       if {@code id} or {@code buttonDto} is {@code null}
     * @throws com.github.starhq.template.common.exception.DuplicateException if new {@code code} conflicts with existing button
     * @see ButtonDTO
     */
    boolean updateButton(Serializable id, ButtonDTO buttonDto);

}