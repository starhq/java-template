package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

import java.io.Serial;

/**
 * Exception thrown when a create or update operation violates a unique constraint (HTTP 409 Conflict).
 *
 * <p>This exception is strictly used for scenarios where the request is well-formed, but it cannot
 * be completed because it conflicts with the current state of the target resource (e.g., attempting
 * to create a user with an already existing username, or saving a role with a duplicate code).
 *
 * <p><b>RESTful Semantics:</b> Maps to {@link HttpStatus#CONFLICT} (409), which is semantically more
 * accurate than a generic 400 (Bad Request) or 500 (Internal Server Error). It explicitly tells the
 * API consumer: "What you sent is valid, but it clashes with data that already exists."
 *
 * @author starhq
 * @see CustomException
 * @see HttpStatus#CONFLICT
 */
public class DuplicateException extends CustomException {

    /**
     * Serial version UID for serialization compatibility.
     *
     * <p>Annotated with {@link Serial} to allow the compiler to verify that this field is
     * declared correctly and matches the serialization contract of the superclass.
     */
    @Serial
    private static final long serialVersionUID = -7252118313259028781L;

    /**
     * Constructs a new duplicate exception with the specified error code and dynamic arguments.
     *
     * @param errorCode the specific error code enum (e.g., {@code ErrorCode.USER_DUPLICATE_USERNAME})
     * @param args      dynamic arguments to substitute into the error message template
     */
    public DuplicateException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, args);
    }

    /**
     * Constructs a new duplicate exception wrapping an underlying cause.
     *
     * <p>Useful when catching a low-level database exception (e.g., {@code java.sql.SQLIntegrityConstraintViolationException})
     * and translating it into a user-friendly 409 response.
     *
     * @param errorCode the specific error code enum
     * @param cause     the underlying database constraint violation exception
     * @param args      dynamic arguments to substitute into the error message template
     */
    public DuplicateException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, cause, args);
    }
}