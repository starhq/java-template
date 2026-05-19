package com.github.starhq.template.common.captcha;


import com.github.starhq.template.common.captcha.util.RandomUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

/**
 * 抽象验证码<br>
 * 抽象验证码实现了验证码字符串的生成、验证，验证码图片的写出<br>
 *
 * @author looly
 */
public abstract class AbstractCaptcha implements ICaptcha {

    @Serial
    private static final long serialVersionUID = 809516698438855771L;
    /**
     * 图片的宽度。
     */
    protected final int width;
    /**
     * 图片的高度。
     */
    protected final int height;
    /**
     * 验证码字符个数
     */
    protected final int codeCount;
    /**
     * 验证码干扰元素个数
     */
    protected final int interfereCount;
    /**
     * 字体
     * -- SETTER --
     * 自定义字体
     *
     */
    protected List<Font> fonts;

    /**
     * 构造
     *
     * @param width          图片宽
     * @param height         图片高
     * @param codeCount      字符个数
     * @param interfereCount 验证码干扰元素个数
     */
    public AbstractCaptcha(
            int width,
            int height,
            int codeCount,
            int interfereCount) {
        this.width = width;
        this.height = height;
        this.codeCount = codeCount;
        this.interfereCount = interfereCount;

        int fontSize = Math.max(18, height - 5);

        this.fonts = List.of(
                new Font("Arial", Font.BOLD, fontSize),
                new Font("Verdana", Font.BOLD, fontSize),
                new Font("Tahoma", Font.BOLD, fontSize),
                new Font("Courier New", Font.BOLD, fontSize),
                new Font("Times New Roman", Font.BOLD, fontSize)
        );
    }

    @Override
    public CaptchaResult generate() {
        String code = generateCode();
        BufferedImage image = generateImage(code);

        return new CaptchaResult(code, image);

    }

    /**
     * Generate captcha text
     */
    protected String generateCode() {
        return RandomUtil.randomLetters(codeCount);
    }

    /**
     * Create captcha image
     */
    protected abstract BufferedImage generateImage(String code);

    /**
     * Get random font
     */
    protected Font getRandomFont() {
        return fonts.get(RandomUtil.randomInt(fonts.size()));
    }
}