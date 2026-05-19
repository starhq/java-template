package com.github.starhq.template.model.vo.resource;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: checked resource vo
 * @date 2026/4/1 21:56
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResourceCheckVO extends BaseIdVO {
    @Serial
    private static final long serialVersionUID = -8709045478621472232L;

    private String name;

    private Boolean checked;
}
