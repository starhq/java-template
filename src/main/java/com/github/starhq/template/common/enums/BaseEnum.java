package com.github.starhq.template.common.enums;


import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Base interface for all persistent enums.
 *
 * <p>Implementations of this interface define the exact data type (e.g., String, Integer)
 * stored in the database, providing a unified contract for enum persistence and serialization.
 *
 * <p>When integrated with MyBatis-Plus type handlers, this seamlessly automates the conversion
 * between database values and Java enum objects. The {@link JsonValue} annotation on the getter
 * ensures that Jackson serializes the persisted value (e.g., {@code 1} or {@code "ACTIVE"})
 * directly to the frontend, rather than the Java enum's name (e.g., {@code ACTIVE_STATUS}).
 *
 * @param <E> the enum type itself, used for self-referencing in generic bounds
 * @param <T> the concrete data type stored in the database column (e.g., String, Integer, Long)
 */
public interface BaseEnum<E extends Enum<E> & BaseEnum<E, T>, T> {

    /**
     * Retrieves the actual value to be persisted in the database.
     *
     * <p>Annotated with {@link JsonValue} so that Jackson uses this value during API
     * serialization, ensuring the JSON response matches the underlying database schema exactly.
     *
     * @return the database-compatible value of the enum
     */
    @JsonValue
    T getValue();
}