package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.resource.ResourceDTO;
import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import com.github.starhq.template.model.vo.resource.ResourcePageVO;
import com.github.starhq.template.model.vo.resource.ResourceSimpleVO;

import java.io.Serializable;
import java.util.List;

/**
 * Service interface for resource management with permission control and role-based access.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysResource} entities, while adding business-level methods for resource queries,
 * user permission resolution, role-based resource assignment, and resource lifecycle management.
 * Designed to centralize resource logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Resource Management</strong>: CRUD operations for defining API endpoints, data scopes, or business resources in admin console</li>
 *     <li><strong>Permission Resolution</strong>: Fetch user-accessible resources for dynamic authorization checks</li>
 *     <li><strong>Role Configuration</strong>: List resources with checked state for role-based permission assignment</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured resource metadata for permission-based UI rendering and API access control</li>
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
 * // Controller: Expose resource management endpoints
 * @RestController
 * @RequestMapping("/api/v1/resources")
 * @RequiredArgsConstructor
 * public class ResourceController {
 *
 *     private final ResourceService resourceService;
 *
 *     @GetMapping
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public Result<IPage<ResourcePageVO>> listResources(PageRequest request) {
 *         IPage<ResourcePageVO> page = resourceService.page(request);
 *         return Result.success(page.getRecords(), page.getTotal());
 *     }
 *
 *     @GetMapping("/user/{userId}")
 *     public Result<List<SysResource>> getUserResources(@PathVariable Long userId) {
 *         List<SysResource> resources = resourceService.selectByUserId(userId);
 *         return Result.success(resources);
 *     }
 * }
 *
 * // Frontend: Use resources for permission-based UI rendering
 * const hasPermission = (resourceCode: string) => {
 *   return userResources.value.some(r => r.code === resourceCode);
 * };
 *
 * // Usage in template
 * <a-button v-if="hasPermission('user:export')">Export</a-button>
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysResource
 * @see PageRequest
 * @see ResourcePageVO
 * @see ResourceCheckVO
 * @see ResourceSimpleVO
 * @see ResourceDTO
 */
public interface ResourceService extends IService<SysResource> {

    /**
     * Retrieves a paginated list of resource definitions matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient resource management:
     * <ul>
     *     <li><strong>Code Filter</strong>: Exact match on resource code (e.g., {@code "user:export"}) for precise lookups</li>
     *     <li><strong>Name Filter</strong>: Fuzzy search on resource name for admin convenience</li>
     *     <li><strong>Type Filter</strong>: Filter by resource type (API, DATA, UI) for scoped queries</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for bulk operations</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageInfo.getCode()}: Optional; performs exact match on resource code</li>
     *     <li>{@code pageInfo.getName()}: Optional; performs right-fuzzy match on resource name</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysResource} entity is converted to {@link ResourcePageVO} with audit fields</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code page()} calls for compliance tracking</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration attacks on resource definitions</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure composite indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_resource_code_name ON sys_resource(code, name);
     *         CREATE INDEX idx_resource_type_status ON sys_resource(type, status);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching frequent queries with short TTL for admin console responsiveness</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service call: Query resources with filters
     * PageRequest request = new PageRequest();
     * request.setPage(1);
     * request.setSize(20);
     * request.setCode("user:export");
     *
     * IPage<ResourcePageVO> result = resourceService.page(request);
     *
     * // Process results
     * result.getRecords().forEach(resource -> {
     *     System.out.println(resource.getCode() + ": " + resource.getName());
     *     // Output: "user:export: Export User Data"
     * });
     * }
     * </pre>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link ResourcePageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException if {@code pageInfo} is {@code null}
     * @see PageRequest
     * @see ResourcePageVO
     * @see IPage
     */
    IPage<ResourcePageVO> page(PageRequest pageInfo);

    /**
     * Retrieves the list of resources accessible to a specific user based on assigned roles and permissions.
     * <p>
     * This method resolves a user's effective resource permissions by:
     * <ol>
     *     <li>Fetching all roles assigned to the user</li>
     *     <li>Collecting all resources assigned to those roles</li>
     *     <li>Filtering by resource status (enabled) for active permissions only</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must not be {@code null}; the primary key of the user to query</li>
     *     <li>Lookup strategy: Typically joins {@code sys_user} → {@code sys_user_role} → {@code sys_role_resource} → {@code sys_resource}</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link SysResource} entities accessible to the user</li>
     *     <li><strong>No Permissions</strong>: Returns empty list if user has no roles or roles have no resources</li>
     *     <li><strong>Null Safety</strong>: Never returns {@code null}; empty list indicates no permissions</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Authorization</strong>: Caller should have permission to query target user's permissions (self or admin)</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching results by {@code userId} with short TTL for repeated permission checks</li>
     *     <li><strong>Real-time Updates</strong>: Cache should be invalidated when user roles or resource assignments change</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Store permissions in Pinia store
     * const userStore = useUserStore();
     * const resources = ref<SysResource[]>([]);
     *
     * // Load permissions on login
     * const loadResources = async (userId: number) => {
     *   const { data } = await api.getUserResources(userId);
     *   resources.value = data;
     *   userStore.setResources(data);
     * };
     *
     * // Usage in template: Permission-based button rendering
     * <a-button v-if="resources.some(r => r.code === 'user:export')">
     *   Export
     * </a-button>
     *
     * // Usage in logic: Programmatic permission check
     * const canExport = () => resources.value.some(r => r.code === 'user:export');
     * }
     * </pre>
     * <p>
     * <strong>Performance Optimization:</strong>
     * <ul>
     *     <li><strong>Batch Query</strong>: Use single JOIN query instead of N+1 lookups for role/resource resolution</li>
     *     <li><strong>Index Strategy</strong>: Ensure indexes on junction tables ({@code user_role}, {@code role_resource}) for efficient joins</li>
     *     <li><strong>Result Deduplication</strong>: Use {@code DISTINCT} or {@code Set} to eliminate duplicate resources from multiple roles</li>
     * </ul>
     *
     * @param userId the primary key of the user to query; must not be {@code null}
     * @return list of {@link SysResource} accessible to the user; never {@code null}
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     */
    List<SysResource> selectByUserId(Serializable userId);

