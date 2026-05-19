package com.github.starhq.template.common.enums;

import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.starhq.template.common.exception.BadRequestException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态枚举
 *
 * @author starhq
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus implements BaseEnum<UserStatus, String> {
    /**
     * 正常
     */
    ACTIVE("active"),
    /**
     * 停用
     */
    INACTIVE("inactive"),
    /**
     * 禁用
     */
    BANNED("banned");

    private final String value;

    private static final Map<String, UserStatus> VALUE_MAP = Map.of(ACTIVE.value, ACTIVE, INACTIVE.value, INACTIVE,
            BANNED.value, BANNED);

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserStatus fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
        }
        UserStatus status = VALUE_MAP.get(value.toLowerCase());
        if (status != null) {
            return status;
        }
        throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
    }
}
