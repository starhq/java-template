package com.github.starhq.template.model.vo.dictData;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: dict data vo
 * @date 2026/4/17 12:45
 */
@Data
public class DictDataVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 5824633509289242057L;

    private String label;
    private String value;
}
