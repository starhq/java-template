package com.github.starhq.template.config.json.serializer;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.PropertyWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;

/**
 * Jackson 敏感字段过滤器
 * <p>
 * 用法：在需要脱敏的类上添加 @JsonFilter("sensitiveFilter")
 */
@RequiredArgsConstructor
public class SensitivePropertyFilter extends SimpleBeanPropertyFilter {

    private final SensitiveFieldProperties sensitiveProperties;

    /**
     * 过滤器名称（与 @JsonFilter 注解中的值一致）
     */
    public static final String FILTER_NAME = "sensitiveFilter";

    @Override
    public void serializeAsProperty(Object pojo, JsonGenerator g, SerializationContext provider, PropertyWriter writer)
            throws Exception {
        String fieldName = writer.getName();

        if (sensitiveProperties.isSensitive(fieldName)) {
            g.writeStringProperty(fieldName, sensitiveProperties.getMaskValue());
        } else {
            writer.serializeAsProperty(pojo, g, provider);
        }
    }
}
