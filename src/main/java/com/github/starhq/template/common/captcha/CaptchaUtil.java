package com.github.starhq.template.common.captcha;

import lombok.experimental.UtilityClass;

/**
 * Factory utility for creating different types of captchas.
 *
 * @author wangjian
 */
@UtilityClass
public class CaptchaUtil {

    /**
     * Creates a line-interference captcha with default 5 characters and 150 interference lines.
     *
     * @param width  image width
     * @param height image height
     * @return {@link LineCaptcha}
     */
    public LineCaptcha createLineCaptcha(int width, int height) {
        return new LineCaptcha(width, height);
    }

    /**
     * Creates a line-interference captcha.
     *
     * @param width     image width
     * @param height    image height
     * @param codeCount number of characters
     * @param lineCount number of interference lines
     * @return {@link LineCaptcha}
     */
    public LineCaptcha createLineCaptcha(int width, int height, int codeCount, int lineCount) {
        return new LineCaptcha(width, height, codeCount, lineCount);
    }

    /**
     * Creates a circle-interference captcha with default 5 characters and 15 interference circles.
     *
     * @param width  image width
     * @param height image height
     * @return {@link CircleCaptcha}
     * @since 3.2.3
     */
    public CircleCaptcha createCircleCaptcha(int width, int height) {
        return new CircleCaptcha(width, height);
    }

    /**
     * Creates a circle-interference captcha.
     *
     * @param width       image width
     * @param height      image height
     * @param codeCount   number of characters
     * @param circleCount number of interference circles
     * @return {@link CircleCaptcha}
     * @since 3.2.3
     */
    public CircleCaptcha createCircleCaptcha(int width, int height, int codeCount, int circleCount) {
        return new CircleCaptcha(width, height, codeCount, circleCount);
    }

    /**
     * Creates a shear-interference captcha with default 5 characters.
     *
     * @param width  image width
     * @param height image height
     * @return {@link ShearCaptcha}
     * @since 3.2.3
     */
    public ShearCaptcha createShearCaptcha(int width, int height) {
        return new ShearCaptcha(width, height);
    }

    /**
     * Creates a shear-interference captcha.
     *
     * @param width     image width
     * @param height    image height
     * @param codeCount number of characters
     * @param thickness interference line thickness
     * @return {@link ShearCaptcha}
     * @since 3.3.0
     */
    public ShearCaptcha createShearCaptcha(int width, int height, int codeCount, int thickness) {
        return new ShearCaptcha(width, height, codeCount, thickness);
    }
}
