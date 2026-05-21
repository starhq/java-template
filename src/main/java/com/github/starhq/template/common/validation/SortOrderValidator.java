package com.github.starhq.template.common.validation;

import java.util.Set;

import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * JSR-380 (Jakarta Validation) validator responsible for verifying that a provided
 * sort field string is within a permitted whitelist.
 *
 * <p>This prevents malicious users from injecting arbitrary database column names
 * into ORDER BY clauses, which could lead to SQL injection or unauthorized data sorting.
 *
 * @author starhq
 * @see SortField
 */
public class SortOrderValidator implements ConstraintValidator<SortField, String> {

    /**
     * The immutable set of allowed database column names or entity field aliases
     * that are safe to use in SQL ORDER BY clauses.
     */
    private Set<String> fields;

    /**
     * Initializes the validator.
     *
     * <p><b>⚠️ Architecture Warning:</b> Currently, this method hardcodes the allowed fields
     * to just {@code "id"}. The {@link SortField} annotation likely has an attribute to define
     * allowed fields dynamically (e.g., {@code @SortField(allowedFields = {"id", "createTime"})}).
     * If so, this method should be updated to:
     * <pre>{@code
     * this.fields = Set.of(constraintAnnotation.allowedFields());
     * }</pre>
     * Hardcoding limits the reusability of the {@code @SortField} annotation across different DTOs.
     */
    @Override
    public void initialize(SortField constraintAnnotation) {
        fields = Set.of("id");
    }

    /**
     * Evaluates whether the provided sort field string is valid and safe.
     *
     * @param value   the sort field string provided by the client (e.g., "id" or "createTime")
     * @param context the Jakarta Validation context
     * @return {@code true} if the value is non-empty and exists in the allowed whitelist;
     * {@code false} otherwise
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Reject null, empty strings, or pure whitespace immediately
        if (!StringUtils.hasText(value)) {
            return false;
        }

        // Verify against the whitelist to prevent SQL injection in ORDER BY clauses
        return fields.contains(value);
    }

}