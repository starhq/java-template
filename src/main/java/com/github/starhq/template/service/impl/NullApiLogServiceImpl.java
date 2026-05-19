package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile(ProfileConstants.NON_DEV)
@RequiredArgsConstructor
@Service("apiLogService")
public class NullApiLogServiceImpl implements ApiLogService {

    @Override
    public void create(SysApiLog apiLog) {
    }

    @Override
    public SysApiLog getByTraceId(String traceId) {
        return null;
    }
}
