package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;

import java.io.Serializable;
import java.util.List;

/**
 * Service interface for dictionary type management with CRUD operations and nested data loading.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysDictType} entities, while adding business-level methods for paginated queries,
 * lightweight metadata retrieval, nested type+data loading, and dictionary type lifecycle management.
 * Designed to centralize dictionary type logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Dictionary Type Management</strong>: CRUD operations for defining type categories in admin console</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured dictionary types with nested data for cascading selectors</li>
 *     <li><strong>Cache Optimization</strong>: Frequent dictionary type reads leverage caching for low-latency UI rendering</li>
 *     <li><strong>Business Logic</strong>: Support type/code separation for i18n-ready configuration management</li>
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
 * // Controller: Expose dictionary type management endpoints
 * @RestController
 * @RequestMapping("/api/v1/dict-types")
 * @RequiredArgsConstructor
 * public class DictTypeController {
 *
 *     private final DictTypeService dictTypeService;
 *
 *     @GetMapping
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public Result<IPage<DictTypePageVO>> listDictTypes(PageRequest request) {
 *         IPage<DictTypePageVO> page = dictTypeService.page(request);
 *         return Result.success(page.getRecords(), page.getTotal());
 *     }
 *
 *     @GetMapping("/with-data")
 *     public Result<List<DictTypeWithDataVO>> getAllTypesWithData() {
 *         List<DictTypeWithDataVO> types = dictTypeService.selectDictTypeAndDataResponses();
 *         return Result.success(types);
 *     }
 * }
 *
 * // Frontend: Use dictionary types for cascading selectors
 * const { data: dictTypes } = useRequest(() => api.getDictTypesWithData());
 * // Render cascading selector
 * <a-cascader :options="dictTypes" v-model="form.dictSelection" />
 * // Options: [{ label: "User Status", value: "user_status", children: [{ label: "Enabled", value: "1" }] }]
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysDictType
 * @see PageRequest
 * @see DictTypePageVO
 * @see DictTypeSimpleVO
 * @see DictTypeWithDataVO
 * @see DictTypeDTO
 */
public interface DictTypeService extends IService<SysDictType> {

    /**
     * Retrieves a paginated list of dictionary type definitions matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient dictionary type management:
     * <ul>
     *     <li><strong>Type Filter</strong>: Exact match on technical type code (e.g., {@code "user_status"})</li>
     *     <li><strong>Name Filter</strong>: Fuzzy search on human-readable type name for admin convenience</li>
     *     <li><strong>Description Filter</strong>: Fuzzy search on type description for documentation lookup</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for bulk operations</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageInfo.getType()}: Optional; performs exact match on type code for precise lookups</li>
     *     <li>{@code pageInfo.getName()}: Optional; performs right-fuzzy match on name for admin search</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysDictType} entity is converted to {@link DictTypePageVO} with audit fields</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code page()} calls for compliance tracking</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration attacks on dictionary definitions</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure composite indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_dict_type_code_name ON sys_dict_type(type, name);
     *         CREATE INDEX idx_dict_type_status ON sys_dict_type(status);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching frequent queries with short TTL for admin console responsiveness</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service call: Query dictionary types with filters
     * PageRequest request = new PageRequest();
     * request.setPage(1);
     * request.setSize(20);
     * request.setType("user_status");
     *
     * IPage<DictTypePageVO> result = dictTypeService.page(request);
     *
     * // Process results
     * result.getRecords().forEach(type -> {
     *     System.out.println(type.getType() + ": " + type.getName());
     *     // Output: "user_status: User Status"
     * });
     * }
     * </pre>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link DictTypePageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException if {@code pageInfo} is {@code null}
     * @see PageRequest
     * @see DictTypePageVO
     * @see IPage
     */
    IPage<DictTypePageVO> page(PageRequest pageInfo);

