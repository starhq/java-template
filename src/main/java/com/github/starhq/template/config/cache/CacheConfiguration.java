package com.github.starhq.template.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache infrastructure configuration.
 *
 * <p>Configures a {@link CacheManager} backed by Caffeine, bridging Spring's abstract
 * caching annotations (e.g., {@code @Cacheable}) with Caffeine's high-performance local cache.
 *
 * @author starhq
 */
@Configuration
public class CacheConfiguration {

    /**
     * Creates and configures the application-wide {@link CacheManager}.
     *
     * <p>The manager is pre-loaded with specific cache configurations extracted from the
     * {@link CacheType} enum using the custom {@link CacheFactory}.
     *
     * @return the configured Spring {@link CacheManager}
     */
    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        // ⚠️ CRITICAL WARNING: This sets a GLOBAL FALLBACK specification for caches that are NOT
        // explicitly registered below (e.g., if someone mistypes a cache name like @Cacheable("userz")).
        //
        // DANGER: This string currently conflicts with the precise configurations built in CacheFactory.
        // Specifically, using `weakKeys` here but `softValues` in CacheFactory can lead to undefined
        // caching behaviors. If the intention is ONLY to use the explicitly registered caches below,
        // it is highly recommended to either:
        // 1. Remove this line entirely (unregistered caches will fail fast, which is safer).
        // 2. Or ensure this string perfectly matches your default baseline strategy.
        manager.setCacheSpecification("maximumSize=1000,expireAfterWrite=30m,softValues,recordStats");

        // Register strictly defined caches based on the centralized enum.
        // When @Cacheable("users") is used, Spring will look up this specific instance instead of the fallback.
        for (CacheType type : CacheType.values()) {
            manager.registerCustomCache(type.getCacheName(),
                    CacheFactory.build(type));
        }

        return manager;
    }

    // TODO: Implement cache warmup logic.
    // It is highly recommended to preload critical, read-heavy caches (like DICT_TYPE, PERMISSION)
    // during application startup using @PostConstruct or ApplicationRunner to prevent cold-start latency spikes.
}