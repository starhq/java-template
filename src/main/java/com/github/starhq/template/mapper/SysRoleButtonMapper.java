package com.github.starhq.template.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysRoleButton;

/**
 * 角色按钮关联 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysRoleButtonMapper extends BaseMapper<SysRoleButton> {

    void upsertRoleButton(List<SysRoleButton> roleButtons);
}
