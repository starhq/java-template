package com.github.starhq.template.common.validation;

import com.github.starhq.template.common.util.PasswordStrengthChecker;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * JSR-380 (Jakarta Validation) validator responsible for enforcing password strength constraints.
 *
 * <p>This class bridges the custom {@link StrongPassword} annotation with the underlying
 * {@link PasswordStrengthChecker} algorithm. It is automatically instantiated and managed
 * by the validation framework (e.g., Hibernate Validator) when the annotation is used.
 *
 * @author starhq
 * @see StrongPassword
 * @see PasswordStrengthChecker
 */
public class PasswordStrengthValidator implements ConstraintValidator<StrongPassword, String> {

    /**
     * The minimum acceptable strength level extracted from the annotation.
     */
    private PasswordStrengthChecker.StrengthLevel minLevel;

    /**
     * Initializes the validator instance.
     *
     * <p>This method is invoked by the validation framework exactly once, when the validator
     * is instantiated, allowing us to extract configuration parameters from the annotation.
     *
     * @param constraintAnnotation the annotation instance applied to the DTO field
     */
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        this.minLevel = constraintAnnotation.minLevel();
    }

    /**
     * Evaluates whether the provided password meets the minimum strength requirement.
     *
     * <p><b>Validation Logic:</b> The method first handles the null/empty edge case. Then, it
     * calculates the actual strength of the password and compares it against the configured minimum.
     *
     * <p><b>Enum Comparison Note:</b> It uses {@code ordinal()} for comparison because the
     * {@link PasswordStrengthChecker.StrengthLevel} enum is strictly ordered from weakest
     * to strongest (EASY=0, MEDIUM=1, etc.). Comparing ordinals is a standard, highly efficient
     * way to evaluate "greater than or equal to" logic for strictly ordered enums.
     *
     * @param password the password string to evaluate (can be null if the field is not required)
     * @param context  the Jakarta Validation context, used to build custom violation messages
     *                 if the default message is insufficient
     * @return {@code true} if the password is valid and meets the strength threshold, {@code false} otherwise
     */
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Delegate null/empty checks to Spring's utility (handles both null and empty strings safely)
        if (!StringUtils.hasText(password)) {
            return true;
        }

        // Evaluate the password against the heuristic algorithm
        PasswordStrengthChecker.StrengthLevel actualLevel = PasswordStrengthChecker.getStrengthLevel(password);

        // Compare enum order: actual (e.g., STRONG=2) must be >= minimum (e.g., MEDIUM=1)
        return actualLevel.ordinal() >= minLevel.ordinal();
    }
}