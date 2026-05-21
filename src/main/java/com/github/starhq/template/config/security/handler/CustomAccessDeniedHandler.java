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
 * Custom handler for Spring Security's HTTP 403 Forbidden responses.
 *
 * <p>This handler is triggered by the security filter chain when an <b>authenticated</b> user
 * attempts to access a resource they do not have the required permissions for.
 *
 * <p><b>Architecture Context (401 vs 403):</b>
 * It is crucial to distinguish this handler from {@code AuthenticationEntryPoint}:
 * <ul>
 *   <li><b>401 Unauthorized (AuthenticationEntryPoint):</b> "I don't know who you are" (Missing/Invalid token).</li>
 *   <li><b>403 Forbidden (This Handler):</b> "I know exactly who you are, but you are not allowed to do this"
 *       (Valid token, but lacking {@code sys:user:delete} permission).</li>
 * </ul>
 *
 * @author starhq
 */
@Slf4j
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final MessageUtils messageUtils;
    private final JsonMapper jsonMapper;

    /**
     * Handles the access denied scenario by logging the security breach attempt and returning
     * a standardized JSON error response to the client.
     *
     * <p>The logging is specifically marked as {@code warn} level. In a production system, a 403 error
     * often indicates either a user trying to exceed their privileges (which should be monitored)
     * or a backend configuration error where an API was not properly assigned to a role.
     *
     * @param request               the current HTTP request
     * @param response              the HTTP response to write to
     * @param accessDeniedException the exception containing details about the failed authorization check
     * @throws IOException      if an I/O error occurs while writing the response
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void handle(HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // Defensive logging: Safely attempts to get the username, defaulting to "anonymousUser" if the
        // SecurityContext happens to be unexpectedly empty (e.g., called from an asynchronous thread).
        log.warn("Permission denied: User [{}] attempted to access resource [{}], reason: {}",
                Objects.requireNonNullElse(
                        SecurityContextUtils.getCurrentUsername(), "anonymousUser"),
                request.getRequestURI(),
                accessDeniedException.getMessage(),
                accessDeniedException.getCause());

        // Build the localized error response using the unified message utility
        Result<Void> errorResponse = messageUtils.buildErrorResponse(ErrorCode.FORBIDDEN);

        // Serialize and write directly to the response (bypassing Spring MVC's exception handling)
        HttpUtils.write(response, HttpStatus.FORBIDDEN.value(), jsonMapper.writeValueAsString(errorResponse));
    }
}