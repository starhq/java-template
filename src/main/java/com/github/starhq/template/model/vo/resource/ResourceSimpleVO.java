package com.github.starhq.template.model.vo.resource;

import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: simple resource vo
 * @date 2026/4/1 22:09
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResourceSimpleVO extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = -6544229209011213164L;

    private String name;

    private String url;

    private List<HttpMethod> methods;

    private String description;
}
