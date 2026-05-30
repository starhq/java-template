package com.github.starhq.template.common.exception;

import com.github.starhq.template.common.enums.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Abstract base class for all custom application exceptions.
 *
 * <p>This class serves as the root of the exception hierarchy, bridging the gap between
 * raw Java exceptions and structured HTTP API responses. It unifies three core concepts:
 * <ul>
 *   <li>{@link ErrorCode}: The specific, machine-readable error code (e.g., 10003) and i18n key.</li>
 *   <li>{@link HttpStatus}: The semantic HTTP status code (e.g., 400, 500) to be returned to the client.</li>
 *   <li>{@code args}: Dynamic variables used to format the human-readable i18n error message.</li>
 * </ul>
 *
 * <p>By design, this extends {@link RuntimeException} to allow unchecked exception propagation,
 * avoiding the need for verbose {@code throws} clauses across the service layer.
 *
 * @author starhq
 * @see BusinessException
 * @see BadRequestException
 */
@Getter
public class CustomException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -5075800038075732247L;
    /**
     * Dynamic arguments used for formatting the localized error message.
     * <p>These are passed to {@link String#format(String, Object...)} or similar mechanisms
     * in the global exception handler to replace placeholders in the i18n message template.
     */
    private final transient Object[] args;

    /**
     * The HTTP status code that should be mapped to the client response.
     */
    private final HttpStatus status;

    /**
     * The specific error code enum detailing the exact nature of the failure.
     * <p>Contains the numeric code for the client and the i18n key for backend message resolution.
     */
    private final ErrorCode errorCode;

    /**
     * Constructs a new custom exception without an underlying cause.
     *
     * @param errorCode the specific error code enum
     * @param status    the HTTP status to return to the client
     * @param args      dynamic arguments to substitute into the error message template
     */
    public CustomException(ErrorCode errorCode, HttpStatus status, Object... args) {
        this(errorCode, status, null, args);
    }

    /**
     * Constructs a new custom exception wrapping an underlying cause.
     *
     * <p>The {@code cause}'s message is intentionally replaced by the {@code errorCode}'s i18n key
     * in the superclass ({@link RuntimeException#RuntimeException(String, Throwable)}). This ensures
     * that when logging the exception stack trace, the logger outputs the standardized i18n key
     * rather than a potentially unhelpful raw database or framework error message.
     *
     * @param errorCode the specific error code enum
     * @param status    the HTTP status to return to the client
     * @param cause     the underlying cause of the exception (preserved for root-cause logging)
     * @param args      dynamic arguments to substitute into the error message template
     */
    public CustomException(ErrorCode errorCode, HttpStatus status, Throwable cause, Object... args) {
        super(errorCode.getI18nKey(), cause);
        this.errorCode = errorCode;
        // Defensive copy to prevent external modification of the array
        this.args = args == null ? new Object[0] : args;
        this.status = status;
    }
}