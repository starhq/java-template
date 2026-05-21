package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

import java.io.Serial;

/**
 * Exception thrown when a requested business resource cannot be found (HTTP 404 Not Found).
 *
 * <p>This exception should be used in service layer logic when a lookup by ID, code, or unique key
 * fails to yield a result (e.g., "User with ID 123 does not exist" or "Role code 'ADMIN' not found").
 *
 * <p><b>Scope Distinction:</b> Do not confuse this with a path-level 404 (e.g., requesting a non-existent
 * API endpoint like {@code /api/v1/unknown-url}). Path-level 404s are handled by Spring MVC's
 * {@link org.springframework.web.servlet.NoHandlerFoundException}. This exception represents a
 * valid API endpoint receiving a valid request, but the targeted *domain entity* simply does not exist.
 *
 * @author starhq
 * @see CustomException
 * @see HttpStatus#NOT_FOUND
 */
public class NotFoundException extends CustomException {

    @Serial
    private static final long serialVersionUID = -6778146173922132686L;

    /**
     * Constructs a new not found exception with the specific error code and dynamic arguments.
     *
     * <p>Typically used when a database query returns an empty {@link java.util.Optional}
     * or {@code null}, and the business rule dictates that the operation cannot continue.
     *
     * @param errorCode the specific error code enum (e.g., {@code ErrorCode.USER_NOT_FOUND})
     * @param args      dynamic arguments to substitute into the error message template (e.g., the missing ID)
     */
    public NotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.NOT_FOUND, args);
    }

    /**
     * Constructs a new not found exception wrapping an underlying cause.
     *
     * <p>Rarely used, but available if a specific data access framework exception
     * (like JPA's {@code EmptyResultDataAccessException}) needs to be translated into a 404 response.
     *
     * @param errorCode the specific error code enum
     * @param cause     the underlying cause of the lookup failure
     * @param args      dynamic arguments to substitute into the error message template
     */
    public NotFoundException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.NOT_FOUND, cause, args);
    }
}