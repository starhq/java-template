package com.github.starhq.template.model.vo.button;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: checked button vo
 * @date 2026/4/3 12:38
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonCheckVO extends BaseIdVO {

    @Serial
    private static final long serialVersionUID = 8927044383616401568L;
    /**
     * Button name
     */
    private String name;

    /**
     * Whether the button is checked
     */
    private Boolean checked;
}
