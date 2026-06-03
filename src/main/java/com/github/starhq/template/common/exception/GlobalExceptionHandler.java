package com.github.starhq.template.common.exception;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.model.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Centralized global exception handler for all REST controllers.
 *
 * <p>Intercepts exceptions thrown by controllers (and downstream services), translates them into
 * a standardized {@link Result} JSON structure, and prevents raw stack traces or internal
 * database details from leaking to the API consumer.
 *
 * <p>It extends {@link ResponseEntityExceptionHandler} to hook into Spring MVC's built-in exception
 * handling mechanisms for standard web errors (like 400, 404, 405), while adding custom handling
 * for application-specific exceptions (like {@link CustomException}).
 *
 * @author starhq
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageUtils messageUtils;

    // ====================== Validation ======================

    /**
     * Handles validation failures triggered by {@code @Valid} or {@code @Validated} annotations.
     *
     * <p>Instead of returning a complex nested structure of field errors, this method flattens
     * all {@link FieldError}s into a single semicolon-separated string to simplify frontend parsing.
     *
     * @param ex      the exception containing binding results
     * @param headers the request headers
     * @param status  the HTTP status (usually 400)
     * @param request the current web request
     * @return a standardized response containing the concatenated validation error messages
     */
    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status,
                                                                                     WebRequest request) {
        String message = formatFieldErrors(ex.getBindingResult().getFieldErrors());
        return buildValidationResponse(status.value(), message);
    }

    // ====================== Common HTTP Errors ======================

    /**
     * Handles malformed JSON requests (e.g., missing quotes, bad syntax).
     *
     * @param ex      the exception thrown by Jackson when it cannot parse the request body
     * @param headers the request headers
     * @param status  the HTTP status (usually 400)
     * @param request the current web request
     * @return a standardized response indicating a body format error
     */
    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                                     HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.PARAM_FORMAT);
    }

    /**
     * Handles type mismatch errors, typically when a query/path parameter cannot be converted
     * to the required Java type (e.g., passing "?id=abc" to a {@code Long id} parameter).
     *
     * @param ex      the exception containing the invalid value and expected type
     * @param headers the request headers
     * @param status  the HTTP status (usually 400)
     * @param request the current web request
     * @return a standardized response pointing out the specific parameter that failed conversion
     */
    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.QUERY_FORMAT, extractParamName(ex));
    }

    /**
     * handles missing required request parameters (e.g., omitting a mandatory {@code @RequestParam}).
     *
     * @param ex      the exception indicating which parameter is missing
     * @param headers the request headers
     * @param status  the HTTP status (usually 400)
     * @param request the current web request
     * @return a standardized response indicating the missing parameter
     */
    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.QUERY_FORMAT, ex.getParameterName());
    }

    /**
     * Handles requests to API endpoints that do not exist.
     *
     * <p><b>Note:</b> For this handler to trigger, {@code spring.mvc.throw-exception-if-no-handler-found}
     * must be set to {@code true} in application properties.
     *
     * @param ex      the exception containing the requested URL
     * @param headers the request headers
     * @param status  the HTTP status (404)
     * @param request the current web request
     * @return a standardized 404 response
     */
    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.NOT_FOUND, ex.getRequestURL());
    }

    /**
     * Handles requests using an unsupported HTTP method (e.g., sending a POST to a GET-only endpoint).
     *
     * @param ex      the exception containing the unsupported method and the list of allowed methods
     * @param headers the request headers
     * @param status  the HTTP status (405)
     * @param request the current web request
     * @return a standardized 405 response
     */
    @Override
    protected @Nullable ResponseEntity<@NonNull Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.NOT_SUPPORT, ex.getMethod(), Objects.requireNonNull(ex.getSupportedHttpMethods()));
    }

    // ====================== Business & System Exceptions ======================

    /**
     * Catches Spring's {@link DataAccessException} (wrapping JDBC/MyBatis errors).
     *
     * <p><b>Security Measure:</b> Database exceptions often contain sensitive information (table names,
     * column names, SQL snippets). This handler ensures a generic 500 error is returned to the client,
     * while the full stack trace is safely logged on the server.
     *
     * @param ex the database access exception
     * @return a standardized 500 response indicating a database execution failure
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDatabaseError(DataAccessException ex) {
        log.error("Database error", ex);
        return buildBizResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.DB_QUERY_ERROR);
    }

    /**
     * Core handler for all custom application exceptions (e.g., {@link BusinessException}, {@link BadRequestException}).
     *
     * <p>Delegates to {@link #logCustomException(CustomException)} to apply differentiated logging
     * based on whether the error is a client fault (4xx) or a server fault (5xx).
     *
     * @param ex the custom exception thrown by business logic
     * @return a standardized response mapped to the specific HTTP status and error code
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Result<Void>> handleCustomException(CustomException ex) {
        logCustomException(ex);
        return buildBizResponse(ex.getStatus(), ex.getErrorCode(), ex.getArgs());
    }

    /**
     * Ultimate fallback handler for any unhandled {@link Exception}.
     *
     * <p>Acts as the last line of defense to prevent the servlet container (like Tomcat) from
     * generating its own ugly HTML error pages. Always logs at ERROR level because this represents
     * an unexpected bug in the system.
     *
     * @param ex the uncaught exception
     * @return a standardized 500 Internal Server Error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        return buildBizResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    // ====================== Helper Methods ======================

    /**
     * Transforms a list of JSR-380 field errors into a flat, human-readable string.
     *
     * <p>Output format: "fieldName1: error message 1; fieldName2: error message 2"
     *
     * @param fieldErrors the list of field-specific validation errors
     * @return a single concatenated string of all validation failures
     */
    private String formatFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream().map(error -> error.getField() + ": " + error.getDefaultMessage()).collect(Collectors.joining("; "));
    }

    /**
     * Applies a smart logging strategy based on the HTTP status of the custom exception.
     *
     * <ul>
     *   <li><b>4xx Client Errors:</b> Logged as {@code WARN}. These are expected business flow
     *       interruptions caused by invalid user input (e.g., wrong password). Logging at ERROR
     *       would flood the logs and trigger false alarms in monitoring systems.</li>
     *   <li><b>5xx Server Errors:</b> Logged as {@code ERROR} with full stack trace. These indicate
     *       actual system failures that require immediate developer attention.</li>
     * </ul>
     *
     * @param ex the custom exception to log
     */
    private void logCustomException(CustomException ex) {
        HttpStatus status = ex.getStatus();
        if (status.is4xxClientError()) {
            log.warn("Client error [{}]: {}", status.value(), ex.getMessage());
        } else {
            log.error("Server error [{}]: {}", status.value(), ex.getMessage(), ex);
        }
    }

    /**
     * Constructs a standardized error response for custom business/system exceptions.
     *
     * @param status    the HTTP status to return
     * @param errorCode the error code enum for i18n message resolution
     * @param args      dynamic arguments for the i18n message template
     * @return a {@link ResponseEntity} wrapping the standardized {@link Result}
     */
    private ResponseEntity<Result<Void>> buildBizResponse(HttpStatus status, ErrorCode errorCode, Object... args) {
        Result<Void> result = messageUtils.buildErrorResponse(errorCode, args);
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Constructs a standardized error response for overridden Spring MVC framework exceptions.
     *
     * <p>Returns {@code ResponseEntity<Object>} because the Spring framework method signatures
     * being overridden strictly require {@code Object} as the body type.
     *
     * @param status    the HTTP status code
     * @param errorCode the error code enum for i18n message resolution
     * @param args      dynamic arguments for the i18n message template
     * @return a {@link ResponseEntity} wrapping the standardized {@link Result}
     */
    private ResponseEntity<Object> buildFrameworkResponse(Integer status, ErrorCode errorCode, Object... args) {
        Result<Void> result = messageUtils.buildErrorResponse(errorCode, args);
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Constructs a standardized error response specifically for JSR-380 validation failures.
     *
     * <p>Unlike other builders, this bypasses the i18n template lookup and directly injects the
     * pre-formatted string of field errors into the response.
     *
     * @param status  the HTTP status code
     * @param message the concatenated validation error message
     * @return a {@link ResponseEntity} wrapping the standardized {@link Result}
     */
    private ResponseEntity<Object> buildValidationResponse(Integer status, String message) {
        Result<Void> result = messageUtils.buildErrorResponse(ErrorCode.VALIDATION_FAILED.getCode(), message);
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Safely extracts the parameter name from a {@link TypeMismatchException}.
     *
     * <p>Tries to get the specific parameter name from {@link MethodArgumentTypeMismatchException},
     * falls back to the generic property name, and defaults to "parameter" if both are unavailable.
     * Also logs a warning to assist developers in debugging the exact cause of the type mismatch.
     *
     * @param ex the type mismatch exception
     * @return the resolved parameter name, or a generic fallback string
     */
    private String extractParamName(TypeMismatchException ex) {
        String name;
        if (ex instanceof MethodArgumentTypeMismatchException matme) {
            name = matme.getName();
        } else if (StringUtils.hasText(ex.getPropertyName())) {
            name = ex.getPropertyName();
        } else {
            name = "parameter";
        }

        String requiredType = Optional.ofNullable(ex.getRequiredType()).map(Class::getSimpleName).orElse("unknown type");

        log.warn("Parameter [{}] has wrong type: expected [{}], but received [{}]", name, requiredType, ex.getValue());

        return name;
    }
}