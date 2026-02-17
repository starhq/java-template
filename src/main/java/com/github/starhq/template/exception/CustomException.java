package com.github.starhq.template.exception;

import lombok.Getter;

/**
 * CustomException is a runtime exception that allows for additional arguments
 * to be passed along with the exception message. This can be useful for
 * providing context or details about the exception.
 */
@Getter
public class CustomException extends RuntimeException {

    // Array to hold additional arguments related to the exception
    private Object[] args;

    // Default constructor
    public CustomException() {
        super();
    }

    /**
     * Constructor that accepts a message and additional arguments.
     *
     * @param message the detail message
     * @param args    additional arguments related to the exception
     */
    public CustomException(String message, Object[] args) {
        super(message);
        this.args = args;
    }

    /**
     * Constructor that accepts a message, additional arguments, and a cause.
     *
     * @param message the detail message
     * @param args    additional arguments related to the exception
     * @param cause   the cause of the exception
     */
    public CustomException(String message, Object[] args, Throwable cause) {
        super(message, cause);
        this.args = args;
    }

    /**
     * Constructor that accepts a cause.
     *
     * @param cause the cause of the exception
     */
    public CustomException(Throwable cause) {
        super(cause);
    }
}
