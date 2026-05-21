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
 * Aspect responsible for intercepting service methods annotated with {@link AuditLoggable}
 * and generating corresponding audit logs upon successful execution.
 *
 * <p>This aspect uses Spring AOP to non-intrusively record user actions. It automatically
 * extracts the target entity ID based on naming conventions and serializes method arguments
 * into JSON format for comprehensive logging.
 *
 * @author wangj
 * @since 1.0.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class AuditLogAspect {

    /**
     * Service responsible for asynchronously publishing audit log events.
     */
    private final EventService eventService;

    /**
     * Mapper used to serialize method arguments into a safe JSON string.
     */
    private final JsonMapper jsonMapper;

    /**
     * Pointcut that matches service layer methods requiring audit logging.
     *
     * <p>The expression captures methods within the service package that are either directly
     * annotated with {@link AuditLoggable} or reside within a class annotated with it.
     * We check both because class-level annotations are not automatically propagated to
     * the method level by the JVM, requiring a fallback check in the advice body.
     */
    @Pointcut("execution(* com.github.starhq.template.service..*.*(..)) && " +
            "(@annotation(com.github.starhq.template.aop.annotation.AuditLoggable) || " +
            "within(@com.github.starhq.template.aop.annotation.AuditLoggable *))")
    public void auditableMethods() {
        // Pointcut marker method; implementation is intentionally empty.
    }

    /**
     * Advises matched methods after they return successfully to record the action.
     *
     * <p>This method retrieves the specific {@link AuditLoggable} annotation to obtain
     * action types (fallback is required if only class-level annotation exists).
     * It wraps the core logic in a try-catch block to ensure that failures in audit
     * logging never interrupt or rollback the primary business transaction.
     *
     * @param joinPoint the join point providing access to the intercepted method and its arguments
     */
    @AfterReturning("auditableMethods()")
    public void logAfterSuccess(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // Fallback check: The pointcut might match via class-level annotation,
        // but we need the method-level annotation to get specific action/targetType values.
        AuditLoggable auditLoggable = method.getAnnotation(AuditLoggable.class);
        if (auditLoggable == null) {
            return;
        }

        try {
            Long targetId = extractTargetId(method, args);

            // Construct the audit log entity
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setAction(auditLoggable.action());
            auditLog.setTargetType(auditLoggable.targetType());
            auditLog.setTargetId(targetId);
            auditLog.setValue(buildParamString(signature.getParameterNames(), args));

            // Delegate persistence to the event service (usually async)
            eventService.notifyAuditLogSave(auditLog);
        } catch (Exception e) {
            // Fail-safe: Log the error but do not re-throw to prevent business failure
            log.warn("Failed to record audit log: method={}, msg={}", joinPoint.getSignature().toShortString(), e.getMessage(), e);
        }
    }

    /**
     * Extracts the primary key of the entity being acted upon based on method naming conventions.
     *
     * <p>The extraction strategy is as follows:
     * <ul>
     *   <li>If the method name starts with "remove" or "update", it assumes the first argument
     *       is directly the ID (e.g., {@code removeById(Long id)}).</li>
     *   <li>Otherwise, it assumes the first argument is a DTO/Entity and attempts to invoke
     *       its {@code getId()} method via reflection.</li>
     * </ul>
     *
     * @param method the intercepted method, used to determine the extraction strategy
     * @param args   the runtime arguments passed to the intercepted method
     * @return the extracted ID as a Long, or {@code null} if it cannot be resolved
     */
    private Long extractTargetId(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }

        Object firstArg = args[0];
        String methodName = method.getName();

        // Strategy 1: For destructive/modify operations, the ID is usually passed directly
        if (methodName.startsWith("remove") || methodName.startsWith("update")) {
            return TypeConvertUtils.toLong(firstArg);
        }

        // Strategy 2: For create/query operations, the ID is usually inside the DTO object
        Method getIdMethod = ReflectionUtils.findMethod(firstArg.getClass(), "getId");
        if (getIdMethod != null) {
            Object idValue = ReflectionUtils.invokeMethod(getIdMethod, firstArg);
            return TypeConvertUtils.toLong(idValue);
        }

        return null;
    }

    /**
     * Serializes method parameters into a structured JSON string.
     *
     * <p>Uses a {@link LinkedHashMap} to preserve the exact order of parameters as declared
     * in the method signature, which makes the resulting JSON much easier to read in logs.
     *
     * @param paramNames the names of the parameters declared in the method signature
     * @param args       the actual values passed at runtime
     * @return a JSON string representing the key-value map of parameters
     */
    private String buildParamString(String[] paramNames, Object[] args) {
        Map<String, Object> paramMap = new LinkedHashMap<>();

        for (int i = 0; i < paramNames.length; i++) {
            paramMap.put(paramNames[i], args[i]);
        }
        return jsonMapper.writeValueAsString(paramMap);
    }
}