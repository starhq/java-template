package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.mvc.WebConfiguration;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.controller.AuditLogController;
import com.github.starhq.template.model.dto.AuditLogPageRequest;
import com.github.starhq.template.model.vo.AuditLogPageVO;
import com.github.starhq.template.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/14 10:31
 */
@WebMvcTest(AuditLogController.class)
@Import(WebConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageUtils messageUtils;
    @MockitoBean
    private AuditLogService auditLogService;

    private IPage<AuditLogPageVO> pageResult;

    @BeforeEach
    void setUp() {
        AuditLogPageVO response = new AuditLogPageVO();
        response.setId(1L);
        response.setCreator("testuser");

        pageResult = new Page<AuditLogPageVO>(1, 10)
                .setRecords(Collections.singletonList(response))
                .setTotal(1);
    }

    @Test
    void queryAuditLogs_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].creator").value("testuser"));
    }

    @Test
    void queryAuditLogs_noSort_success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "")
                )
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvSource({
            // 格式：参数1, 参数2, 参数3, 参数4 (对应下面的占位符)
            "1,       10,    id,       ''",           // 对应原来的 noOrder (asc为空字符串)
            "1,       10,    id,       asc",        // 对应原来的 Asc
            "1,       10,    id,       whatever"    // 对应原来的 Fallback
    })
    @DisplayName("查询审计日志: 不同的排序参数都应该成功")
    void queryAuditLogs_WithVariousSortParams_ShouldReturnSuccess(
            String page, String size, String sort, String asc) throws Exception {

        // Mock 行为（对所有传入的参数都生效）
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        // 执行请求（使用参数化传入的变量）
        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", page)
                        .param("size", size)
                        .param("sort", sort)
                        .param("asc", asc)
                )
                .andExpect(status().isOk());
    }

    @Test
    void queryAuditLogs_WithTargetType_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("targetType", "user")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].creator").value("testuser"));
    }

    @Test
    void queryAuditLogs_WithEmptyTargetType_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("targetType", "")
                )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].creator").value("testuser"));
    }

    @Test
    void queryAuditLogs_WithInvalidTargetType_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("targetType", "invalid")
                )
                .andExpect(status().isBadRequest())
                .andDo(print());
    }
}
