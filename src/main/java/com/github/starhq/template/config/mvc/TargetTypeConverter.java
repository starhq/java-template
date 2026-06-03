package com.github.starhq.template.config.mvc;

import com.github.starhq.template.common.enums.TargetType;
import org.springframework.core.convert.converter.Converter;

/**
 * Spring MVC {@link Converter} for deserializing string parameters into {@link TargetType} enumerations.
 *
 * <p>This converter is essential for cleanly mapping frontend string values (e.g., "user", "role")
 * from request parameters directly into typed Java enums during the controller argument binding phase.
 *
 * <p><b>Error Handling:</b> Unlike some lenient converters, this delegates to {@link TargetType#fromValue(String)},
 * which enforces strict validation. If the frontend submits an unrecognized target type string,
 * this converter will trigger the underlying {@link com.github.starhq.template.common.exception.BadRequestException},
 * automatically resulting in a clean 400 Bad Request response before the controller logic is even executed.
 *
 * @author wangjian
 * @see TargetType#fromValue(String)
 */
public class TargetTypeConverter implements Converter<String, TargetType> {

    /**
     * Converts the raw string into a {@link TargetType}.
     *
     * @param source the raw string value extracted from the HTTP request
     * @return the corresponding {@link TargetType}
     * @throws com.github.starhq.template.common.exception.BadRequestException indirectly,
     *                                                                         if the source string does not match any defined target type
     */
    @Override
    public TargetType convert(String source) {
        return TargetType.fromValue(source);
    }
}