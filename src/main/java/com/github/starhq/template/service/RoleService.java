package com.github.starhq.template.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.dto.PageRequest;
import com.github.starhq.template.dto.RoleCreateDTO;
import com.github.starhq.template.dto.RoleUpdateDTO;
import com.github.starhq.template.vo.RoleVO;

/**
 * 角色服务接口
 *
 * @author starhq
 */
public interface RoleService {
    /**
     * 创建角色
     *
     * @param dto 角色创建DTO
     * @return 角色VO
     */
    RoleVO createRole(RoleCreateDTO dto);

    /**
     * 更新角色
     *
     * @param id  角色ID
     * @param dto 角色更新DTO
     * @return 角色VO
     */
    RoleVO updateRole(Long id, RoleUpdateDTO dto);

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色VO
     */
    RoleVO getRoleById(Long id);

    /**
     * 分页查询角色
     *
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    Page<RoleVO> listRoles(PageRequest pageRequest);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);
}
