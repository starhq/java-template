package com.github.starhq.template.model.dto.user;

import java.io.Serial;

import com.github.starhq.template.common.validation.StrongPassword;
import com.github.starhq.template.model.dto.SensitiveDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data Transfer Object for resetting a user's password.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResetPasswordDTO extends SensitiveDTO {

    @Serial
    private static final long serialVersionUID = 149698384L; // Serialization ID for this class

    /**
     * The user's current password. Must not be blank, must be between 8 and 20
     * characters,
     * and must meet the defined password strength requirements.
     */
    @NotBlank(message = "{error.param.blank}") // Validation for non-empty password
    @Size(min = 8, max = 20, message = "{error.param.range}") // Validation for password length
    @StrongPassword // Custom annotation for validating password strength
    private String oldPassword;

    /**
     * The user's new password. Must not be blank, must be between 8 and 20
     * characters,
     * and must meet the defined password strength requirements.
     */
    @NotBlank(message = "{error.param.blank}") // Validation for non-empty password
    @Size(min = 8, max = 20, message = "{error.param.range}") // Validation for password length
    @StrongPassword // Custom annotation for validating password strength
    private String newPassword;
}
