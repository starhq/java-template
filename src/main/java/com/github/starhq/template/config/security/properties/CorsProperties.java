package com.github.starhq.template.config.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: cors config properties
 * @date 2026/4/11 21:49
 */
@Data
@Component
@ConfigurationProperties(prefix = "star.cors")
public class CorsProperties {

    /**
     * 默认允许本地常见前端端口跨域 (Vue/React 默认端口)
     * 注意：使用 patterns 而不是 origins，是为了配合下面的 allowCredentials=true
     */
    private List<String> allowedOriginPatterns = List.of(
            "http://localhost:8080",
            "http://localhost:3000",
            "http://localhost:5173"  // Vite 默认端口
    );

    /**
     * 默认允许基本的 RESTful 请求方法
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /**
     * 默认允许所有请求头
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 默认允许携带凭证 (Cookie、Authorization header 等)
     * 如果设为 false，前端 axios 的 withCredentials: true 将失效
     */
    private boolean allowCredentials = true;

    /**
     * 默认预检请求缓存 1 小时 (3600秒)
     */
    private long maxAge = 3600L;
}
