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
import com.github.starhq.template.converter.DictDataConverter;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysDictDataMapper;
import com.github.starhq.template.mapper.SysDictTypeMapper;
import com.github.starhq.template.model.dto.DictDataDTO;
import com.github.starhq.template.model.dto.DictDataPageRequest;
import com.github.starhq.template.model.vo.DictDataPageVO;
import com.github.starhq.template.model.vo.DictDataSimpleVO;
import com.github.starhq.template.service.DictDataService;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Objects;

/**
 * Service implementation for dictionary data management with CRUD operations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link DictDataService} for dictionary data-specific business operations.
 * Designed to centralize dictionary data management logic with consistent validation, cache integration, and
 * distributed audit logging for compliance tracking.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>Dictionary Data CRUD</strong>: Create, read, update, delete label/value pairs with duplicate prevention</li>
 *     <li><strong>Type Validation</strong>: Ensure dictionary data references valid parent dictionary types</li>
 *     <li><strong>Cache Management</strong>: Invalidate dictionary type cache when data changes to ensure consistency</li>
 *     <li><strong>Audit Integration</strong>: Record all write operations via {@code @AuditLoggable} for compliance</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Cache-Consistent</strong>: Changes trigger cache invalidation via {@link #clearCache()} for multi-node consistency</li>
 *     <li><strong>Audit-Ready</strong>: All write operations annotated with {@code @AuditLoggable} for compliance tracking</li>
 *     <li><strong>Null-Safe</strong>: Uses {@link Objects} and {@link AuditBaseServiceImpl#getAndCheckById} to prevent NPE</li>
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
 *     @GetMapping("/{id}")
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
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-10
 * @see DictDataService
 * @see AuditBaseServiceImpl
 * @see SysDictData
 * @see DictDataPageVO
 * @see DictDataSimpleVO
 */
@Service("dictDataService")
public class DictDataServiceImpl extends AuditBaseServiceImpl<SysDictDataMapper, SysDictData> implements DictDataService {

    /**
     * Mapper for {@link SysDictType} database operations (dictionary type validation).
     * <p>
     * Used to validate that dictionary data's {@code typeId} references an existing dictionary type
     * before insertion or update, ensuring referential integrity between types and data entries.
     *
     * @see SysDictTypeMapper
     * @see #validateDictType(Serializable)
     */
    private final SysDictTypeMapper dictTypeMapper;

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
     * Converter for transforming between {@link DictDataDTO}, {@link SysDictData}, and various VO types.
     * <p>
     * Ensures consistent field mapping and avoids boilerplate conversion code across service methods.
     * Supports:
     * <ul>
     *     <li>DTO to entity conversion for persistence</li>
     *     <li>Entity to {@link DictDataPageVO} for admin console pagination</li>
     *     <li>Entity to {@link DictDataSimpleVO} for lightweight UI references</li>
     * </ul>
     *
     * @see DictDataConverter
     */
    private final DictDataConverter dictDataConverter;

    /**
     * Constructs a new {@code DictDataServiceImpl} with the required dependencies.
     *
     * @param cacheHelper       the cache utility for batch username resolution and cache invalidation
     * @param dictTypeMapper    the mapper for dictionary type validation and database operations
     * @param userMapperHelper  the helper for resolving user IDs to usernames
     * @param dictDataConverter the converter for transforming between DTOs, entities, and VOs
     */
    public DictDataServiceImpl(
            CacheHelper cacheHelper,
            SysDictTypeMapper dictTypeMapper,
            SysUserMapperHelper userMapperHelper,
            DictDataConverter dictDataConverter) {

        super(cacheHelper);

        this.dictTypeMapper = dictTypeMapper;
        this.userMapperHelper = userMapperHelper;
        this.dictDataConverter = dictDataConverter;
    }


