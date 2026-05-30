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

        // Register strictly defined caches based on the centralized enum.
        // When @Cacheable("users") is used, Spring will look up this specific instance instead of the fallback.
        for (CacheType type : CacheType.values()) {
            manager.registerCustomCache(type.getCacheName(),
                    CacheFactory.build(type));
        }

        return manager;
    }
}