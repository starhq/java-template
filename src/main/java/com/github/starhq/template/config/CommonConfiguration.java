package com.github.starhq.template.config;

import com.github.starhq.template.aop.aspect.AuditLogAspect;
import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.event.listener.ApiLogEventListener;
import com.github.starhq.template.event.listener.AuditLogListener;
import com.github.starhq.template.event.listener.CacheEvictListener;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.LoggerJsonSensitiveHelper;
import com.github.starhq.template.mapper.SysApiLogMapper;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import org.springframework.cache.CacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author wangjian
 * @version v1.0.0
 *          Copyright (C), 2020-2026, starimba@outlook.com
 * @description: some common beans
 * @date 2026/3/29 15:50
 */
@Configuration
public class CommonConfiguration {

    @Bean
    AuditLogAspect auditLogAspect(EventService eventService, JsonMapper jsonMapper) {
        return new AuditLogAspect(eventService, jsonMapper);
    }

    @Bean
    CacheEvictListener roleCacheEvictListener(CacheHelper cacheHelper) {
        return new CacheEvictListener(cacheHelper);
    }

    @Bean
    AuditLogListener auditLogListener(SysAuditLogMapper sysAuditLogMapper) {
        return new AuditLogListener(sysAuditLogMapper);
    }

    @Bean
    ApiLogEventListener apiLogEventListener(SysApiLogMapper apiLogMapper) {
        return new ApiLogEventListener(apiLogMapper);
    }

    @Bean
    CacheHelper cacheHelper(CacheManager cacheManager) {
        return new CacheHelper(cacheManager);
    }

    @Bean
    EventService cacheEventService(ApplicationEventPublisher eventPublisher) {
        return new EventService(eventPublisher);
    }

    @Bean
    LoggerJsonSensitiveHelper loggerJsonSensitiveHelper(JsonMapper jsonMapper,
            SensitiveFieldProperties sensitiveFieldProperties) {
        return new LoggerJsonSensitiveHelper(jsonMapper, sensitiveFieldProperties);
    }
}
