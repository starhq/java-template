package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysAuditLog;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: audit log save event
 * @date 2026/4/16 12:57
 */
public record AuditLogEvent(SysAuditLog auditLog) {
}
