package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.converter.ResourceConverter;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.entity.SysRoleResource;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysResourceMapper;
import com.github.starhq.template.mapper.SysRoleResourceMapper;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.resource.ResourceDTO;
import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import com.github.starhq.template.model.vo.resource.ResourcePageVO;
import com.github.starhq.template.model.vo.resource.ResourceSimpleVO;
import com.github.starhq.template.service.ResourceService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Service implementation for resource management with CRUD operations, caching, and audit trail support.
 * <p>
 * This class extends {@link AuditBaseServiceImpl} to provide reusable pagination logic with automatic
 * audit field population, while implementing {@link ResourceService} for resource-specific business operations.
 * Designed to centralize resource management logic with consistent validation, cache integration, and
 * distributed audit logging for compliance tracking.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-24
 * @see ResourceService
 * @see AuditBaseServiceImpl
 * @see SysResource
 */
@Service("resourceService")
public class ResourceServiceImpl extends AuditBaseServiceImpl<SysResourceMapper, SysResource> implements ResourceService {

    /**
     * Helper for efficient batch username resolution in audit field population.
     * <p>Injected into {@link AuditBaseServiceImpl#pageVO} to resolve {@code createdBy}/{@code updatedBy}
     * IDs to human-readable usernames via cached batch lookup.</p>
     *
     * @see SysUserMapperHelper
     */
    private final SysUserMapperHelper userMapperHelper;

    /**
     * Mapper for {@link SysRoleResource} junction table operations (role-resource assignments).
     * <p>Used for cascading delete: removing role-resource assignments when resource is deleted.</p>
     *
     * @see SysRoleResourceMapper
     * @see #removeById(Serializable)
     */
    private final SysRoleResourceMapper roleResourceMapper;

    /**
     * Converter for transforming between {@link ResourceDTO}, {@link SysResource}, and various VO types.
     * <p>Ensures consistent field mapping and avoids boilerplate conversion code.</p>
     *
     * @see ResourceConverter
     */
    private final ResourceConverter resourceConverter;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>Ensures cache consistency when resource data changes by notifying all nodes to evict related entries.</p>
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Constructs a new {@code ResourceServiceImpl} with the required dependencies.
     *
     * @param cacheHelper        the cache utility for batch username resolution (inherited from base class)
     * @param userMapperHelper   the helper for resolving user IDs to usernames during audit field population
     * @param roleResourceMapper the mapper for managing role-resource relationships and permission bindings
     * @param resourceConverter  the converter for transforming between resource entities, DTOs, and VOs
     * @param eventService       the service for publishing domain events (e.g., cache invalidation triggers)
     */
    public ResourceServiceImpl(CacheHelper cacheHelper,
                               SysUserMapperHelper userMapperHelper,
                               SysRoleResourceMapper roleResourceMapper,
                               ResourceConverter resourceConverter,
                               EventService eventService) {
        super(cacheHelper);
        this.userMapperHelper = userMapperHelper;
        this.roleResourceMapper = roleResourceMapper;
        this.resourceConverter = resourceConverter;
        this.eventService = eventService;
    }


    /**
     * Retrieves a paginated list of resource definitions with audit field resolution.
     * <p>Delegates to {@link AuditBaseServiceImpl#pageVO} for base query building, batch username resolution,
     * and entity-to-VO conversion.</p>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link ResourcePageVO}; never {@code null}
     * @see PageRequest
     * @see AuditBaseServiceImpl#pageVO
     * @see ResourceConverter#toPageVO(SysResource)
     */
    @Override
    public IPage<ResourcePageVO> page(PageRequest pageInfo) {
        return pageVO(pageInfo,
                null, // No dynamic filters
                userMapperHelper, // Batch username loader
                resourceConverter::toPageVO); // Entity to VO converter
    }

    /**
     * Retrieves the list of resources accessible to a specific user based on assigned roles.
     * <p>
     * <strong>Cache Strategy:</strong>
     * <ul>
     *     <li>Annotated with {@code @Cacheable(value = "resources", key = "#p0")} to cache by {@code userId}</li>
     *     <li>Cache entries should be evicted when user roles or resource assignments change</li>
     * </ul>
     *
     * @param userId the primary key of the user to query; must not be {@code null}
     * @return list of {@link SysResource} accessible to the user; never {@code null}
     * @see SysResourceMapper#selectAssignedResourceByUserId(Serializable)
     * @see Cacheable
     */
    @Cacheable(value = "resources", key = "#p0")
    @Override
    public List<SysResource> selectByUserId(Serializable userId) {
        // Fetch assigned resources via custom mapper method (avoids N+1 query)
        return getBaseMapper().selectAssignedResourceByUserId(userId);
    }

