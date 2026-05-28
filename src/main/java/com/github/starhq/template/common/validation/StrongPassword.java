package com.github.starhq.template.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.starhq.template.common.util.PasswordStrengthChecker;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Marks a field or parameter as requiring a minimum level of password strength.
 *
 * <p>When applied to a String field, the Jakarta Validation framework will invoke the
 * {@link PasswordStrengthValidator} to evaluate the password against the heuristic algorithm
 * in {@link PasswordStrengthChecker}.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * public class UserCreateDTO {
 *     @StrongPassword(minLevel = PasswordStrengthChecker.StrengthLevel.VERY_STRONG)
 *     private String password;
 * }
 * }</pre>
 *
 * @see PasswordStrengthValidator
 * @see PasswordStrengthChecker.StrengthLevel
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Constraint(validatedBy = PasswordStrengthValidator.class)
public @interface StrongPassword {

    /**
     * The error message template to be used when the password fails the strength check.
     *
     * <p>This key is resolved by the {@code MessageSource} (e.g., in {@code ValidationMessages.properties}).
     * If the key is not found, the raw string itself is returned to the client.
     *
     * @return the i18n error message key
     */
    String message() default "{error.password.weak}";

    /**
     * Specifies the minimum acceptable strength level for the password.
     *
     * <p>Defaults to {@link PasswordStrengthChecker.StrengthLevel#STRONG} to enforce
     * a high baseline of security out-of-the-box.
     *
     * @return the required minimum {@link PasswordStrengthChecker.StrengthLevel}
     */
    PasswordStrengthChecker.StrengthLevel minLevel() default PasswordStrengthChecker.StrengthLevel.MEDIUM;

    /**
     * Allows specifying validation groups to which this constraint belongs.
     *
     * <p>This is used to apply different validation rules in different scenarios
     * (e.g., applying stricter password rules during creation, but skipping the check during updates
     * if the password field is null).
     *
     * @return the array of validation group classes
     */
    Class<?>[] groups() default {};

    /**
     * Payloads can be attached to a constraint declaration to be carried along by the validation framework.
     *
     * <p>This is typically ignored in standard application logic but can be utilized by heavy-weight
     * validation frameworks or client-side metadata generators to associate custom metadata
     * (like severity levels) with the constraint.
     *
     * @return the array of payload classes
     */
    Class<? extends Payload>[] payload() default {};
}