    /**
     * Retrieves simplified dictionary type metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full dictionary type queries when only
     * basic identification fields ({@code id}, {@code type}, {@code name}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the dictionary type to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link DictTypeSimpleVO} with {@code id}, {@code type}, {@code name}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no dictionary type exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes audit metadata for minimal payload</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: Dictionary type metadata is referenced frequently in UI rendering</li>
     *     <li><strong>Low Write Frequency</strong>: Dictionary type definitions change infrequently</li>
     *     <li><strong>Small Payload</strong>: {@link DictTypeSimpleVO} contains minimal fields for efficient cache storage</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "dictType", key = "#id", unless = "#result == null")
     * public DictTypeSimpleVO getDictDataById(Serializable id) { ... }
     *
     * // Cache invalidation on dictionary type update/delete
     * @CacheEvict(value = "dictType", key = "#id")
     * public boolean updateDictType(Serializable id, DictTypeDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch dictionary type for dropdown population
     * DictTypeSimpleVO type = dictTypeService.getDictDataById(1001L);
     * if (type != null) {
     *     // Use for UI rendering
     *     dropdownOptions.add(new SelectOption(type.getType(), type.getName()));
     * }
     *
     * // Frontend: Populate select with dictionary type options
     * const { data: typeOptions } = useRequest(() => api.getDictTypeOptions());
     * const typeOptions = computed(() =>
     *   typeOptions.value?.map(opt => ({ label: opt.name, value: opt.type })) || []
     * );
     * }
     * </pre>
     *
     * @param id the primary key of the dictionary type to retrieve; must not be {@code null}
     * @return {@link DictTypeSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see DictTypeSimpleVO
     */
    DictTypeSimpleVO getDictDataById(Serializable id);

    /**
     * Retrieves all dictionary types with their associated data entries for cascading selectors or configuration export.
     * <p>
     * This method provides a nested structure where each dictionary type includes its child data entries:
     * <pre>
     * {@code
     * [
     *   {
     *     "id": 1001,
     *     "type": "user_status",
     *     "name": "User Status",
     *     "children": [
     *       { "id": 2001, "label": "Enabled", "value": "1" },
     *       { "id": 2002, "label": "Disabled", "value": "0" }
     *     ]
     *   }
     * ]
     * }
     * </pre>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link DictTypeWithDataVO} with nested {@code children} data entries</li>
     *     <li><strong>Empty Result</strong>: Returns empty list if no dictionary types exist</li>
     *     <li><strong>Ordering</strong>: Results typically sorted by {@code sort_order} for UI consistency</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is highly cacheable due to:
     * <ul>
     *     <li><strong>Read-Heavy</strong>: Called frequently for frontend configuration loading</li>
     *     <li><strong>Write-Rare</strong>: Dictionary type definitions change infrequently</li>
     *     <li><strong>Aggregated Payload</strong>: Single query returns complete type+data hierarchy</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Cache entire type+data hierarchy with moderate TTL
     * @Cacheable(value = "dictTypeWithData", key = "'all'")
     * public List<DictTypeWithDataVO> selectDictTypeAndDataResponses() { ... }
     *
     * // Invalidate on any dictionary type or data change
     * @CacheEvict(value = "dictTypeWithData", key = "'all'")
     * public boolean updateDictType(Serializable id, DictTypeDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>N+1 Prevention</strong>: Use single JOIN query or batch loading to fetch types + data efficiently</li>
     *     <li><strong>Result Size</strong>: Monitor total payload size; consider pagination if dictionary definitions grow large</li>
     *     <li><strong>Cache Invalidation</strong>: Ensure cache is evicted when any dictionary type or data entry changes</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Return all dictionary types with nested data for frontend config
     * @GetMapping("/with-data")
     * public Result<List<DictTypeWithDataVO>> getAllTypesWithData() {
     *     List<DictTypeWithDataVO> types = dictTypeService.selectDictTypeAndDataResponses();
     *     return Result.success(types);
     * }
     *
     * // Frontend: Use for cascading selector or configuration initialization
     * const { data: dictConfig } = useRequest(() => api.getDictTypesWithData());
     *
     * // Render cascading selector
     * <a-cascader :options="dictConfig" v-model="form.selection" />
     *
     * // Or initialize form with default values
     * const initForm = () => {
     *   form.value.status = dictConfig.value
     *     .find(t => t.type === 'user_status')
     *     ?.children?.find(d => d.value === '1')?.value;
     * };
     * }
     * </pre>
     *
     * @return list of {@link DictTypeWithDataVO} with nested data entries; never {@code null}
     * @see DictTypeWithDataVO
     */
    List<DictTypeWithDataVO> selectDictTypeAndDataResponses();

