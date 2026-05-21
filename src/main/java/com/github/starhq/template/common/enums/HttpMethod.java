package com.github.starhq.template.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.starhq.template.common.exception.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Enumeration of standard HTTP methods.
 *
 * <p>This enum uses a <b>bitmask strategy</b> where each method is assigned a power of two
 * (1, 2, 4, 8...). This allows multiple methods to be combined into a single {@code Integer}
 * database column, and checked efficiently using bitwise operators.
 *
 * <p>Example: A resource permitting GET and POST can be stored as {@code 3} (1 | 2).
 *
 * @author starhq
 * @see BaseEnum
 */
@Getter
@RequiredArgsConstructor
public enum HttpMethod implements BaseEnum<HttpMethod, Integer> {

    /**
     * Represents the HTTP GET method. Mask: 1 (2^0).
     */
    GET(1),
    /**
     * Represents the HTTP POST method. Mask: 2 (2^1).
     */
    POST(2),
    /**
     * Represents the HTTP PUT method. Mask: 4 (2^2).
     */
    PUT(4),
    /**
     * Represents the HTTP DELETE method. Mask: 8 (2^3).
     */
    DELETE(8),
    /**
     * Represents the HTTP PATCH method. Mask: 16 (2^4).
     */
    PATCH(16),
    /**
     * Represents the HTTP HEAD method. Mask: 32 (2^5).
     */
    HEAD(32),
    /**
     * Represents the HTTP OPTIONS method. Mask: 64 (2^6).
     */
    OPTIONS(64);

    /**
     * The bitmask value representing this specific HTTP method.
     */
    private final Integer value;

    /**
     * Immutable lookup map for deserialization and validation.
     * Provides O(1) performance when resolving a database integer back to an enum instance.
     */
    private static final Map<Integer, HttpMethod> VALUE_MAP = Map.of(
            1, GET,
            2, POST,
            4, PUT,
            8, DELETE,
            16, PATCH,
            32, HEAD,
            64, OPTIONS
    );

    /**
     * Checks if a given combined method bitmask contains a specific HTTP method.
     *
     * <p>Uses the bitwise AND operator ({@code &}). If the result is not zero, the bit
     * representing the target method is flipped 'on' in the mask.
     *
     * <p>Example: {@code contains(3, POST)} -> {@code (3 & 2) != 0} -> returns {@code true}.
     *
     * @param methodMask the combined integer mask retrieved from the database
     * @param method     the specific HTTP method to check for
     * @return {@code true} if the mask includes the specified method, {@code false} otherwise
     */
    public static boolean contains(int methodMask, HttpMethod method) {
        if (method == null) {
            return false;
        }
        return (methodMask & method.value) != 0;
    }

    /**
     * Combines a list of HTTP methods into a single integer bitmask using the bitwise OR operator.
     *
     * <p>This is typically used before saving a resource's allowed methods to the database.
     *
     * <p>Example: Combining [GET, PUT] results in {@code 1 | 4 = 5}.
     *
     * @param methods the list of HTTP methods to combine
     * @return the resulting integer bitmask, or {@code 0} if the list is null or empty
     */
    public static int combine(List<HttpMethod> methods) {
        int mask = 0;
        if (CollectionUtils.isEmpty(methods)) {
            return mask;
        }
        for (HttpMethod method : methods) {
            mask |= method.value; // Bitwise OR assignment
        }
        return mask;
    }

    /**
     * Deserializes an integer value from a JSON payload or database column back into an {@code HttpMethod} enum.
     *
     * <p>annotated with {@link JsonCreator} to allow Jackson to automatically map frontend integer
     * values (e.g., {@code 4}) directly to this enum during request parsing.
     *
     * @param value the integer bitmask or single method value
     * @return the corresponding {@code HttpMethod} enum
     * @throws BadRequestException if the provided integer does not match any defined method
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static HttpMethod fromValue(Integer value) {
        HttpMethod method = VALUE_MAP.get(value);
        if (method != null) {
            return method;
        }
        throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
    }
}