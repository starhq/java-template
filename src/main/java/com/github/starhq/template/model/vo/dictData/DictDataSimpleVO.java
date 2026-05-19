package com.github.starhq.template.model.vo.dictData;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: dict data simple's response
 * @date 2026/4/10 09:42
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataSimpleVO extends BaseIdVO {
    @Serial
    private static final long serialVersionUID = -1167008823137757214L;

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
