package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import lombok.experimental.UtilityClass;

/**
 * Utility class for safe and flexible type conversions.
 *
 * <p>Provides centralized methods to convert values from generic {@link Object} types
 * (often extracted from Maps, JSON nodes, or reflection) into strict target types,
 * with built-in exception translation to prevent raw system exceptions from leaking.
 *
 * @author starhq
 */
@UtilityClass
public class TypeConvertUtils {

    /**
     * Safely converts an arbitrary object to a {@link Long}.
     *
     * <p>This method provides flexible type coercion. It handles direct {@link Long} casting efficiently,
     * but also acts as a fallback parser for other common types (e.g., {@link String} "123",
     * {@link Integer} 456) by leveraging the object's {@code toString()} method.
     *
     * <p><b>Exception Handling:</b> If the object cannot be parsed into a valid numeric format,
     * it does not throw the raw {@link NumberFormatException}. Instead, it catches it and
     * translates it into a standardized {@link BusinessException} with {@link ErrorCode#PARAM_FORMAT}.
     * This ensures the global exception handler returns a clean 400 Bad Request to the client.
     *
     * @param value the object to convert (can be Long, String, Integer, etc.)
     * @return the parsed {@link Long} value, or {@code null} if the input object is {@code null}
     * @throws BusinessException if the value is not null but cannot be parsed as a valid Long
     */
    public static Long toLong(Object value) {
        if (value == null) {
            return null;
        }

        // Fast path for the most common expected type
        if (value instanceof Long longValue) {
            return longValue;
        }

        // Fallback path: convert to string and parse (handles String, Integer, BigDecimal, etc.)
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            // Translate raw system exception into a friendly API error contract
            throw new BusinessException(ErrorCode.PARAM_FORMAT, e);
        }
    }
}
