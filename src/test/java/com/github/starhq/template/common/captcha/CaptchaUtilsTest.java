package com.github.starhq.template.common.captcha;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/18 21:30
 */
class CaptchaUtilsTest {

    @TempDir
    Path tempDirPath;

    @Test
    void testLineCaptcha_WithWidthAndHeight_Success() {
        ICaptcha captcha = CaptchaUtil.createLineCaptcha(100, 10);
        CaptchaResult result = captcha.generate();
        assertNotNull(result.code());
        File imageFile = tempDirPath.resolve("captcha.png").toFile();
        try (OutputStream testImage = new FileOutputStream(imageFile)) {
            captcha.write(result, testImage);
            assertTrue(imageFile.exists());
        } catch (IOException _) {
            assertFalse(imageFile.exists());
        }
    }

    @Test
    void testLineCaptcha_WithFullParam_Success() {
        ICaptcha captcha = CaptchaUtil.createLineCaptcha(100, 10, 6, 20);
        CaptchaResult result = captcha.generate();
        assertEquals(6, result.code().length());
    }

    @Test
    void testCircleCaptcha_WithWidthAndHeight_Success() {
        ICaptcha captcha = CaptchaUtil.createCircleCaptcha(100, 10);
        CaptchaResult result = captcha.generate();
        assertNotNull(result.code());
        File imageFile = tempDirPath.resolve("captcha.png").toFile();
        try (OutputStream testImage = new FileOutputStream(imageFile)) {
            captcha.write(result, testImage);
            assertTrue(imageFile.exists());
        } catch (IOException _) {
            assertFalse(imageFile.exists());
        }
    }

    @Test
    void testCircleCaptcha_WithFullParam_Success() {
        ICaptcha captcha = CaptchaUtil.createCircleCaptcha(100, 10, 6, 20);
        CaptchaResult result = captcha.generate();
        assertEquals(6, result.code().length());
    }

    @Test
    void testShearCaptcha_WithWidthAndHeight_Success() {
        ICaptcha captcha = CaptchaUtil.createShearCaptcha(100, 10);
        CaptchaResult result = captcha.generate();
        assertNotNull(result.code());
        File imageFile = tempDirPath.resolve("captcha.png").toFile();
        try (OutputStream testImage = new FileOutputStream(imageFile)) {
            captcha.write(result, testImage);
            assertTrue(imageFile.exists());
        } catch (IOException _) {
            assertFalse(imageFile.exists());
        }
    }

    @Test
    void testShearCaptcha_WithFullParam_Success() {
        ICaptcha captcha = CaptchaUtil.createShearCaptcha(100, 10, 6, 20);
        CaptchaResult result = captcha.generate();
        assertEquals(6, result.code().length());
    }
}
