package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.model.dto.user.LoginDTO;
import com.github.starhq.template.service.CaptchaService;
import com.github.starhq.template.service.LoginService;
import com.github.starhq.template.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/23 12:12
 */
@Service("loginService")
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final CaptchaService captchaService;

    @Override
    public JwtToken login(LoginDTO loginDTO) {
        // 1. 校验验证码 (失败抛出自定义 BusinessException)
        captchaService.verify(loginDTO.getUuid(), loginDTO.getCaptcha());

        // 2. 封装认证 Token
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword());
        Authentication authentication;
        try {
            // 3. 执行认证 (失败会抛出 BadCredentialsException, DisabledException 等)
            authentication = authenticationManager.authenticate(authenticationToken);
        } catch (BadCredentialsException e) {
            // ✅ 细化异常：密码错误
            throw new CustomException(ErrorCode.CREDENTIALS, HttpStatus.UNAUTHORIZED);
        } catch (DisabledException | LockedException e) {
            // ✅ 细化异常：账号被禁用
            throw new CustomException(ErrorCode.DISABLED, HttpStatus.UNAUTHORIZED);
        } catch (AuthenticationException e) {
            // ✅ 其他认证异常兜底
            throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        // 4. 认证成功，获取用户信息
        Object principal = authentication.getPrincipal();

        UserDetails userDetails = (UserDetails) principal;

        // 5. 生成 JWT 并保存会话
        return tokenService.build(userDetails);
    }
}
