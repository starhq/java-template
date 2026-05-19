package com.github.starhq.template;

import com.github.starhq.template.common.captcha.*;
import com.github.starhq.template.common.enums.HttpMethod;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/9 12:49
 */
public class CommonTest {

    @Test
    void test() throws IOException, InterruptedException {
        Map<String, HttpMethod> maps = Map.of("methods", HttpMethod.GET);
        JsonMapper jsonMapper = new JsonMapper();
        System.out.println(jsonMapper.writeValueAsString(maps));
    }
}
