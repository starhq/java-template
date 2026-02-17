package com.github.starhq.template.enums;

import java.util.Objects;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.github.starhq.template.exception.BusinessException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * HTTP方法枚举
 * 使用位掩码表示多个HTTP方法
 *
 * @author starhq
 */
@Getter
@RequiredArgsConstructor
public enum HttpMethod implements BaseEnum<HttpMethod, Integer> {
    GET(1),
    POST(2),
    PUT(4),
    DELETE(8),
    PATCH(16),
    HEAD(32),
    OPTIONS(64);

    @EnumValue
    private final Integer value;

    /**
     * 检查方法位掩码是否包含指定的HTTP方法
     *
     * @param methodMask 方法位掩码
     * @param method     要检查的HTTP方法
     * @return 是否包含
     */
    public static boolean contains(int methodMask, HttpMethod method) {
        return (methodMask & method.value) != 0;
    }

    /**
     * 将多个HTTP方法组合成位掩码
     *
     * @param methods HTTP方法数组
     * @return 位掩码
     */
    public static int combine(HttpMethod... methods) {
        int mask = 0;
        for (HttpMethod method : methods) {
            mask |= method.value;
        }
        return mask;
    }

    @Override
    public HttpMethod fromValue(Integer value) {
        for (HttpMethod method : values()) {
            if (Objects.equals(method.getValue(), value)) {
                return method;
            }
        }
        throw new BusinessException("validation.enum.invalid", new Object[] { value });
    }
}
