package com.github.starhq.template.model.dto.menu;

import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: search menu page with role id dto
 * @date 2026/4/6 23:18
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuRoleIdPageRequest extends PageRequest {
    @Serial
    private static final long serialVersionUID = 5293766598391302696L;

    private Long roleId;
}
