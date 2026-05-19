package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.vo.token.TokenPageVO;

/**
 * 系统Token Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysTokenMapper extends BaseMapper<SysToken> {

    IPage<TokenPageVO> selectTokenPage(@Param("page") Page<TokenPageVO> page,
            @Param(Constants.WRAPPER) Wrapper<TokenPageVO> wrapper);

    /**
     * 插入或更新 Token (单点登录核心)
     */
    void upsertToken(SysToken token);
}
