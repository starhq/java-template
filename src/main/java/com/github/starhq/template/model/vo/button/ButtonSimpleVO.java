package com.github.starhq.template.model.vo.button;

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
 * @description: simple button vo
 * @date 2026/4/3 12:40
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonSimpleVO extends BaseIdVO {
    @Serial
    private static final long serialVersionUID = -7649357997606348917L;

    /**
     * Menu ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    /**
     * Button name
     */
    private String name;

    /**
     * Button code
     */
    private String code;

    /**
     * Description
     */
    private String description;
}
