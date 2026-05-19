package com.github.starhq.template.common.exception;

import org.springframework.http.HttpStatus;

import com.github.starhq.template.common.enums.ErrorCode;

import java.io.Serial;

public class DuplicateException extends CustomException {

    @Serial
    private static final long serialVersionUID = -7252118313259028781L;

    public DuplicateException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, args);
    }

    public DuplicateException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.CONFLICT, cause, args);
    }
}
