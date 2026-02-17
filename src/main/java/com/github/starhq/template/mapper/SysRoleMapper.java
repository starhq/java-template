package com.github.starhq.template.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.vo.RoleVO;

/**
 * 系统角色Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    IPage<RoleVO> selectRolePage(@Param("page") Page<RoleVO> page,
            @Param("ew") Wrapper<RoleVO> wrapper);

    List<RoleVO> selectRolesByUserId(@Param("userId") Long userId);

    List<RoleVO> selectAssignedRolesByUserId(@Param("userId") Long userId);

}
