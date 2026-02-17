package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统角色实体
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Alias("role")
@TableName("sys_role")
public class SysRole extends BaseEntity {

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

}
