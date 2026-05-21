package com.github.starhq.template.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * Unified system error code enumeration.
 *
 * <p>This enum acts as the single source of truth for all API exception responses.
 * Error codes are segmented by module (e.g., 1xxxx for generic, 2xxxx for auth, 3xxxx for business)
 * to enable easy categorization and handling by the frontend or API gateway.
 *
 * <p>Each constant maps to an {@code i18nKey} for internationalization, and a {@code defaultMessage}
 * used as a fallback key when the specific i18n translation is missing.
 *
 * @author starhq
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 1. Generic Errors (1xxxx) ==========
    /**
     * Request body parameter format is invalid (HTTP 400).
     */
    PARAM_FORMAT(10000, "error.param.format", "error.external"),
    /**
     * Query string parameter format is invalid (HTTP 400).
     */
    QUERY_FORMAT(10001, "error.query.format", "error.external"),
    /**
     * Provided enum value is out of range or invalid (HTTP 400).
     */
    ENUM_FORMAT(10002, "error.enum.format", "error.external"),
    /**
     * Bean validation (e.g., {@code @NotNull}, {@code @Size}) failed (HTTP 400).
     */
    VALIDATION_FAILED(10003, "error.validation", "error.external"),
    /**
     * Requested API resource or endpoint does not exist (HTTP 404).
     */
    NOT_FOUND(10004, "error.not_found", "error.external"),
    /**
     * HTTP method (GET/POST/PUT/DELETE) is not supported for this endpoint (HTTP 405).
     */
    NOT_SUPPORT(10005, "error.method.not_support", "error.external"),

    // ========== 2. Authentication & Authorization Module (2xxxx) ==========
    /**
     * Generic unauthorized access (HTTP 401).
     */
    UNAUTHORIZED(20000, "error.auth.unauthorized", "error.auth.unauthorized"),
    /**
     * Authorization token is missing from the request header (HTTP 401).
     */
    TOKEN_MISSING(20001, "error.auth.token.missing", "error.auth.unauthorized"),
    /**
     * The provided authorization token is structurally invalid or tampered with (HTTP 401).
     */
    TOKEN_INVALID(20002, "error.auth.token.invalid", "error.auth.unauthorized"),
    /**
     * The authorization token has exceeded its time-to-live (TTL) (HTTP 401).
     */
    TOKEN_EXPIRED(20003, "error.auth.token.expired", "error.auth.unauthorized"),
    /**
     * The refresh token provided is invalid or unrecognized (HTTP 401).
     */
    TOKEN_REFRESH_INVALID(20004, "error.auth.token.refresh.invalid", "error.auth.unauthorized"),
    /**
     * The access token provided is invalid or unrecognized (HTTP 401).
     */
    TOKEN_ACCESS_INVALID(20005, "error.auth.token.access.invalid", "error.auth.unauthorized"),
    /**
     * The server-side user session is invalid or does not exist (HTTP 401).
     */
    SESSION_INVALID(20006, "error.auth.session.invalid", "error.auth.unauthorized"),
    /**
     * The server-side user session has expired (HTTP 401).
     */
    SESSION_EXPIRED(20007, "error.auth.session.expired", "error.auth.unauthorized"),
    /**
     * Required device fingerprint header is missing from the request (HTTP 401).
     */
    FINGERPRINT_MISSING(20008, "error.auth.fingerprint.missing", "error.auth.unauthorized"),
    /**
     * Client environment changed (e.g., IP change, device change), requiring re-authentication (HTTP 401).
     */
    DEVICE_MISMATCH(20009, "error.auth.device.mismatch", "error.auth.unauthorized"),
    /**
     * Username or password is incorrect (HTTP 401).
     */
    CREDENTIALS(20010, "error.auth.credentials", "error.auth.unauthorized"),
    /**
     * The user account has been manually disabled or locked (HTTP 401).
     */
    DISABLED(20011, "error.auth.disabled", "error.auth.unauthorized"),
    /**
     * The user account has not been assigned any roles (HTTP 401).
     */
    NO_ROLES(20012, "error.auth.no_roles", "error.auth.unauthorized"),
    /**
     * The original password provided does not match the current record (HTTP 401).
     */
    MISMATCH_PASSWORD(20013, "error.password.mismatch", "error.auth.unauthorized"),
    /**
     * The new password is the same as the old password (HTTP 401).
     */
    SAME_PASSWORD(20014, "error.password.same", "error.auth.unauthorized"),
    /**
     * Failed to process the password reset request (HTTP 401).
     */
    RESET_PASSWORD(20015, "error.auth.password.failed", "error.auth.unauthorized"),
    /**
     * Internal failure while generating the captcha image (HTTP 500).
     */
    CAPTCHA_FAILED(20016, "error.auth.captcha.failed", "error.internal"),
    /**
     * The submitted captcha answer is incorrect (HTTP 400).
     */
    CAPTCHA_VERIFY(20017, "error.auth.captcha.verify", "error.internal"),
    /**
     * Captcha requests are too frequent, triggering rate limiting (HTTP 429).
     */
    CAPTCHA_REQUEST_TOO_OFTEN(20018, "error.auth.captcha.often", "error.internal"),
    /**
     * Too many failed verification attempts, temporarily banning the IP/user (HTTP 429/500).
     */
    CAPTCHA_VERIFY_BANNED(20019, "error.auth.captcha.banned", "error.internal"),
    /**
     * The captcha has expired and is no longer valid (HTTP 500).
     */
    CAPTCHA_EXPIRED(20020, "error.auth.captcha.expired", "error.internal"),

    /**
     * Authenticated user lacks the required permissions for this operation (HTTP 403).
     */
    FORBIDDEN(21000, "error.auth.forbidden", "error.auth.forbidden"),

    // ========== 3. Business Module (3xxxx) ==========
    /**
     * Requested user entity does not exist in the database (HTTP 404).
     */
    USER_NOT_FOUND(30000, "error.user.not_found", "error.not_found"),
    /**
     * Failed to assign roles to the user due to an internal error (HTTP 500).
     */
    USER_ASSIGN_FAILED(30001, "error.user.assign.role.fail", "error.business"),
    /**
     * The provided username is already taken by another user (HTTP 429/409).
     */
    USER_DUPLICATE_USERNAME(30002, "error.user.duplicate.username", "error.external"),
    /**
     * Database operation failed while updating user information (HTTP 500).
     */
    USER_UPDATE_FAILED(30003, "error.user.update.failed", "error.business"),
    /**
     * Database operation failed while deleting the user (HTTP 500).
     */
    USER_DELETE_FAILED(30004, "error.user.delete.failed", "error.business"),
    /**
     * Database operation failed while inserting the new user (HTTP 500).
     */
    USER_INSERT_FAILED(30005, "error.user.insert.failed", "error.business"),
    /**
     * User deletion blocked due to existing relational constraints (e.g., tied to logs/roles) (HTTP 500).
     */
    USER_CONSTRAINT(30006, "error.user.delete.constraint", "error.business"),

    /**
     * Failed to revoke the user's token during logout (HTTP 500).
     */
    TOKEN_REVOKED(31000, "error.token.revoked", "error.business"),
    /**
     * Failed to issue a new token during login or refresh (HTTP 500).
     */
    TOKEN_ISSUED(31001, "error.token.issued", "error.business"),

    /**
     * Requested role entity does not exist in the database (HTTP 404).
     */
    ROLE_NOT_FOUND(32000, "error.role.not_found", "error.not_found"),
    /**
     * The provided role code is already in use by another role (HTTP 429/409).
     */
    ROLE_DUPLICATE_CODE(32001, "error.role.duplicate_code", "error.external"),
    /**
     * Database operation failed while inserting the new role (HTTP 500).
     */
    ROLE_INSERT_FAILED(32002, "error.role.insert.failed", "error.business"),
    /**
     * Database operation failed while updating the role (HTTP 500).
     */
    ROLE_UPDATE_FAILED(32003, "error.role.update.failed", "error.business"),
    /**
     * Database operation failed while deleting the role (HTTP 500).
     */
    ROLE_DELETE_FAILED(32004, "error.role.delete.failed", "error.business"),
    /**
     * Failed to bind menus to the specified role (HTTP 500).
     */
    ROLE_ASSIGN_MENUS_FAILED(32005, "error.role.assign.menus.fail", "error.business"),
    /**
     * Failed to bind buttons to the specified role (HTTP 500).
     */
    ROLE_ASSIGN_BUTTONS_FAILED(32006, "error.role.assign.buttons.fail", "error.business"),
    /**
     * Failed to bind API resources to the specified role (HTTP 500).
     */
    ROLE_ASSIGN_RESOURCES_FAILED(32007, "error.role.assign.resources.fail", "error.business"),

    /**
     * Requested menu entity does not exist in the database (HTTP 404).
     */
    MENU_NOT_FOUND(33000, "error.menu.not_found", "error.not_found"),
    /**
     * Database operation failed while inserting the new menu (HTTP 500).
     */
    MENU_INSERT_FAILED(33001, "error.menu.insert.failed", "error.business"),
    /**
     * Database operation failed while deleting the menu (HTTP 500).
     */
    MENU_DELETE_FAILED(33002, "error.menu.delete.failed", "error.business"),
    /**
     * Database operation failed while updating the menu (HTTP 500).
     */
    MENU_UPDATE_FAILED(33003, "error.menu.update.failed", "error.business"),
    /**
     * Menu deletion blocked because it contains associated child menus (HTTP 500).
     */
    MENU_HAS_CHILD(33004, "error.menu.delete.constraint", "error.business"),

    /**
     * Requested button entity does not exist in the database (HTTP 404).
     */
    BUTTON_NOT_FOUND(34000, "error.button.not_found", "error.not_found"),
    /**
     * The provided button code is already in use by another button (HTTP 429/409).
     */
    BUTTON_DUPLICATE_CODE(34001, "error.button.duplicate_code", "error.external"),
    /**
     * Database operation failed while inserting the new button (HTTP 500).
     */
    BUTTON_INSERT_FAILED(34002, "error.button.insert.failed", "error.business"),
    /**
     * Database operation failed while updating the button (HTTP 500).
     */
    BUTTON_UPDATE_FAILED(34003, "error.button.update.failed", "error.business"),
    /**
     * Database operation failed while deleting the button (HTTP 500).
     */
    BUTTON_DELETE_FAILED(34004, "error.button.delete.failed", "error.business"),

    /**
     * Requested API resource entity does not exist in the database (HTTP 404).
     */
    RESOURCE_NOT_FOUND(35000, "error.resource.not_found", "error.not_found"),
    /**
     * The combination of API URL and HTTP method is already registered (HTTP 429/409).
     */
    RESOURCE_DUPLICATE_URL_METHOD(35001, "error.resource.duplicate_url_method", "error.external"),
    /**
     * Database operation failed while inserting the new resource (HTTP 500).
     */
    RESOURCE_INSERT_FAILED(35002, "error.resource.insert.failed", "error.business"),
    /**
     * Database operation failed while updating the resource (HTTP 500).
     */
    RESOURCE_UPDATE_FAILED(35003, "error.resource.update.failed", "error.business"),
    /**
     * Database operation failed while deleting the resource (HTTP 500).
     */
    RESOURCE_DELETE_FAILED(35004, "error.resource.delete.failed", "error.business"),

    /**
     * Requested dictionary type entity does not exist in the database (HTTP 404).
     */
    DICT_TYPE_NOT_FOUND(36000, "error.dict_type.not_found", "error.not_found"),
    /**
     * The provided dictionary type key is already in use (HTTP 429/409).
     */
    DICT_TYPE_DUPLICATE_TYPE(36001, "error.dict_type.duplicate_type", "error.external"),
    /**
     * Database operation failed while inserting the new dictionary type (HTTP 500).
     */
    DICT_TYPE_INSERT_FAILED(36002, "error.dict_type.insert.failed", "error.business"),
    /**
     * Database operation failed while updating the dictionary type (HTTP 500).
     */
    DICT_TYPE_UPDATE_FAILED(36003, "error.dict_type.update.failed", "error.business"),
    /**
     * Database operation failed while deleting the dictionary type (HTTP 500).
     */
    DICT_TYPE_DELETE_FAILED(36004, "error.dict_type.delete.failed", "error.business"),

    /**
     * Requested dictionary data entity does not exist in the database (HTTP 404).
     */
    DICT_DATA_NOT_FOUND(37000, "error.dict_data.not_found", "error.not_found"),
    /**
     * The provided dictionary data value is already in use under the same type (HTTP 429/409).
     */
    DICT_DATA_DUPLICATE_VALUE(37001, "error.dict_data.duplicate_value", "error.external"),
    /**
     * Database operation failed while inserting the new dictionary data (HTTP 500).
     */
    DICT_DATA_INSERT_FAILED(37002, "error.dict_data.insert.failed", "error.business"),
    /**
     * Database operation failed while updating the dictionary data (HTTP 500).
     */
    DICT_DATA_UPDATE_FAILED(37003, "error.dict_data.update.failed", "error.business"),
    /**
     * Database operation failed while deleting the dictionary data (HTTP 500).
     */
    DICT_DATA_DELETE_FAILED(37004, "error.dict_data.delete.failed", "error.business"),

    // ========== 9. System Exceptions (9xxxx) ==========
    /**
     * A generic exception occurred during database query execution (HTTP 500).
     */
    DB_QUERY_ERROR(90000, "error.db.execute", "error.internal"),
    /**
     * Database query succeeded, but failed to map the ResultSet to Java objects (HTTP 500).
     */
    DB_MAPPING_ERROR(90001, "error.db.mapping", "error.internal"),
    /**
     * An unhandled, generic internal server error occurred (HTTP 500).
     */
    INTERNAL_ERROR(99999, "error.internal", "error.internal");

    /**
     * The specific numeric error code returned to the client.
     */
    private final Integer code;

    /**
     * The key used to look up the localized error message in the i18n message source.
     */
    private final String i18nKey;

    /**
     * Fallback message key used by the exception handler if the specific {@code i18nKey}
     * translation is missing from the resource bundles.
     */
    private final String defaultMessage;
}
