package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.mapper.SysApiLogMapper;
import com.github.starhq.template.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile(ProfileConstants.DEV)
@RequiredArgsConstructor
@Service("apiLogService")
public class ApiLogServiceImpl implements ApiLogService {

    private final SysApiLogMapper apiLogMapper;

    @Override
    public void create(SysApiLog apiLog) {
        apiLogMapper.insert(apiLog);
    }

    @Override
    public SysApiLog getByTraceId(String traceId) {
        SysApiLog apiLog = apiLogMapper.selectOne(new LambdaQueryWrapper<SysApiLog>().eq(SysApiLog::getTraceId, traceId));
        if (null == apiLog) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }
        return apiLog;
    }
}
