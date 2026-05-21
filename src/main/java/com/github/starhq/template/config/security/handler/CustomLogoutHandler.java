package com.github.starhq.template.config.security.handler;

import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.service.TokenService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * Custom handler triggered upon successful user logout.
 *
 * <p>This component is responsible for cleaning up server-side state associated with the user's session.
 * It is typically wired into Spring Security's logout configuration
 * ({@code http.logout().addLogoutHandler(...)}).
 *
 * @author starhq
 */
@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final TokenService tokenService;

    /**
     * Performs the actual logout logic.
     *
     * <p><b>Execution Sequence Importance:</b> The order of operations here is strictly designed:
     * <ol>
     *   <li><b>1. Remove Database Session:</b> First, forcefully invalidate the user's session record in the database.
     *       This ensures that even if the client keeps using the old JWT, the backend will reject it.</li>
     *   <li><b>2. Clear SecurityContext:</b> Second, manually clear the {@link SecurityContextHolder}.
     *       If we cleared the context first, {@link SecurityContextUtils#getRequiredUserId()} would throw
     *       an exception, preventing us from knowing *who* is logging out, and thus failing to clear the database record.</li>
     * </ol>
     *
     * @param request        the current HTTP request
     * @param response       the current HTTP response
     * @param authentication the authenticated user's token (populated by Spring Security just before this call)
     */
    @Override
    public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication) {
        // 1. Identify the user and destroy their persistent session in the database/cache
        Long userId = SecurityContextUtils.getRequiredUserId();
        tokenService.removeByUserId(userId);

        // 2. Clean up the current thread's security context to ensure true logout state
        SecurityContextHolder.clearContext();
    }

}