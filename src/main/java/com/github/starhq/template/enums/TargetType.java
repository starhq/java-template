package com.github.starhq.template.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 目标类型枚举（用于审计日志）
 *
 * @author starhq
 */
@Getter
@RequiredArgsConstructor
public enum TargetType implements BaseEnum<TargetType, String> {
    /**
     * 用户
     */
    USER("user"),
    /**
     * 角色
     */
    ROLE("role"),
    /**
     * 菜单
     */
    MENU("menu"),
    /**
     * 资源
     */
    RESOURCE("resource"),
    /**
     * 按钮
     */
    BUTTON("button"),
    /**
     * 字典类型
     */
    DICT_TYPE("dict_type"),
    /**
     * 字典数据
     */
    DICT_DATA("dict_data");

    private final String value;

    @Override
    public TargetType fromValue(String value) {
        for (TargetType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown target type: " + value);
    }
}
