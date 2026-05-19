package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import com.github.starhq.template.model.dto.auditlog.AuditLogPageRequest;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    private AuditLogPageRequest pageInfo;

    @BeforeEach
    void setUp() {
        pageInfo = new AuditLogPageRequest();
        pageInfo.setPage(1L);
        pageInfo.setSize(10L);
    }

    // ==================== page 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success_WithAllConditions() {
        // Given: 带上所有查询条件
        pageInfo.setTargetType(TargetType.RESOURCE); // 假设这是个枚举
        pageInfo.setUsername("admin");

        IPage<AuditLogPageVO> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(new AuditLogPageVO()));
        when(auditLogMapper.selectAuditLogPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockDbPage);

        // When
        IPage<AuditLogPageVO> result = auditLogService.page(pageInfo);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(auditLogMapper).selectAuditLogPage(any(Page.class), any(QueryWrapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success_WithoutTargetType() {
        // Given: 只带用户名，不带 TargetType
        pageInfo.setTargetType(null);
        pageInfo.setUsername("admin");

        IPage<AuditLogPageVO> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(new AuditLogPageVO()));
        when(auditLogMapper.selectAuditLogPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockDbPage);

        // When
        IPage<AuditLogPageVO> result = auditLogService.page(pageInfo);

        // Then
        assertNotNull(result);
        verify(auditLogMapper).selectAuditLogPage(any(Page.class), any(QueryWrapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success_WithoutUsername() {
        // Given: 只带 TargetType，不带用户名
        pageInfo.setTargetType(TargetType.RESOURCE);
        pageInfo.setUsername(null); // 或者是空字符串 ""

        IPage<AuditLogPageVO> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(new AuditLogPageVO()));
        when(auditLogMapper.selectAuditLogPage(any(Page.class), any(QueryWrapper.class))).thenReturn(mockDbPage);

        // When
        IPage<AuditLogPageVO> result = auditLogService.page(pageInfo);

        // Then
        assertNotNull(result);
        verify(auditLogMapper).selectAuditLogPage(any(Page.class), any(QueryWrapper.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success_WithNoConditions() {
        // Given: 不带任何查询条件
        pageInfo.setTargetType(null);
        pageInfo.setUsername(null);

        IPage<AuditLogPageVO> emptyPage = new Page<>(1, 10, 0);
        when(auditLogMapper.selectAuditLogPage(any(Page.class), any(QueryWrapper.class))).thenReturn(emptyPage);

        // When
        IPage<AuditLogPageVO> result = auditLogService.page(pageInfo);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
        verify(auditLogMapper).selectAuditLogPage(any(Page.class), any(QueryWrapper.class));
    }
}
