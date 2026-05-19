package com.github.starhq.template.config;

import com.github.starhq.template.config.converter.SortEnumConverter;
import com.github.starhq.template.config.converter.TargetTypeConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/17 14:46
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new TargetTypeConverter());
        registry.addConverter(new SortEnumConverter());
    }
}
