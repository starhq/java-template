package com.github.starhq.template.common.captcha;


import com.github.starhq.template.common.util.RandomUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

/**
 * Abstract captcha implementation.
 * Provides captcha string generation, validation, and image rendering.
 *
 * @author looly
 */
public abstract class AbstractCaptcha implements ICaptcha {

    @Serial
    private static final long serialVersionUID = 809516698438855771L;
    /** Image width */
    protected final int width;
    /** Image height */
    protected final int height;
    /** Number of captcha characters */
    protected final int codeCount;
    /** Number of interference elements */
    protected final int interfereCount;
    /**
     * Font list for captcha rendering.
     * -- SETTER --
     * Set custom fonts
     *
     */
    protected transient List<Font> fonts;

    /**
     * Constructor.
     *
     * @param width          image width
     * @param height         image height
     * @param codeCount      number of characters
     * @param interfereCount number of interference elements
     */
    protected AbstractCaptcha(
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