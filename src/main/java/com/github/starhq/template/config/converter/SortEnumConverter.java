package com.github.starhq.template.config.converter;

import com.github.starhq.template.common.enums.SortEnum;
import org.springframework.core.convert.converter.Converter;

/**
 * Spring MVC {@link Converter} for deserializing raw HTTP request parameters into {@link SortEnum} enumerations.
 *
 * <p><b>Why is this needed?</b> By default, Spring can only convert a String to an Enum if the string
 * exactly matches the Enum's {@code .name()} (e.g., "ASC"). However, our API might receive values like
 * "asc", "Asc", or even empty strings. This converter intercepts the request parameter binding process
 * and delegates to {@link SortEnum#fromValue(String)}, which contains custom fallback and case-insensitive logic.
 *
 * <p>This converter is typically registered with the {@link org.springframework.core.convert.ConversionService}
 * or discovered automatically via {@link org.springframework.web.bind.annotation.InitBinder} to handle
 * parameters like {@code @RequestParam SortEnum sort}.
 *
 * @author wangjian
 * @see SortEnum#fromValue(String)
 */
public class SortEnumConverter implements Converter<String, SortEnum> {

    /**
     * Converts the incoming string source into a {@link SortEnum}.
     *
     * @param source the raw string value extracted from the HTTP request (e.g., from URL query parameters)
     * @return the corresponding {@link SortEnum}
     * @throws IllegalArgumentException indirectly, if the source is deemed invalid by {@link SortEnum#fromValue}
     */
    @Override
    public SortEnum convert(String source) {
        return SortEnum.fromValue(source);
    }
}