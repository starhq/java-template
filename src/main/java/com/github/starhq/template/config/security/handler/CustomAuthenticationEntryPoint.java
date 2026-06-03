package com.github.starhq.template.config.security.handler;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.config.i18n.MessageUtils;
import com.github.starhq.template.model.vo.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Custom entry point for handling unauthenticated access attempts (HTTP 401 Unauthorized).
 *
 * <p>This component is invoked by Spring Security when an unauthenticated user attempts to access
 * a protected resource. It acts as the catch-all handler for authentication failures occurring in filters
 * (e.g., missing token, expired token, invalid signature) before the request reaches the {@code DispatcherServlet}.
 *
 * <p><b>Architecture Context:</b> In a stateless JWT architecture, this replaces Spring Security's default
 * behavior of redirecting to a login page. Instead, it directly writes a structured JSON error response,
 * allowing frontend Single Page Applications (SPAs) to intercept the 401 status code and redirect the user
 * to the login view.
 *
 * @author starhq
 * @see com.github.starhq.template.config.security.handler.CustomAccessDeniedHandler
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageUtils messageUtils;
    private final JsonMapper jsonMapper;

    /**
     * Handles the authentication failure by logging the attempt and returning a standardized JSON error response.
     *
     * <p><b>Security Note:</b> The exception message from Spring Security (e.g., "JWT signature does not match")
     * is intentionally passed through a message resolver fallback chain. This prevents raw, potentially sensitive
     * internal framework error details from being exposed directly to the client.
     *
     * @param request       the current HTTP request
     * @param response      the HTTP response to write the error to
     * @param authException the exception thrown by the security filter chain (contains the failure reason)
     * @throws IOException      if an I/O error occurs during response writing
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void commence(HttpServletRequest request, @NonNull HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // Log the security breach attempt for backend auditing.
        // The stack trace is included implicitly by Slf4j to help diagnose signature issues.
        log.warn("Authentication failed: accessing resource [{}], reason: {}",
                request.getRequestURI(),
                authException.getMessage(),
                authException.getCause());

        // Resolve the error message safely.
        // Passes the raw framework exception message as the primary key, and the standard UNAUTHORIZED
        // key as the fallback. The MessageUtils will handle i18n lookup or fallback to a generic message,
        // ensuring we never accidentally leak raw Java exception strings like "JWTExpiredException" to the frontend.
        String errorMessage = messageUtils.getMessage(authException.getMessage(), ErrorCode.UNAUTHORIZED.getI18nKey());

        Result<Void> errorResponse = messageUtils.buildErrorResponse(ErrorCode.UNAUTHORIZED.getCode(), errorMessage);

        // Serialize and write the JSON directly to the response body
        HttpUtils.write(response, HttpStatus.UNAUTHORIZED.value(), jsonMapper.writeValueAsString(errorResponse));
    }
}