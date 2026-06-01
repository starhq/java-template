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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Central configuration class for instantiating common infrastructure beans.
 *
 * <p>This class acts as the central registry for beans that are not suited for standard component scanning.
 * By defining them explicitly as `@Bean` here, we solve common architectural issues such as:
 * <ul>
 *   <li><b>Dependency Cycles:</b> Component A needs Component B, and Component B needs Component A.</li>
 *   <li><b>Interface Polymorphism:</b> Defining multiple implementations for a single interface and explicitly choosing which one to use.</li>
 *   <li><b>Third-party Integration:</b> Wrapping third-party objects before injecting them into the context.</li>
 * </ul>
 *
 * @author wangjian
 * @version v1.0.0
 */
@Configuration
public class CommonConfiguration {

    /**
     * Creates the AOP aspect responsible for intercepting service methods to generate audit logs.
     *
     * <p>Explicit bean definition prevents potential circular dependency issues if the aspect relies on
     * beans that are also annotated with {@code @CacheEvict} or other aspects.
     *
     * @param eventService The service used to publish the audit event.
     * @param jsonMapper   The JSON serializer to mask sensitive fields in the log.
     * @return a configured {@link AuditLogAspect} instance
     */
    @Bean
    AuditLogAspect auditLogAspect(EventService eventService, JsonMapper jsonMapper) {
        return new AuditLogAspect(eventService, jsonMapper);
    }

    /**
     * Creates the listener that clears cache when audit logs are created/updated.
     *
     * <p>Usually, listeneing to {@code CacheEvictEvent} is done via {@code @EventListener}.
     * Defining it as a bean allows for conditional instantiation (e.g., disabling it in test profiles),
     * or replacing it with a more optimized custom implementation.
     *
     * @param cacheHelper The cache utility used to perform eviction logic.
     * @return a configured {@link CacheEvictListener} instance
     */
    @Bean
    CacheEvictListener roleCacheEvictListener(CacheHelper cacheHelper) {
        return new CacheEvictListener(cacheHelper);
    }

    /**
     * Creates the listener that handles persistence of standard business audit logs (e.g., user updates, role assignments).
     *
     * @param sysAuditLogMapper The MyBatis mapper for inserting audit records into the database.
     * @return a configured {@link AuditLogListener} instance
     */
    @Bean
    AuditLogListener auditLogListener(SysAuditLogMapper sysAuditLogMapper) {
        return new AuditLogListener(sysAuditLogMapper);
    }

    /**
     * Creates the listener that handles persistence of detailed HTTP API request/response logs.
     *
     * @param apiLogMapper The MyBatis mapper for inserting API request logs.
     * @return a configured {@link ApiLogEventListener} instance
     */
    @Bean
    ApiLogEventListener apiLogEventListener(SysApiLogMapper apiLogMapper) {
        return new ApiLogEventListener(apiLogMapper);
    }

    /**
     * Utility wrapper around Spring's {@link CacheManager}.
     *
     * <p>Wrapping it in our custom {@link CacheHelper} provides a higher-level, simplified API
     * (e.g., {@code cacheHelper.getBatchWithCache(...)}).
     * Creating it as a bean here makes it easy to mock in unit tests without relying on static method calls.
     *
     * @param cacheManager The underlying Spring CacheManager abstraction (Caffeine, Redis, etc.).
     * @return a configured {@link CacheHelper} instance
     */
    @Bean
    CacheHelper cacheHelper(CacheManager cacheManager) {
        return new CacheHelper(cacheManager);
    }

    /**
     * Service acting as the bridge between the application and Spring's underlying event publishing mechanism.
     *
     * <p>Hides the default {@link org.springframework.context.ApplicationEventPublisher} to add our custom
     * validation logic (e.g., filtering out irrelevant events) or to mock in tests without using reflection.
     *
     * @param eventPublisher The underlying Spring publisher.
     * @return a configured {@link EventService} instance
     */
    @Bean
    EventService cacheEventService(org.springframework.context.ApplicationEventPublisher eventPublisher) {
        return new EventService(eventPublisher);
    }

    /**
     * Utility for masking sensitive fields in JSON payloads before logging.
     *
     * <p>Exposed as a bean so it can be easily injected into filters (like {@link com.github.starhq.template.config.security.filter.RequestResponseLoggingFilter})
     * and aspects without manually instantiating the mapper.
     *
     * @param jsonMapper               The Jackson mapper used for JSON processing.
     * @param sensitiveFieldProperties The rules defining what constitutes a sensitive field.
     * @return a configured {@link LoggerJsonSensitiveHelper} instance
     */
    @Bean
    LoggerJsonSensitiveHelper loggerJsonSensitiveHelper(tools.jackson.databind.json.JsonMapper jsonMapper,
                                                        SensitiveFieldProperties sensitiveFieldProperties) {
        return new LoggerJsonSensitiveHelper(jsonMapper, sensitiveFieldProperties);
    }

}