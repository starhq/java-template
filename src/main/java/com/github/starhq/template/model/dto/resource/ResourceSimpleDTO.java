package com.github.starhq.template.model.dto.resource;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: simple resource dto (only method mask and url)
 * @date 2026/4/1 22:15
 */
@Data
public class ResourceSimpleDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 662183419069882855L;

    private String url;

    private Integer method;
}
