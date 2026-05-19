package com.github.starhq.template.event.listener;

import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.event.AuditLogEvent;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: evict user relations(such as role, menus, buttons and etc) cache listener
 * @date 2026/3/29 15:34
 */
@Slf4j
@RequiredArgsConstructor
public class AuditLogListener {

    private final SysAuditLogMapper auditLogMapper;

    @Async
    @TransactionalEventListener
    public void handleEvictEvent(AuditLogEvent event) {
        if (event == null) {
            return;
        }
        SysAuditLog sysAuditLog = event.auditLog();
        try {
            auditLogMapper.insert(sysAuditLog);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage(), e);
        }
    }
}
