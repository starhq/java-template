package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统资源实体
 *
 * @author starhq
 */
@Data
@Alias("resource")
@TableName("sys_resource")
@EqualsAndHashCode(callSuper = false)
public class SysResource extends BaseEntity {

    /**
     * 资源名称
     */
    private String name;

    /**
     * 资源 URL
     */
    private String url;

    /**
     * HTTP方法（位掩码）
     */
    private Integer methods;

    /**
     * 描述
     */
    private String description;

}
