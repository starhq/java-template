package com.github.starhq.template.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/8 13:19
 */
public interface CaptchaService {

    void generateCode(String uuid, HttpServletResponse response);

    void verify(String uuid, String captcha);
}
