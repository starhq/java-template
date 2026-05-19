package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.converter.RoleConverter;
import com.github.starhq.template.entity.*;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.RelationHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.*;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.dto.role.RoleDTO;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import com.github.starhq.template.model.vo.role.RolePageVO;
import com.github.starhq.template.model.vo.role.RoleSimpleVO;
import com.github.starhq.template.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 角色服务实现类
 *
 * @author starhq
 */
@Service("roleService")
@RequiredArgsConstructor
public class RoleServiceImpl extends AuditBaseServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    private final SysMenuMapper menuMapper;
    private final SysButtonMapper buttonMapper;
    private final SysResourceMapper resourceMapper;
    private final SysUserMapperHelper userMapperHelper;

    private final SysRoleResourceMapper roleResourceMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleButtonMapper roleButtonMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final RelationHelper relationHelper;
    private final EventService eventService;
    private final RoleConverter roleConverter;


    @Override
    public IPage<RolePageVO> page(PageRequest pageInfo) {
        return pageVO(pageInfo,
                null,
                userMapperHelper,
                roleConverter::toPageVO);
    }

    @Override
    public RoleSimpleVO getRoleById(Serializable id) {
        SysRole sysRole = getAndCheckById(id, ErrorCode.ROLE_NOT_FOUND);

        return roleConverter.toSimpleVO(sysRole);
    }

    @Override
    public List<RoleCheckVO> selectCheckedRoles(Serializable userId) {
        return getBaseMapper().selectRolesByUserId(userId);
    }

    @AuditLoggable(targetType = TargetType.ROLE, action = AuditLogConstant.ROLE_UPDATE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateRole(Serializable id, RoleDTO roleDto) {
        SysRole role = getAndCheckById(id, ErrorCode.ROLE_NOT_FOUND);

        roleConverter.updateEntity(roleDto, role);

        update(role, ErrorCode.ROLE_DUPLICATE_CODE, ErrorCode.ROLE_UPDATE_FAILED, ErrorCode.ROLE_NOT_FOUND);

        updateRoleAssociations(role.getId(), roleDto);

        clearRoleCache();

        return true;
    }

    @AuditLoggable(targetType = TargetType.ROLE, action = AuditLogConstant.ROLE_INSERT)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createRole(RoleDTO roleDto) {
        SysRole role = roleConverter.toEntity(roleDto);

        insert(role, ErrorCode.ROLE_DUPLICATE_CODE, ErrorCode.ROLE_INSERT_FAILED);

        updateRoleAssociations(role.getId(), roleDto);

        return true;
    }

    @AuditLoggable(targetType = TargetType.ROLE, action = AuditLogConstant.ROLE_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        deleteRoleRelations(id);

        // Delete the role
        delete(id, ErrorCode.ROLE_NOT_FOUND, ErrorCode.ROLE_DELETE_FAILED);

        clearRoleCache();

        return true;
    }

    /**
     * 更新角色的所有关联关系
     */
    private void updateRoleAssociations(Long roleId, RoleDTO roleDto) {
        assignResourcesToRole(roleId, roleDto.getResourceIds());
        assignMenusToRole(roleId, roleDto.getMenuIds());
        assignButtonsToRole(roleId, roleDto.getButtonIds());
    }

    // ==================== 关联关系分配方法 ====================

    private void assignResourcesToRole(Long roleId, Set<Long> resourceIds) {
        Consumer<Long> delete = id -> roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getRoleId, id));

        relationHelper.assignRelations(roleId, resourceIds, delete, SysRoleResource::new, roleResourceMapper::upsertRoleResource, this::validateResourceIds, RelationHelper.AssociationType.RESOURCE);
    }

    private void assignMenusToRole(Long roleId, Set<Long> menuIds) {
        Consumer<Long> delete = id -> roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));

        relationHelper.assignRelations(roleId, menuIds, delete, SysRoleMenu::new, roleMenuMapper::upsertRoleMenu, this::validateMenuIds, RelationHelper.AssociationType.MENU);
    }

    private void assignButtonsToRole(Long roleId, Set<Long> buttonIds) {
        Consumer<Long> delete = id -> roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, id));

        relationHelper.assignRelations(roleId, buttonIds, delete, SysRoleButton::new, roleButtonMapper::upsertRoleButton, this::validateButtonIds, RelationHelper.AssociationType.BUTTON);
    }


    // ==================== 校验方法 ====================

    /**
     * 校验资源 ID 的有效性
     */
    private void validateResourceIds(Set<Long> resourceIds) {
        relationHelper.validateEntityExists(resourceIds, resourceMapper, SysResource::getId, RelationHelper.AssociationType.RESOURCE.getNotFoundError());
    }

    /**
     * 校验菜单 ID 的有效性
     */
    private void validateMenuIds(Set<Long> menuIds) {
        relationHelper.validateEntityExists(menuIds, menuMapper, SysMenu::getId, RelationHelper.AssociationType.MENU.getNotFoundError());
    }

    /**
     * 校验按钮 ID 的有效性
     */
    private void validateButtonIds(Set<Long> buttonIds) {
        relationHelper.validateEntityExists(buttonIds, buttonMapper, SysButton::getId, RelationHelper.AssociationType.BUTTON.getNotFoundError());
    }


    // ==================== Delete Helpers ====================

    private void deleteRoleRelations(Serializable roleId) {
        roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>().eq(SysRoleResource::getRoleId, roleId));
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getRoleId, roleId));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
    }

    private void clearRoleCache() {
        List<Serializable> keys = Collections.emptyList();
        List<String> cacheNames = List.of(CacheConstant.USER, CacheConstant.BUTTON, CacheConstant.RESOURCE, CacheConstant.MENU);
        eventService.notifyCacheEvict(keys, cacheNames);
    }
}
