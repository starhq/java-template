package com.github.starhq.template.config.json;

import com.github.starhq.template.config.json.properties.SensitiveFieldProperties;
import com.github.starhq.template.config.json.serializer.SensitivePropertyFilter;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

@Configuration
public class JacksonConfiguration {

    @Bean
    JsonMapperBuilderCustomizer jacksonCustomizer(SensitivePropertyFilter sensitivePropertyFilter) {
        return builder -> {
            SimpleFilterProvider filterProvider = new SimpleFilterProvider()
                    .addFilter(SensitivePropertyFilter.FILTER_NAME, sensitivePropertyFilter)
                    .setFailOnUnknownId(false)
                    .setDefaultFilter(SimpleBeanPropertyFilter.serializeAll());

            // 把过滤器设置到 builder 上
            builder.filterProvider(filterProvider);
        };
    }

    @Bean
    SensitivePropertyFilter sensitiveFilter(SensitiveFieldProperties sensitiveFieldProperties) {
        return new SensitivePropertyFilter(sensitiveFieldProperties);
    }
}
