package com.github.starhq.template.model.vo.dictData;

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
 * @description: dict data page's response
 * @date 2026/4/10 09:41
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataPageVO extends BaseAuditVO {

    @Serial
    private static final long serialVersionUID = 7807113097916961890L;
    /**
     * Dictionary type ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long typeId;

    /**
     * Dictionary label
     */
    private String label;

    /**
     * Dictionary value
     */
    private String value;

    /**
     * Description
     */
    private String description;
}
