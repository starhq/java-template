package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.dto.menu.MenuDTO;
import com.github.starhq.template.model.vo.menu.MenuPageVO;
import com.github.starhq.template.model.vo.menu.MenuSimpleVO;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.menu.tree.MenuListVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: menu converter
 * @date 2026/4/6 23:32
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuConverter {

    MenuSimpleVO toSimpleVO(SysMenu entity);

    MenuPageVO toPageVO(SysMenu entity);

    MenuListVO toListVO(SysMenu entity);

    LeftNavVO toLeftNavVO(SysMenu entity);

    SysMenu toEntity(MenuDTO dto);

    void updateEntity(MenuDTO dto, @MappingTarget SysMenu entity);
}
