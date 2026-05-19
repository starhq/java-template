package com.github.starhq.template.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.starhq.template.common.exception.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

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

    private final Integer value;

    private static final Map<Integer, HttpMethod> VALUE_MAP = Map.of(1, GET, 2, POST, 4, PUT, 8, DELETE
            , 16, PATCH, 32, HEAD, 64, OPTIONS);


    /**
     * 检查方法位掩码是否包含指定的 HTTP 方法
     *
     * @param methodMask 方法位掩码
     * @param method     要检查的 HTTP 方法
     * @return 是否包含
     */
    public static boolean contains(int methodMask, HttpMethod method) {
        if (method == null) {
            return false;
        }
        return (methodMask & method.value) != 0;
    }

    /**
     * 将多个 HTTP 方法组合成位掩码
     *
     * @param methods HTTP 方法数组
     * @return 位掩码
     */
    public static int combine(List<HttpMethod> methods) {
        int mask = 0;
        if (CollectionUtils.isEmpty(methods)) {
            return mask;
        }
        for (HttpMethod method : methods) {
            mask |= method.value;
        }
        return mask;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HttpMethod fromValue(Integer value) {
        HttpMethod method = VALUE_MAP.get(value);
        if (method != null) {
            return method;
        }
        throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
    }
}
