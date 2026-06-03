package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.model.dto.DictDataDTO;
import com.github.starhq.template.model.dto.DictDataPageRequest;
import com.github.starhq.template.model.vo.DictDataPageVO;
import com.github.starhq.template.model.vo.DictDataSimpleVO;

import java.io.Serializable;

/**
 * Service interface for dictionary data management with CRUD operations and caching support.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysDictData} entities, while adding business-level methods for paginated queries,
 * lightweight metadata retrieval, and dictionary data lifecycle management. Designed to centralize
 * dictionary data logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Dictionary Data Management</strong>: CRUD operations for defining label/value pairs in admin console</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured dictionary data for dropdowns, radio buttons, and tags</li>
 *     <li><strong>Cache Optimization</strong>: Frequent dictionary reads leverage caching for low-latency UI rendering</li>
 *     <li><strong>Business Logic</strong>: Support label/value separation for i18n-ready UI components</li>
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
 * // Controller: Expose dictionary data management endpoints
 * @RestController
 * @RequestMapping("/api/v1/dict-data")
 * @RequiredArgsConstructor
 * public class DictDataController {
 *
 *     private final DictDataService dictDataService;
 *
 *     @GetMapping
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public Result<IPage<DictDataPageVO>> listDictData(DictDataPageRequest request) {
 *         IPage<DictDataPageVO> page = dictDataService.page(request);
 *         return Result.success(page.getRecords(), page.getTotal());
 *     }
 *
 *     @GetMapping("/simple/{id}")
 *     public Result<DictDataSimpleVO> getDictData(@PathVariable Long id) {
 *         DictDataSimpleVO data = dictDataService.getDictDataById(id);
 *         return Result.success(data);
 *     }
 * }
 *
 * // Frontend: Use dictionary data for UI components
 * const { data: dictOptions } = useRequest(() => api.getDictDataByType('user_status'));
 * // Render dropdown
 * <a-select :options="dictOptions" v-model="form.status" />
 * // Options: [{ label: "Enabled", value: "1" }, { label: "Disabled", value: "0" }]
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysDictData
 * @see DictDataPageRequest
 * @see DictDataPageVO
 * @see DictDataSimpleVO
 * @see DictDataDTO
 */
public interface DictDataService extends IService<SysDictData> {

    /**
     * Retrieves a paginated list of dictionary data entries matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient dictionary management:
     * <ul>
     *     <li><strong>Type Filter</strong>: Filter by dictionary type ID to scope queries to specific domains</li>
     *     <li><strong>Label Filter</strong>: Fuzzy search on human-readable label for admin convenience</li>
     *     <li><strong>Value Filter</strong>: Exact match on machine-readable value for precise lookups</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for bulk operations</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageInfo.getTypeId()}: Optional; filters dictionary data by parent type ID</li>
     *     <li>{@code pageInfo.getLabel()}: Optional; performs right-fuzzy match on label for admin search</li>
     *     <li>{@code pageInfo.getValue()}: Optional; performs exact match on value for precise queries</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysDictData} entity is converted to {@link DictDataPageVO} with audit fields</li>
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
     *         CREATE INDEX idx_dict_data_type_label ON sys_dict_data(type_id, label);
     *         CREATE INDEX idx_dict_data_value_status ON sys_dict_data(value, status);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Cache Strategy</strong>: Consider caching frequent queries with short TTL for admin console responsiveness</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service call: Query dictionary data with filters
     * DictDataPageRequest request = new DictDataPageRequest();
     * request.setPage(1);
     * request.setSize(20);
     * request.setTypeId(1001L);
     * request.setLabel("Enabled");
     *
     * IPage<DictDataPageVO> result = dictDataService.page(request);
     *
     * // Process results
     * result.getRecords().forEach(data -> {
     *     System.out.println(data.getLabel() + " = " + data.getValue());
     *     // Output: "Enabled = 1"
     * });
     * }
     * </pre>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link DictDataPageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException if {@code pageInfo} is {@code null}
     * @see DictDataPageRequest
     * @see DictDataPageVO
     * @see IPage
     */
    IPage<DictDataPageVO> page(DictDataPageRequest pageInfo);

    /**
     * Retrieves simplified dictionary data metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full dictionary data queries when only
     * basic identification fields ({@code id}, {@code label}, {@code value}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the dictionary data to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link DictDataSimpleVO} with {@code id}, {@code label}, {@code value}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no dictionary data exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes audit metadata for minimal payload</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: Dictionary data is referenced frequently in UI rendering</li>
     *     <li><strong>Low Write Frequency</strong>: Dictionary definitions change infrequently</li>
     *     <li><strong>Small Payload</strong>: {@link DictDataSimpleVO} contains minimal fields for efficient cache storage</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "dictData", key = "#id", unless = "#result == null")
     * public DictDataSimpleVO getDictDataById(Serializable id) { ... }
     *
     * // Cache invalidation on dictionary data update/delete
     * @CacheEvict(value = "dictData", key = "#id")
     * public boolean updateDictData(Serializable id, DictDataDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch dictionary data for dropdown population
     * DictDataSimpleVO data = dictDataService.getDictDataById(2001L);
     * if (data != null) {
     *     // Use for UI rendering
     *     dropdownOptions.add(new SelectOption(data.getValue(), data.getLabel()));
     * }
     *
     * // Frontend: Populate select with dictionary options
     * const { data: dictOptions } = useRequest(() => api.getDictDataOptions());
     * const dictOptions = computed(() =>
     *   dictOptions.value?.map(opt => ({ label: opt.label, value: opt.value })) || []
     * );
     * }
     * </pre>
     *
     * @param id the primary key of the dictionary data to retrieve; must not be {@code null}
     * @return {@link DictDataSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see DictDataSimpleVO
     */
    DictDataSimpleVO getDictDataById(Serializable id);

