package com.github.starhq.template.model.dto.dictType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: create or update dict type dto
 * @date 2026/4/9 13:17
 */
@Data
public class DictTypeDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -3504131929015871485L;
    /**
     * Description
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

    /**
     * Dictionary type
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 100, message = "{error.param.range}")
    private String type;

    /**
     * Dictionary name
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String name;
}
