package com.github.starhq.template.config.messages;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * Configuration class responsible for setting up internationalization (i18n) components.
 *
 * <p>Bridges Spring's {@link MessageSource} with the JSR-380 (Jakarta Validation) framework
 * and provides a customized utility bean for resolving error messages dynamically.
 *
 * @author starhq
 */
@Configuration
public class MessageConfiguration {

    /**
     * Configures the JSR-380 Validator to use the Spring {@link MessageSource} for message resolution.
     *
     * <p>By default, Hibernate Validator (the standard JSR-380 implementation) looks for validation
     * messages in {@code ValidationMessages.properties}. This configuration overrides that behavior,
     * instructing it to look in Spring's standard {@code messages.properties} (or {@code messages_xx.properties})
     * instead. This allows you to unify all application texts (both business errors and validation errors)
     * under one i18n management system.
     *
     * <p><b>TODO Verification Note:</b> Whether this bean is strictly necessary depends on your
     * Spring Boot version and Jackson setup:
     * <ul>
     *   <li>In modern Spring Boot 3.x, if you are using standard {@code spring-boot-starter-validation}
     *       and a custom {@link org.springframework.context.support.ReloadableResourceBundleMessageSource},
     *       Spring Boot often auto-wires the validator to use the primary MessageSource anyway.</li>
     *   <li>However, if you find that your validation messages (e.g., {@code {error.password.weak}})
     *       are not being resolved and are falling back to raw keys, this explicit bean definition
     *       is the guaranteed fix.</li>
     * </ul>
     *
     * @param messageSource the centralized Spring MessageSource bean containing the i18n properties
     * @return a configured {@link LocalValidatorFactoryBean} instance
     */
    @Bean
    LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        // Link the Validator to Spring's i18n system instead of Hibernate Validator's default path
        validatorFactoryBean.setValidationMessageSource(messageSource);

        return validatorFactoryBean;
    }

    /**
     * Creates the central utility bean for constructing localized API error responses.
     *
     * <p>This utility is injected into the {@link com.github.starhq.template.common.exception.GlobalExceptionHandler}
     * and AOP aspects to translate {@link com.github.starhq.template.common.enums.ErrorCode} enums
     * into human-readable strings based on the client's {@code Accept-Language} header.
     *
     * @param messageSource the source used to look up the raw i18n message templates
     * @param environment   the Spring Environment, typically used to detect the active profile
     *                      (e.g., falling back to English if no specific locale file matches)
     * @return a fully initialized {@link MessageUtils} instance
     */
    @Bean
    MessageUtils messageUtils(MessageSource messageSource, Environment environment) {
        return new MessageUtils(messageSource, environment);
    }
}