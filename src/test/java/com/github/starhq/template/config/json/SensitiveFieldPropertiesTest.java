package com.github.starhq.template.config.json;


import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveFieldPropertiesTest {

    private SensitiveFieldProperties properties;

    @BeforeEach
    void setUp() {
        // 直接 new，不走 Spring 容器
        properties = new SensitiveFieldProperties();
    }

    @Test
    void isSensitive_MatchExactField_ReturnTrue() {
        assertTrue(properties.isSensitive("password"));
        assertTrue(properties.isSensitive("accessToken"));
    }

    @Test
    void isSensitive_MatchCaseInsensitive_ReturnTrue() {
        // 验证忽略大小写逻辑是否生效
        assertTrue(properties.isSensitive("PASSWORD"));
        assertTrue(properties.isSensitive("PASSWORD"));
        assertTrue(properties.isSensitive("AcCeSsToKeN"));
    }

    @Test
    void isSensitive_NoMatchField_ReturnFalse() {
        assertFalse(properties.isSensitive("username"));
        assertFalse(properties.isSensitive("id"));
        assertFalse(properties.isSensitive("email"));
    }

    @Test
    void isSensitive_NullInput_ReturnFalse() {
        // 验证防御性编程
        assertFalse(properties.isSensitive(null));
    }

    @Test
    void isSensitive_EmptyFieldsSet_ReturnFalse() {
        // 模拟配置文件里什么都没配
        properties.setFields(java.util.Set.of());
        assertFalse(properties.isSensitive("password"));
    }
}
