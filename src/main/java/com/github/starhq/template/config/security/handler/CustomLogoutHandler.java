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

@RequiredArgsConstructor
public class CustomLogoutHandler implements LogoutHandler {

    private final TokenService tokenService;

    @Override
    public void logout(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, Authentication authentication) {
        Long userId = SecurityContextUtils.getRequiredUserId();
        tokenService.removeByUserId(userId);
        SecurityContextHolder.clearContext();
    }

}
