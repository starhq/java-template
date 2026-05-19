package com.github.starhq.template.helper;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: cache's helper
 * @date 2026/4/14 11:25
 */
@Slf4j
@RequiredArgsConstructor
public class CacheHelper {

    private final CacheManager cacheManager;

    public <K, V> void put(K key, V value, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (null != cache) {
            cache.put(key, value);
        }
    }

    public <K, V> V get(K key, Class<V> clazz, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (null != cache) {
            return cache.get(key, clazz);
        }
        return null;
    }

    public <K> void evict(Collection<K> ids, Collection<String> cacheNames) {
        if (CollectionUtils.isEmpty(cacheNames)) {
            return;
        }

        for (String name : cacheNames) {
            Optional.ofNullable(cacheManager.getCache(name)).ifPresent(c -> {
                if (CollectionUtils.isEmpty(ids)) {
                    c.clear();
                } else {
                    ids.forEach(c::evict);
                }
            });
        }
    }

    public void clear(String cacheName) {
        Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(Cache::clear);
    }

    /**
     * Generic Cache-Aside Pattern for ID-to-Name mapping
     */
    public Map<Long, String> getBatchWithCache(Set<Long> ids, String cacheName, Function<Set<Long>, Map<Long, String>> dbLoader) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        Cache cache = cacheManager.getCache(cacheName);
        Map<Long, String> result = new HashMap<>();
        Set<Long> missIds = new HashSet<>();

        // 1. Hit Cache
        for (Long id : ids) {
            String value = (cache != null) ? cache.get(id, String.class) : null;
            if (value != null) {
                result.put(id, value);
            } else {
                missIds.add(id);
            }
        }

        // 2. Hit DB and write back
        if (!missIds.isEmpty()) {
            Map<Long, String> dbData = dbLoader.apply(missIds);
            result.putAll(dbData);
            if (cache != null) {
                dbData.forEach(cache::put);
            }
        }
        return result;
    }

    public Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (Objects.isNull(cache)) {
            log.warn("{}'s cache is null", cacheName);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return cache;
    }
}
