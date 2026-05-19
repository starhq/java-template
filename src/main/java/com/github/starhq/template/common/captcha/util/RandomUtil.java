package com.github.starhq.template.common.captcha.util;


import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtil {
    /**
     * 用于随机选的字符
     */
    private static final String LETTER = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";

    /**
     * 初始化random
     */
    private final static Random SECURE_RANDOM = new SecureRandom();

    private final static ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private RandomUtil() {
    }

    /**
     * 获得定长的随机字母串
     *
     * @param length 定长
     * @return 字母串
     */
    public static String randomLetters(final int length) {
        return randomString(LETTER, length);
    }

    /**
     * 获得0到limit范围的随机数
     *
     * @param limit 最大值不包括
     * @return 随机数
     */
    public static int randomInt(final int limit) {
        return RANDOM.nextInt(limit);
    }

    public static int randomInt(int origin, int bound) {
        return RANDOM.nextInt(origin,bound);
    }

    public static double randomDouble(double origin, double bound) {
        return RANDOM.nextDouble(origin, bound);
    }

    public static float randomFloat(float origin, float bound) {
        return RANDOM.nextFloat(origin, bound);
    }

    public static boolean randomBoolean() {
        return RANDOM.nextBoolean();
    }


    /**
     * 获得一个随机字符串
     *
     * @param baseString 在这个字符串里选择
     * @param length     指定长度
     * @return 随机字符串
     */
    public static String randomString(final String baseString, final int length) {
        StringBuilder builder = new StringBuilder(length);
        int tmp = Math.max(length, 1);
        int baseLength = baseString.length();
        for (int i = 0; i < tmp; i++) {
            builder.append(baseString.charAt(SECURE_RANDOM.nextInt(baseLength)));
        }

        return builder.toString();
    }
}