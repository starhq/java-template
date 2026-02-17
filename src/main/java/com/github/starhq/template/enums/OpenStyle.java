package com.github.starhq.template.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 菜单打开方式枚举
 *
 * @author starhq
 */
@Getter
@RequiredArgsConstructor
public enum OpenStyle implements BaseEnum<OpenStyle,Integer> {
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

    @Override
    public OpenStyle fromValue(Integer value) {
        for (OpenStyle style : values()) {
            if (style.value == value) {
                return style;
            }
        }
        throw new IllegalArgumentException("Unknown open style: " + value);
    }
}
