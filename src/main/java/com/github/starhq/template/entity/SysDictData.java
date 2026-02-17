package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统字典数据实体
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Alias("dictData")
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /**
     * 字典类型ID
     */
    private Long typeId;

    /**
     * 标签
     */
    private String label;

    /**
     * 值
     */
    private String value;

    /**
     * 描述
     */
    private String description;

}