    /**
     * Retrieves simplified resource metadata by ID for dropdowns or internal references.
     * <p>Uses {@link #getAndCheckById} to throw {@link com.github.starhq.template.common.exception.NotFoundException}
     * if resource not found, ensuring caller receives valid VO or handles exception.</p>
     *
     * @param id the primary key of the resource to retrieve; must not be {@code null}
     * @return {@link ResourceSimpleVO} if found
     * @throws com.github.starhq.template.common.exception.NotFoundException if no resource exists with given {@code id}
     * @see ResourceSimpleVO
     * @see ResourceConverter#toSimpleVO(SysResource)
     */
    @Override
    public ResourceSimpleVO getResourceById(Serializable id) {
        // Fetch resource with not-found check
        SysResource resource = getAndCheckById(id, ErrorCode.RESOURCE_NOT_FOUND);
        // Convert to lightweight VO
        return resourceConverter.toSimpleVO(resource);
    }

    /**
     * Retrieves a list of resources with checked state for role-based permission configuration.
     * <p>Delegates to custom mapper method that computes {@code checked} state via {@code LEFT JOIN}.</p>
     *
     * @param roleId the primary key of the role to configure; must not be {@code null}
     * @return list of {@link ResourceCheckVO} with computed {@code checked} state; never {@code null}
     * @see SysResourceMapper#selectResourcesByRoleId(Serializable)
     */
    @Override
    public List<ResourceCheckVO> selectCheckedResources(Serializable roleId) {
        return getBaseMapper().selectResourcesByRoleId(roleId);
    }

    /**
     * Updates an existing resource definition with validation and cache invalidation.
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Existence check via {@link #getAndCheckById}</li>
     *     <li>Entity update via {@link ResourceConverter#updateEntity}</li>
     *     <li>Update with error handling via {@link #update}</li>
     *     <li>Cache invalidation via {@code cacheHelper.clear}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param id          the primary key of the resource to update; must not be {@code null}
     * @param resourceDto the update data; must not be {@code null}
     * @return {@code true} if successfully updated
     * @throws com.github.starhq.template.common.exception.NotFoundException  if resource not found
     * @throws com.github.starhq.template.common.exception.DuplicateException if URL+method conflicts
     * @see ResourceDTO
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.RESOURCE, action = AuditLogConstant.RESOURCE_UPDATE)
    @Override
    public boolean updateResource(Serializable id, ResourceDTO resourceDto) {
        // Fetch existing resource with not-found check
        SysResource resource = getAndCheckById(id, ErrorCode.RESOURCE_NOT_FOUND);

        // Apply updates from DTO to entity
        resourceConverter.updateEntity(resourceDto, resource);

        // Update with error handling (duplicate URL+method, not-found, general)
        update(resource, ErrorCode.RESOURCE_DUPLICATE_URL_METHOD, ErrorCode.RESOURCE_UPDATE_FAILED, ErrorCode.RESOURCE_NOT_FOUND);

        // Invalidate resource cache to ensure consistency
        cacheHelper.clear(CacheConstant.RESOURCE);

        return true;
    }

    /**
     * Creates a new resource definition with validation and duplicate prevention.
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>DTO-to-entity conversion via {@link ResourceConverter}</li>
     *     <li>Insertion with duplicate URL+method handling via {@link #insert}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param resourceDto the resource creation data; must not be {@code null}
     * @return {@code true} if successfully created
     * @throws com.github.starhq.template.common.exception.DuplicateException if URL+method already exists
     * @see ResourceDTO
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.RESOURCE, action = AuditLogConstant.RESOURCE_INSERT)
    @Override
    public boolean createResource(ResourceDTO resourceDto) {
        // Convert DTO to entity
        SysResource resource = resourceConverter.toEntity(resourceDto);

        // Insert with error handling (duplicate URL+method, general insert failure)
        insert(resource, ErrorCode.RESOURCE_DUPLICATE_URL_METHOD, ErrorCode.RESOURCE_INSERT_FAILED);

        return true;
    }

    /**
     * Deletes a resource definition with cascading role-resource cleanup and cache invalidation.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <ul>
     *     <li>Annotated with {@code @Transactional} to ensure atomicity of cascade delete + resource delete</li>
     *     <li>If resource deletion fails, role-resource assignments are rolled back</li>
     * </ul>
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Cascading delete: Remove all role-resource assignments for this resource</li>
     *     <li>Resource deletion via {@link #delete}</li>
     *     <li>Distributed cache invalidation via {@link EventService}</li>
     *     <li>Audit log recording via {@code @AuditLoggable}</li>
     * </ol>
     *
     * @param id the primary key of the resource to delete; must not be {@code null}
     * @return {@code true} if successfully deleted
     * @see EventService#notifyCacheEvict(List, List)
     * @see AuditLoggable
     */
    @AuditLoggable(targetType = TargetType.RESOURCE, action = AuditLogConstant.RESOURCE_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        // 1. Cascading delete: Remove all role-resource assignments
        roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getResourceId, id));

        // 2. Delete resource with error handling
        delete(id, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_DELETE_FAILED);

        // 3. Publish distributed cache eviction event
        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.RESOURCE));

        return true;
    }

}