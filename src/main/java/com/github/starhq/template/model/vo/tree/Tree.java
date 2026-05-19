package com.github.starhq.template.model.vo.tree;

import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/19 08:56
 */
public interface Tree<T> {

    Long getId();

    void setId(Long id);

    Long getParentId();

    void setParentId(Long parentId);

    List<T> getChildren();

    void setChildren(List<T> children);
}
