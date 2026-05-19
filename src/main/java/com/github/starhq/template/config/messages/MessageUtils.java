package com.github.starhq.template.config.messages;

import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.model.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class MessageUtils {

    private final MessageSource messageSource;
    private final Environment environment;

    /**
     * Get internationalized message with fallback strategy
     */
    public String getMessage(String i18nKey, String defaultMessage, Object... args) {
        if (!StringUtils.hasText(i18nKey) && !StringUtils.hasText(defaultMessage)) {
            return "System error, please contact administrator";
        }

        Locale locale = LocaleContextHolder.getLocale();

        // 1. Try primary key
        if (StringUtils.hasText(i18nKey)) {
            try {
                return messageSource.getMessage(i18nKey, args, locale);
            } catch (NoSuchMessageException e) {
                log.debug("Message key '{}' not found, trying fallback...", i18nKey);
            }
        }

        // 2. Try default key as i18n key
        if (StringUtils.hasText(defaultMessage) && looksLikeI18nKey(defaultMessage)) {
            try {
                return messageSource.getMessage(defaultMessage, args, locale);
            } catch (NoSuchMessageException e) {
                log.debug("Fallback key '{}' not found", defaultMessage);
            }
        }

        // 3. Final fallback: format raw message
        return formatMessage(defaultMessage, args);
    }

    private boolean looksLikeI18nKey(String str) {
        return str != null && str.contains(".") && !str.contains(" ");
    }

    private String formatMessage(String pattern, Object... args) {
        if (!StringUtils.hasText(pattern) || ArrayUtils.isEmpty(args)) {
            return Objects.toString(pattern, "");
        }
        return MessageFormat.format(pattern.replace("'", "''"), args);
    }


    // ====================== Error Response Builders ======================

    public Result<Void> buildErrorResponse(ErrorCode errorCode, Object... args) {
        errorCode = Objects.requireNonNullElse(errorCode, ErrorCode.INTERNAL_ERROR);

        String message = getMessage(isProdProfile() ? mask(errorCode.getCode()) : errorCode.getI18nKey(), errorCode.getDefaultMessage(), args);
        return buildFinalResponse(errorCode.getCode(), message);
    }

    public Result<Void> buildErrorResponse(CustomException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getArgs());
    }

    public Result<Void> buildErrorResponse(Integer errorCode, String message) {
        return buildFinalResponse(errorCode, message);
    }

    private Result<Void> buildFinalResponse(Integer errorCode, String message) {
        return Result.fail(errorCode, message);
    }

    private String mask(Integer code) {
        if (code >= 10000 && code < 20000) {
            return "error.external";
        } else if (code >= 2000 && code < 21000) {
            return "error.auth.unauthorized";
        } else if (code == 21000) {
            return "error.auth.forbidden";
        } else if (code >= 30000 && code < 90000) {
            return "error.business";
        } else {
            return "error.internal";
        }
    }

    private boolean isProdProfile() {
        return environment.acceptsProfiles(Profiles.of(ProfileConstants.PROD));
    }
}
