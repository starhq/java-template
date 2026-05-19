package com.github.starhq.template.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheFactory {

    public static Cache<Object, Object> build(CacheType cacheType) {
        return Caffeine
                .newBuilder()
                .expireAfterWrite(cacheType.getTtl())
                .maximumSize(cacheType.getMaxSize())
                .softValues()
                .recordStats()
                .removalListener((key, value, cause) -> {
                    // 监听缓存移除，便于调试
                    if (cause.wasEvicted()) {
                        log.debug("Cache evicted: {} -> {}, cause: {}", key, value, cause);
                    }
                })
                .build();
    }

}
