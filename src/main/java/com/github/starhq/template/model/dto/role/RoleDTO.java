package com.github.starhq.template.model.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: role create dto
 * @date 2026/3/25 14:32
 */
@Data
public class RoleDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 972602042L;

    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 50, message = "{error.param.range}")
    private String code;

    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 50, message = "{error.param.range}")
    private String name;

    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

    private Set<Long> resourceIds;

    private Set<Long> menuIds;

    private Set<Long> buttonIds;
}
