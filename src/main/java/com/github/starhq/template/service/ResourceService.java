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

public interface ResourceService extends IService<SysResource> {
    /**
     * Retrieves a paginated list of resources based on the provided pagination
     * info.
     *
     * @param pageInfo the pagination and sorting information
     * @return a paginated response of resources
     */
    IPage<ResourcePageVO> page(PageRequest pageInfo);

    /**
     * Retrieves current user's resources.
     *
     * @param userId the current user's id
     * @return a list of resources
     */
    List<SysResource> selectByUserId(Serializable userId);

    /**
     * Retrieves a resource by its ID.
     *
     * @param id the ID of the resource
     * @return the resource's details
     */
    ResourceSimpleVO getResourceById(Serializable id);

    /**
     * Retrieves a list of resources with checked status for a specific user.
     *
     * @param roleId the ID of the role
     * @return a list of checked resource responses
     */
    List<ResourceCheckVO> selectCheckedResources(Serializable roleId);

    /**
     * Updates an existing resource in the database.
     *
     * @param resourceDto the DTO containing updated resource information
     * @return true if the update was successful
     * @throws BusinessException if the update fails
     */
    boolean updateResource(Serializable id, ResourceDTO resourceDto);

    /**
     * Creates a new resource in the database.
     *
     * @param resourceDto the DTO containing new resource information
     * @return true if the creation was successful
     * @throws BusinessException if the creation fails
     */
    boolean createResource(ResourceDTO resourceDto);
}
