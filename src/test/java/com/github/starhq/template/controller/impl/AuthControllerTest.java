package com.github.starhq.template.controller.impl;

import com.github.starhq.template.common.captcha.util.RandomUtil;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.controller.AuthController;
import com.github.starhq.template.model.dto.user.LoginDTO;
import com.github.starhq.template.model.dto.user.ResetPasswordDTO;
import com.github.starhq.template.service.AuthService;
import com.github.starhq.template.service.CaptchaService;
import com.github.starhq.template.service.LoginService;
import com.github.starhq.template.service.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/14 23:55
 */
@Import(TestConfig.class)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private MessageUtils messageUtils;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CaptchaService captchaService;

    @MockitoBean
    private HttpServletResponse response;

    private static final String DEVICE_FINGERPRINT = "test-device-fingerprint";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "Wj@64066195";
    private static final String TEST_TOKEN = "test-jwt-token";
    private static final String TEST_CAPTCHA = RandomUtil.randomLetters(5);
    private static final String TEST_UUID = UUID.randomUUID().toString();

    private JwtToken jwtToken;

    @BeforeEach
    void setUp() {
        jwtToken = JwtToken.builder()
                .accessToken(TEST_TOKEN).build();
    }

    @Test
    void login_Success() throws Exception {
        LoginDTO loginRequest = new LoginDTO();
        loginRequest.setUsername(TEST_USERNAME);
        loginRequest.setPassword(TEST_PASSWORD);
        loginRequest.setCaptcha(TEST_CAPTCHA);
        loginRequest.setUuid(TEST_UUID);

        when(loginService.login(loginRequest)).thenReturn(jwtToken);

        mockMvc.perform(post(TestConstant.VERSION + "/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", DEVICE_FINGERPRINT)
                        .content(jsonMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value(TEST_TOKEN));
    }


    @Test
    void refresh_Success() throws Exception {
        when(tokenService.refresh()).thenReturn(jwtToken);

        mockMvc.perform(post(TestConstant.VERSION + "/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Fingerprint", DEVICE_FINGERPRINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access_token").value(TEST_TOKEN));
    }

    @Test
    void resetPassword_Success() throws Exception {
        ResetPasswordDTO resetRequest = new ResetPasswordDTO();
        resetRequest.setOldPassword(TEST_PASSWORD);
        resetRequest.setNewPassword("Wj@64066197");

        when(authService.resetPassword(resetRequest)).thenReturn(true);

        mockMvc.perform(patch(TestConstant.VERSION + "/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(resetRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void resetCaptcha_Success() throws Exception {
        doNothing().when(captchaService).generateCode(TEST_UUID, response);

        mockMvc.perform(get(TestConstant.VERSION + "/auth/captcha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("uuid", TEST_UUID)
                )
                .andExpect(status().isOk());
    }
}
