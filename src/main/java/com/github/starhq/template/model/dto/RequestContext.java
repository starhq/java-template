package com.github.starhq.template.model.dto;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: request details
 * @date 2026/5/12 23:13
 */
public record RequestContext(
        String deviceFingerprint,
        String clientIp
) {
}