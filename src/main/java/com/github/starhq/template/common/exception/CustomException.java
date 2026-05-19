package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {

    private final Object[] args;

    private final HttpStatus status;

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode, HttpStatus status, Object... args) {
        this(errorCode, status, null, args);
    }

    public CustomException(ErrorCode errorCode, HttpStatus status, Throwable cause, Object... args) {
        super(errorCode.getI18nKey(), cause);
        this.errorCode = errorCode;
        this.args = args == null ? new Object[0] : args;
        this.status = status;
    }
}
