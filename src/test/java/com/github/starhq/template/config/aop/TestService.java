package com.github.starhq.template.config.aop;

import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.enums.TargetType;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/17 22:30
 */
public class TestService {

    @AuditLoggable(
            action = "USER_CREATE",
            targetType = TargetType.USER
    )
    public void create(TestDTO dto) {
        throw new UnsupportedOperationException();
    }

    @AuditLoggable(
            action = "USER_DELETE",
            targetType = TargetType.USER
    )
    public void remove(Long id) {
        throw new UnsupportedOperationException();
    }

    @AuditLoggable(
            action = "USER_UPDATE",
            targetType = TargetType.USER
    )
    public void update(Long id) {
        throw new UnsupportedOperationException();
    }

    @AuditLoggable(
            action = "USER_UPDATE",
            targetType = TargetType.USER
    )
    public void updateWithNoArg() {
        throw new UnsupportedOperationException();
    }

    public void noAudit() {
        throw new UnsupportedOperationException();
    }

    @AuditLoggable(
            action = "NO_ID",
            targetType = TargetType.USER
    )
    public void createNoId(NoIdDTO dto) {
        throw new UnsupportedOperationException();
    }
}
