package com.github.starhq.template.common.validation;

import java.util.Set;

import org.springframework.util.StringUtils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SortOrderValidator implements ConstraintValidator<SortField, String> {

    private Set<String> fields;

    @Override
    public void initialize(SortField constraintAnnotation) {
        fields = Set.of("id");
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        return fields.contains(value);
    }

}
