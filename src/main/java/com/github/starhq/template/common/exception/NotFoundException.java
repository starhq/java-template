package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

public class NotFoundException extends CustomException {

    public NotFoundException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.NOT_FOUND, args);
    }

    public NotFoundException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.NOT_FOUND, cause, args);
    }
}
