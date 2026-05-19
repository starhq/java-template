package com.github.starhq.template.model.dto.dictData;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/10 08:24
 */
@Data
public class DictDataDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 981980663L;

    /**
     * Dictionary type ID
     */
    @NotNull(message = "{error.param.blank}")
    private Long typeId;

    /**
     * Dictionary label
     */
    @NotNull(message = "{error.param.blank}")
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String label;

    /**
     * Dictionary value
     */
    @NotNull(message = "{error.param.blank}")
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String value;

    /**
     * Description
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

}
