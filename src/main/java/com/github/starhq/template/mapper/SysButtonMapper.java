package com.github.starhq.template.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.vo.ButtonVO;

/**
 * 系统按钮Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysButtonMapper extends BaseMapper<SysButton> {

    IPage<ButtonVO> selectButtonPage(@Param("menuId") Long menuId, @Param("page") Page<ButtonVO> page,
            @Param("ew") QueryWrapper<ButtonVO> queryWrapper);

    List<ButtonVO> selectButtonsByRoleId(@Param("roleId") Long roleId);

    List<SysButton> selectAssignedButtonsByRoleIds(@Param("roleIds") List<Long> roleIds);
}
