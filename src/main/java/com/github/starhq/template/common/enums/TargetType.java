package com.github.starhq.template.common.enums;

import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.starhq.template.common.exception.BadRequestException;

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

    private static final Map<String, TargetType> VALUE_MAP = Map.of(USER.value, USER, ROLE.value, ROLE, MENU.value,
            MENU, RESOURCE.value, RESOURCE, BUTTON.value, BUTTON, DICT_TYPE.value, DICT_TYPE, DICT_DATA.value,
            DICT_DATA);

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TargetType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        TargetType targetType = VALUE_MAP.get(value.toLowerCase());
        if (targetType != null) {
            return targetType;
        }
        throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
    }
}
