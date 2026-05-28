package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.entity.SysAuditLog;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.mockito.Mockito.verify;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/16 22:51
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EventService eventService;

    @Test
    void testNotifyCacheEvent() {
        List<Long> keys = List.of(1L, 2L);
        List<String> cacheNames = List.of("user", "token");
        CacheEvictEvent<Long> event = new CacheEvictEvent<>(keys, cacheNames);

        eventService.notifyCacheEvict(keys, cacheNames);

        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void notifyAuditLogSave() {
        SysAuditLog auditLog = new SysAuditLog();
        AuditLogEvent event = new AuditLogEvent(auditLog);

        eventService.notifyAuditLogSave(auditLog);

        verify(eventPublisher).publishEvent(event);
    }

    @Test
    void notifyApiLogSave() {
        SysApiLog apiLog = new SysApiLog();
        ApiLogEvent event = new ApiLogEvent(apiLog);

        eventService.notifyApiLogSave(apiLog);

        verify(eventPublisher).publishEvent(event);
    }
}
