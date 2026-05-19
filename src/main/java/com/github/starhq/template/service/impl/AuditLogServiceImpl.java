package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import com.github.starhq.template.model.dto.auditlog.AuditLogPageRequest;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import com.github.starhq.template.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/10 18:56
 */
@Service("auditLogService")
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final SysAuditLogMapper auditLogMapper;

    @Override
    public IPage<AuditLogPageVO> page(AuditLogPageRequest pageInfo) {
        Page<AuditLogPageVO> page = pageInfo.toPage();

        QueryWrapper<AuditLogPageVO> wrapper = pageInfo.toQueryWrapper();
        if (!Objects.isNull(pageInfo.getTargetType())) {
            wrapper.eq(QueryConstant.TARGET_TYPE, pageInfo.getTargetType());
        }
        if (StringUtils.hasText(pageInfo.getUsername())) {
            wrapper.likeRight(QueryConstant.CREATOR, pageInfo.getUsername());
        }

        return auditLogMapper.selectAuditLogPage(page, wrapper);
    }
}