    /**
     * Retrieves a paginated list of dictionary data entries with dynamic filtering and audit field resolution.
     * <p>
     * This method delegates to {@link AuditBaseServiceImpl#pageVO} to provide:
     * <ul>
     *     <li>Base query building from {@link DictDataPageRequest}</li>
     *     <li>Dynamic dictionary type ID filter via {@code Consumer<QueryWrapper>} callback</li>
     *     <li>Batch username resolution for {@code creator}/{@code updater} fields</li>
     *     <li>Entity-to-VO conversion via {@link DictDataConverter#toPageVO}</li>
     * </ul>
     * <p>
     * <strong>Filter Logic:</strong>
     * <pre>
     * {@code
     * // Apply dictionary type ID filter if specified
     * if (pageInfo.getDictTypeId() != null) {
     *     wrapper.eq("type_id", pageInfo.getDictTypeId());
     *     // SQL: AND type_id = 1001
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
     * @return paginated list of {@link DictDataPageVO} matching the criteria; never {@code null}
     * @see DictDataPageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see DictDataConverter#toPageVO(SysDictData)
     */
    @Override
    public IPage<DictDataPageVO> page(DictDataPageRequest pageInfo) {
        return pageVO(pageInfo,
                // Dynamic filter: add dictionary type ID condition if specified
                wrapper -> {
                    if (!Objects.isNull(pageInfo.getDictTypeId())) {
                        wrapper.eq(QueryConstant.TYPE_ID, pageInfo.getDictTypeId());
                    }
                },
                // Batch username loader for audit field resolution
                userMapperHelper,
                // Entity to VO converter
                dictDataConverter::toPageVO);
    }

    /**
     * Retrieves simplified dictionary data metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full dictionary data queries when only
     * basic identification fields ({@code id}, {@code label}, {@code value}) are needed.
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>Uses {@link #getAndCheckById} to throw {@link NotFoundException} if dictionary data not found</li>
     *     <li>Ensures caller receives valid {@link DictDataSimpleVO} or handles exception</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch dictionary data for dropdown population
     * DictDataSimpleVO data = dictDataService.getDictDataById(2001L);
     * // Use for UI rendering
     * dropdownOptions.add(new SelectOption(data.getValue(), data.getLabel()));
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
     * @return {@link DictDataSimpleVO} if found
     * @throws NotFoundException if no dictionary data exists with given {@code id}
     * @see DictDataSimpleVO
     * @see DictDataConverter#toSimpleVO(SysDictData)
     * @see #getAndCheckById(Serializable, ErrorCode)
     */
    @Override
    public DictDataSimpleVO getDictDataById(Serializable id) {
        // Fetch dictionary data with not-found check (throws NotFoundException if missing)
        SysDictData dictData = getAndCheckById(id, ErrorCode.DICT_DATA_NOT_FOUND);

        // Convert to lightweight VO for UI rendering
        return dictDataConverter.toSimpleVO(dictData);
    }

