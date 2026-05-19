package com.github.starhq.template.common.constant;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: some constants for captcha
 * @date 2026/5/7 13:50
 */
public interface CaptchaConstants {

    /**
     * 图片的宽度。
     */
    int width = 120;
    /**
     * 图片的高度。
     */
    int height = 40;
    /**
     * 验证码字符个数
     */
    int codeCount = 4;
    /**
     * 验证码干扰元素个数
     */
    int interfereCount = 20;
}
