package com.github.starhq.template.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();

        manager.setCacheSpecification("maximumSize=1000,expireAfterWrite=30m,weakKeys,recordStats");

        for (CacheType type : CacheType.values()) {
            manager.registerCustomCache(type.getCacheName(),
                    CacheFactory.build(type));
        }

        return manager;
    }

    @Bean("customKeyGenerator")
    KeyGenerator keyGenerator() {
        return new CacheKeyGenerator();
    }

    // TODO not forget add cache warmup
}
