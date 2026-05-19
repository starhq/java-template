package com.github.starhq.template.service;

import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.DuplicateException;
import com.github.starhq.template.model.dto.user.ResetPasswordDTO;
import com.github.starhq.template.model.dto.user.UserDTO;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: 安全服务
 * @date 2026/3/24 12:12
 */
public interface AuthService extends UserDetailsService {

    /**
     * Registers a new user and assigns a default role.
     * This method is transactional to ensure consistency.
     *
     * @param userDto Data transfer object containing username and password
     * @return registered user with default role assigned
     * @throws DuplicateException if username already exists
     * @throws BusinessException  if default role is not configured or insertion
     *                            fails
     */
    UserDetails register(UserDTO userDto);

    /**
     * Allows currently authenticated user to reset their password.
     * Validates old password before performing the update.
     *
     * @param resetPasswordDto containing old and new password
     * @return true if password was successfully updated, false otherwise
     * @throws BadCredentialsException if user is not found or old password does not
     *                                 match
     */
    boolean resetPassword(ResetPasswordDTO resetPasswordDto);
}
