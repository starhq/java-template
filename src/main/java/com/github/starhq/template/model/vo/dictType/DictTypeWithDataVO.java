package com.github.starhq.template.model.vo.dictType;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: dict type with data response
 * @date 2026/4/9 13:28
 */
@Data
public class DictTypeWithDataVO implements Serializable {

    @Serial
    private static final long serialVersionUID = -5804201726027503401L;
    private String dictType;
    private List<DictDataVO> dataList;
}
