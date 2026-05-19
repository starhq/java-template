package com.github.starhq.template.controller.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.TestConstant;
import com.github.starhq.template.config.WebConfiguration;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.controller.AuditLogController;
import com.github.starhq.template.model.dto.auditlog.AuditLogPageRequest;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import com.github.starhq.template.service.AuditLogService;
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
    void queryAuditLogs_noSort_badRequest() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryAuditLogs_noOrder_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("asc", "")
                )
                .andExpect(status().isOk());
    }

    @Test
    void queryAuditLogs_Asc_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("asc", "asc")
                )
                .andExpect(status().isOk());
    }

    @Test
    void queryAuditLogs_Order_Fallback_Success() throws Exception {
        when(auditLogService.page(any(AuditLogPageRequest.class))).thenReturn(pageResult);

        mockMvc.perform(get(TestConstant.VERSION + "/audit-logs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "10")
                        .param("sort", "id")
                        .param("asc", "whatever")
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
