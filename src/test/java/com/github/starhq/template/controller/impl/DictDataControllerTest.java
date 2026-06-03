package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.controller.DictDataController;
import com.github.starhq.template.model.dto.DictDataDTO;
import com.github.starhq.template.model.dto.DictDataPageRequest;
import com.github.starhq.template.model.vo.DictDataPageVO;
import com.github.starhq.template.model.vo.DictDataSimpleVO;
import com.github.starhq.template.service.DictDataService;
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
 * @date 2026/5/15 10:44
 */
@Import(TestConfig.class)
@WebMvcTest(DictDataController.class)
@AutoConfigureMockMvc(addFilters = false)
class DictDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @MockitoBean
    private MessageUtils messageUtils;
    @MockitoBean
    private DictDataService dictDataService;

    private static final Long TEST_DICT_DATA_ID = 1L;
    private static final String TEST_DICT_DATA_LABEL = "test-label";
    private static final String TEST_DICT_DATA_VALUE = "test-value";
    private static final Long TEST_DICT_TYPE_ID = 1L;

    private DictDataPageRequest pageRequest;
    private IPage<DictDataPageVO> pageResult;
    private DictDataSimpleVO dictDataResponse;
    private DictDataDTO request;

    @BeforeEach
    void setUp() {
        // Initialize page request
        pageRequest = new DictDataPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);

        // Initialize dict data response
        DictDataPageVO response = new DictDataPageVO();
        response.setId(TEST_DICT_DATA_ID);
        response.setLabel(TEST_DICT_DATA_LABEL);
        response.setValue(TEST_DICT_DATA_VALUE);
        response.setTypeId(TEST_DICT_TYPE_ID);

        // Initialize page result
        pageResult = new Page<DictDataPageVO>(1, 10)
                .setRecords(Collections.singletonList(response))
                .setTotal(1);

        // Initialize simple dict data response
        dictDataResponse = new DictDataSimpleVO();
        dictDataResponse.setId(TEST_DICT_DATA_ID);
        dictDataResponse.setLabel(TEST_DICT_DATA_LABEL);
        dictDataResponse.setValue(TEST_DICT_DATA_VALUE);
        dictDataResponse.setTypeId(TEST_DICT_TYPE_ID);

        // Initialize create request
        request = new DictDataDTO();
        request.setLabel(TEST_DICT_DATA_LABEL);
        request.setValue(TEST_DICT_DATA_VALUE);
        request.setTypeId(TEST_DICT_TYPE_ID);

    }

    @Test
    void create_Success() throws Exception {
        when(dictDataService.createDictData(request)).thenReturn(true);

        mockMvc.perform(post(TestConstant.VERSION + "/dict-datas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void update_Success() throws Exception {
        when(dictDataService.updateDictData(TEST_DICT_DATA_ID, request)).thenReturn(true);

        mockMvc.perform(put(TestConstant.VERSION + "/dict-datas/" + TEST_DICT_DATA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_Success() throws Exception {
        when(dictDataService.removeById(TEST_DICT_DATA_ID)).thenReturn(true);

        mockMvc.perform(delete(TestConstant.VERSION + "/dict-datas/" + TEST_DICT_DATA_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void queryDictDatas_Success() throws Exception {
        when(dictDataService.page(pageRequest)).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/dict-datas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(TEST_DICT_DATA_ID))
                .andExpect(jsonPath("$.data[0].label").value(TEST_DICT_DATA_LABEL))
                .andExpect(jsonPath("$.data[0].value").value(TEST_DICT_DATA_VALUE))
                .andExpect(jsonPath("$.data[0].typeId").value(TEST_DICT_TYPE_ID));
    }

    @Test
    void queryDictDataById_Success() throws Exception {
        when(dictDataService.getDictDataById(TEST_DICT_DATA_ID)).thenReturn(dictDataResponse);

        mockMvc.perform(get(TestConstant.VERSION + "/dict-datas/" + TEST_DICT_DATA_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(TEST_DICT_DATA_ID))
                .andExpect(jsonPath("$.data.label").value(TEST_DICT_DATA_LABEL))
                .andExpect(jsonPath("$.data.value").value(TEST_DICT_DATA_VALUE))
                .andExpect(jsonPath("$.data.typeId").value(TEST_DICT_TYPE_ID));
    }
}

