package com.github.starhq.template.vo;

import java.io.Serializable;

import com.github.starhq.template.enums.HttpMethod;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class ResourceVO extends BaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

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

    /**
     * 是否被选中
     */
    private Boolean checked;

}
