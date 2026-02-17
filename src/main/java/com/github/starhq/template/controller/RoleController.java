package com.github.starhq.template.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.dto.PageRequest;
import com.github.starhq.template.dto.RoleCreateDTO;
import com.github.starhq.template.dto.RoleUpdateDTO;
import com.github.starhq.template.service.RoleService;
import com.github.starhq.template.vo.Result;
import com.github.starhq.template.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色控制器
 *
 * @author starhq
 */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    /**
     * 创建角色
     *
     * @param dto 角色创建DTO
     * @return 角色VO
     */
    @PostMapping
    public Result<RoleVO> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        RoleVO roleVO = roleService.createRole(dto);
        return Result.success(roleVO);
    }

    /**
     * 更新角色
     *
     * @param id  角色ID
     * @param dto 角色更新DTO
     * @return 角色VO
     */
    @PutMapping("/{id}")
    public Result<RoleVO> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        RoleVO roleVO = roleService.updateRole(id, dto);
        return Result.success(roleVO);
    }

    /**
     * 根据ID查询角色
     *
     * @param id 角色ID
     * @return 角色VO
     */
    @GetMapping("/{id}")
    public Result<RoleVO> getRoleById(@PathVariable Long id) {
        RoleVO roleVO = roleService.getRoleById(id);
        return Result.success(roleVO);
    }

    /**
     * 分页查询角色
     *
     * @param pageRequest 分页请求
     * @return 分页结果
     */
    @GetMapping
    public Result<Page<RoleVO>> listRoles(@Valid PageRequest pageRequest) {
        Page<RoleVO> page = roleService.listRoles(pageRequest);
        return Result.success(page);
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.success();
    }
}
