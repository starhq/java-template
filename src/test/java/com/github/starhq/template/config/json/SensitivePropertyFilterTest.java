package com.github.starhq.template.config.json;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import com.github.starhq.template.config.json.serializer.SensitivePropertyFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.PropertyWriter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensitivePropertyFilterTest {

    @Mock
    private SensitiveFieldProperties sensitiveProperties;
    @Mock
    private JsonGenerator jsonGenerator;
    @Mock
    private PropertyWriter propertyWriter;
    @Mock
    private SerializationContext serializationContext;

    @Test
    void serializeAsProperty_WhenSensitive_ShouldWriteMaskValue() throws Exception {
        // Given: 模拟当前序列化的字段是 "password"
        when(propertyWriter.getName()).thenReturn("password");
        // 模拟属性类判断它是敏感的
        when(sensitiveProperties.isSensitive("password")).thenReturn(true);
        when(sensitiveProperties.getMaskValue()).thenReturn("*****");

        SensitivePropertyFilter filter = new SensitivePropertyFilter(sensitiveProperties);
        Object pojo = new Object();

        // When: 执行过滤序列化
        filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, propertyWriter);

        // Then: 验证调用了写脱敏值的方法，且参数正确
        verify(jsonGenerator).writeStringProperty("password", "*****");
        // 验证绝对没有调用原始的序列化方法
        verify(propertyWriter, never()).serializeAsProperty(any(), any(), any());
    }

    @Test
    void serializeAsProperty_WhenNotSensitive_ShouldCallDefaultSerializer() throws Exception {
        // Given: 模拟当前序列化的字段是 "username"
        when(propertyWriter.getName()).thenReturn("username");
        // 模拟属性类判断它不是敏感的
        when(sensitiveProperties.isSensitive("username")).thenReturn(false);

        SensitivePropertyFilter filter = new SensitivePropertyFilter(sensitiveProperties);
        Object pojo = new Object();

        // When
        filter.serializeAsProperty(pojo, jsonGenerator, serializationContext, propertyWriter);

        // Then: 验证没有调用脱敏方法
        verify(jsonGenerator, never()).writeStringProperty(anyString(), anyString());
        // 验证调用了原始的默认序列化方法放行
        verify(propertyWriter).serializeAsProperty(pojo, jsonGenerator, serializationContext);
    }
}