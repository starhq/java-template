package com.github.starhq.template.common.exception;

import com.github.starhq.template.common.enums.ErrorCode;
import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Exception thrown to indicate a database execution or data access failure (HTTP 500).
 *
 * <p>This exception should be used to wrap or represent low-level data access errors,
 * such as SQL syntax errors, connection timeouts, deadlocks, or unexpected JDBC/MyBatis failures.
 *
 * <p><b>Security & Architecture Note:</b> Raw database exceptions (like {@code SQLException})
 * often contain sensitive information about table structures, column names, or SQL queries.
 * This exception ensures that such details are stripped from the HTTP response, returning a
 * generic 500 Internal Server Error to the client, while the wrapped {@link Throwable} cause
 * is safely preserved for server-side logging and debugging.
 *
 * @author starhq
 * @see CustomException
 * @see org.springframework.dao.DataAccessException
 */
public class DatabaseException extends CustomException {

    @Serial
    private static final long serialVersionUID = -4827396054478671386L;

    /**
     * Constructs a new database exception without an underlying cause.
     *
     * <p>Typically used when you explicitly know a DB operation failed and want to throw
     * a standardized error code, but the original framework exception was already handled or lost.
     *
     * @param errorCode the specific error code enum (e.g., {@code ErrorCode.DB_QUERY_ERROR})
     * @param args      dynamic arguments to substitute into the error message template
     */
    public DatabaseException(ErrorCode errorCode, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, args);
    }

    /**
     * Constructs a new database exception wrapping an underlying data access cause.
     *
     * <p><b>Primary Use Case:</b> Use this in your DAO/Mapper layer catch blocks to wrap
     * Spring's {@code DataAccessException} or native {@code SQLException}. This ensures
     * the specific technical failure (e.g., "Column 'abc' not found") is passed to the logging
     * system via the {@code cause} parameter, without ever leaking it to the API response.
     *
     * @param errorCode the specific error code enum detailing the database operation phase
     * @param cause     the underlying database or data access exception
     * @param args      dynamic arguments to substitute into the error message template
     */
    public DatabaseException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode, HttpStatus.INTERNAL_SERVER_ERROR, cause, args);
    }

}