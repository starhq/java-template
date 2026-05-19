package com.github.starhq.template.config.messages;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@Configuration
public class MessageConfiguration {

    /**
     * Sets up the ValidatorFactoryBean with the custom message source.
     * This enables validation messages to be resolved from the configured message
     * bundles.
     * 
     * @param messageSource the MessageSource bean to use for resolving validation
     *                      messages
     * @return an instance of LocalValidatorFactoryBean configured with the message
     *         source
     */
    // todo 可能不需要,待验证
    @Bean
    LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();

        // Integrate the message source to support internationalization of validation
        // messages
        validatorFactoryBean.setValidationMessageSource(messageSource);

        return validatorFactoryBean;
    }

    @Bean
    MessageUtils messageUtils(MessageSource messageSource, Environment environment) {
        return new MessageUtils(messageSource, environment);
    }
}
