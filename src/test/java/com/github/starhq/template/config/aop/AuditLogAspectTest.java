package com.github.starhq.template.config.aop;

import com.github.starhq.template.aop.aspect.AuditLogAspect;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.event.EventService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAspectTest {

    @Mock
    private EventService eventService;

    private AuditLogAspect auditLogAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Captor
    private ArgumentCaptor<SysAuditLog> auditLogCaptor;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = new JsonMapper();
        auditLogAspect = new AuditLogAspect(eventService, jsonMapper);
    }

    // =========================================================
    // SUCCESS CASE
    // =========================================================

    @Test
    void logAfterSuccess_ShouldSaveAuditLog() throws Exception {

        // Arrange
        TestDTO dto = new TestDTO();
        dto.setId(100L);

        Method method = TestService.class
                .getMethod("create", TestDTO.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(methodSignature.getParameterNames())
                .thenReturn(new String[]{"dto"});

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{dto});

        // Act
        auditLogAspect.logAfterSuccess(joinPoint);

        // Assert
        verify(eventService)
                .notifyAuditLogSave(auditLogCaptor.capture());

        SysAuditLog auditLog = auditLogCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                "USER_CREATE",
                auditLog.getAction()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                TargetType.USER,
                auditLog.getTargetType()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                100L,
                auditLog.getTargetId()
        );
    }

    // =========================================================
    // REMOVE METHOD SHOULD USE FIRST ARG AS ID
    // =========================================================

    @Test
    void logAfterSuccess_RemoveMethod_ShouldExtractFirstArgAsId()
            throws Exception {

        Method method = TestService.class
                .getMethod("remove", Long.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(methodSignature.getParameterNames())
                .thenReturn(new String[]{"id"});

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{999L});

        auditLogAspect.logAfterSuccess(joinPoint);

        verify(eventService)
                .notifyAuditLogSave(auditLogCaptor.capture());

        SysAuditLog auditLog = auditLogCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                999L,
                auditLog.getTargetId()
        );
    }

    // =========================================================
    // UPDATE METHOD SHOULD USE FIRST ARG AS ID
    // =========================================================

    @Test
    void logAfterSuccess_UpdateMethod_ShouldExtractFirstArgAsId()
            throws Exception {

        Method method = TestService.class
                .getMethod("update", Long.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(methodSignature.getParameterNames())
                .thenReturn(new String[]{"id"});

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{999L});

        auditLogAspect.logAfterSuccess(joinPoint);

        verify(eventService)
                .notifyAuditLogSave(auditLogCaptor.capture());

        SysAuditLog auditLog = auditLogCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                999L,
                auditLog.getTargetId()
        );
    }

    // =========================================================
    // NO ANNOTATION -> SHOULD DO NOTHING
    // =========================================================

    @Test
    void logAfterSuccess_NoAnnotation_ShouldDoNothing()
            throws Exception {

        Method method = TestService.class
                .getMethod("noAudit");

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{});

        auditLogAspect.logAfterSuccess(joinPoint);

        verify(eventService, never())
                .notifyAuditLogSave(any());
    }

    // =========================================================
    // NO Argument -> SHOULD DO NOTHING
    // =========================================================

    @Test
    void logAfterSuccess_NoArg_ShouldDoNothing()
            throws Exception {

        Method method = TestService.class
                .getMethod("updateWithNoArg");

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{});

        auditLogAspect.logAfterSuccess(joinPoint);

        verify(eventService, never())
                .notifyAuditLogSave(any());
    }

    @Test
    void logAfterSuccess_Null_Arg_ShouldDoNothing()
            throws Exception {

        Method method = TestService.class
                .getMethod("updateWithNoArg");

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(joinPoint.getArgs())
                .thenReturn(null);

        auditLogAspect.logAfterSuccess(joinPoint);

        verify(eventService, never())
                .notifyAuditLogSave(any());
    }

    // =========================================================
    // EVENT FAILURE SHOULD NOT BREAK BUSINESS
    // =========================================================

    @Test
    void logAfterSuccess_WhenEventFails_ShouldNotThrow()
            throws Exception {

        TestDTO dto = new TestDTO();
        dto.setId(1L);

        Method method = TestService.class
                .getMethod("create", TestDTO.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(methodSignature.getParameterNames())
                .thenReturn(new String[]{"dto"});

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{dto});

        doThrow(new RuntimeException("boom"))
                .when(eventService)
                .notifyAuditLogSave(any());

        assertDoesNotThrow(() ->
                auditLogAspect.logAfterSuccess(joinPoint)
        );
    }

    // =========================================================
    // DTO WITHOUT getId() SHOULD RETURN NULL
    // =========================================================

    @Test
    void logAfterSuccess_DtoWithoutId_ShouldUseNullId()
            throws Exception {

        NoIdDTO dto = new NoIdDTO();

        Method method = TestService.class
                .getMethod("createNoId", NoIdDTO.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);

        when(methodSignature.getMethod()).thenReturn(method);

        when(methodSignature.getParameterNames())
                .thenReturn(new String[]{"dto"});

        when(joinPoint.getArgs())
                .thenReturn(new Object[]{dto});

        auditLogAspect.logAfterSuccess(joinPoint);

        verify(eventService)
                .notifyAuditLogSave(auditLogCaptor.capture());

        SysAuditLog auditLog = auditLogCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertNull(
                auditLog.getTargetId()
        );
    }

}
