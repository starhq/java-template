package com.github.starhq.template.common.captcha.util;

import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/7 12:24
 */
@UtilityClass
public class ImageUtil {

    public static void write(Image image, String imageType, OutputStream out) throws IOException {
        write(image, imageType, getImageOutputStream(out));
    }

    public static void write(Image image, String imageType, ImageOutputStream destImageStream) throws IOException {
        ImageIO.write(toBufferedImage(image), imageType, destImageStream);
    }

    public static ImageOutputStream getImageOutputStream(OutputStream out) throws IOException {
        return ImageIO.createImageOutputStream(out);
    }

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        return copyImage(img, BufferedImage.TYPE_INT_RGB);
    }

    public static BufferedImage copyImage(Image img, int imageType) {
        final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), imageType);
        final Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }

    public static Color randomColor() {
        return new Color(RandomUtil.randomInt(255), RandomUtil.randomInt(255), RandomUtil.randomInt(255));
    }

    public static Graphics2D createGraphics(BufferedImage image, Color color) {
        final Graphics2D g = image.createGraphics();
        // 填充背景
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        return g;
    }


}
