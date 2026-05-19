package com.github.starhq.template.model.dto.button;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: button create or update request
 * @date 2026/4/3 12:33
 */
@Data
public class ButtonDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = -6134204419617079273L;

    /**
     * Menu ID
     */
    @NotNull(message = "{error.param.blank}")
    private Long menuId;

    /**
     * Button name
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 100, message = "{error.param.range}")
    private String name;

    /**
     * Button code
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 100, message = "{error.param.range}")
    private String code;

    /**
     * Description
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;
}
