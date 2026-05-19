package com.github.starhq.template.model.dto.resource;

import com.github.starhq.template.common.enums.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: resource create or update request's dto
 * @date 2026/4/1 11:04
 */
@Data
public class ResourceDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -4807247444100943891L;

    @NotBlank(message = "{error.param.blank}")
    @Size(min = 5, max = 100, message = "{error.param.range}")
    private String url;

    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 30, message = "{error.param.range}")
    private String name;

    @NotEmpty(message = "{error.param.blank}")
    private List<HttpMethod> methods;

    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;
}
