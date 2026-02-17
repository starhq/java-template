package com.github.starhq.template.vo;

import java.io.Serializable;
import java.util.List;

import com.github.starhq.template.enums.OpenStyle;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class MenuVO extends BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 父菜单ID
     */
    private Long parentId;

    /**
     * 菜单名称
     */
    private String name;

    /**
     * 菜单URL
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

    /**
     * 是否被选中
     */
    private Boolean checked;

    /**
     * 子菜单
     */
    private List<MenuVO> children;

}
