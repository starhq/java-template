package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

/**
 * @author starhq
 */
public class BadRequestException extends CustomException {

    public BadRequestException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.BAD_REQUEST, args);
    }

    public BadRequestException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.BAD_REQUEST, cause, args);
    }
}