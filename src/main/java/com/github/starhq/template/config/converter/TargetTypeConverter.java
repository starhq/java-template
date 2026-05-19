package com.github.starhq.template.config.converter;

import com.github.starhq.template.common.enums.TargetType;
import org.springframework.core.convert.converter.Converter;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/17 13:33
 */
public class TargetTypeConverter implements Converter<String, TargetType> {

    @Override
    public TargetType convert(String source) {
        return TargetType.fromValue(source);
    }
}