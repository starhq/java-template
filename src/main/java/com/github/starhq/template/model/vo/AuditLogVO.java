package com.github.starhq.template.model.vo;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.github.starhq.template.common.enums.TargetType;

import lombok.Data;

@Data
public class AuditLogVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 操作值（JSON格式）
     */
    private String value;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 创建人
     */
    private String creator;

}
