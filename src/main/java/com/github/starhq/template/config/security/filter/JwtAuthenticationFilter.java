package com.github.starhq.template.config.security.filter;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.config.messages.MessageUtils;
import com.github.starhq.template.config.security.WhiteListPathMatcher;
import com.github.starhq.template.config.security.jwt.JwtService;
import com.github.starhq.template.model.dto.token.TokenSimpleDTO;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Objects;

import static com.github.starhq.template.common.constant.FilterConstant.LOGOUT_ENDPOINT;
import static com.github.starhq.template.common.constant.FilterConstant.REFRESH_ENDPOINT;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final WhiteListPathMatcher whiteListPathMatcher;
    private final JsonMapper jsonMapper;
    private final MessageUtils messageUtils;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return whiteListPathMatcher.isWhiteListPath(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String token = HttpUtils.extractToken(request);

        // 1. 基础校验：Token 和 指纹
        if (!StringUtils.hasText(token)) {
            handleUnauthorized(response, ErrorCode.TOKEN_MISSING);
            return;
        }

        String fingerPrint = RequestContextUtil.getContext().deviceFingerprint();
        if (!StringUtils.hasText(fingerPrint)) {
            handleUnauthorized(response, ErrorCode.FINGERPRINT_MISSING);
            return;
        }

        // 2. JWT 解析与校验 (如果解析失败，JJWT 会抛出 JwtException，会被 Spring Security 捕获)
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (ExpiredJwtException e) {
            handleUnauthorized(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (JwtException e) {
            handleUnauthorized(response, ErrorCode.TOKEN_INVALID);
            return;
        }
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        // 3. 加载用户信息
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            handleUnauthorized(response, ErrorCode.CREDENTIALS);
            return;
        }

        // 4. 数据库会话校验 (封装校验逻辑)
        TokenSimpleDTO sessionToken;
        try {
            sessionToken = tokenService.getByUserId(userId);
            validateSession(sessionToken, token, requestPath, fingerPrint, HttpUtils.getClientIp(request));
        } catch (Exception e) {
            ErrorCode errorCode = (e instanceof CustomException ce) ? ce.getErrorCode() : ErrorCode.UNAUTHORIZED;
            handleUnauthorized(response, errorCode);
            return;
        }


        // 5. 设置 SecurityContext
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("User [{}] authenticated successfully.", username);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 校验数据库中的会话状态，失败直接抛出异常
     */
    private void validateSession(TokenSimpleDTO sessionToken, String incomingToken, String requestPath, String fingerPrint, String clientIp) {

        if (sessionToken == null) {
            throw new CustomException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED);
        }

        boolean isRefresh = requestPath.contains(REFRESH_ENDPOINT);
        boolean isLogout = requestPath.contains(LOGOUT_ENDPOINT);

        // Token 匹配校验
        if (isRefresh) {
            if (!Objects.equals(incomingToken, sessionToken.getRefreshToken())) {
                throw new CustomException(ErrorCode.TOKEN_REFRESH_INVALID, HttpStatus.UNAUTHORIZED);
            }
        } else if (isLogout) {
            // 注销接口允许 Access 或 Refresh Token
            if (!Objects.equals(incomingToken, sessionToken.getAccessToken()) && !Objects.equals(incomingToken, sessionToken.getRefreshToken())) {
                throw new CustomException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
            }
        } else {
            // 普通接口必须匹配 Access Token
            if (!Objects.equals(incomingToken, sessionToken.getAccessToken())) {
                throw new CustomException(ErrorCode.TOKEN_ACCESS_INVALID, HttpStatus.UNAUTHORIZED);
            }
        }

        // 设备校验
        if (!Objects.equals(sessionToken.getLoginIp(), clientIp) || !Objects.equals(sessionToken.getDeviceFingerprint(), fingerPrint)) {
            throw new CustomException(ErrorCode.DEVICE_MISMATCH, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * 提取的统一返回方法（复用你的 AuthenticationEntryPoint 的逻辑）
     */
    private void handleUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        Result<Void> result = messageUtils.buildErrorResponse(errorCode);

        String message = jsonMapper.writeValueAsString(result);

        HttpUtils.write(response, HttpStatus.UNAUTHORIZED.value(), message);
    }
}
