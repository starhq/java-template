package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.vo.menu.tree.MenuCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * 系统菜单 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<SysMenu> selectAssignedMenus(@Param(Constants.WRAPPER) QueryWrapper<SysMenu> wrapper);

    List<MenuCheckVO> selectMenusByRoleId(@Param("roleId") Serializable roleId);

    List<Long> selectUserIdsByMenuId(@Param("menuId") Serializable menuId);
}
