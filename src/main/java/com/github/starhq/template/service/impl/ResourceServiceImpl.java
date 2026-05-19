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
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysResourceMapper;
import com.github.starhq.template.mapper.SysRoleResourceMapper;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.resource.ResourceDTO;
import com.github.starhq.template.model.vo.resource.ResourceCheckVO;
import com.github.starhq.template.model.vo.resource.ResourcePageVO;
import com.github.starhq.template.model.vo.resource.ResourceSimpleVO;
import com.github.starhq.template.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/3/24 20:39
 */
@Service("resourceService")
@RequiredArgsConstructor
public class ResourceServiceImpl extends AuditBaseServiceImpl<SysResourceMapper, SysResource> implements ResourceService {

    private final SysUserMapperHelper userMapperHelper;
    private final SysRoleResourceMapper roleResourceMapper;

    private final ResourceConverter resourceConverter;
    private final EventService eventService;

    @Override
    public IPage<ResourcePageVO> page(PageRequest pageInfo) {
        return pageVO(pageInfo,
                null,
                userMapperHelper,
                resourceConverter::toPageVO);
    }

    @Cacheable(value = "resources", key = "#p0")
    @Override
    public List<SysResource> selectByUserId(Serializable userId) {
        return getBaseMapper().selectAssignedResourceByUserId(userId);
    }

    @Override
    public ResourceSimpleVO getResourceById(Serializable id) {
        SysResource resource = getAndCheckById(id, ErrorCode.RESOURCE_NOT_FOUND);

        return resourceConverter.toSimpleVO(resource);
    }

    @Override
    public List<ResourceCheckVO> selectCheckedResources(Serializable roleId) {
        return getBaseMapper().selectResourcesByRoleId(roleId);
    }

    @AuditLoggable(targetType = TargetType.RESOURCE, action = AuditLogConstant.RESOURCE_UPDATE)
    @Override
    public boolean updateResource(Serializable id, ResourceDTO resourceDto) {
        SysResource resource = getAndCheckById(id, ErrorCode.RESOURCE_NOT_FOUND);

        resourceConverter.updateEntity(resourceDto, resource);

        update(resource, ErrorCode.RESOURCE_DUPLICATE_URL_METHOD, ErrorCode.RESOURCE_UPDATE_FAILED, ErrorCode.RESOURCE_NOT_FOUND);

        cacheHelper.clear(CacheConstant.RESOURCE);

        return true;
    }

    @AuditLoggable(targetType = TargetType.RESOURCE, action = AuditLogConstant.RESOURCE_INSERT)
    @Override
    public boolean createResource(ResourceDTO resourceDto) {
        SysResource resource = resourceConverter.toEntity(resourceDto);

        insert(resource, ErrorCode.RESOURCE_DUPLICATE_URL_METHOD, ErrorCode.RESOURCE_INSERT_FAILED);

        return true;
    }

    @AuditLoggable(targetType = TargetType.RESOURCE, action = AuditLogConstant.RESOURCE_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getResourceId, id));

        delete(id, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_DELETE_FAILED);

        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.RESOURCE));

        return true;
    }
}
