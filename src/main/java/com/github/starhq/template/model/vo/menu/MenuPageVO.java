package com.github.starhq.template.model.vo.menu;

import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: menu page vo
 * @date 2026/4/5 21:15
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuPageVO extends BaseAuditVO {

    @Serial
    private static final long serialVersionUID = -7255986205687410292L;
    /**
     * 父菜单 ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

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
