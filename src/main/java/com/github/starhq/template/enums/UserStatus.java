package com.github.starhq.template.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.github.starhq.template.exception.BusinessException;

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

    @EnumValue
    private final String value;

    @Override
    public UserStatus fromValue(String value) {
        for (UserStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new BusinessException("validation.enum.invalid", new Object[] { value });
    }
}
