package com.github.starhq.template.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.starhq.template.common.util.PasswordStrengthChecker;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 用于校验密码强度的注解。
 * 确保被注解的密码字符串满足指定的安全强度等级。
 * 默认要求密码强度至少为 STRONG (强)。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE }) // 增加 ANNOTATION_TYPE 支持元注解
@Constraint(validatedBy = PasswordStrengthValidator.class)
public @interface StrongPassword {

    /**
     * 校验失败时的错误消息键。
     * 默认读取 i18n 资源文件中的 validation.password.weak。
     */
    String message() default "{error.password.weak}";

    /**
     * 最低要求的密码强度等级。
     * 默认要求：STRONG (强)。
     * 
     * @return 密码强度的最低等级
     */
    PasswordStrengthChecker.StrengthLevel minLevel() default PasswordStrengthChecker.StrengthLevel.STRONG;

    /**
     * 校验分组。
     */
    Class<?>[] groups() default {};

    /**
     * 校验负载信息。
     */
    Class<? extends Payload>[] payload() default {};
}