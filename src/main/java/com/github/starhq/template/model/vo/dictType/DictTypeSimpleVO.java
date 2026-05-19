package com.github.starhq.template.model.vo.dictType;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: simple dict type response
 * @date 2026/4/9 13:22
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictTypeSimpleVO extends BaseIdVO {
    @Serial
    private static final long serialVersionUID = 2986620577899369251L;

    /**
     * Dictionary type
     */
    private String type;

    /**
     * Dictionary name
     */
    private String name;

    /**
     * Description
     */
    private String description;
}
