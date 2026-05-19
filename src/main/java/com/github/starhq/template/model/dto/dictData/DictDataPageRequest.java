package com.github.starhq.template.model.dto.dictData;

import com.github.starhq.template.model.dto.page.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/10 08:25
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataPageRequest extends PageRequest {
    @Serial
    private static final long serialVersionUID = -2125107931331827571L;
    private Long dictTypeId;
}

