package com.github.starhq.template.model.vo.button;

import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: button page vo
 * @date 2026/4/3 12:37
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonPageVO extends BaseAuditVO {

    @Serial
    private static final long serialVersionUID = -4855817067297408727L;
    /**
     * Menu ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    /**
     * Button name
     */
    private String name;

    /**
     * Button code
     */
    private String code;

    /**
     * Description
     */
    private String description;
}
