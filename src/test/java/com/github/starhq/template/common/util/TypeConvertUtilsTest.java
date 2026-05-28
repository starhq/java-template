package com.github.starhq.template.common.util;

import com.github.starhq.template.common.exception.CustomException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/18 11:19
 */
class TypeConvertUtilsTest {

    @Test
    void convert_String_Success() {
        String value = "123";
        var aLong = TypeConvertUtils.toLong(value);
        assertEquals(123, aLong);
    }

    @Test
    void convert_Long_Success() {
        Long value = 123L;
        var aLong = TypeConvertUtils.toLong(value);
        assertEquals(123, aLong);
    }

    @Test
    void convert_Null_Success() {
        Object value = null;
        var aLong = TypeConvertUtils.toLong(value);
        assertNull(aLong);
    }

    @Test
    void convert_Failure() {
        String value = "adbc";
        assertThrows(CustomException.class, () -> TypeConvertUtils.toLong(value));
    }
}
