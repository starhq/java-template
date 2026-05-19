package com.github.starhq.template.model.vo.tree;

import java.io.Serial;
import java.util.List;

import com.github.starhq.template.model.vo.BaseAuditVO;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * @author wangjian
 * @version v1.0.0
 *          Copyright (C), 2020-2026, starimba@outlook.com
 * @description: base tree vo with audit
 * @date 2026/4/4 21:52
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseAuditTreeVO<T> extends BaseAuditVO {

    @Serial
    private static final long serialVersionUID = 1103929040429395290L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private List<T> children;
}
