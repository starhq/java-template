package com.github.starhq.template.config.messages;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

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