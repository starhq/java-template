package com.github.starhq.template.common.captcha;

import com.github.starhq.template.common.captcha.util.ImageUtil;
import com.github.starhq.template.common.captcha.util.RandomUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/8 10:42
 */
public class CircleCaptcha extends AbstractCaptcha {

    @Serial
    private static final long serialVersionUID = -2272774482943086016L;

    /**
     * 构造
     *
     * @param width  图片宽
     * @param height 图片高
     */
    public CircleCaptcha(int width, int height) {
        this(width, height, 5);
    }

    /**
     * 构造
     *
     * @param width     图片宽
     * @param height    图片高
     * @param codeCount 字符个数
     */
    public CircleCaptcha(int width, int height, int codeCount) {
        this(width, height, codeCount, 15);
    }

    /**
     * 构造
     *
     * @param width          图片宽
     * @param height         图片高
     * @param codeCount      字符个数
     * @param interfereCount 验证码干扰元素个数
     */
    public CircleCaptcha(int width, int height, int codeCount, int interfereCount) {
        super(width, height, codeCount, interfereCount);
    }


    @Override
    protected BufferedImage generateImage(String code) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = ImageUtil.createGraphics(image, background);

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 背景填充
            g.setColor(background != null ? background : Color.WHITE);
            g.fillRect(0, 0, width, height);

            // 1. 先画干扰气泡（作为背景层）
            drawInterfere(g);

            // 2. 再画字符串（作为前景层）
            drawLargeCode(g, code);

        } finally {
            g.dispose();
        }
        return image;
    }

    private void drawLargeCode(Graphics2D g, String code) {
        int len = code.length();
        int fontSize = (int) (height * 0.85);
        int paddingX = width / 12;
        int usableWidth = width - (paddingX * 2);
        int step = (len > 1) ? usableWidth / (len - 1) : 0;

        Font baseFont = getRandomFont().deriveFont(Font.BOLD, fontSize);
        g.setFont(baseFont);
        FontMetrics fm = g.getFontMetrics();
        int baseY = (height / 2) + (fm.getAscent() / 2) - 5;

        for (int i = 0; i < len; i++) {
            g.setFont(baseFont.deriveFont(fontSize * RandomUtil.randomFloat(0.9f, 1.1f)));

            // --- 核心优化 1：强制深色字符 ---
            // 限制 RGB 在 0-120 之间，确保颜色偏深
            Color charColor = new Color(
                    // RandomUtil.randomInt(120),
                    // RandomUtil.randomInt(120),
                    // RandomUtil.randomInt(120)

                    RandomUtil.randomInt(30, 140),  // 红色通道拉高一点
                    RandomUtil.randomInt(20, 100),  // 绿色通道压低
                    RandomUtil.randomInt(40, 150)   // 蓝色通道拉高一点
            );

            int charW = fm.charWidth(code.charAt(i));
            int centerX = paddingX + (i * step);
            int y = baseY + RandomUtil.randomInt(-5, 6);
            double angle = RandomUtil.randomDouble(-0.4, 0.4);

            AffineTransform old = g.getTransform();
            g.translate(centerX, y);
            g.rotate(angle);

            // --- 核心优化 2：绘制微弱阴影/描边 ---
            // 在字符左下方 1px 处画一个淡灰色阴影，增加立体感和隔离度
            g.setColor(new Color(255, 255, 255, 180)); // 半透明白色描边
            g.drawString(String.valueOf(code.charAt(i)), -charW / 2 + 1, 1);

            // 绘制主字符
            g.setColor(charColor);
            g.drawString(String.valueOf(code.charAt(i)), -charW / 2, 0);

            g.setTransform(old);
        }
    }

    private void drawInterfere(Graphics2D g) {
        for (int i = 0; i < this.interfereCount; i++) {
            int r = RandomUtil.randomInt(30, 160);
            int gVal = RandomUtil.randomInt(30, 160);
            int b = RandomUtil.randomInt(30, 160);

            // --- 核心改动：极淡的颜色 ---
            // Alpha 设为 40-70，这样气泡就像半透明的水印
            g.setColor(new Color(r, gVal, b, RandomUtil.randomInt(70, 120)));

            int circleSize = RandomUtil.randomInt(height / 3, height / 2);
            int x = RandomUtil.randomInt(width) - (circleSize / 2);
            int y = RandomUtil.randomInt(height) - (circleSize / 2);

            // 增加线条宽度，使空心圆更有干扰力但又不遮挡
            g.setStroke(new BasicStroke(RandomUtil.randomFloat(1.0f, 2.5f)));

            if (RandomUtil.randomInt(10) > 3) {
                // 70% 概率画空心圆圈，减少视觉色块堆积
                g.drawOval(x, y, circleSize, circleSize);
            } else {
                // 30% 概率画实心圆，且因为 Alpha 低，不会遮死文字
                g.fillOval(x, y, circleSize, circleSize);
            }
        }
    }
}
