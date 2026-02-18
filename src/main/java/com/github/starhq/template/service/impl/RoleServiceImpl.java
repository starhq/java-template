package com.github.starhq.template.service.impl;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.converter.RoleConverter;
import com.github.starhq.template.dto.PageRequest;
import com.github.starhq.template.dto.RoleCreateDTO;
import com.github.starhq.template.dto.RoleUpdateDTO;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.exception.BusinessException;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.service.RoleService;
import com.github.starhq.template.vo.RoleVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 角色服务实现类
 *
 * @author starhq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final SysRoleMapper roleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO createRole(RoleCreateDTO dto) {
        // 检查角色代码是否已存在
        SysRole existingRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getCode, dto.getCode())
        );

        if (existingRole != null) {
            throw new BusinessException("角色代码已存在");
        }

        SysRole role = RoleConverter.INSTANCE.toEntity(dto);
        role.setCreatedAt(OffsetDateTime.now());
        // TODO: 从SecurityContext获取当前用户ID
        // role.setCreatedBy(getCurrentUserId());

        roleMapper.insert(role);
        return RoleConverter.INSTANCE.toVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RoleVO updateRole(Long id, RoleUpdateDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }

        RoleConverter.INSTANCE.updateEntity(dto, role);
        role.setUpdatedAt(OffsetDateTime.now());
        // TODO: 从SecurityContext获取当前用户ID
        // role.setUpdatedBy(getCurrentUserId());

        roleMapper.updateById(role);
        return RoleConverter.INSTANCE.toVO(role);
    }

    @Override
    public RoleVO getRoleById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return RoleConverter.INSTANCE.toVO(role);
    }

    @Override
    public Page<RoleVO> listRoles(PageRequest pageRequest) {
        Page<SysRole> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        Page<SysRole> rolePage = roleMapper.selectPage(page, null);
        return (Page<RoleVO>) rolePage.convert(RoleConverter.INSTANCE::toVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        roleMapper.deleteById(id);
    }
}