    /**
     * Retrieves simplified resource metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full resource queries when only
     * basic identification fields ({@code id}, {@code name}, {@code code}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the resource to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link ResourceSimpleVO} with {@code id}, {@code name}, {@code code}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no resource exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes audit metadata for minimal payload</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: Resource metadata is referenced frequently in permission checks</li>
     *     <li><strong>Low Write Frequency</strong>: Resource definitions change infrequently</li>
     *     <li><strong>Small Payload</strong>: {@link ResourceSimpleVO} contains minimal fields for efficient cache storage</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "resources", key = "#id", unless = "#result == null")
     * public ResourceSimpleVO getResourceById(Serializable id) { ... }
     *
     * // Cache invalidation on resource update/delete
     * @CacheEvict(value = "resources", key = "#id")
     * public boolean updateResource(Serializable id, ResourceDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch resource for dropdown population
     * ResourceSimpleVO resource = resourceService.getResourceById(2001L);
     * if (resource != null) {
     *     // Use for UI rendering
     *     dropdownOptions.add(new SelectOption(resource.getCode(), resource.getName()));
     * }
     *
     * // Frontend: Populate select with resource options
     * const { data: resourceOptions } = useRequest(() => api.getResourceOptions());
     * const resourceOptions = computed(() =>
     *   resourceOptions.value?.map(opt => ({ label: opt.name, value: opt.code })) || []
     * );
     * }
     * </pre>
     *
     * @param id the primary key of the resource to retrieve; must not be {@code null}
     * @return {@link ResourceSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see ResourceSimpleVO
     */
    ResourceSimpleVO getResourceById(Serializable id);

    /**
     * Retrieves a list of resources with checked state for role-based permission configuration.
     * <p>
     * This method supports the "assign resources to role" workflow in admin consoles by:
     * <ol>
     *     <li>Fetching all available resources (typically filtered by status=enabled)</li>
     *     <li>Joining with role-resource assignments to determine {@code checked} state</li>
     *     <li>Returning {@link ResourceCheckVO} with {@code id}, {@code name}, {@code code}, {@code checked} fields</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleId}: Must not be {@code null}; the primary key of the role to configure</li>
     *     <li>Checked state logic: {@code checked = true} if resource is assigned to role; {@code false} otherwise</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link ResourceCheckVO} with pre-computed {@code checked} flags</li>
     *     <li><strong>Empty Result</strong>: Returns empty list if no resources exist or role has no assignments</li>
     *     <li><strong>Ordering</strong>: Results typically sorted by {@code type}, {@code code} for UI consistency</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Checkbox group for role resource assignment
     * <a-checkbox-group v-model="checkedResourceCodes">
     *   <a-checkbox
     *     v-for="res in resources"
     *     :key="res.id"
     *     :value="res.code"
     *     :checked="res.checked"
     *   >
     *     {{ res.name }} <small class="text-gray-500">({{ res.code }})</small>
     *   </a-checkbox>
     * </a-checkbox-group>
     *
     * // Submit: Send only changed resource codes to backend
     * const handleSubmit = async () => {
     *   const added = checkedResourceCodes.value.filter(code => !initialCodes.includes(code));
     *   const removed = initialCodes.filter(code => !checkedResourceCodes.value.includes(code));
     *
     *   await api.updateRoleResources(roleId.value, { added, removed });
     * };
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Single Query</strong>: Use {@code LEFT JOIN} + {@code CASE WHEN} to compute {@code checked} in database</li>
     *     <li><strong>Index Strategy</strong>: Ensure index on {@code sys_role_resource(role_id, resource_id)} for efficient join</li>
     *     <li><strong>Cache Strategy</strong>: Cache results by {@code roleId} with invalidation on role-resource changes</li>
     * </ul>
     *
     * @param roleId the primary key of the role to configure; must not be {@code null}
     * @return list of {@link ResourceCheckVO} with computed {@code checked} state; never {@code null}
     * @throws IllegalArgumentException if {@code roleId} is {@code null}
     * @see ResourceCheckVO
     */
    List<ResourceCheckVO> selectCheckedResources(Serializable roleId);

