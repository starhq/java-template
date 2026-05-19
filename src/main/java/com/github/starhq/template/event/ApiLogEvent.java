package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysApiLog;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: api log save event
 * @date 2026/5/5 11:19
 */
public record ApiLogEvent(SysApiLog apiLog) {
}
