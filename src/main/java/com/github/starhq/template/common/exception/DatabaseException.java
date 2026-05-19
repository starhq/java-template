package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

public class DatabaseException extends CustomException {

    public DatabaseException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    public DatabaseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, cause, args);
    }

}