    /**
     * Updates an existing resource definition with validation and conflict prevention.
     * <p>
     * This method handles the complete resource update workflow including:
     * <ul>
     *     <li>Existence check (ensure resource with given {@code id} exists)</li>
     *     <li>Code uniqueness validation (if {@code code} is being changed)</li>
     *     <li>Field-level updates (name, code, type, description, status, etc.)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected resource metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the resource to update</li>
     *     <li>{@code resourceDto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code resourceDto.getCode()}: If changed, must be unique across all resources (excluding current entry)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if resource was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no resource exists with given {@code id}</li>
     *     <li><strong>Duplicate Code</strong>: Returns {@code false} or throws exception if new {@code code} conflicts</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same resource are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidate related caches to ensure consistency:
     * <pre>
     * {@code
     * // Invalidate resource metadata cache by ID
     * cacheHelper.evict(List.of(id), List.of(CacheConstant.RESOURCE));
     *
     * // Invalidate user permission cache if resource code changed
     * if (codeChanged) {
     *     cacheHelper.evictByPattern(CacheConstant.USER_RESOURCES, "*");
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle resource update request
     * @PutMapping("/{id}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Void> updateResource(@PathVariable Long id, @Valid @RequestBody ResourceDTO dto) {
     *     boolean success = resourceService.updateResource(id, dto);
     *
     *     if (success) {
     *         return Result.success("Resource updated successfully");
     *     } else {
     *         return Result.fail(ErrorCode.RESOURCE_UPDATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Update logic
     * @Transactional
     * @Override
     * public boolean updateResource(Serializable id, ResourceDTO dto) {
     *     // 1. Check existence
     *     SysResource existing = getById(id);
     *     if (existing == null) {
     *         return false;
     *     }
     *
     *     // 2. Validate code uniqueness if changing
     *     if (!existing.getCode().equals(dto.getCode()) &&
     *         lambdaQuery().eq(SysResource::getCode, dto.getCode()).ne(SysResource::getId, id).exists()) {
     *         throw new DuplicateException(ErrorCode.RESOURCE_CODE_EXISTS);
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
     *         cacheHelper.evict(List.of(id), List.of(CacheConstant.RESOURCE));
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param id          the primary key of the resource to update; must not be {@code null}
     * @param resourceDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if resource was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException if {@code id} or {@code resourceDto} is {@code null}
     * @throws BusinessException        if new {@code code} conflicts with existing resource
     * @see ResourceDTO
     */
    boolean updateResource(Serializable id, ResourceDTO resourceDto);

    /**
     * Creates a new resource definition with validation and duplicate prevention.
     * <p>
     * This method handles the complete resource creation workflow including:
     * <ul>
     *     <li>Input validation (code uniqueness, name format, type validation)</li>
     *     <li>Resource code format enforcement (e.g., {@code module:resource:action})</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code resourceDto}: Must not be {@code null}; should include {@code name}, {@code code}, {@code type}</li>
     *     <li>{@code resourceDto.getCode()}: Must be unique across all resources; format: {@code [a-z][a-z0-9]*(:[a-z][a-z0-9]*){1,3}}</li>
     *     <li>{@code resourceDto.getType()}: Must be valid resource type (API, DATA, UI)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if resource was successfully created</li>
     *     <li><strong>Duplicate Code</strong>: Returns {@code false} or throws exception if {@code code} already exists</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Resource creation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same code are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Code Validation</strong>: Enforce resource code format to prevent injection or privilege escalation</li>
     *     <li><strong>Audit Logging</strong>: Log resource creation events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle resource creation request
     * @PostMapping
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Long> createResource(@Valid @RequestBody ResourceDTO dto) {
     *     boolean success = resourceService.createResource(dto);
     *
     *     if (success) {
     *         // Optional: Return new resource ID for frontend reference
     *         return Result.success(dto.getId());
     *     } else {
     *         return Result.fail(ErrorCode.RESOURCE_CREATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Creation logic
     * @Transactional
     * @Override
     * public boolean createResource(ResourceDTO dto) {
     *     // 1. Validate input
     *     validateResourceDto(dto);
     *
     *     // 2. Check code uniqueness
     *     if (lambdaQuery().eq(SysResource::getCode, dto.getCode()).exists()) {
     *         throw new DuplicateException(ErrorCode.RESOURCE_CODE_EXISTS);
     *     }
     *
     *     // 3. Convert DTO to entity
     *     SysResource entity = converter.toEntity(dto);
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
     * @param resourceDto the resource creation data; must not be {@code null}
     * @return {@code true} if resource was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException if {@code resourceDto} is {@code null} or missing required fields
     * @throws BusinessException        if {@code code} already exists or validation fails
     * @see ResourceDTO
     */
    boolean createResource(ResourceDTO resourceDto);

}