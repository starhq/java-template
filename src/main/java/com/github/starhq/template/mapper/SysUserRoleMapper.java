package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户角色关联 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    void upsertUserRole(List<SysUserRole> userRoles);
}
