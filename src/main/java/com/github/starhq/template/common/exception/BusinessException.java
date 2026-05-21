package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

import java.io.Serial;

/**
 * Exception thrown to indicate an internal business logic failure (HTTP 500 Internal Server Error).
 *
 * <p>This exception should be used when the request is structurally valid, but the server
 * cannot fulfill it due to internal business rule violations or expected runtime constraints.
 *
 * <p><b>Key Distinction:</b> Unlike {@link BadRequestException} (which implies the client sent
 * bad data), a {@code BusinessException} implies the client's request was valid, but an
 * expected server-side business state prevented completion (e.g., "Insufficient balance",
 * "User already assigned to this role", or "Database constraint violation").
 *
 * <p>It automatically maps to {@link HttpStatus#INTERNAL_SERVER_ERROR} (500) in the global
 * exception handler. In a mature system, you might further distinguish between a 500
 * (unexpected system error) and a 422/409 (processable business rule failure), but this
 * serves as the standard catch-all for expected business flow interruptions.
 *
 * @author starhq
 * @see CustomException
 * @see BadRequestException
 * @see HttpStatus#INTERNAL_SERVER_ERROR
 */
public class BusinessException extends CustomException {

    @Serial
    private static final long serialVersionUID = 7838455735010607022L;

    /**
     * Constructs a new business exception with the specified error code and dynamic arguments.
     *
     * @param errorCode the specific error code enum detailing the business rule that was violated
     * @param args      dynamic arguments to substitute into the error message template
     */
    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    /**
     * Constructs a new business exception wrapping an underlying cause.
     *
     * <p>Use this constructor when a lower-level exception (e.g., {@link java.sql.SQLException}
     * wrapped in a Spring data exception) triggered the business failure, and you want to
     * translate it into a user-friendly business error while preserving the root cause for logging.
     *
     * @param errorCode the specific error code enum detailing the business rule that was violated
     * @param cause     the underlying cause of the business logic interruption
     * @param args      dynamic arguments to substitute into the error message template
     */
    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, cause, args);
    }
}