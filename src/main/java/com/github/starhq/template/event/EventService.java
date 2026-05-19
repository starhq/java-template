package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.entity.SysAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: fired event service
 * @date 2026/4/15 13:44
 */
@RequiredArgsConstructor
public class EventService {

    private final ApplicationEventPublisher eventPublisher;

    public <T> void notifyCacheEvict(List<T> keys, List<String> cacheNames) {
        eventPublisher.publishEvent(new CacheEvictEvent<>(keys, cacheNames));
    }

    public void notifyAuditLogSave(SysAuditLog auditLog) {
        eventPublisher.publishEvent(new AuditLogEvent(auditLog));
    }

    public void notifyApiLogSave(SysApiLog apiLog) {
        eventPublisher.publishEvent(new ApiLogEvent(apiLog));
    }
}
