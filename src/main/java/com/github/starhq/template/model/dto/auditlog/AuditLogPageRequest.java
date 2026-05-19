package com.github.starhq.template.model.dto.auditlog;

import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: audit log page request
 * @date 2026/4/10 14:17
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AuditLogPageRequest extends PageRequest {
    @Serial
    private static final long serialVersionUID = 5444876205894752120L;

    private TargetType targetType;

    private String username;
}
