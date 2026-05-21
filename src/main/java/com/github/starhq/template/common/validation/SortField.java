package com.github.starhq.template.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Marks a field or parameter as requiring validation to ensure it represents a safe,
 * explicitly allowed sorting field.
 *
 * <p>This annotation acts as a security gatekeeper for dynamic sorting features (e.g.,
 * {@code /api/users?sortBy=createTime}). Without this validation, malicious users could
 * inject arbitrary database column names into ORDER BY clauses, potentially leading to
 * SQL injection or unauthorized data exposure.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * public class PageRequest {
 *     @SortField
 *     private String sortBy;
 * }
 * }</pre>
 *
 * @see SortOrderValidator
 */
@Documented
@Constraint(validatedBy = SortOrderValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SortField {

    /**
     * The error message template to be used when the provided sort field is invalid or unauthorized.
     *
     * <p>This key is resolved by the {@code MessageSource}. Defaults to a generic parameter format error.
     *
     * @return the i18n error message key
     */
    String message() default "{error.param.format}";

    /**
     * Allows specifying validation groups to which this constraint belongs.
     *
     * @return the array of validation group classes
     */
    Class<?>[] groups() default {};

    /**
     * Payloads can be attached to a constraint declaration to be carried along by the validation framework.
     *
     * @return the array of payload classes
     */
    Class<? extends Payload>[] payload() default {};
}