package com.github.starhq.template.event.listener;

import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.event.ApiLogEvent;
import com.github.starhq.template.mapper.SysApiLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/5 11:20
 */
@Slf4j
@RequiredArgsConstructor
public class ApiLogEventListener {

    private final SysApiLogMapper apiLogMapper;

    @Async
    public void handleEvictEvent(ApiLogEvent event) {
        if (event == null) {
            return;
        }
        SysApiLog apiLog = event.apiLog();
        try {
            apiLogMapper.insert(apiLog);
        } catch (Exception e) {
            log.warn("Failed to save api log: {}", e.getMessage(), e);
        }
    }
}
