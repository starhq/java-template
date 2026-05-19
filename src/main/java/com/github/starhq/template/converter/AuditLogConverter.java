package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: audit log converter
 * @date 2026/4/10 19:00
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AuditLogConverter {

    AuditLogPageVO toPageVO(SysAuditLog entity);

}
