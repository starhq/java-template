package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.github.starhq.template.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    SysUser selectUserWithRole(@Param(Constants.WRAPPER) Wrapper<SysUser> wrapper);
}
