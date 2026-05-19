package com.github.starhq.template.common.captcha;

import java.awt.image.BufferedImage;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/8 19:12
 */
public record CaptchaResult(String code,
                            BufferedImage image) {
}
