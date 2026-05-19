package com.github.starhq.template.common.enums;

import java.util.Objects;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 菜单打开方式枚举
 *
 * @author starhq
 */
@Getter
@RequiredArgsConstructor
public enum OpenStyle implements BaseEnum<OpenStyle, Integer> {
    /**
     * 内部打开
     */
    INTERNAL(0),
    /**
     * 外部打开
     */
    EXTERNAL(1);

    @EnumValue
    private final Integer value;

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OpenStyle fromValue(Integer value) {
        for (OpenStyle style : values()) {
            if (Objects.equals(style.value, value)) {
                return style;
            }
        }
        return INTERNAL;
    }
}
