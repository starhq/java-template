package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TypeConvertUtils {
    /**
     * 将 Serializable 安全地转换为 Long
     */
    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        // 尝试转换为字符串再解析，兼容 String、Integer 等类型
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_FORMAT, e);
        }
    }
}
