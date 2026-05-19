package com.github.starhq.template.service;

import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.model.dto.user.LoginDTO;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/23 11:43
 */
public interface LoginService {

    JwtToken login(LoginDTO loginDTO);
}
