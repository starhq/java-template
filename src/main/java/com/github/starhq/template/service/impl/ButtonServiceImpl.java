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
import com.github.starhq.template.converter.ButtonConverter;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.entity.SysRoleButton;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysButtonMapper;
import com.github.starhq.template.mapper.SysMenuMapper;
import com.github.starhq.template.mapper.SysRoleButtonMapper;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;
import com.github.starhq.template.service.ButtonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/3 12:54
 */
@Slf4j
@Service("buttonService")
@RequiredArgsConstructor
public class ButtonServiceImpl extends AuditBaseServiceImpl<SysButtonMapper, SysButton> implements ButtonService {

    private final SysMenuMapper menuMapper;
    private final SysRoleButtonMapper roleButtonMapper;
    private final SysUserMapperHelper userMapperHelper;

    private final ButtonConverter buttonConverter;
    private final EventService eventService;

    @Override
    public IPage<ButtonPageVO> page(ButtonPageRequest pageInfo) {
        return pageVO(pageInfo,
                wrapper -> {
                    if (!Objects.isNull(pageInfo.getMenuId())) {
                        wrapper.eq(QueryConstant.MENU_ID, pageInfo.getMenuId());
                    }
                },
                userMapperHelper,
                buttonConverter::toPageVO);
    }

    @Cacheable(value = "buttons", key = "#p0")
    @Override
    public List<String> select(Serializable userId) {
        List<SysButton> buttons = getBaseMapper().selectAssignedButtonsByUserId(userId);
        if (CollectionUtils.isEmpty(buttons)) {
            return Collections.emptyList();
        }
        return buttons.stream().map(SysButton::getCode).toList();
    }

    @Override
    public ButtonSimpleVO getButtonById(Serializable id) {
        SysButton button = getAndCheckById(id, ErrorCode.BUTTON_NOT_FOUND);

        return buttonConverter.toSimpleVO(button);
    }

    @Override
    public List<ButtonCheckVO> selectCheckedButtons(Serializable roleId) {
        return getBaseMapper().selectButtonsByRoleId(roleId);
    }

    @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_INSERT)
    @Override
    public boolean createButton(ButtonDTO buttonDto) {
        validateMenu(buttonDto);

        SysButton button = buttonConverter.toEntity(buttonDto);
        insert(button, ErrorCode.BUTTON_DUPLICATE_CODE, ErrorCode.BUTTON_INSERT_FAILED);

        return true;
    }


    @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_UPDATE)
    @Override
    public boolean updateButton(Serializable id, ButtonDTO buttonDto) {
        validateMenu(buttonDto);

        SysButton button = getAndCheckById(id, ErrorCode.BUTTON_NOT_FOUND);
        buttonConverter.updateEntity(buttonDto, button);

        update(button, ErrorCode.BUTTON_DUPLICATE_CODE, ErrorCode.BUTTON_UPDATE_FAILED, ErrorCode.BUTTON_NOT_FOUND);

        cacheHelper.clear(CacheConstant.BUTTON);

        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    @AuditLoggable(targetType = TargetType.BUTTON, action = AuditLogConstant.BUTTON_REMOVE)
    @Override
    public boolean removeById(Serializable id) {
        roleButtonMapper.delete(new LambdaQueryWrapper<SysRoleButton>().eq(SysRoleButton::getButtonId, id));
        delete(id, ErrorCode.BUTTON_NOT_FOUND, ErrorCode.BUTTON_DELETE_FAILED);

        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.BUTTON));

        return true;
    }

    private void validateMenu(ButtonDTO buttonDto) {
        boolean exists = menuMapper.exists(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getId, buttonDto.getMenuId()));
        if (!exists) {
            throw new NotFoundException(ErrorCode.MENU_NOT_FOUND);
        }
    }
}
