package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

import java.io.Serial;

/**
 * Exception thrown to indicate a client-side request error (HTTP 400 Bad Request).
 *
 * <p>This exception should be used when the request is malformed, missing required parameters,
 * contains invalid enum values, or otherwise fails to meet pre-condition validation rules
 * before reaching the core business logic.
 *
 * <p>It automatically maps to {@link HttpStatus#BAD_REQUEST} (400) in the global exception handler,
 * signaling to the API consumer that the fault lies in their request and it should not be retried
 * without modification.
 *
 * @author starhq
 * @see CustomException
 * @see HttpStatus#BAD_REQUEST
 */
public class BadRequestException extends CustomException {

    @Serial
    private static final long serialVersionUID = 6438750420234702632L;

    /**
     * Constructs a new bad request exception with the specified error code and dynamic arguments.
     *
     * <p>The provided {@code args} will be used to format the i18n message template associated
     * with the {@link ErrorCode}.
     *
     * @param errorCode the specific error code enum detailing the cause of the failure
     * @param args      dynamic arguments to substitute into the error message template
     */
    public BadRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.BAD_REQUEST, args);
    }

    /**
     * Constructs a new bad request exception wrapping an underlying cause.
     *
     * <p>Use this constructor when a lower-level exception (e.g., {@link jakarta.validation.ConstraintViolationException})
     * triggered the bad request, and you want to preserve the original stack trace for debugging purposes.
     *
     * @param errorCode the specific error code enum detailing the cause of the failure
     * @param cause     the underlying cause of the bad request
     * @param args      dynamic arguments to substitute into the error message template
     */
    public BadRequestException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.BAD_REQUEST, cause, args);
    }
}