package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.controller.DictTypeController;
import com.github.starhq.template.model.dto.dict.type.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.dict.type.DictTypePageVO;
import com.github.starhq.template.model.vo.dict.type.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dict.type.DictTypeWithDataVO;
import com.github.starhq.template.service.DictTypeService;
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
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;


/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/15 10:59
 */
@Import(TestConfig.class)
@WebMvcTest(DictTypeController.class)
@AutoConfigureMockMvc(addFilters = false)
class DictTypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @MockitoBean
    private DictTypeService dictTypeService;
    @MockitoBean
    private MessageUtils messageUtils;

    private static final Long TEST_DICT_TYPE_ID = 1L;
    private static final String TEST_DICT_TYPE_NAME = "test-type";
    private static final String TEST_DICT_TYPE_CODE = "TEST_TYPE";

    private IPage<DictTypePageVO> pageResult;
    private DictTypeSimpleVO dictTypeResponse;
    private DictTypeDTO request;

    @BeforeEach
    void setUp() {
        // Initialize dict type response
        DictTypePageVO response = new DictTypePageVO();
        response.setId(TEST_DICT_TYPE_ID);
        response.setName(TEST_DICT_TYPE_NAME);
        response.setType(TEST_DICT_TYPE_CODE);

        // Initialize page result
        pageResult = new Page<DictTypePageVO>(1, 10)
                .setRecords(Collections.singletonList(response))
                .setTotal(1);

        // Initialize simple dict type response
        dictTypeResponse = new DictTypeSimpleVO();
        dictTypeResponse.setId(TEST_DICT_TYPE_ID);
        dictTypeResponse.setName(TEST_DICT_TYPE_NAME);
        dictTypeResponse.setType(TEST_DICT_TYPE_CODE);

        // Initialize create request
        request = new DictTypeDTO();
        request.setName(TEST_DICT_TYPE_NAME);
        request.setType(TEST_DICT_TYPE_CODE);
    }

    @Test
    void create_Success() throws Exception {
        when(dictTypeService.createDictType(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/dict-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void update_Success() throws Exception {
        when(dictTypeService.updateDictType(TEST_DICT_TYPE_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/dict-types/" + TEST_DICT_TYPE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(dictTypeService.removeById(TEST_DICT_TYPE_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/dict-types/" + TEST_DICT_TYPE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryDictTypes_Success() throws Exception {
        when(dictTypeService.page(any(PageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/dict-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(TEST_DICT_TYPE_ID))
                .andExpect(jsonPath("$.data[0].name").value(TEST_DICT_TYPE_NAME))
                .andExpect(jsonPath("$.data[0].type").value(TEST_DICT_TYPE_CODE));
    }

    @Test
    void queryDictTypeById_Success() throws Exception {
        when(dictTypeService.getDictDataById(TEST_DICT_TYPE_ID)).thenReturn(dictTypeResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/dict-types/" + TEST_DICT_TYPE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_DICT_TYPE_ID))
                .andExpect(jsonPath("$.data.name").value(TEST_DICT_TYPE_NAME))
                .andExpect(jsonPath("$.data.type").value(TEST_DICT_TYPE_CODE));
    }

    @Test
    void queryDictTypeAndData_Success() throws Exception {
        // 准备测试数据
        DictTypeWithDataVO response = new DictTypeWithDataVO();
        response.setDictType(TEST_DICT_TYPE_CODE);
        List<DictTypeWithDataVO> responses = Collections.singletonList(response);

        when(dictTypeService.selectDictTypeAndDataResponses()).thenReturn(responses);

        mockMvc.perform(get(TestConstant.VERSION + "/dict-types/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].dictType").value(TEST_DICT_TYPE_CODE));
    }
}
