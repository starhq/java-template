package com.github.starhq.template.event;

import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.event.listener.AuditLogListener;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogListenerTest {

    @Mock
    private SysAuditLogMapper auditLogMapper;

    @InjectMocks
    private AuditLogListener auditLogListener;

    @Test
    void handleEvictEvent_Success() {
        // Given: 准备正常的审计日志数据
        SysAuditLog mockLog = new SysAuditLog();
        mockLog.setAction("USER_LOGIN");
        AuditLogEvent event = new AuditLogEvent(mockLog);

        // When
        auditLogListener.handleEvictEvent(event);

        // Then: 验证底层的 Mapper 被正确调用了一次
        verify(auditLogMapper, times(1)).insert(mockLog);
    }

    @Test
    void handleEvictEvent_Fail_EventIsNull() {
        // Given: 传入 null 事件
        // When
        auditLogListener.handleEvictEvent(null);

        // Then: 验证防御性编程生效，Mapper 绝对没有被调用
        verify(auditLogMapper, never()).insert(any(SysAuditLog.class));
    }

    @Test
    void handleEvictEvent_Fail_DbException() {
        // Given: 准备数据，但模拟数据库插入时抛出异常
        SysAuditLog mockLog = new SysAuditLog();
        AuditLogEvent event = new AuditLogEvent(mockLog);

        // 模拟数据库异常 (如主键冲突、连接断开等)
        doThrow(new RuntimeException("Connection timed out")).when(auditLogMapper).insert(any(SysAuditLog.class));

        // When & Then:
        // 使用 assertDoesNotThrow 确保方法内部的 try-catch 真的兜住了异常，没有抛给调用方
        assertDoesNotThrow(() -> auditLogListener.handleEvictEvent(event));

        // 验证虽然发生了异常，但 insert 操作确实被触发执行了
        verify(auditLogMapper, times(1)).insert(mockLog);
    }
}