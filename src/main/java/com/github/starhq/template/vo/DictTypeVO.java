package com.github.starhq.template.vo;

import java.io.Serializable;
import java.util.List;

import com.github.starhq.template.entity.SysDictData;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class DictTypeVO extends BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * 关联的数据信息
     */
    private List<SysDictData> dataList;

}
