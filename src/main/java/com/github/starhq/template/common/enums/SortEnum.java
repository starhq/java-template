package com.github.starhq.template.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * Enumeration for sorting directions.
 *
 * <p>Maps to standard SQL {@code ORDER BY} clauses (e.g., {@code ORDER BY create_time ASC}).
 *
 * @author starhq
 * @see BaseEnum
 */
@Getter
@AllArgsConstructor
public enum SortEnum implements BaseEnum<SortEnum, String> {

    /**
     * Sorts results in ascending order (e.g., 1, 2, 3 or A, B, C).
     */
    ASC("asc"),

    /**
     * Sorts results in descending order (e.g., 3, 2, 1 or C, B, A).
     */
    DESC("desc");

    /**
     * The string representation mapped to the database sorting keyword.
     */
    private final String value;

    /**
     * Deserializes a string value from a JSON payload back into a {@code SortEnum}.
     *
     * <p>Annotated with {@link JsonCreator} to allow Jackson to automatically parse
     * frontend sorting parameters into this enum.
     *
     * <p>This method performs case-insensitive matching (e.g., "ASC", "Asc", or "asc"
     * will all map to {@link #ASC}) to improve API fault tolerance.
     *
     * <p><b>Design Note:</b> If the input is blank or unrecognized, it silently defaults
     * to {@link #DESC}. This prevents query failures from bad input, but if strict validation
     * is required, consider throwing a {@link com.github.starhq.template.common.exception.BadRequestException}
     * instead of returning a default.
     *
     * @param value the sorting direction string provided by the client
     * @return the corresponding {@code SortEnum}, defaulting to {@link #DESC} if invalid or blank
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static SortEnum fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return DESC;
        }
        for (SortEnum order : values()) {
            if (order.value.equals(value.toLowerCase())) {
                return order;
            }
        }
        return DESC; // Default descending
    }
}