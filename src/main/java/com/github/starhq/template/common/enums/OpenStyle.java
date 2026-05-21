package com.github.starhq.template.common.enums;

import java.util.Objects;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration defining how a menu item should be opened or rendered.
 *
 * <p>Typically used in frontend layouts to determine routing behavior:
 * <ul>
 *   <li>{@link #INTERNAL}: Opens the target URL within the system's internal iframe or router-view.</li>
 *   <li>{@link #EXTERNAL}: Opens the target URL in a new browser tab or external window.</li>
 * </ul>
 *
 * @author starhq
 * @see BaseEnum
 */
@Getter
@RequiredArgsConstructor
public enum OpenStyle implements BaseEnum<OpenStyle, Integer> {

    /**
     * Indicates the menu link should be handled by the internal application routing.
     */
    INTERNAL(0),

    /**
     * Indicates the menu link should be treated as an absolute external URL.
     */
    EXTERNAL(1);

    /**
     * The integer value persisted in the database.
     * <p>Annotated with {@link EnumValue} for MyBatis-Plus to map DB integer columns to this enum.
     */
    @EnumValue
    private final Integer value;

    /**
     * Deserializes an integer value from a JSON payload back into an {@code OpenStyle} enum.
     *
     * <p>Annotated with {@link JsonCreator} to allow Jackson to automatically map frontend integer
     * values (e.g., {@code 0} or {@code 1}) to this enum during request parsing.
     *
     * <p><b>Design Note:</b> Currently defaults to {@link #INTERNAL} if an unrecognized value is passed.
     * Consider whether throwing a {@link com.github.starhq.template.common.exception.BadRequestException}
     * would be safer to prevent silently ignoring invalid frontend data.
     *
     * @param value the integer value provided by the client
     * @return the corresponding {@code OpenStyle}, defaulting to {@link #INTERNAL} if no match is found
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static OpenStyle fromValue(Integer value) {
        for (OpenStyle style : values()) {
            if (Objects.equals(style.value, value)) {
                return style;
            }
        }
        return INTERNAL;
    }
}