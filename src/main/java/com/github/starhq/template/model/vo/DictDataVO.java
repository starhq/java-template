package com.github.starhq.template.model.vo;

import java.io.Serializable;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataVO extends BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

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
