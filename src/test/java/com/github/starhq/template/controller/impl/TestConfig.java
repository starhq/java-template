package com.github.starhq.template.controller.impl;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/15 10:02
 */
@TestConfiguration
public class TestConfig {

    @Bean
    public JsonMapper jsonMapper() {
        // 1. 创建一个过滤器：遇到 sensitiveFilter 时，不做任何脱敏，直接序列化所有字段
        SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                .addFilter("sensitiveFilter", SimpleBeanPropertyFilter.serializeAll())
                .setFailOnUnknownId(false);

        // 2. 构建定制化的 JsonMapper (Jackson 3.x 的构建方式)
        return JsonMapper.builder()
                .filterProvider(filterProvider)
                .build();
    }
}
