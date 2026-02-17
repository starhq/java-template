package com.github.starhq.template.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 角色更新DTO
 *
 * @author starhq
 */
@Data
public class RoleUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 角色名称
     */
    @Size(max = 50, message = "角色名称长度不能超过50")
    private String name;

    /**
     * 描述
     */
    @Size(max = 255, message = "描述长度不能超过255")
    private String description;

    /**
     * 是否为默认角色
     */
    private Boolean isDefault;
}
