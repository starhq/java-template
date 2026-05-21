package com.github.starhq.template.config.security.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for generating graphical CAPTCHAs.
 *
 * <p>Binds to the {@code star.captcha} namespace in application YAML files.
 * These parameters allow fine-tuning of the CAPTCHA's visual complexity to balance
 * security (difficulty for OCR bots) against usability (readability for real users).
 *
 * <p><b>YAML Binding Example:</b>
 * <pre>
 * star:
 *   captcha:
 *     type: circle
 *     width: 120
 *     height: 40
 *     codeCount: 4
 *     interfereCount: 150
 * </pre>
 *
 * @author wangjian
 */
@Data
@Component
@ConfigurationProperties(prefix = "star.captcha")
public class CaptchaProperties {

    /**
     * The visual rendering style of the CAPTCHA image.
     * <p>Common options: "line" (线条干扰), "circle" (圆圈干扰), "shear" (扭曲变形).
     * Different algorithms have different effectiveness against OCR recognition algorithms.
     */
    private String type = "line";

    /**
     * The width of the generated CAPTCHA image in pixels.
     * <p>Should be wide enough to clearly display the required number of characters without overlap.
     */
    private int width = 120;

    /**
     * The height of the generated CAPTCHA image in pixels.
     * <p>Should provide enough vertical space for characters and their descenders (like 'g', 'y', 'p').
     */
    private int height = 40;

    /**
     * The exact number of random characters to render in the image.
     * <p>Typically 4 or 5. Increasing this makes it significantly harder for bots to guess but also
     * increases the cognitive load on human users.
     */
    private int codeCount = 5;

    /**
     * The number of random interference lines/shapes to draw over the text.
     * <p>Higher values make the CAPTCHA more secure against automated parsing, but values that are
     * too high will obscure the actual text, causing extreme frustration for legitimate users.
     */
    private int interfereCount = 170;
}