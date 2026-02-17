package com.github.starhq.template.vo;

import java.io.Serializable;

import com.github.starhq.template.entity.BaseEntity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色视图对象
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RoleVO extends BaseEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 角色代码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否为默认角色
     */
    private Boolean isDefault;

    /**
     * 是否被选中
     */
    private Boolean checked;
}
