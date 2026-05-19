package com.github.starhq.template.mapper;

import com.github.starhq.template.common.constant.ProfileConstants;
import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysApiLog;
import org.springframework.context.annotation.Profile;

@Profile(ProfileConstants.DEV)
@Mapper
public interface SysApiLogMapper extends BaseMapper<SysApiLog> {

}
