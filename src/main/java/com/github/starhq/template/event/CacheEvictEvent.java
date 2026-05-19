package com.github.starhq.template.event;

import java.util.Collection;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: evict cache event
 * @date 2026/3/29 15:33
 */
public record CacheEvictEvent<ID>(Collection<ID> keys, Collection<String> cacheNames) {
}
