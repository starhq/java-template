package com.github.starhq.template.config.security.handler;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.config.messages.MessageUtils;
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
 * Custom implementation of AuthenticationEntryPoint to handle unauthorized
 * access attempts.
 * This class is invoked when an unauthenticated user tries to access a
 * protected resource.
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final MessageUtils messageUtils; // Utility for retrieving messages
    private final JsonMapper jsonMapper;

    /**
     * Commences an authentication scheme when an authentication exception occurs.
     *
     * @param request       the HttpServletRequest
     * @param response      the HttpServletResponse
     * @param authException the AuthenticationException that was thrown
     * @throws IOException      if an input or output exception occurs
     * @throws ServletException if the request for the GET could not be handled
     */
    @Override
    public void commence(HttpServletRequest request, @NonNull HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.warn("Authentication failed: accessing resource [{}], reason: {}",
                request.getRequestURI(),
                authException.getMessage(),
                authException.getCause());

        String errorMessage = messageUtils.getMessage(authException.getMessage(), ErrorCode.UNAUTHORIZED.getI18nKey());
        Result<Void> errorResponse = messageUtils.buildErrorResponse(ErrorCode.UNAUTHORIZED.getCode(), errorMessage);

        HttpUtils.write(response, HttpStatus.UNAUTHORIZED.value(), jsonMapper.writeValueAsString(errorResponse));
    }
}
