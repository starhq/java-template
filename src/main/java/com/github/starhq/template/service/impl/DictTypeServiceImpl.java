package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.converter.DictTypeConverter;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysDictDataMapper;
import com.github.starhq.template.mapper.SysDictTypeMapper;
import com.github.starhq.template.model.dto.dict.type.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.dict.type.DictTypePageVO;
import com.github.starhq.template.model.vo.dict.type.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dict.type.DictTypeWithDataVO;
import com.github.starhq.template.service.DictTypeService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation for dictionary type management with CRUD operations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link DictTypeService} for dictionary type-specific business operations.
 * Designed to centralize dictionary type management logic with consistent validation, cache integration, and
 * distributed audit logging for compliance tracking.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>Dictionary Type CRUD</strong>: Create, read, update, delete type categories with duplicate prevention</li>
 *     <li><strong>Nested Data Loading</strong>: Provide dictionary types with associated data entries for cascading selectors</li>
 *     <li><strong>Cache Management</strong>: Integrate with Spring Cache and distributed events for consistent type metadata</li>
 *     <li><strong>Audit Integration</strong>: Record all write operations via {@code @AuditLoggable} for compliance</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Cache-Consistent</strong>: Changes trigger cache invalidation via {@link EventService} for multi-node consistency</li>
 *     <li><strong>Audit-Ready</strong>: All write operations annotated with {@code @AuditLoggable} for compliance tracking</li>
 *     <li><strong>Null-Safe</strong>: Uses {@link AuditBaseServiceImpl#getAndCheckById} to prevent NPE in security-critical paths</li>
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
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-09
 * @see DictTypeService
 * @see AuditBaseServiceImpl
 * @see SysDictType
 * @see DictTypePageVO
 * @see DictTypeWithDataVO
 */
@Service("dictTypeService")
public class DictTypeServiceImpl extends AuditBaseServiceImpl<SysDictTypeMapper, SysDictType> implements DictTypeService {

    /**
     * Mapper for {@link SysDictData} database operations (dictionary data entries).
     * <p>
     * Used for cascading delete: removing all dictionary data entries when a dictionary type is deleted,
     * ensuring referential integrity and preventing orphaned data entries.
     *
     * @see SysDictDataMapper
     * @see #removeById(Serializable)
     */
    private final SysDictDataMapper dictDataMapper;

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
     * Converter for transforming between {@link DictTypeDTO}, {@link SysDictType}, and various VO types.
     * <p>
     * Ensures consistent field mapping and avoids boilerplate conversion code across service methods.
     * Supports:
     * <ul>
     *     <li>DTO to entity conversion for persistence</li>
     *     <li>Entity to {@link DictTypePageVO} for admin console pagination</li>
     *     <li>Entity to {@link DictTypeSimpleVO} for lightweight UI references</li>
     *     <li>Entity to {@link DictTypeWithDataVO} for nested type+data hierarchy</li>
     * </ul>
     *
     * @see DictTypeConverter
     */
    private final DictTypeConverter dictTypeConverter;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>
     * Ensures cache consistency when dictionary type data changes (e.g., update, delete) by notifying
     * all nodes to evict related cache entries via distributed messaging.
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Constructs a new {@code DictTypeServiceImpl} with the required dependencies.
     *
     * @param cacheHelper       the cache utility for batch username resolution (inherited from base class)
     * @param dictDataMapper    the mapper for dictionary data operations, used to check data associations before deletion
     * @param userMapperHelper  the helper for resolving user IDs to usernames during audit field population
     * @param dictTypeConverter the converter for transforming between dictionary type entities, DTOs, and VOs
     * @param eventService      the service for publishing domain events (e.g., cache invalidation triggers)
     */
    public DictTypeServiceImpl(CacheHelper cacheHelper,
                               SysDictDataMapper dictDataMapper,
                               SysUserMapperHelper userMapperHelper,
                               DictTypeConverter dictTypeConverter,
                               EventService eventService) {
        super(cacheHelper);
        this.dictDataMapper = dictDataMapper;
        this.userMapperHelper = userMapperHelper;
        this.dictTypeConverter = dictTypeConverter;
        this.eventService = eventService;
    }

    /**
     * Retrieves a paginated list of dictionary type definitions with audit field resolution.
     * <p>
     * This method delegates to {@link AuditBaseServiceImpl#pageVO} to provide:
     * <ul>
     *     <li>Base query building from {@link PageRequest}</li>
     *     <li>Batch username resolution for {@code creator}/{@code updater} fields</li>
     *     <li>Entity-to-VO conversion via {@link DictTypeConverter#toPageVO}</li>
     * </ul>
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
     * @return paginated list of {@link DictTypePageVO} matching the criteria; never {@code null}
     * @see PageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see DictTypeConverter#toPageVO(SysDictType)
     */
    @Override
    public IPage<DictTypePageVO> page(PageRequest pageInfo) {
        return pageVO(pageInfo,
                // No dynamic filters needed for basic pagination
                null,
                // Batch username loader for audit field resolution
                userMapperHelper,
                // Entity to VO converter
                dictTypeConverter::toPageVO);
    }

    /**
     * Retrieves simplified dictionary type metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full dictionary type queries when only
     * basic identification fields ({@code id}, {@code type}, {@code name}) are needed.
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>Uses {@link #getAndCheckById} to throw {@link com.github.starhq.template.common.exception.NotFoundException} if type not found</li>
     *     <li>Ensures caller receives valid {@link DictTypeSimpleVO} or handles exception</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch dictionary type for dropdown population
     * DictTypeSimpleVO type = dictTypeService.getDictDataById(1001L);
     * // Use for UI rendering
     * dropdownOptions.add(new SelectOption(type.getType(), type.getName()));
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
     * @return {@link DictTypeSimpleVO} if found
     * @throws com.github.starhq.template.common.exception.NotFoundException if no dictionary type exists with given {@code id}
     * @see DictTypeSimpleVO
     * @see DictTypeConverter#toSimpleVO(SysDictType)
     * @see #getAndCheckById(Serializable, ErrorCode)
     */
    @Override
    public DictTypeSimpleVO getDictDataById(Serializable id) {
        // Fetch dictionary type with not-found check (throws NotFoundException if missing)
        SysDictType dictType = getAndCheckById(id, ErrorCode.DICT_TYPE_NOT_FOUND);

        // Convert to lightweight VO for UI rendering
        return dictTypeConverter.toSimpleVO(dictType);
    }

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
     * <strong>Cache Strategy:</strong>
     * <p>
     * Annotated with {@code @Cacheable(value = "dictTypes")} to:
     * <ul>
     *     <li>Cache the entire type+data hierarchy under key {@code "dictTypes"} for efficient frontend config loading</li>
     *     <li>Use default cache TTL configured in {@link CacheConstant} (typically 30 min)</li>
     *     <li>Automatically return cached result on subsequent calls to reduce database load</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation:</strong>
     * <p>
     * Cache entries should be evicted when:
     * <ul>
     *     <li>Dictionary type is created/updated/deleted (via {@link #createDictType}, {@link #updateDictType}, {@link #removeById})</li>
     *     <li>Dictionary data entries under a type are modified (via {@code DictDataService})</li>
     *     <li>Use {@link EventService#notifyCacheEvict} for distributed cache invalidation across nodes</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>N+1 Prevention</strong>: {@code selectDictTypesWithData} should use single JOIN query or batch loading</li>
     *     <li><strong>Result Size</strong>: Monitor total payload size; consider pagination if dictionary definitions grow large</li>
     *     <li><strong>Cache TTL</strong>: Short TTL (e.g., 5-10 min) provides fallback consistency if invalidation fails</li>
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
     * @see SysDictTypeMapper#selectDictTypesWithData()
     * @see Cacheable
     */
    @Cacheable(value = "dictTypes")
    @Override
    public List<DictTypeWithDataVO> selectDictTypeAndDataResponses() {
        // Delegate to custom mapper method that fetches types with nested data via efficient JOIN
        return getBaseMapper().selectDictTypesWithData();
    }

    /**
     * Creates a new dictionary type definition with validation, duplicate prevention, and audit logging.
     * <p>
     * This method handles the complete dictionary type creation workflow including:
     * <ol>
     *     <li>DTO-to-entity conversion via {@link DictTypeConverter}</li>
     *     <li>Insertion with duplicate type code handling via {@link #insert}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_INSERT)} to:
     * <ul>
     *     <li>Automatically record dictionary type creation events for compliance tracking</li>
     *     <li>Capture operator ID, timestamp, and operation details in audit log</li>
     *     <li>Enable audit trail queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Duplicate Prevention</strong>: {@link #insert} catches {@code DuplicateKeyException} and throws business-friendly error</li>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Type Code Validation</strong>: Ensure type code format is validated at DTO layer before reaching service</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #insert} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Dictionary type insertion either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Audit log entry is recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same type code are properly serialized by database</li>
     * </ul>
     *
     * @param dictTypeDto the dictionary type creation data; must not be {@code null}
     * @return {@code true} if dictionary type was successfully created
     * @throws IllegalArgumentException                                       if {@code dictTypeDto} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.DuplicateException if {@code type} code already exists
     * @throws com.github.starhq.template.common.exception.BusinessException  if insertion fails for other reasons
     * @see DictTypeDTO
     * @see #insert(Object, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_INSERT)
    @Override
    public boolean createDictType(DictTypeDTO dictTypeDto) {
        // Convert DTO to entity for persistence
        SysDictType entity = dictTypeConverter.toEntity(dictTypeDto);

        // Insert with business error handling (duplicate type code → DICT_TYPE_DUPLICATE_TYPE)
        insert(entity, ErrorCode.DICT_TYPE_DUPLICATE_TYPE, ErrorCode.DICT_TYPE_INSERT_FAILED);

        return true;
    }

    /**
     * Updates an existing dictionary type definition with validation, conflict prevention, and cache invalidation.
     * <p>
     * This method handles the complete dictionary type update workflow including:
     * <ol>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link DictTypeConverter#updateEntity}</li>
     *     <li>Update with comprehensive error handling via {@link #update}</li>
     *     <li>Cache invalidation via {@code cacheHelper.clear}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidates dictionary type cache to ensure consistency:
     * <pre>
     * {@code
     * // Clear dictionary type metadata cache (affects dropdown options and type references)
     * cacheHelper.clear(CacheConstant.DICT_TYPE);
     *
     * // Note: Also consider invalidating aggregated cache if type code changed
     * // eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.DICT_TYPE_WITH_DATA));
     * }
     * </pre>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_UPDATE)} to:
     * <ul>
     *     <li>Automatically record dictionary type modification events for compliance tracking</li>
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
     *     <li>Isolation: Concurrent updates to same dictionary type are properly serialized</li>
     * </ul>
     *
     * @param id          the primary key of the dictionary type to update; must not be {@code null}
     * @param dictTypeDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if dictionary type was successfully updated
     * @throws IllegalArgumentException                                       if {@code id} or {@code dictTypeDto} is {@code null}
     * @throws com.github.starhq.template.common.exception.NotFoundException  if no dictionary type exists with given {@code id}
     * @throws com.github.starhq.template.common.exception.DuplicateException if new {@code type} code conflicts with existing entry
     * @throws com.github.starhq.template.common.exception.BusinessException  if update fails for other reasons
     * @see DictTypeDTO
     * @see #getAndCheckById(Serializable, ErrorCode)
     * @see #update(Object, ErrorCode, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_UPDATE)
    @Override
    public boolean updateDictType(Serializable id, DictTypeDTO dictTypeDto) {
        // Fetch existing dictionary type with not-found check
        SysDictType entity = getAndCheckById(id, ErrorCode.DICT_TYPE_NOT_FOUND);

        // Apply updates from DTO to entity (partial update support)
        dictTypeConverter.updateEntity(dictTypeDto, entity);

        // Update with comprehensive error handling (duplicate/not-found/general)
        update(entity, ErrorCode.DICT_TYPE_DUPLICATE_TYPE, ErrorCode.DICT_TYPE_UPDATE_FAILED, ErrorCode.DICT_TYPE_NOT_FOUND);

        // Invalidate dictionary type cache to ensure consistency across nodes
        cacheHelper.clear(CacheConstant.DICT_TYPE);

        return true;
    }

    /**
     * Deletes a dictionary type definition with cascading data cleanup, audit logging, and distributed cache invalidation.
     * <p>
     * This method handles the complete dictionary type deletion workflow including:
     * <ol>
     *     <li>Cascading delete: Remove all dictionary data entries under this type</li>
     *     <li>Type deletion with not-found handling via {@link #delete}</li>
     *     <li>Distributed cache invalidation via {@link EventService}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Annotated with {@code @Transactional(rollbackFor = Exception.class)} to ensure:
     * <ul>
     *     <li>Atomicity: Data cleanup and type deletion succeed or fail together</li>
     *     <li>Consistency: If type deletion fails, data cleanup is rolled back</li>
     *     <li>Isolation: Concurrent deletions of same type are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Cascading Delete Strategy:</strong>
     * <pre>
     * {@code
     * // Remove all dictionary data entries under this type
     * dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>()
     *     .eq(SysDictData::getTypeId, id));
     *
     * // Then delete the type itself
     * delete(id, ErrorCode.DICT_TYPE_NOT_FOUND, ErrorCode.DICT_TYPE_DELETE_FAILED);
     * }
     * </pre>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful deletion, publishes distributed cache eviction event:
     * <pre>
     * {@code
     * // Notify all nodes to evict dictionary type cache entries
     * eventService.notifyCacheEvict(
     *     Collections.emptyList(),        // No specific keys (evict by pattern)
     *     List.of(CacheConstant.DICT_TYPE) // Cache region to evict
     * );
     * }
     * </pre>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_REMOVE)} to:
     * <ul>
     *     <li>Automatically record dictionary type deletion events for compliance tracking</li>
     *     <li>Capture deleted type details, operator ID, and timestamp in audit log</li>
     *     <li>Enable deletion history queries in admin console for security analysis</li>
     * </ul>
     *
     * @param id the primary key of the dictionary type to delete; must not be {@code null}
     * @return {@code true} if dictionary type was successfully deleted
     * @throws IllegalArgumentException                                      if {@code id} is {@code null}
     * @throws com.github.starhq.template.common.exception.NotFoundException if no dictionary type exists with given {@code id}
     * @throws com.github.starhq.template.common.exception.BusinessException if deletion fails due to foreign key constraints
     * @see #delete(Serializable, ErrorCode, ErrorCode)
     * @see EventService#notifyCacheEvict(List, List)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        // 1. Cascading delete: Remove all dictionary data entries under this type
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getTypeId, id));

        // 2. Delete dictionary type with business error handling
        delete(id, ErrorCode.DICT_TYPE_NOT_FOUND, ErrorCode.DICT_TYPE_DELETE_FAILED);

        // 3. Publish distributed cache eviction event for multi-node consistency
        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.DICT_TYPE));

        return true;
    }

}