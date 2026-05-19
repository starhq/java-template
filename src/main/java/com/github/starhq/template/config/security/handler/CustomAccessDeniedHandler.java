package com.github.starhq.template.config.security.handler;

import java.io.IOException;
import java.util.Objects;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.model.vo.Result;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

/**
 * Custom handler for access denied responses in Spring Security.
 * This handler is invoked when an authenticated user tries to access a resource
 * they do not have permission to access.
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

        private final MessageUtils messageUtils; // Utility for building error messages
        private final JsonMapper jsonMapper;

        /**
         * Handles access denied exceptions by sending a JSON response with an error
         * message.
         *
         * @param request               the HttpServletRequest
         * @param response              the HttpServletResponse
         * @param accessDeniedException the AccessDeniedException that was thrown
         * @throws IOException      if an input or output exception occurs
         * @throws ServletException if the request for the GET could not be handled
         */
        @Override
        public void handle(HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           AccessDeniedException accessDeniedException) throws IOException, ServletException {

                log.warn("Permission denied: User [{}] attempted to access resource [{}], reason: {}",
                                Objects.requireNonNullElse(
                                                SecurityContextUtils.getCurrentUsername(), "anonymousUser"),
                                request.getRequestURI(),
                                accessDeniedException.getMessage(),
                                accessDeniedException.getCause());

                Result<Void> errorResponse = messageUtils.buildErrorResponse(ErrorCode.FORBIDDEN);

                // Write the error message to the response body
                HttpUtils.write(response, HttpStatus.FORBIDDEN.value(), jsonMapper.writeValueAsString(errorResponse));
        }
}
