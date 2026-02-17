package com.github.starhq.template.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.vo.TokenVO;

/**
 * 系统Token Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysTokenMapper extends BaseMapper<SysToken> {

    IPage<TokenVO> selectTokenPage(@Param("username") String username, @Param("page") Page<TokenVO> page,
            @Param("ew") Wrapper<TokenVO> wrapper);
}
