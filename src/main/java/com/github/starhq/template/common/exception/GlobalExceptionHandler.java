package com.github.starhq.template.common.exception;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.config.messages.MessageUtils;
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
 * 全局异常处理器
 *
 * @author starhq
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final MessageUtils messageUtils;

    // ====================== Validation ======================

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, HttpStatusCode status, @NonNull WebRequest request) {
        String message = formatFieldErrors(ex.getBindingResult().getFieldErrors());
        return buildValidationResponse(status.value(), message);
    }

    // ====================== Common HTTP Errors ======================

    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.PARAM_FORMAT);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.QUERY_FORMAT, extractParamName(ex));
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.QUERY_FORMAT, ex.getParameterName());
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.NOT_FOUND, ex.getRequestURL());
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return buildFrameworkResponse(status.value(), ErrorCode.NOT_SUPPORT, ex.getMethod(), ex.getSupportedHttpMethods());
    }

    // ====================== Business & System Exceptions ======================

    /**
     * Handle database access errors
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Result<Void>> handleDatabaseError(DataAccessException ex) {
        log.error("Database error", ex);
        return buildBizResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.DB_QUERY_ERROR);
    }

    /**
     * Handle custom business exceptions
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Result<Void>> handleCustomException(CustomException ex) {
        logCustomException(ex);
        return buildBizResponse(ex.getStatus(), ex.getErrorCode(), ex.getArgs());
    }

    /**
     * Handle all other uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        return buildBizResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR);
    }

    // ====================== Helper Methods ======================

    /**
     * 提取出来的公共字段错误格式化逻辑
     */
    private String formatFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
    }

    /**
     * Log custom exception based on HTTP status
     */
    private void logCustomException(CustomException ex) {
        HttpStatus status = Objects.requireNonNullElse(ex.getStatus(), HttpStatus.INTERNAL_SERVER_ERROR);
        if (status.is4xxClientError()) {
            log.warn("Client error [{}]: {}", status.value(), ex.getMessage());
        } else {
            log.error("Server error [{}]: {}", status.value(), ex.getMessage(), ex);
        }
    }

    /**
     * Build standard error response
     */
    private ResponseEntity<Result<Void>> buildBizResponse(HttpStatus status, ErrorCode errorCode, Object... args) {
        Result<Void> result = messageUtils.buildErrorResponse(errorCode, args);
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Build standard error response
     */
    private ResponseEntity<Object> buildFrameworkResponse(Integer status, ErrorCode errorCode, Object... args) {
        Result<Void> result = messageUtils.buildErrorResponse(errorCode, args);
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Build standard error response
     */
    private ResponseEntity<Object> buildValidationResponse(Integer status, String message) {
        Result<Void> result = messageUtils.buildErrorResponse(ErrorCode.VALIDATION_FAILED, message);
        return ResponseEntity.status(status).body(result);
    }

    private String extractParamName(TypeMismatchException ex) {
        String name = ex instanceof MethodArgumentTypeMismatchException matme ?
                matme.getName() : StringUtils.hasText(ex.getPropertyName()) ?
                ex.getPropertyName() : "parameter";

        String requiredType = Optional.ofNullable(ex.getRequiredType()).map(Class::getSimpleName).orElse("unknown type");

        log.warn("Parameter [{}] has a wrong type: the expected type is [{}], but the received value is [{}]", name, requiredType, ex.getValue());

        return name;
    }
}
