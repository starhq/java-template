package com.github.starhq.template.config.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/10 00:06
 */
@Data
@Component
@ConfigurationProperties(prefix = "star.captcha")
public class CaptchaProperties {

    // 支持类型: line、circle、shear
    private String type = "line";
    private int width = 120;
    private int height = 40;
    private int codeCount = 5;
    private int interfereCount = 170;
}
