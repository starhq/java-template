package com.github.starhq.template.config.converter;


import com.github.starhq.template.common.enums.SortEnum;
import org.springframework.core.convert.converter.Converter;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/17 16:19
 */
public class SortEnumConverter implements Converter<String, SortEnum> {
    @Override
    public SortEnum convert(String source) {
        return SortEnum.fromValue(source);
    }
}
