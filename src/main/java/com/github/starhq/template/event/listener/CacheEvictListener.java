package com.github.starhq.template.event.listener;

import com.github.starhq.template.event.CacheEvictEvent;
import com.github.starhq.template.helper.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: evict user relations(such as role, menus, buttons and etc) cache listener
 * @date 2026/3/29 15:34
 */
@Slf4j
@RequiredArgsConstructor
public class CacheEvictListener {

    private final CacheHelper cacheHelper;

    @Async
    @TransactionalEventListener
    public void handleEvictEvent(CacheEvictEvent<?> event) {
        if (event == null) {
            return;
        }
        Collection<String> cacheNames = event.cacheNames();
        if (CollectionUtils.isEmpty(cacheNames)) {
            return;
        }

        Collection<?> keys = event.keys();
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }



        log.debug("Received cache evict event for keys: {}, caches: {}", keys, event.cacheNames());

        cacheHelper.evict(keys, cacheNames);
    }
}
