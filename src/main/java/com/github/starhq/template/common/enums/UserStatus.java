package com.github.starhq.template.common.enums;

import java.util.Map;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.github.starhq.template.common.exception.BadRequestException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration representing the current status of a user account.
 *
 * @author starhq
 * @see BaseEnum
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus implements BaseEnum<UserStatus, String> {

    /**
     * The user account is fully functional and permitted to access the system.
     */
    ACTIVE("active"),

    /**
     * The user account is temporarily suspended or deactivated by an administrator.
     */
    INACTIVE("inactive"),

    /**
     * The user account is permanently or temporarily blocked due to policy violations.
     */
    BANNED("banned");

    /**
     * The string value persisted in the database.
     */
    private final String value;

    /**
     * Immutable lookup map for O(1) deserialization.
     */
    private static final Map<String, UserStatus> VALUE_MAP = Map.of(
            ACTIVE.value, ACTIVE,
            INACTIVE.value, INACTIVE,
            BANNED.value, BANNED
    );

    /**
     * Deserializes a string value from a JSON payload back into a {@code UserStatus} enum.
     *
     * <p>Annotated with {@link JsonCreator} to allow Jackson to automatically parse
     * frontend string values (e.g., "active", "banned") into this enum.
     *
     * <p><b>Validation Strategy:</b> This method enforces strict validation. Unlike some other
     * enums (e.g., {@code TargetType}) that might default to {@code null} on empty input, this
     * method throws a {@link BadRequestException} immediately if the input is blank or unrecognized.
     * This strictness is intentional because user status is a critical security gateway; defaulting
     * to an active or null status silently could lead to severe authorization bypasses.
     *
     * @param value the user status string provided by the client
     * @return the corresponding {@code UserStatus}
     * @throws BadRequestException if the provided string is blank or does not match any defined status
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UserStatus fromValue(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
        }
        UserStatus status = VALUE_MAP.get(value.toLowerCase());
        if (status != null) {
            return status;
        }
        throw new BadRequestException(ErrorCode.ENUM_FORMAT, VALUE_MAP.keySet());
    }
}