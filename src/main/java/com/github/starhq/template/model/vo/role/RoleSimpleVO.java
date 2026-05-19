package com.github.starhq.template.model.vo.role;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: simple role response (Used in the update scenario)
 * @date 2026/3/25 15:44
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleSimpleVO extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = -4170990859019139982L;
    /**
     * 角色代码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否为默认角色
     */
    private Boolean isDefault;
}
