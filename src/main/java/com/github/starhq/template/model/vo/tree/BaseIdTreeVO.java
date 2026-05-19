package com.github.starhq.template.model.vo.tree;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: base tree vo with id
 * @date 2026/4/4 21:52
 */
@Data
@EqualsAndHashCode(callSuper = false)
public abstract class BaseIdTreeVO<T> extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = 1103929040429395290L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    private List<T> children;
}
