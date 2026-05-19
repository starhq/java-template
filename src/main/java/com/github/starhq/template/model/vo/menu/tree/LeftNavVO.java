package com.github.starhq.template.model.vo.menu.tree;

import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.model.vo.tree.BaseIdTreeVO;
import com.github.starhq.template.model.vo.tree.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: left nav
 * @date 2026/4/4 22:06
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LeftNavVO extends BaseIdTreeVO<LeftNavVO> implements Tree<LeftNavVO> {

    @Serial
    private static final long serialVersionUID = 4586445110027566788L;
    /**
     * Menu name
     */
    private String name;

    /**
     * URL
     */
    private String url;

    /**
     * Icon
     */
    private String icon;

    /**
     * Sort order
     */
    private Integer sortOrder;

    /**
     * Open style
     */
    private OpenStyle openStyle;
}
