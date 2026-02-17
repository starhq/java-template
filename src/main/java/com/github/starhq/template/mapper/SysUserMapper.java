package com.github.starhq.template.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.vo.UserVO;

/**
 * 系统用户Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    IPage<UserVO> selectUserPage(@Param("username") String username, @Param("page") Page<UserVO> page,
            @Param("ew") Wrapper<UserVO> wrapper);
}
