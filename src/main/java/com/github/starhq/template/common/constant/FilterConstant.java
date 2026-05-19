package com.github.starhq.template.common.constant;

import com.github.starhq.template.common.enums.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: some constants for filter
 * @date 2026/5/6 21:23
 */
public interface FilterConstant {

    String REFRESH_ENDPOINT = "auth/refresh";

    String LOGOUT_ENDPOINT = "auth/logout";

    Map<String, HttpMethod> HTTP_METHOD_CACHE = Map.of("GET", HttpMethod.GET, "POST",
            HttpMethod.POST, "PUT", HttpMethod.PUT, "DELETE", HttpMethod.DELETE, "PATCH", HttpMethod.PATCH, "HEAD",
            HttpMethod.HEAD, "OPTIONS", HttpMethod.OPTIONS);

    List<String> EXCLUDE_PATHS = List.of(
            "/actuator/health", "/favicon.ico", "/static/", "/webjars/", "/error");
}
