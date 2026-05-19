package com.github.starhq.template.model.vo.menu.tree;

import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.model.vo.tree.BaseAuditTreeVO;
import com.github.starhq.template.model.vo.tree.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: audit menu vo
 * @date 2026/4/4 22:01
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuListVO extends BaseAuditTreeVO<MenuListVO> implements Tree<MenuListVO> {

    @Serial
    private static final long serialVersionUID = -2089058260613851880L;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单 URL
     */
    private String url;

    /**
     * 图标
     */
    private String icon;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 打开方式
     */
    private OpenStyle openStyle;
}
