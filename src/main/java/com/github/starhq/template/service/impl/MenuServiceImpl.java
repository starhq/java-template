package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.common.util.TreeBuildUtils;
import com.github.starhq.template.common.util.TypeConvertUtils;
import com.github.starhq.template.converter.MenuConverter;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.entity.SysRoleMenu;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysMenuMapper;
import com.github.starhq.template.mapper.SysRoleMenuMapper;
import com.github.starhq.template.model.dto.menu.MenuDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.menu.MenuSimpleVO;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.menu.tree.MenuCheckVO;
import com.github.starhq.template.model.vo.menu.tree.MenuListVO;
import com.github.starhq.template.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/4 22:17
 */
@Service("menuService")
@RequiredArgsConstructor
public class MenuServiceImpl extends AuditBaseServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    private final SysUserMapperHelper userMapperHelper;
    private final SysRoleMenuMapper roleMenuMapper;

    private final MenuConverter menuConverter;
    private final EventService eventService;

    @Override
    public List<MenuListVO> selectList(PageRequest pageRequest) {
        pageRequest.setSearchCount(false);
        List<MenuListVO> result = pageVO(pageRequest,
                null,
                userMapperHelper,
                menuConverter::toListVO).getRecords();
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }

        return TreeBuildUtils.build(result);
    }

    @Cacheable(value = "menus", key = "#p0")
    @Override
    public List<LeftNavVO> selectSidebar(Serializable userId) {
        QueryWrapper<SysMenu> wrapper = new QueryWrapper<>();

        wrapper.eq(QueryConstant.USER_ID, userId).orderBy(true, false, QueryConstant.SORT);

        List<SysMenu> menus = getBaseMapper().selectAssignedMenus(wrapper);
        if (CollectionUtils.isEmpty(menus)) {
            return Collections.emptyList();
        }

        List<LeftNavVO> sidebars = menus.stream().map(menuConverter::toLeftNavVO).toList();
        return TreeBuildUtils.build(sidebars);
    }

    @Override
    public MenuSimpleVO getMenuById(Serializable id) {
        SysMenu menu = getAndCheckById(id, ErrorCode.MENU_NOT_FOUND);

        return menuConverter.toSimpleVO(menu);
    }

    @Override
    public List<MenuCheckVO> selectCheckedMenus(Serializable roleId) {
        List<MenuCheckVO> result = getBaseMapper().selectMenusByRoleId(roleId);
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        return TreeBuildUtils.build(result);
    }

    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_UPDATE)
    @Override
    public boolean updateMenu(Serializable id, MenuDTO menuDto) {
        validateParent(menuDto.getParentId());

        SysMenu menu = getAndCheckById(id, ErrorCode.MENU_NOT_FOUND);
        menuConverter.updateEntity(menuDto, menu);

        update(menu, null, ErrorCode.MENU_UPDATE_FAILED, ErrorCode.MENU_NOT_FOUND);

        cacheHelper.clear(CacheConstant.BUTTON);

        return true;
    }

    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_INSERT)
    @Override
    public boolean createMenu(MenuDTO menuDto) {
        validateParent(menuDto.getParentId());

        SysMenu menu = menuConverter.toEntity(menuDto);
        insert(menu, null, ErrorCode.MENU_INSERT_FAILED);

        return true;
    }

    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        return this.removeByIds(Collections.singletonList(id));
    }

    @AuditLoggable(targetType = TargetType.MENU, action = AuditLogConstant.MENU_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeByIds(Collection<?> list) {
        if (CollectionUtils.isEmpty(list)) {
            return true;
        }

        List<Long> ids = list.stream().map(TypeConvertUtils::toLong).toList();

        // 1. 校验是否存在
        validateMenuExists(ids);

        // 2. 校验是否包含子节点
        validateNotHasChildren(ids);

        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getMenuId, ids));

        boolean result = super.removeByIds(ids);

        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.MENU));

        return result;
    }

    private void validateParent(Serializable parentId) {
        if (Objects.isNull(parentId)) {
            return;
        }
        boolean exists = getBaseMapper().exists(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getId, parentId));
        if (!exists) {
            throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
        }
    }

    /**
     * 优化点：抽取校验逻辑，提升可读性
     */
    private void validateMenuExists(List<Long> ids) {
        long existCount = this.count(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, ids));
        if (existCount != ids.size()) {
            throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
        }
    }

    /**
     * 优化点：抽取校验逻辑，提升可读性
     */
    private void validateNotHasChildren(List<Long> ids) {
        long childCount = this.count(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getParentId, ids));
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.MENU_HAS_CHILD);
        }
    }
}