    /**
     * Creates a new dictionary data entry with validation, duplicate prevention, and audit logging.
     * <p>
     * This method handles the complete dictionary data creation workflow including:
     * <ol>
     *     <li>Dictionary type reference validation via {@link #validateDictType}</li>
     *     <li>DTO-to-entity conversion via {@link DictDataConverter}</li>
     *     <li>Insertion with duplicate value handling via {@link #insert}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_INSERT)} to:
     * <ul>
     *     <li>Automatically record dictionary data creation events for compliance tracking</li>
     *     <li>Capture operator ID, timestamp, and operation details in audit log</li>
     *     <li>Enable audit trail queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Type Validation</strong>: Ensures dictionary data references existing type to prevent orphaned entries</li>
     *     <li><strong>Duplicate Prevention</strong>: {@link #insert} catches {@code DuplicateKeyException} and throws business-friendly error</li>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #insert} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Dictionary data insertion either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Audit log entry is recorded in same transaction</li>
     *     <li>Isolation: Concurrent creations with same value are properly serialized by database</li>
     * </ul>
     *
     * @param dictDataDto the dictionary data creation data; must not be {@code null}
     * @return {@code true} if dictionary data was successfully created
     * @throws IllegalArgumentException                                       if {@code dictDataDto} is {@code null} or missing required fields
     * @throws NotFoundException                                              if referenced dictionary type does not exist
     * @throws com.github.starhq.template.common.exception.DuplicateException if {@code value} already exists within same type
     * @see DictDataDTO
     * @see #validateDictType(Serializable)
     * @see #insert(Object, ErrorCode, ErrorCode)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_INSERT)
    @Override
    public boolean createDictData(DictDataDTO dictDataDto) {
        // Validate that referenced dictionary type exists (prevent orphaned data)
        validateDictType(dictDataDto.getTypeId());

        // Convert DTO to entity for persistence
        SysDictData dictData = dictDataConverter.toEntity(dictDataDto);

        // Insert with business error handling (duplicate value → DICT_DATA_DUPLICATE_VALUE)
        insert(dictData, ErrorCode.DICT_DATA_DUPLICATE_VALUE, ErrorCode.DICT_DATA_INSERT_FAILED);

        return true;
    }

    /**
     * Updates an existing dictionary data entry with validation, conflict prevention, and cache invalidation.
     * <p>
     * This method handles the complete dictionary data update workflow including:
     * <ol>
     *     <li>Dictionary type reference validation via {@link #validateDictType}</li>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link DictDataConverter#updateEntity}</li>
     *     <li>Update with comprehensive error handling via {@link #update}</li>
     *     <li>Cache invalidation via {@link #clearCache}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful update, invalidates dictionary type cache to ensure consistency:
     * <pre>
     * {@code
     * // Clear dictionary type cache (affects dropdown options that include this data)
     * clearCache(); // Evicts CacheConstant.DICT_TYPE entries
     * }
     * </pre>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_UPDATE)} to:
     * <ul>
     *     <li>Automatically record dictionary data modification events for compliance tracking</li>
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
     *     <li>Isolation: Concurrent updates to same dictionary data are properly serialized</li>
     * </ul>
     *
     * @param id          the primary key of the dictionary data to update; must not be {@code null}
     * @param dictDataDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if dictionary data was successfully updated
     * @throws IllegalArgumentException                                       if {@code id} or {@code dictDataDto} is {@code null}
     * @throws NotFoundException                                              if no dictionary data exists with given {@code id} or referenced type not found
     * @throws com.github.starhq.template.common.exception.DuplicateException if new {@code value} conflicts with existing entry within same type
     * @see DictDataDTO
     * @see #validateDictType(Serializable)
     * @see #getAndCheckById(Serializable, ErrorCode)
     * @see #update(Object, ErrorCode, ErrorCode, ErrorCode)
     * @see #clearCache()
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_UPDATE)
    @Override
    public boolean updateDictData(Serializable id, DictDataDTO dictDataDto) {
        // Validate that referenced dictionary type exists
        validateDictType(dictDataDto.getTypeId());

        // Fetch existing dictionary data with not-found check
        SysDictData dictData = getAndCheckById(id, ErrorCode.DICT_DATA_NOT_FOUND);

        // Apply updates from DTO to entity (partial update support)
        dictDataConverter.updateEntity(dictDataDto, dictData);

        // Update with comprehensive error handling (duplicate/not-found/general)
        update(dictData, ErrorCode.DICT_DATA_DUPLICATE_VALUE, ErrorCode.DICT_DATA_UPDATE_FAILED, ErrorCode.DICT_DATA_NOT_FOUND);

        // Invalidate dictionary type cache to ensure consistency across nodes
        clearCache();

        return true;
    }

    /**
     * Deletes a dictionary data entry with audit logging and cache invalidation.
     * <p>
     * This method handles the complete dictionary data deletion workflow including:
     * <ol>
     *     <li>Dictionary data deletion with not-found handling via {@link #delete}</li>
     *     <li>Cache invalidation via {@link #clearCache}</li>
     *     <li>Audit log recording via {@code @AuditLoggable} annotation</li>
     * </ol>
     * <p>
     * <strong>Audit Integration:</strong>
     * <p>
     * Annotated with {@code @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_REMOVE)} to:
     * <ul>
     *     <li>Automatically record dictionary data deletion events for compliance tracking</li>
     *     <li>Capture deleted data details, operator ID, and timestamp in audit log</li>
     *     <li>Enable deletion history queries in admin console for security analysis</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <p>
     * After successful deletion, invalidates dictionary type cache to ensure consistency:
     * <pre>
     * {@code
     * // Clear dictionary type cache (affects dropdown options that included this data)
     * clearCache(); // Evicts CacheConstant.DICT_TYPE entries
     * }
     * </pre>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Inherits transaction boundary from {@link #delete} method in {@link AuditBaseServiceImpl}, ensuring:
     * <ul>
     *     <li>Atomicity: Deletion either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent deletions of same dictionary data are properly serialized</li>
     * </ul>
     *
     * @param id the primary key of the dictionary data to delete; must not be {@code null}
     * @return {@code true} if dictionary data was successfully deleted
     * @throws IllegalArgumentException                                      if {@code id} is {@code null}
     * @throws com.github.starhq.template.common.exception.BusinessException if deletion fails due to foreign key constraints
     * @see #delete(Serializable, ErrorCode, ErrorCode)
     * @see #clearCache()
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_REMOVE)
    @Override
    public boolean removeById(Serializable id) {
        // Delete dictionary data with business error handling
        delete(id, ErrorCode.DICT_DATA_NOT_FOUND, ErrorCode.DICT_DATA_DELETE_FAILED);

        // Invalidate dictionary type cache to ensure consistency across nodes
        clearCache();

        return true;
    }

    // ====================== Private Helper Methods ======================

    /**
     * Validates that the dictionary data's referenced dictionary type exists in the database.
     * <p>
     * This method enforces referential integrity by ensuring that every dictionary data entry
     * is associated with a valid parent dictionary type, preventing orphaned label/value definitions.
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * // Check if dictionary type with given ID exists
     * boolean exists = dictTypeMapper.exists(
     *     new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getId, dictTypeId)
     * );
     *
     * // Throw not-found exception if type does not exist
     * if (!exists) {
     *     throw new NotFoundException(ErrorCode.DICT_TYPE_NOT_FOUND);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from {@link #createDictData} and {@link #updateDictData} before persistence
     * to ensure data integrity. Not intended for public API exposure.
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li><strong>Type Not Found</strong>: Throws {@link NotFoundException} with {@link ErrorCode#DICT_TYPE_NOT_FOUND}</li>
     *     <li><strong>Null Safety</strong>: Assumes {@code dictTypeId} is non-null (validated at DTO layer)</li>
     * </ul>
     *
     * @param dictTypeId the primary key of the dictionary type to validate; must not be {@code null}
     * @throws NotFoundException if referenced dictionary type does not exist
     * @see ErrorCode#DICT_TYPE_NOT_FOUND
     */
    private void validateDictType(Serializable dictTypeId) {
        // Check if dictionary type with given ID exists (enforce foreign key constraint at application level)
        boolean exists = dictTypeMapper.exists(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getId, dictTypeId));

