package com.github.starhq.template.model.vo.resource;

import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: resource pagination vo
 * @date 2026/4/1 21:53
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResourcePageVO extends BaseAuditVO {
    @Serial
    private static final long serialVersionUID = 3597420852784856922L;

    private String name;

    private String url;

    private List<HttpMethod> methods;

    private String description;
}
