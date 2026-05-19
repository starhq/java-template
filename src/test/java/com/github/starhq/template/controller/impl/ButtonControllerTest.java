package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.controller.ButtonController;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;
import com.github.starhq.template.service.ButtonService;
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

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/15 10:27
 */
@Import(TestConfig.class)
@WebMvcTest(ButtonController.class)
@AutoConfigureMockMvc(addFilters = false)
class ButtonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private MessageUtils messageUtils;
    @MockitoBean
    private ButtonService buttonService;

    private static final Long TEST_BUTTON_ID = 1L;
    private static final String TEST_BUTTON_NAME = "test-button";
    private static final String TEST_BUTTON_CODE = "TEST_BUTTON";

    private ButtonPageRequest pageRequest;
    private IPage<ButtonPageVO> pageResult;
    private ButtonSimpleVO buttonResponse;
    private ButtonDTO request;

    @BeforeEach
    void setUp() {
        // Initialize page request
        pageRequest = new ButtonPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);

        // Initialize button response
        ButtonPageVO response = new ButtonPageVO();
        response.setId(TEST_BUTTON_ID);
        response.setName(TEST_BUTTON_NAME);
        response.setCode(TEST_BUTTON_CODE);

        // Initialize page result
        pageResult = new Page<ButtonPageVO>(1, 10)
                .setRecords(Collections.singletonList(response))
                .setTotal(1);

        // Initialize simple button response
        buttonResponse = new ButtonSimpleVO();
        buttonResponse.setId(TEST_BUTTON_ID);
        buttonResponse.setName(TEST_BUTTON_NAME);
        buttonResponse.setCode(TEST_BUTTON_CODE);

        // Initialize create request
        request = new ButtonDTO();
        request.setName(TEST_BUTTON_NAME);
        request.setCode(TEST_BUTTON_CODE);
        request.setMenuId(TEST_BUTTON_ID);

    }

    @Test
    void create_Success() throws Exception {
        when(buttonService.createButton(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/buttons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void update_Success() throws Exception {
        when(buttonService.updateButton(TEST_BUTTON_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/buttons/" + TEST_BUTTON_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(buttonService.removeById(TEST_BUTTON_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/buttons/" + TEST_BUTTON_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryButtons_Success() throws Exception {
        when(buttonService.page(pageRequest)).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/buttons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(TEST_BUTTON_ID))
                .andExpect(jsonPath("$.data[0].name").value(TEST_BUTTON_NAME))
                .andExpect(jsonPath("$.data[0].code").value(TEST_BUTTON_CODE));
    }

    @Test
    void queryButtonById_Success() throws Exception {
        when(buttonService.getButtonById(TEST_BUTTON_ID)).thenReturn(buttonResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/buttons/" + TEST_BUTTON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_BUTTON_ID))
                .andExpect(jsonPath("$.data.name").value(TEST_BUTTON_NAME))
                .andExpect(jsonPath("$.data.code").value(TEST_BUTTON_CODE));
    }
}
