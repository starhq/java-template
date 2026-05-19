package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.model.vo.role.RoleCheckVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.List;

/**
 * 系统角色 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    List<RoleCheckVO> selectRolesByUserId(@Param("userId") Serializable userId);
}
