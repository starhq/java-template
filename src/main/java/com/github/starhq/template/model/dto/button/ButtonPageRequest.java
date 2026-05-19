package com.github.starhq.template.model.dto.button;

import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: button page dto
 * @date 2026/4/3 12:28
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonPageRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = -4711061179951332492L;
    private Long menuId;
}
