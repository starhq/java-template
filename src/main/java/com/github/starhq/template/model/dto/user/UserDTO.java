package com.github.starhq.template.model.dto.user;

import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.validation.StrongPassword;
import com.github.starhq.template.model.dto.SensitiveDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Set;

/**
 * Data Transfer Object for creating a new user.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserDTO extends SensitiveDTO {

    @Serial
    private static final long serialVersionUID = 60288L;

    /**
     * The username of the user. Must not be blank and must be between 3 and 20
     * characters.
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 3, max = 20, message = "{error.param.range}")
    private String username;

    /**
     * The password for the user. Must not be blank and must meet password strength
     * requirements.
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 8, max = 20, message = "{error.param.range}")
    @StrongPassword // Custom annotation for validating password strength
    private String password;

    // @NotBlank(message = "{error.param.blank}")
    private UserStatus status;

    /**
     * A list of role IDs assigned to the user.
     */
    private Set<Long> roleIds; // List of role IDs associated with the user
}
