package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统字典类型实体
 *
 * @author starhq
 */
@Data
@Alias("dictType")
@TableName("sys_dict_type")
@EqualsAndHashCode(callSuper = false)
public class SysDictType extends BaseEntity {

    /**
     * 字典类型
     */
    private String type;

    /**
     * 字典名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

}
