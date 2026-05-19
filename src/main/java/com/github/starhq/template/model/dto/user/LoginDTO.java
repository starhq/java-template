package com.github.starhq.template.model.dto.user;

import com.github.starhq.template.common.validation.StrongPassword;
import com.github.starhq.template.model.dto.SensitiveDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = false)
public class LoginDTO extends SensitiveDTO {
    @Serial
    private static final long serialVersionUID = -5231672189461799210L;
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 3, max = 20, message = "{error.param.range}")
    private String username;
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 8, max = 20, message = "{error.param.range}")
    @StrongPassword
    private String password;
    @NotBlank(message = "{error.param.blank}")
    private String captcha;
    @NotBlank(message = "{error.param.blank}")
    private String uuid;
}