    /**
     * Creates a new dictionary data entry with validation and duplicate prevention.
     * <p>
     * This method handles the complete dictionary data creation workflow including:
     * <ul>
     *     <li>Input validation (label uniqueness within type, value format, type reference)</li>
     *     <li>Label/value format enforcement (e.g., label: human-readable, value: machine-readable code)</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code dictDataDto}: Must not be {@code null}; should include {@code label}, {@code value}, {@code typeId}</li>
     *     <li>{@code dictDataDto.getLabel()}: Must be unique within the same {@code typeId}; human-readable for UI display</li>
     *     <li>{@code dictDataDto.getValue()}: Must be unique within the same {@code typeId}; machine-readable for business logic</li>
     *     <li>{@code dictDataDto.getTypeId()}: Must reference existing dictionary type; foreign key constraint enforced</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if dictionary data was successfully created</li>
     *     <li><strong>Duplicate Label/Value</strong>: Returns {@code false} or throws exception if label/value already exists within type</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Dictionary data creation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same label/value are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Label/Value Validation</strong>: Enforce format to prevent injection or privilege escalation</li>
     *     <li><strong>Audit Logging</strong>: Log dictionary data creation events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle dictionary data creation request
     * @PostMapping
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Long> createDictData(@Valid @RequestBody DictDataDTO dto) {
     *     boolean success = dictDataService.createDictData(dto);
     *
     *     if (success) {
     *         // Optional: Return new dictionary data ID for frontend reference
     *         return Result.success(dto.getId());
     *     } else {
     *         return Result.fail(ErrorCode.DICT_DATA_CREATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Creation logic
     * @Transactional
     * @Override
     * public boolean createDictData(DictDataDTO dto) {
     *     // 1. Validate input
     *     validateDictDataDto(dto);
     *
     *     // 2. Check label/value uniqueness within type
     *     if (lambdaQuery().eq(SysDictData::getTypeId, dto.getTypeId())
     *         .and(q -> q.eq(SysDictData::getLabel, dto.getLabel())
     *                  .or().eq(SysDictData::getValue, dto.getValue())))
     *         .exists()) {
     *         throw new DuplicateException(ErrorCode.DICT_DATA_DUPLICATE);
     *     }
     *
     *     // 3. Convert DTO to entity
     *     SysDictData entity = converter.toEntity(dto);
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
     * @param dictDataDto the dictionary data creation data; must not be {@code null}
     * @return {@code true} if dictionary data was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException if {@code dictDataDto} is {@code null} or missing required fields
     * @throws BusinessException        if label/value already exists within type or validation fails
     * @see DictDataDTO
     */
    boolean createDictData(DictDataDTO dictDataDto);

    /**
     * Updates an existing dictionary data entry with validation and conflict prevention.
     * <p>
     * This method handles the complete dictionary data update workflow including:
     * <ul>
     *     <li>Existence check (ensure dictionary data with given {@code id} exists)</li>
     *     <li>Label/value uniqueness validation (if {@code label} or {@code value} is being changed)</li>
     *     <li>Field-level updates (label, value, sort, description, status, etc.)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected dictionary data metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the dictionary data to update</li>
     *     <li>{@code dictDataDto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code dictDataDto.getLabel()}/{@code getValue()}: If changed, must be unique within same {@code typeId} (excluding current entry)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if dictionary data was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no dictionary data exists with given {@code id}</li>
     *     <li><strong>Duplicate Label/Value</strong>: Returns {@code false} or throws exception if new label/value conflicts</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same dictionary data are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidate related caches to ensure consistency:
     * <pre>
     * {@code
     * // Invalidate dictionary data metadata cache by ID
     * cacheHelper.evict(List.of(id), List.of(CacheConstant.DICT_DATA));
     *
     * // Invalidate type-level cache if label/value changed (affects dropdown options)
     * if (labelOrValueChanged) {
     *     cacheHelper.evictByPattern(CacheConstant.DICT_DATA_BY_TYPE, dto.getTypeId() + "*");
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle dictionary data update request
     * @PutMapping("/{id}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Void> updateDictData(@PathVariable Long id, @Valid @RequestBody DictDataDTO dto) {
     *     boolean success = dictDataService.updateDictData(id, dto);
     *
     *     if (success) {
     *         return Result.success("Dictionary data updated successfully");
     *     } else {
     *         return Result.fail(ErrorCode.DICT_DATA_UPDATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Update logic
     * @Transactional
     * @Override
     * public boolean updateDictData(Serializable id, DictDataDTO dto) {
     *     // 1. Check existence
     *     SysDictData existing = getById(id);
     *     if (existing == null) {
     *         return false;
     *     }
     *
     *     // 2. Validate label/value uniqueness if changing
     *     if ((!existing.getLabel().equals(dto.getLabel()) || !existing.getValue().equals(dto.getValue())) &&
     *         lambdaQuery().eq(SysDictData::getTypeId, dto.getTypeId())
     *             .and(q -> q.eq(SysDictData::getLabel, dto.getLabel())
     *                      .or().eq(SysDictData::getValue, dto.getValue()))
     *             .ne(SysDictData::getId, id)
     *             .exists()) {
     *         throw new DuplicateException(ErrorCode.DICT_DATA_DUPLICATE);
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
     *         cacheHelper.evict(List.of(id), List.of(CacheConstant.DICT_DATA));
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param id          the primary key of the dictionary data to update; must not be {@code null}
     * @param dictDataDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if dictionary data was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException if {@code id} or {@code dictDataDto} is {@code null}
     * @throws BusinessException        if new label/value conflicts with existing entry within same type
     * @see DictDataDTO
     */
    boolean updateDictData(Serializable id, DictDataDTO dictDataDto);

}