package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.controller.TokenController;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.vo.token.TokenPageVO;
import com.github.starhq.template.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/15 14:08
 */
@Import(TestConfig.class)
@WebMvcTest(TokenController.class)
@AutoConfigureMockMvc(addFilters = false)
class TokenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private MessageUtils messageUtils;

    private static final Long TEST_TOKEN_ID = 1L;
    private static final String LOGIN_IP = "127.0.0.1";
    private KeyWordPageRequest pageRequest;
    private IPage<TokenPageVO> pageResult;

    @BeforeEach
    void setUp() {
        // Initialize page request
        pageRequest = new KeyWordPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);

        // Initialize token response
        TokenPageVO tokenResponse = new TokenPageVO();
        tokenResponse.setId(TEST_TOKEN_ID);
        tokenResponse.setLoginIp(LOGIN_IP);

        // Initialize page result
        pageResult = new Page<TokenPageVO>(1, 10)
                .setRecords(Collections.singletonList(tokenResponse))
                .setTotal(1);
    }

    @Test
    void revoked_Success() throws Exception {
        Long userId = 100L;
        when(tokenService.removeByUserId(userId)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/tokens/" + userId + "/revoked"))
                .andExpect(status().isOk());
    }

    @Test
    void queryTokens_Success() throws Exception {
        when(tokenService.page(pageRequest)).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/tokens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(TEST_TOKEN_ID))
                .andExpect(jsonPath("$.data[0].loginIp").value(LOGIN_IP));
    }
}
