package com.github.starhq.template.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.vo.ResourceVO;

/**
 * 系统资源Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysResourceMapper extends BaseMapper<SysResource> {

    IPage<ResourceVO> selectResourcePage(Page<ResourceVO> page, @Param("ew") Wrapper<ResourceVO> queryWrapper);

    List<ResourceVO> selectResourcesByRoleId(@Param("roleId") Long roleId);

    List<ResourceVO> selectAssignedResourceByRoleIds(@Param("roleIds") List<Long> roleIds);

}
