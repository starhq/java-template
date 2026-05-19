package com.github.starhq.template.model.vo.menu;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: checked menu vo
 * @date 2026/4/6 09:33
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuCheckVO extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = -6263663296433871145L;

    private String name;

    private Boolean checked;
}
