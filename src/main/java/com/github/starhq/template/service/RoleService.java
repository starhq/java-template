package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.role.RoleDTO;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import com.github.starhq.template.model.vo.role.RolePageVO;
import com.github.starhq.template.model.vo.role.RoleSimpleVO;

import java.io.Serializable;
import java.util.List;


public interface RoleService extends IService<SysRole> {

    /**
     * Retrieves a paginated list of roles based on the provided pagination info.
     *
     * @param pageInfo the pagination and sorting information
     * @return a paginated response of roles
     */
    IPage<RolePageVO> page(PageRequest pageInfo);

    /**
     * Retrieves a role by its ID.
     *
     * @param id the ID of the role
     * @return the role's details
     */
    RoleSimpleVO getRoleById(Serializable id);

    /**
     * Retrieves a list of roles with checked status for a specific user.
     *
     * @param userId the ID of the user
     * @return a list of checked role responses
     */
    List<RoleCheckVO> selectCheckedRoles(Serializable userId);

    /**
     * Updates an existing role and its associated resources, menus, and buttons.
     *
     * @param roleDto the DTO containing updated role information
     * @return true if the update was successful
     * @throws BusinessException if the update fails
     */
    boolean updateRole(Serializable id, RoleDTO roleDto);

    /**
     * Creates a new role and assigns resources, menus, and buttons.
     *
     * @param roleDto the DTO containing new role information
     * @return true if the creation was successful
     * @throws BusinessException if the creation fails
     */
    boolean createRole(RoleDTO roleDto);
}
