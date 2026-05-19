package com.github.starhq.template.model.vo.role;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: only name and checked status role
 * @date 2026/3/25 15:51
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleCheckVO extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = 4185116018187356828L;

    private String name;

    private Boolean checked;
}