        // Throw not-found exception if type does not exist
        if (!exists) {
            throw new NotFoundException(ErrorCode.DICT_TYPE_NOT_FOUND);
        }
    }

    /**
     * Invalidates dictionary type cache to ensure consistency after dictionary data changes.
     * <p>
     * This method clears all cache entries associated with {@link CacheConstant#DICT_TYPE}
     * to ensure that:
     * <ul>
     *     <li>Updated dictionary data is reflected in dropdown options immediately</li>
     *     <li>Deleted dictionary data no longer appears in type-level queries</li>
     *     <li>Multi-node deployments maintain consistent cache state via distributed invalidation</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <pre>
     * {@code
     * // Clear all dictionary type cache entries (by pattern)
     * cacheHelper.clear(CacheConstant.DICT_TYPE);
     *
     * // Alternative: Precise invalidation by type ID if known
     * // cacheHelper.evict(List.of(typeId), List.of(CacheConstant.DICT_TYPE));
     * }
     * </pre>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called from {@link #updateDictData} and {@link #removeById} after successful persistence
     * to ensure cache consistency. Not intended for public API exposure.
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Broad Invalidation</strong>: Clears all dictionary type cache entries; consider precise invalidation if performance critical</li>
     *     <li><strong>Distributed Systems</strong>: {@link CacheHelper#clear} should publish invalidation events to all nodes</li>
     *     <li><strong>Cache TTL</strong>: Short TTL (e.g., 5-10 min) provides fallback consistency if invalidation fails</li>
     * </ul>
     *
     * @see CacheHelper#clear(String)
     * @see CacheConstant#DICT_TYPE
     */
    private void clearCache() {
        // Invalidate dictionary type cache to ensure consistency after data changes
        cacheHelper.clear(CacheConstant.DICT_TYPE);
    }

}