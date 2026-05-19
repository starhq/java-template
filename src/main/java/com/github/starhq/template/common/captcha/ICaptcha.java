package com.github.starhq.template.common.captcha;


import com.github.starhq.template.common.captcha.util.ImageUtil;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/**
 * 验证码接口，提供验证码对象接口定义
 *
 * @author looly
 */
public interface ICaptcha extends Serializable {

    Color background = Color.WHITE;

    CaptchaResult generate();

    /**
     * Write image to output stream
     */
    default void write(CaptchaResult result, OutputStream out) throws IOException {
        ImageUtil.write(result.image(), "png", out);
    }
}
