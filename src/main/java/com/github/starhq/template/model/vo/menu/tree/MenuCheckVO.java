package com.github.starhq.template.model.vo.menu.tree;

import com.github.starhq.template.model.vo.tree.BaseIdTreeVO;
import com.github.starhq.template.model.vo.tree.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: checked menu vo
 * @date 2026/4/4 21:59
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuCheckVO extends BaseIdTreeVO<MenuCheckVO> implements Tree<MenuCheckVO> {

    @Serial
    private static final long serialVersionUID = -6263663296433871145L;

    /**
     * Menu name
     */
    private String name;

    /**
     * Whether the menu is checked/selected
     */
    private Boolean checked;
}
