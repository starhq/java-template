package com.github.starhq.template.common.captcha;

import com.github.starhq.template.common.util.ImageUtil;
import com.github.starhq.template.common.util.RandomUtil;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.Serial;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/8 11:31
 */
public class LineCaptcha extends AbstractCaptcha {
    @Serial
    private static final long serialVersionUID = -2967078300965114195L;

    /**
     * 构造，默认5位验证码，150条干扰线
     *
     * @param width  图片宽
     * @param height 图片高
     */
    public LineCaptcha(int width, int height) {
        this(width, height, 5, 150);
    }

    /**
     * 构造
     *
     * @param width     图片宽
     * @param height    图片高
     * @param codeCount 字符个数
     * @param lineCount 干扰线条数
     */
    public LineCaptcha(int width, int height, int codeCount, int lineCount) {
        super(width, height, codeCount, lineCount);
    }
    // -------------------------------------------------------------------- Constructor end

    @Override
    public BufferedImage generateImage(String code) {
        // 1. 初始化画布，使用抗锯齿
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 2. 填充背景（建议浅色或白色，否则干扰线和文字看不清）
            g.setColor(background != null ? background : Color.WHITE);
            g.fillRect(0, 0, width, height);

            // 3. 绘制干扰线（放在文字下方，避免完全遮挡文字导致识别困难）
            drawInterfere(g);

            // 4. 绘制文字（增强版大字号绘制）
            drawLargeCode(g, code);

        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * 绘制大字号、带旋转和起伏感的文字
     */
    private void drawLargeCode(Graphics2D g, String code) {
        int len = code.length();

        // 计算安全字号：取高度的 85%，并根据字符数微调宽度限制
        int fontSize = (int) (height * 0.85);
        int maxWidthFontSize = (width / len);
        // 最终字号取两者平衡，既要高也要能塞下
        fontSize = Math.min(fontSize, (int)(maxWidthFontSize * 1.3));

        // 计算水平分布逻辑
        int paddingX = width / 12; // 留出左右边距，防止首尾切边
        int usableWidth = width - (paddingX * 2);
        int step = usableWidth / (len - 1 == 0 ? 1 : len - 1);

        for (int i = 0; i < len; i++) {
            // 每个字符随机字体和大小微调
            Font font = getRandomFont().deriveFont(Font.BOLD, fontSize * RandomUtil.randomFloat(0.9f, 1.05f));
            g.setFont(font);
            g.setColor(ImageUtil.randomColor());

            FontMetrics fm = g.getFontMetrics();
            char c = code.charAt(i);
            int charW = fm.charWidth(c);
            int charH = fm.getAscent();

            // 计算中心坐标
            int x = (len == 1) ? width / 2 : paddingX + (i * step);
            // 垂直中心 + 随机微调（起伏感）
            int y = (height / 2) + (charH / 2) - 5 + RandomUtil.randomInt(-5, 6);

            // 随机旋转角度（±20度左右，增加识别难度但不至于看不清）
            double angle = RandomUtil.randomDouble(-0.35, 0.35);

            AffineTransform old = g.getTransform();
            g.translate(x, y);
            g.rotate(angle);

            // 绘制字符：水平居中对齐
            g.drawString(String.valueOf(c), -charW / 2, 0);

            g.setTransform(old);
        }
    }

    /**
     * 优化后的干扰线：增加透明度，避免干扰过重
     */
    private void drawInterfere(Graphics2D g) {
        for (int i = 0; i < this.interfereCount; i++) {
            Color c = ImageUtil.randomColor();
            // 设置半透明，这样150条线才不会把验证码糊死
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));

            int xs = RandomUtil.randomInt(width);
            int ys = RandomUtil.randomInt(height);
            // 稍微加长干扰线，让它更具干扰性
            int xe = xs + RandomUtil.randomInt(-width / 4, width / 4);
            int ye = ys + RandomUtil.randomInt(-height / 4, height / 4);

            g.setStroke(new BasicStroke(1.1f));
            g.drawLine(xs, ys, xe, ye);
        }
    }
}
