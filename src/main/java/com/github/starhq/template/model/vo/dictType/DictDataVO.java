package com.github.starhq.template.model.vo.dictType;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/9 14:26
 */
@Data
public class DictDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 5724782551937991433L;
    private String dictLabel;
    private String dictValue;
}
