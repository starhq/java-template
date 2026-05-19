package com.github.starhq.template.model.dto.menu;

import com.github.starhq.template.common.enums.OpenStyle;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: menu request's dto
 * @date 2026/4/4 14:05
 */
@Data
public class MenuDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 995675792L;

    /**
     * Parent menu ID
     */
    private Long parentId;

    /**
     * Menu name
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 30, message = "{error.param.range}")
    private String name;

    /**
     * URL
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 100, message = "{error.param.range}")
    private String url;

    /**
     * Icon
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 50, message = "{error.param.range}")
    private String icon;

    /**
     * Sort order
     */
    @NotNull(message = "{error.param.blank}")
    @Min(value = 0, message = "{error.param.min}")
    @Max(value = 9999, message = "{error.param.max}")
    private Integer sortOrder;

    /**
     * Open style
     */
    @NotNull(message = "{error.param.blank}")
    private OpenStyle openStyle;
}
