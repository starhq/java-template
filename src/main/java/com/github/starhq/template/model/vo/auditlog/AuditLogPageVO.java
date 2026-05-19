package com.github.starhq.template.model.vo.auditlog;

import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: audit log page response
 * @date 2026/4/10 14:21
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AuditLogPageVO extends BaseAuditVO {

    @Serial
    private static final long serialVersionUID = -7698919411727493781L;
    /**
     * Action performed
     */
    private String action;

    /**
     * Target ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetId;

    /**
     * Target type
     */
    private TargetType targetType;

    /**
     * Value
     */
    private String value;

}