    /**
     * Creates a new dictionary type definition with validation and duplicate prevention.
     * <p>
     * This method handles the complete dictionary type creation workflow including:
     * <ul>
     *     <li>Input validation (type code uniqueness, name format, description length)</li>
     *     <li>Type code format enforcement (e.g., {@code snake_case} or {@code camelCase})</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code dictTypeDto}: Must not be {@code null}; should include {@code type}, {@code name}, {@code description}</li>
     *     <li>{@code dictTypeDto.getType()}: Must be unique across all dictionary types; format: {@code [a-z][a-z0-9_]*}</li>
     *     <li>{@code dictTypeDto.getName()}: Human-readable name for admin UI; should be concise (≤ 50 chars)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if dictionary type was successfully created</li>
     *     <li><strong>Duplicate Type Code</strong>: Returns {@code false} or throws exception if {@code type} already exists</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Dictionary type creation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same type code are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Type Code Validation</strong>: Enforce format to prevent injection or privilege escalation</li>
     *     <li><strong>Audit Logging</strong>: Log dictionary type creation events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle dictionary type creation request
     * @PostMapping
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Long> createDictType(@Valid @RequestBody DictTypeDTO dto) {
     *     boolean success = dictTypeService.createDictType(dto);
     *
     *     if (success) {
     *         // Optional: Return new dictionary type ID for frontend reference
     *         return Result.success(dto.getId());
     *     } else {
     *         return Result.fail(ErrorCode.DICT_TYPE_CREATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Creation logic
     * @Transactional
     * @Override
     * public boolean createDictType(DictTypeDTO dto) {
     *     // 1. Validate input
     *     validateDictTypeDto(dto);
     *
     *     // 2. Check type code uniqueness
     *     if (lambdaQuery().eq(SysDictType::getType, dto.getType()).exists()) {
     *         throw new DuplicateException(ErrorCode.DICT_TYPE_DUPLICATE_CODE);
     *     }
     *
     *     // 3. Convert DTO to entity
     *     SysDictType entity = converter.toEntity(dto);
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
     * @param dictTypeDto the dictionary type creation data; must not be {@code null}
     * @return {@code true} if dictionary type was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException if {@code dictTypeDto} is {@code null} or missing required fields
     * @throws BusinessException        if type code already exists or validation fails
     * @see DictTypeDTO
     */
    boolean createDictType(DictTypeDTO dictTypeDto);

    /**
     * Updates an existing dictionary type definition with validation and conflict prevention.
     * <p>
     * This method handles the complete dictionary type update workflow including:
     * <ul>
     *     <li>Existence check (ensure dictionary type with given {@code id} exists)</li>
     *     <li>Type code uniqueness validation (if {@code type} is being changed)</li>
     *     <li>Field-level updates (type, name, description, sort, status, etc.)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected dictionary type metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the dictionary type to update</li>
     *     <li>{@code dictTypeDto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code dictTypeDto.getType()}: If changed, must be unique across all dictionary types (excluding current entry)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if dictionary type was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no dictionary type exists with given {@code id}</li>
     *     <li><strong>Duplicate Type Code</strong>: Returns {@code false} or throws exception if new {@code type} conflicts</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same dictionary type are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidate related caches to ensure consistency:
     * <pre>
     * {@code
     * // Invalidate dictionary type metadata cache by ID
     * cacheHelper.evict(List.of(id), List.of(CacheConstant.DICT_TYPE));
     *
     * // Invalidate aggregated type+data cache if type code changed (affects cascading selectors)
     * if (typeCodeChanged) {
     *     cacheHelper.evict(List.of("all"), List.of(CacheConstant.DICT_TYPE_WITH_DATA));
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle dictionary type update request
     * @PutMapping("/{id}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Void> updateDictType(@PathVariable Long id, @Valid @RequestBody DictTypeDTO dto) {
     *     boolean success = dictTypeService.updateDictType(id, dto);
     *
     *     if (success) {
     *         return Result.success("Dictionary type updated successfully");
     *     } else {
     *         return Result.fail(ErrorCode.DICT_TYPE_UPDATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Update logic
     * @Transactional
     * @Override
     * public boolean updateDictType(Serializable id, DictTypeDTO dto) {
     *     // 1. Check existence
     *     SysDictType existing = getById(id);
     *     if (existing == null) {
     *         return false;
     *     }
     *
     *     // 2. Validate type code uniqueness if changing
     *     if (!existing.getType().equals(dto.getType()) &&
     *         lambdaQuery().eq(SysDictType::getType, dto.getType()).ne(SysDictType::getId, id).exists()) {
     *         throw new DuplicateException(ErrorCode.DICT_TYPE_DUPLICATE_CODE);
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
     *         cacheHelper.evict(List.of(id), List.of(CacheConstant.DICT_TYPE));
     *         if (!existing.getType().equals(dto.getType())) {
     *             cacheHelper.evict(List.of("all"), List.of(CacheConstant.DICT_TYPE_WITH_DATA));
     *         }
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param id          the primary key of the dictionary type to update; must not be {@code null}
     * @param dictTypeDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if dictionary type was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException if {@code id} or {@code dictTypeDto} is {@code null}
     * @throws BusinessException        if new type code conflicts with existing entry
     * @see DictTypeDTO
     */
    boolean updateDictType(Serializable id, DictTypeDTO dictTypeDto);

}