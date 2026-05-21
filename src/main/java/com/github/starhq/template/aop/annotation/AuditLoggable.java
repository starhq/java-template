package com.github.starhq.template.aop.annotation;

import com.github.starhq.template.common.enums.TargetType;

import java.lang.annotation.*;

/**
 * Marks a service method as requiring audit logging upon successful execution.
 *
 * <p>When applied to a method, the {@link com.github.starhq.template.aop.aspect.AuditLogAspect}
 * will intercept the call, extract the target entity ID, serialize the input parameters,
 * and asynchronously persist an audit log record.
 *
 * <p><b>Usage Example:</b>
 * <pre>{@code
 * @AuditLoggable(action = "CREATE_USER", targetType = TargetType.USER)
 * public void createUser(UserCreateDTO dto) { ... }
 * }</pre>
 *
 * @author wangj
 * @see com.github.starhq.template.aop.aspect.AuditLogAspect
 * @see TargetType
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditLoggable {

    /**
     * Specifies the specific business action being performed.
     *
     * <p>It is highly recommended to use uppercase constants (e.g., "CREATE_USER", "DELETE_ORDER")
     * for easier querying and filtering in the audit log management interface.
     *
     * @return the business action identifier
     */
    String action();

    /**
     * Specifies the type of target entity the operation is acting upon.
     *
     * <p>This helps categorize audit logs by domain objects (e.g., USER, ROLE, CONFIG).
     *
     * @return the target entity type enum
     */
    TargetType targetType();
}
