package com.github.starhq.template.service;

import com.github.starhq.template.entity.SysApiLog;

public interface ApiLogService {

    void create(SysApiLog apiLog);

    SysApiLog getByTraceId(String traceId);
}
