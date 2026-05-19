package com.github.starhq.template.service.impl;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.mapper.SysApiLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/5/16 11:33
 */
@ExtendWith(MockitoExtension.class)
class ApiLogServiceImplTest {

    @Mock
    private SysApiLogMapper apiLogMapper;

    @InjectMocks
    private ApiLogServiceImpl apiLogService;

    private SysApiLog mockLog;

    @BeforeEach
    void setUp() {
        mockLog = new SysApiLog();
        mockLog.setClientIp("127.0.0.1");
        mockLog.setId(1L);
        mockLog.setTraceId("existing-trace");
        mockLog.setUri("/api/v1/test");
    }

    @Test
    void create() {
        when(apiLogMapper.insert(mockLog)).thenReturn(1);

        // When: 调用创建方法
        apiLogService.create(mockLog);

        // Then: 验证底层的 Mapper.insert 被精确调用了 1 次，且传入的是同一个对象
        verify(apiLogMapper, times(1)).insert(mockLog);
    }

    @Test
    void getByTraceId_Success() {
        // Given: 准备 traceId 和预期的返回对象
        String traceId = "existing-trace";


        // 模拟当调用 selectOne 时，返回 expectedLog
        // 注意：MyBatis Plus 的 LambdaQueryWrapper 是匿名内部类，很难用 eq() 精确匹配，用 any() 即可
        when(apiLogMapper.selectOne(any())).thenReturn(mockLog);

        // When: 调用查询方法
        SysApiLog actualLog = apiLogService.getByTraceId(traceId);

        // Then: 验证返回结果正确
        assertNotNull(actualLog);
        assertEquals(traceId, actualLog.getTraceId());
        assertEquals("/api/v1/test", actualLog.getUri());

        // 验证 Mapper 的查询方法被调用了一次
        verify(apiLogMapper, times(1)).selectOne(any());
    }

    @Test
    void getByTraceId_NotFound_ThrowsException() {
        // Given: 准备一个不存在的 traceId
        String traceId = "non-existing-trace";

        // 模拟数据库查不到数据，返回 null
        when(apiLogMapper.selectOne(any())).thenReturn(null);

        // When & Then: 预期抛出 NotFoundException
        NotFoundException exception = assertThrows(NotFoundException.class, () -> apiLogService.getByTraceId(traceId));

        // 可选：验证异常信息是否包含你预期的 ErrorCode (前提是你的 NotFoundException 支持获取 ErrorCode)
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());

        // 验证虽然抛了异常，但查询操作确实执行了
        verify(apiLogMapper, times(1)).selectOne(any());
    }
}
