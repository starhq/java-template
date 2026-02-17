package com.github.starhq.template.vo;

import java.io.Serializable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色视图对象
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonVO extends BaseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 菜单ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    /**
     * 按钮名称
     */
    private String name;

    /**
     * 按钮代码
     */
    private String code;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否被选中
     */
    private Boolean checked;
}
