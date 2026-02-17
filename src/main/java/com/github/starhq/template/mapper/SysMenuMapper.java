package com.github.starhq.template.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.vo.MenuVO;

/**
 * 系统菜单Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    List<MenuVO> selectMenus();

    List<MenuVO> selectMenusByRoleId(@Param("roleId") Long roleId);

    List<MenuVO> selectAssignedMenusByRoleIds(@Param("roleIds") List<Long> roleId);

}
