package com.github.starhq.template.aop.aspect;

import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.util.TypeConvertUtils;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.event.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.util.ReflectionUtils;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aspect for logging auditable service method calls.
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class AuditLogAspect {

    private final EventService eventService;
    private final JsonMapper jsonMapper;

    /**
     * Pointcut that matches:
     * - Any method in the service package
     * - Annotated with @AuditLoggable OR inside a class annotated with it
     */
    @Pointcut("execution(* com.github.starhq.template.service..*.*(..)) && " + "(@annotation(com.github.starhq.template.aop.annotation.AuditLoggable) || " + "within(@com.github.starhq.template.aop.annotation.AuditLoggable *))")
    public void auditableMethods() {
    }

    /**
     * Logs audit information after successful execution of matched methods.
     */
    @AfterReturning("auditableMethods()")
    public void logAfterSuccess(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Get the annotation, return early if not present
        AuditLoggable auditLoggable = method.getAnnotation(AuditLoggable.class);
        if (auditLoggable == null) {
            return;
        }

        try {
            Long targetId = extractTargetId(method, args);

            // Build and save audit log entry
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setAction(auditLoggable.action());
            auditLog.setTargetType(auditLoggable.targetType());
            auditLog.setTargetId(targetId);
            auditLog.setValue(buildParamString(signature.getParameterNames(), args));

            eventService.notifyAuditLogSave(auditLog);
        } catch (Exception e) {
            log.warn("Failed to record audit log: method={}, msg={}", joinPoint.getSignature().toShortString(), e.getMessage(), e);
        }
    }

    /**
     * Extracts the ID of the entity being acted upon.
     * - If method starts with "remove", assumes ID is first argument.
     * - Otherwise, tries to invoke `getId()` on the first argument.
     *
     * @param method the intercepted method
     * @param args   method arguments
     * @return extracted ID or null if not resolvable
     */
    private Long extractTargetId(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        Object firstArg = args[0];
        String methodName = method.getName();

        // 1. 处理删除场景 (通常 ID 作为第一个参数)
        if (methodName.startsWith("remove") || methodName.startsWith("update")) {
            return TypeConvertUtils.toLong(firstArg);
        }

        // 2. 处理 DTO 场景 (尝试调用 getId())
        // 使用反射工具类更安全
        Method getIdMethod = ReflectionUtils.findMethod(firstArg.getClass(), "getId");
        if (getIdMethod != null) {
            Object idValue = ReflectionUtils.invokeMethod(getIdMethod, firstArg);
            return TypeConvertUtils.toLong(idValue);
        }

        return null;
    }

    private String buildParamString(String[] paramNames, Object[] args) {
        Map<String, Object> paramMap = new LinkedHashMap<>();

        for (int i = 0; i < paramNames.length; i++) {
            paramMap.put(paramNames[i], args[i]);
        }
        return jsonMapper.writeValueAsString(paramMap);
    }
}
