package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统按钮实体
 *
 * @author starhq
 */
@Data
@Alias("button")
@TableName("sys_button")
@EqualsAndHashCode(callSuper = false)
public class SysButton extends BaseEntity {

    /**
     * 菜单 ID
     */
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

}
