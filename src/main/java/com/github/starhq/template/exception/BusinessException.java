package com.github.starhq.template.exception;

/**
 * BusinessException is a custom exception class that extends CustomException.
 * It is used to represent business-related exceptions.
 */
public class BusinessException extends CustomException {

    /**
     * Constructor that accepts a message.
     *
     * @param message the detail message
     */
    public BusinessException(String message) {
        super(message, null);
    }

    /**
     * Constructor that accepts a message and a cause.
     *
     * @param message the detail message
     * @param cause   the cause
     * 
     */
    public BusinessException(String message, Throwable throwable) {
        super(message, null, throwable);
    }

    /**
     * Constructor that accepts a message and additional arguments.
     *
     * @param message the detail message
     * @param args    additional arguments related to the exception
     */
    public BusinessException(String message, Object[] args) {
        super(message, args);
    }

    /**
     * Constructor that accepts a message and additional arguments.
     *
     * @param message   the detail message
     * @param args      additional arguments related to the exception
     * @param throwable the cause
     */
    public BusinessException(String message, Object[] args, Throwable throwable) {
        super(message, args, throwable);
    }
}
