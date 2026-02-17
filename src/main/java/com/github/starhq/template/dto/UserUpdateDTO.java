package com.github.starhq.template.dto;

import com.github.starhq.template.enums.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新DTO
 *
 * @author starhq
 */
@Data
public class UserUpdateDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 状态
     */
    private UserStatus status;

    /**
     * 密码（可选）
     */
    @Size(min = 6, max = 50, message = "密码长度必须在6-50之间")
    private String password;
}
