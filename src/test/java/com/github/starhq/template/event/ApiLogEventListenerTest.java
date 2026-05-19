package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.event.listener.ApiLogEventListener;
import com.github.starhq.template.mapper.SysApiLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiLogEventListenerTest {

    @Mock
    private SysApiLogMapper apiLogMapper;

    @InjectMocks
    private ApiLogEventListener eventListener;

    @Test
    void handleEvictEvent_Success() {
        // Given: 准备正常的数据
        SysApiLog mockLog = new SysApiLog();
        mockLog.setTraceId("trace-123");
        ApiLogEvent event = new ApiLogEvent(mockLog);

        // When
        eventListener.handleEvictEvent(event);

        // Then: 验证 mapper 的 insert 被调用了一次
        verify(apiLogMapper, times(1)).insert(mockLog);
    }

    @Test
    void handleEvictEvent_Fail_EventIsNull() {
        // Given: 传入 null
        // When
        eventListener.handleEvictEvent(null);

        // Then: 验证 mapper 绝对没有被调用（覆盖 if 分支）
        verify(apiLogMapper, never()).insert(any(SysApiLog.class));
    }

    @Test
    void handleEvictEvent_Fail_DbException() {
        // Given: 准备数据，但模拟数据库插入抛出异常
        SysApiLog mockLog = new SysApiLog();
        ApiLogEvent event = new ApiLogEvent(mockLog);

        // 模拟数据库报错
        doThrow(new RuntimeException("Connection lost")).when(apiLogMapper).insert(any(SysApiLog.class));

        // When: 调用方法
        // 注意：因为代码里有 try-catch，所以这里必须用 assertDoesNotThrow
        // 如果代码写错了没有 catch，这里就会报错导致测试失败（这正是我们想要验证的）
        assertDoesNotThrow(() -> eventListener.handleEvictEvent(event));

        // Then: 虽然报错了，但 insert 确实被调用过了（覆盖 try 块正常进入，catch 块成功兜底）
        verify(apiLogMapper, times(1)).insert(mockLog);
    }
}