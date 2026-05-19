package com.github.starhq.template.common.captcha;

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
 * @date 2026/5/8 11:38
 */
public class ShearCaptcha extends AbstractCaptcha {

    @Serial
    private static final long serialVersionUID = -6553683708300918428L;

    public ShearCaptcha(
            int width,
            int height,
            int codeCount,
            int interfereCount) {
        super(width, height, codeCount, interfereCount);
    }

    public ShearCaptcha(int width, int height) {
        this(width, height, 5, 2);
    }

    @Override
    protected BufferedImage generateImage(String code) {
        // 增加内边距缓冲区，防止扭曲和旋转导致切边
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(background);
            g.fillRect(0, 0, width, height);

            // 1. 降低扭曲强度，先执行扭曲或在绘制中直接变换
            // 注意：先画字再扭曲（shear）容易导致字模糊或出界
            // 我们改用更受控的扭曲

            // 2. 绘制字符
            drawCode(g, code);

            // 3. 绘制干扰线（放在字符后，增加干扰度但控制透明度）
            drawNoise(g);

        } finally {
            g.dispose();
        }

        // 最后可以执行一个轻微的全局扭曲（可选）
        return image;
    }

    private void drawCode(Graphics2D g, String code) {
        int len = code.length();

        // 1. 更加科学的最大字号计算
        // 预留 15% 的垂直空间给旋转和位移，防止触顶或踩底
        int finalFontSize = (int) (height * 0.85);

        // 2. 宽度自适应调整
        // 确保字符在旋转后不会超出左右边界，计算每个字符可分配的最大宽度
        int horizontalSpacePerChar = width / len;
        // 字号不能超过分配宽度的 1.2 倍（防止字符过宽重叠太严重）
        finalFontSize = Math.min(finalFontSize, (int)(horizontalSpacePerChar * 1.2));

        Font baseFont = getRandomFont().deriveFont(Font.BOLD, (float) finalFontSize);
        g.setFont(baseFont);
        FontMetrics metrics = g.getFontMetrics();

        // 3. 计算起始位置，增加左右内边距 (Padding)
        int paddingX = width / 10;
        int usableWidth = width - (paddingX * 2);
        int step = usableWidth / (len - 1 == 0 ? 1 : len - 1); // 间距

        // 4. 垂直居中修正 (使用 Ascent 和 Descent 的平衡点)
        // 字符的物理中心线通常在基线上方约 1/3 处
        int baseY = (height / 2) + (metrics.getAscent() / 2) - 5;

        for (int i = 0; i < len; i++) {
            // 每个字符使用略微不同的字号，增加识别难度（±10%）
            float dynamicSize = finalFontSize * RandomUtil.randomFloat(0.9f, 1.1f);
            g.setFont(baseFont.deriveFont(dynamicSize));
            g.setColor(randomColor());

            char c = code.charAt(i);
            int charW = g.getFontMetrics().charWidth(c);

            // 计算该字符的中心 X 坐标
            int centerX = (len == 1) ? width / 2 : paddingX + (i * step);

            // 5. 限制旋转角度（弧度制）
            // 角度太大不仅容易出界，用户也看不懂
            double angle = RandomUtil.randomDouble(-0.4, 0.4); // 约 ±23度

            // 6. 随机小范围偏移，增加“起伏感”
            int randY = baseY + RandomUtil.randomInt(-3, 4);

            // 7. 执行变换
            AffineTransform old = g.getTransform();

            // 移到目标点
            g.translate(centerX, randY);
            // 旋转
            g.rotate(angle);
            // 绘制：将字符中心对齐到 translate 的坐标点
            // -charW / 2 确保水平居中，0 是基线对齐
            g.drawString(String.valueOf(c), -charW / 2, 0);

            g.setTransform(old);
        }
    }

    private void drawNoise(Graphics2D g) {
        // 将干扰线设为半透明，或者颜色稍微淡一点，避免遮挡字符
        for (int i = 0; i < interfereCount; i++) {
            Color c = randomColor();
            // 增加透明度控制 (100-150)
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 120));

            int x1 = RandomUtil.randomInt(width);
            int y1 = RandomUtil.randomInt(height);
            int x2 = RandomUtil.randomInt(width);
            int y2 = RandomUtil.randomInt(height);

            // 使用抗锯齿的细线
            g.setStroke(new BasicStroke(1.2f));
            g.drawLine(x1, y1, x2, y2);
        }
    }

    private Color randomColor() {
        return new Color(
                RandomUtil.randomInt(30, 200),
                RandomUtil.randomInt(30, 200),
                RandomUtil.randomInt(30, 200));
    }
}
