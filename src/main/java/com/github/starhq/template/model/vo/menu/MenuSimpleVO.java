package com.github.starhq.template.model.vo.menu;

import com.github.starhq.template.common.enums.OpenStyle;
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
 * @description: simple menu vo
 * @date 2026/4/4 21:22
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuSimpleVO extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = -8830378623396258161L;
    /**
     * Parent menu ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /**
     * Menu name
     */
    private String name;

    /**
     * URL
     */
    private String url;

    /**
     * Icon
     */
    private String icon;

    /**
     * Sort order
     */
    private Integer sortOrder;

    /**
     * Open style
     */
    private OpenStyle openStyle;
}
