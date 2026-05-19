package com.github.starhq.template.common.validation;

import org.springframework.util.StringUtils;

import com.github.starhq.template.common.util.PasswordStrengthChecker;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for checking the strength of passwords.
 */
public class PasswordStrengthValidator implements ConstraintValidator<StrongPassword, String> {

    private PasswordStrengthChecker.StrengthLevel minLevel; // Minimum required password strength level

    /**
     * Initializes the validator with the specified annotation.
     *
     * @param constraintAnnotation the annotation that contains the minimum level
     */
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLevel = constraintAnnotation.minLevel(); // Set the minimum level from annotation
    }

    /**
     * Validates the given password against the required strength level.
     *
     * @param password the password to validate
     * @param context  the context in which the validation is performed
     * @return true if the password meets the strength requirements; false otherwise
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Check if password is null or empty
        if (!StringUtils.hasText(password)) {
            return false; // Invalid if null or empty
        }

        // Get the actual strength level of the password
        PasswordStrengthChecker.StrengthLevel actualLevel = PasswordStrengthChecker.getStrengthLevel(password);
        // Compare the actual level with the minimum required level
        return actualLevel.ordinal() >= minLevel.ordinal();
    }
}
