package com.github.starhq.template.model.vo.dictType;

import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: page dict type response
 * @date 2026/4/9 13:24
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictTypePageVO extends BaseAuditVO {
    @Serial
    private static final long serialVersionUID = 2095749722209419518L;

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
