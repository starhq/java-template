package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.enums.HttpMethod;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统资源实体
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Alias("resource")
@TableName("sys_resource")
public class SysResource extends BaseEntity {

    /**
     * 资源名称
     */
    private String name;

    /**
     * 资源URL
     */
    private String url;

    /**
     * HTTP方法（位掩码）
     */
    private HttpMethod method;

    /**
     * 描述
     */
    private String description;

}
