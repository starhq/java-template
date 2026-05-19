package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysRoleResource;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 角色资源关联 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysRoleResourceMapper extends BaseMapper<SysRoleResource> {

    void upsertRoleResource(List<SysRoleResource> roleResources);
}
