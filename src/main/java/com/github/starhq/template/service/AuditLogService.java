package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.model.dto.auditlog.AuditLogPageRequest;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: audit log service
 * @date 2026/4/10 18:54
 */
public interface AuditLogService {

    /**
     * Retrieves a paginated list of audit logs based on the provided filtering and
     * pagination information.
     *
     * @param pageInfo the pagination and filtering information for audit logs
     * @return a paginated response containing audit log details
     */
    IPage<AuditLogPageVO> page(AuditLogPageRequest pageInfo);

}
