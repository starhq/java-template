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

/**
 * Centralized utility for resolving internationalized (i18n) messages and constructing standardized API error responses.
 *
 * <p>This class bridges the gap between raw internal error codes and user-friendly localized messages,
 * implementing a robust fallback strategy to ensure the API never returns raw system keys to the client.
 *
 * @author starhq
 */
@Slf4j
@RequiredArgsConstructor
public class MessageUtils {

    /**
     * Spring's interface for accessing i18n message bundles (e.g., messages.properties).
     */
    private final MessageSource messageSource;

    /**
     * Spring Environment, used to detect active profiles to toggle security behaviors.
     */
    private final Environment environment;

    /**
     * Resolves an internationalized message using a sophisticated three-tier fallback strategy.
     *
     * <p><b>Fallback Chain:</b>
     * <ol>
     *   <li><b>Primary Key:</b> Tries to find an exact match for the {@code i18nKey} in the message properties.</li>
     *   <li><b>Default Key as I18n:</b> If the primary fails, and the {@code defaultMessage} looks like an i18n key
     *       (e.g., "error.internal"), it attempts to look that up.</li>
     *   <li><b>Raw Format:</b> If both lookups fail, it treats the {@code defaultMessage} as a raw string template
     *       (e.g., "User {0} not found") and formats it with the provided arguments.</li>
     * </ol>
     *
     * @param i18nKey        the primary message key (e.g., "error.user.not_found")
     * @param defaultMessage the fallback string or secondary message key
     * @param args           dynamic arguments to substitute into the message template (e.g., user ID)
     * @return the resolved localized message string, or a hardcoded fallback if everything fails
     */
    public String getMessage(String i18nKey, String defaultMessage, Object... args) {
        if (!StringUtils.hasText(i18nKey) && !StringUtils.hasText(defaultMessage)) {
            return "System error, please contact administrator";
        }

        // Get the locale from the current thread context (usually parsed from the Accept-Language HTTP header)
        Locale locale = LocaleContextHolder.getLocale();

        // 1. Try primary key
        if (StringUtils.hasText(i18nKey)) {
            try {
                return messageSource.getMessage(i18nKey, args, locale);
            } catch (NoSuchMessageException _) {
                // Expected behavior if the key is intentionally missing for this specific error
                log.debug("Message key '{}' not found, trying fallback...", i18nKey);
            }
        }

        // 2. Try default key as i18n key
        if (StringUtils.hasText(defaultMessage) && looksLikeI18nKey(defaultMessage)) {
            try {
                return messageSource.getMessage(defaultMessage, args, locale);
            } catch (NoSuchMessageException _) {
                log.debug("Fallback key '{}' not found", defaultMessage);
            }
        }

        // 3. Final fallback: format raw message (e.g., treating "Invalid value: {0}" as a literal template)
        return formatMessage(defaultMessage, args);
    }

    /**
     * Heuristic to determine if a string is intended to be an i18n key rather than literal text.
     *
     * <p>Simple rule: i18n keys almost always contain a dot (e.g., "error.auth") and rarely contain spaces.
     *
     * @param str the string to evaluate
     * @return {@code true} if it looks like a message key, {@code false} otherwise
     */
    private boolean looksLikeI18nKey(String str) {
        return str != null && str.contains(".") && !str.contains(" ");
    }

    /**
     * Formats a raw string template using standard Java {@link MessageFormat}.
     *
     * <p>Security Note: Single quotes have special meaning in {@link MessageFormat} (they are used to escape
     * curly braces). This method sanitizes the input by escaping single quotes to double single quotes
     * to prevent {@link IllegalArgumentException} if user input is used in the message template.
     *
     * @param pattern the raw string template (e.g., "Missing field: {0}")
     * @param args    the arguments to inject into the template
     * @return the formatted string, or the raw pattern if formatting fails
     */
    private String formatMessage(String pattern, Object... args) {
        if (!StringUtils.hasText(pattern) || ArrayUtils.isEmpty(args)) {
            return Objects.toString(pattern, "");
        }
        // Escape single quotes to prevent MessageFormat parsing errors
        return MessageFormat.format(pattern.replace("'", "''"), args);
    }

    // ====================== Error Response Builders ======================

    /**
     * Primary builder for constructing localized error responses based on an {@link ErrorCode}.
     *
     * <p><b>Security Feature (Production Masking):</b> If the application is running in the {@code prod} profile,
     * this method intentionally passes a masked, generic i18n key (e.g., "error.external") to the message resolver
     * instead of the specific key (e.g., "error.user.duplicate.username"). This prevents the frontend from
     * receiving specific technical error messages in production that could aid malicious users in probing the system.
     *
     * @param errorCode the enumerated error code containing the i18n keys and default fallback
     * @param args      dynamic arguments for message formatting
     * @return a standardized {@link Result} containing the masked/resolved error message
     */
    public Result<Void> buildErrorResponse(ErrorCode errorCode, Object... args) {
        errorCode = Objects.requireNonNullElse(errorCode, ErrorCode.INTERNAL_ERROR);

        // In production, we hide specific error codes to avoid information leakage.
        // In dev/test, we expose exact keys to help developers debug.
        String message = getMessage(isProdProfile() ? mask(errorCode.getCode()) : errorCode.getI18nKey(), errorCode.getDefaultMessage(), args);
        return buildFinalResponse(errorCode.getCode(), message);
    }

    /**
     * Convenience builder for wrapping a caught {@link CustomException} into an error response.
     *
     * @param ex the exception thrown by the business logic
     * @return a standardized {@link Result}
     */
    public Result<Void> buildErrorResponse(CustomException ex) {
        return buildErrorResponse(ex.getErrorCode(), ex.getArgs());
    }

    /**
     * Convenience builder for constructing an error response when the message has already been pre-resolved
     * (e.g., by JSR-380 validation frameworks returning concatenated field error strings).
     *
     * @param errorCode the numeric HTTP business error code
     * @param message   the pre-formatted error message string
     * @return a standardized {@link Result}
     */
    public Result<Void> buildErrorResponse(Integer errorCode, String message) {
        return buildFinalResponse(errorCode, message);
    }

    /**
     * Final internal assembly step to package the code and message into the {@link Result} wrapper.
     *
     * @param errorCode the numeric error code
     * @param message   the resolved error message string
     * @return the final {@link Result} DTO
     */
    private Result<Void> buildFinalResponse(Integer errorCode, String message) {
        return Result.fail(errorCode, message);
    }

    /**
     * Maps a specific numeric error code to a generic, category-based fallback i18n key.
     *
     * <p>This ensures that in production, users only see generic messages like "Request failed"
     * or "Unauthorized", rather than specific messages like "User already exists" or "Role code duplicated".
     *
     * @param code the specific numeric error code (e.g., 30002)
     * @return a generic string key representing the error category
     */
    private String mask(Integer code) {
        if (code >= 10000 && code < 20000) {
            return "error.external";          // Hide generic parameter/client errors
        } else if (code >= 2000 && code < 21000) {
            return "error.auth.unauthorized"; // Hide specific auth failures
        } else if (code == 21000) {
            return "error.auth.forbidden";    // Hide specific permission details
        } else if (code >= 30000 && code < 90000) {
            return "error.business";          // Hide specific business rule violations
        } else {
            return "error.internal";          // Hide internal system exceptions
        }
    }

    /**
     * Checks if the application is currently running under the production profile.
     *
     * @return {@code true} if "prod" is in the active profiles list, {@code false} otherwise
     */
    private boolean isProdProfile() {
        return environment.acceptsProfiles(Profiles.of(ProfileConstants.PROD));
    }
}
