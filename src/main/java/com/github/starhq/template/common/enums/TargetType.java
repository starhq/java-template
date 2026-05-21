package com.github.starhq.template.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.starhq.template.common.exception.BadRequestException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Enumeration of target entity types for audit logging.
 *
 * <p>Used to categorize audit log records, indicating which type of system entity
 * (e.g., User, Role, Menu) was affected by the audited operation.
 *
 * @author starhq
 * @see BaseEnum
 */
@Getter
@RequiredArgsConstructor
public enum TargetType implements BaseEnum<TargetType, String> {

    /**
     * Represents a user entity operation.
     */
    USER("user"),

    /**
     * Represents a role entity operation.
     */
    ROLE("role"),

    /**
     * Represents a menu entity operation.
     */
    MENU("menu"),

    /**
     * Represents an API resource entity operation.
     */
    RESOURCE("resource"),

    /**
     * Represents a UI button permission entity operation.
     */
    BUTTON("button"),

    /**
     * Represents a dictionary type entity operation.
     */
    DICT_TYPE("dict_type"),

    /**
     * Represents a dictionary data entry entity operation.
     */
    DICT_DATA("dict_data");

    /**
     * The string value stored in the audit log database table.
     */
    private final String value;

    /**
     * Immutable lookup map for O(1) deserialization.
     */
    private static final Map<String, TargetType> VALUE_MAP = Map.of(
            USER.value, USER,
            ROLE.value, ROLE,
            MENU.value, MENU,
            RESOURCE.value, RESOURCE,
            BUTTON.value, BUTTON,
            DICT_TYPE.value, DICT_TYPE,
            DICT_DATA.value, DICT_DATA
    );

    /**
     * Deserializes a string value from a JSON payload back into a {@code TargetType} enum.
     *
     * <p>Annotated with {@link JsonCreator} to allow Jackson to automatically parse
     * frontend string values (e.g., "user", "role") into this enum.
     *
     * <p><b>⚠️ Critical Bug Warning:</b> The current implementation applies {@code toLowerCase()}
     * to the input value before looking it up in the map. However, the {@code VALUE_MAP} is
     * initialized with <i>lowercase</i> keys (e.g., "user"), but if the enum {@code value} field
     * were ever changed to uppercase (e.g., "USER"), this lookup would fail and throw an exception.
     * If case-insensitive matching is intended, it is safer to normalize the keys in the map itself
     * or the enum values, rather than relying on runtime string manipulation of the input.
     *
     * @param value the target type string provided by the client
     * @return the corresponding {@code TargetType}, or {@code null} if the input string is blank
     * @throws BadRequestException if the provided string does not match any known target type
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TargetType fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        TargetType targetType = VALUE_MAP.get(value.toLowerCase());
        if (targetType != null) {
            return targetType;
        }
        throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
    }
}