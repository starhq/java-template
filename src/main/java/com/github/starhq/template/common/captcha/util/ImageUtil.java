package com.github.starhq.template.common.captcha.util;

import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Image utility class for captcha generation.
 *
 * @author wangjian
 */
@UtilityClass
public class ImageUtil {

    public void write(Image image, String imageType, OutputStream out) throws IOException {
        write(image, imageType, getImageOutputStream(out));
    }

    public void write(Image image, String imageType, ImageOutputStream destImageStream) throws IOException {
        ImageIO.write(toBufferedImage(image), imageType, destImageStream);
    }

    public ImageOutputStream getImageOutputStream(OutputStream out) throws IOException {
        return ImageIO.createImageOutputStream(out);
    }

    public BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage bufferedImage) {
            return bufferedImage;
        }

        return copyImage(img, BufferedImage.TYPE_INT_RGB);
    }

    public BufferedImage copyImage(Image img, int imageType) {
        final BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), imageType);
        final Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        return bimage;
    }

    public Color randomColor() {
        return new Color(RandomUtil.randomInt(255), RandomUtil.randomInt(255), RandomUtil.randomInt(255));
    }

    public Graphics2D createGraphics(BufferedImage image, Color color) {
        final Graphics2D g = image.createGraphics();
        // Fill background
        g.setColor(color);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        return g;
    }


}
