package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

public class BusinessException extends CustomException {

    public BusinessException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, cause, args);
    }
}
