package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.model.dto.LoginDTO;
import com.github.starhq.template.service.CaptchaService;
import com.github.starhq.template.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/16 15:34
 */
@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenService tokenService;
    @Mock
    private CaptchaService captchaService;
    @Mock
    private Authentication authentication;
    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private LoginServiceImpl loginService;

    private LoginDTO loginDTO;
    private JwtToken expectedJwtToken;

    @BeforeEach
    void setUp() {
        loginDTO = new LoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("123456");
        loginDTO.setUuid("test-uuid");
        loginDTO.setCaptcha("abcd");

        expectedJwtToken = JwtToken.builder()
                .accessToken("mock-access-token")
                .build();
    }

    @Test
    void login_Success() {
        // Given: 验证码通过，认证成功
        doNothing().when(captchaService).verify(anyString(), anyString());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(tokenService.build(userDetails)).thenReturn(expectedJwtToken);

        // When
        JwtToken result = loginService.login(loginDTO);

        // Then
        assertNotNull(result);
        assertEquals("mock-access-token", result.getAccessToken());

        // 验证调用链路
        verify(captchaService).verify("test-uuid", "abcd");
        verify(authenticationManager).authenticate(any());
        verify(tokenService).build(userDetails);
    }

    @Test
    void login_Fail_CaptchaInvalid() {
        // Given: 验证码校验失败，直接抛出异常
        doThrow(new BusinessException(ErrorCode.CAPTCHA_VERIFY))
                .when(captchaService).verify(anyString(), anyString());

        // When & Then
        assertThrows(BusinessException.class, () -> loginService.login(loginDTO));

        // 验证如果验证码都不对，根本不应该去调用认证器
        verify(authenticationManager, never()).authenticate(any());
        verify(tokenService, never()).build(any());
    }

    @Test
    void login_Fail_BadCredentials() {
        // Given: 验证码通过，但密码错误抛出 BadCredentialsException
        doNothing().when(captchaService).verify(anyString(), anyString());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> loginService.login(loginDTO));

        // 验证异常被精准转换为 CREDENTIALS 错误码，且状态码为 401
        assertEquals(ErrorCode.CREDENTIALS, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());

        verify(tokenService, never()).build(any());
    }

    @Test
    void login_Fail_AccountDisabled() {
        // Given: 验证码通过，但账号被禁用抛出 DisabledException
        doNothing().when(captchaService).verify(anyString(), anyString());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new DisabledException("User is disabled"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> loginService.login(loginDTO));

        // 验证异常被精准转换为 DISABLED 错误码
        assertEquals(ErrorCode.DISABLED, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }

    @Test
    void login_Fail_AccountLocked() {
        // Given: 验证码通过，但账号被锁定抛出 LockedException
        doNothing().when(captchaService).verify(anyString(), anyString());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new LockedException("User is locked"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> loginService.login(loginDTO));

        // LockedException 和 DisabledException 在你的代码里走的是同一个 catch 块
        assertEquals(ErrorCode.DISABLED, exception.getErrorCode());
    }

    @Test
    void login_Fail_OtherAuthenticationException() {
        // Given: 抛出其他未知的认证异常（比如 CredentialsExpiredException 等）
        doNothing().when(captchaService).verify(anyString(), anyString());
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.CredentialsExpiredException("Expired"));

        // When & Then
        CustomException exception = assertThrows(CustomException.class, () -> loginService.login(loginDTO));

        // 验证走了兜底的异常转换逻辑
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
    }
}
