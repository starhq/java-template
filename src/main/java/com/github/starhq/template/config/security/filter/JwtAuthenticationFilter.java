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

/**
 * Core JWT-based authentication filter for the application.
 *
 * <p>Intercepts incoming HTTP requests to validate JSON Web Tokens, verify database session states,
 * and check device fingerprints. If all checks pass, it populates the Spring Security
 * {@link org.springframework.security.core.context.SecurityContext}, allowing request access
 * to protected APIs.
 *
 * @author starhq
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final WhiteListPathMatcher whiteListPathMatcher;
    private final JsonMapper jsonMapper;
    private final MessageUtils messageUtils;

    /**
     * Short-circuits the filter chain for whitelisted paths (e.g., public APIs, static resources).
     *
     * <p>Returning {@code true} here tells Spring Security to completely skip {@link #doFilterInternal}
     * for these requests, optimizing performance and avoiding unnecessary security checks.
     *
     * @param request the incoming HTTP request
     * @return {@code true} if the request path is in the whitelist, {@code false} otherwise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return whiteListPathMatcher.isWhiteListPath(request.getServletPath());
    }

    /**
     * The core authentication workflow. Implements a strict 5-step validation chain.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the filter chain to pass the request to the next filter/controller
     * @throws ServletException if an internal servlet error occurs
     * @throws IOException      if an I/O error occurs when writing the error response
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        String token = HttpUtils.extractToken(request);

        // --- Step 1: Basic presence checks (Fail fast) ---
        if (!StringUtils.hasText(token)) {
            handleUnauthorized(response, ErrorCode.TOKEN_MISSING);
            return;
        }

        // Retrieve fingerprint pre-populated by the upstream RequestContextFilter
        String fingerPrint = RequestContextUtil.getContext().deviceFingerprint();
        if (!StringUtils.hasText(fingerPrint)) {
            handleUnauthorized(response, ErrorCode.FINGERPRINT_MISSING);
            return;
        }

        // --- Step 2: Cryptographic JWT validation ---
        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (ExpiredJwtException e) {
            handleUnauthorized(response, ErrorCode.TOKEN_EXPIRED);
            return;
        } catch (JwtException e) {
            // Catches malformed signatures, unsupported tokens, etc.
            handleUnauthorized(response, ErrorCode.TOKEN_INVALID);
            return;
        }
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        // --- Step 3: User existence validation ---
        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            // Prevent username enumeration by returning a generic credential error
            handleUnauthorized(response, ErrorCode.CREDENTIALS);
            return;
        }

        // --- Step 4: Database session state validation (Strict binding) ---
        TokenSimpleDTO sessionToken;
        try {
            sessionToken = tokenService.getByUserId(userId);
            // Delegates to strict token matching and device binding checks
            validateSession(sessionToken, token, requestPath, fingerPrint, HttpUtils.getClientIp(request));
        } catch (Exception e) {
            // Gracefully handle custom business exceptions or unexpected DB errors
            ErrorCode errorCode = (e instanceof CustomException ce) ? ce.getErrorCode() : ErrorCode.UNAUTHORIZED;
            handleUnauthorized(response, errorCode);
            return;
        }

        // --- Step 5: Establish Spring Security Context ---
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("User [{}] authenticated successfully via JWT.", username);
        }

        // Proceed to the next filter or controller
        filterChain.doFilter(request, response);
    }

    /**
     * Validates the active database session against the incoming request context.
     *
     * <p>This method implements a crucial security measure: <b>Token Type Separation</b>.
     * It strictly verifies that the provided token matches the expected type based on the requested endpoint:
     * <ul>
     *   <li><b>Normal APIs:</b> Must present a valid Access Token.</li>
     *   <li><b>Refresh API:</b> Must present the corresponding Refresh Token.</li>
     *   <li><b>Logout API:</b> Accepts either token to ensure the session can actually be destroyed.</li>
     * </ul>
     * It also enforces <b>Device/IP Binding</b> to mitigate session hijacking.
     *
     * @param sessionToken  the active session record retrieved from the database
     * @param incomingToken the raw token string extracted from the current HTTP request
     * @param requestPath   the URL path of the current request (used to determine expected token type)
     * @param fingerPrint   the device fingerprint extracted from the request context
     * @param clientIp      the client's IP address
     * @throws CustomException with specific {@link ErrorCode} if any session rule is violated
     */
    private void validateSession(TokenSimpleDTO sessionToken, String incomingToken, String requestPath, String fingerPrint, String clientIp) {

        // 1. Session existence check
        if (sessionToken == null) {
            throw new CustomException(ErrorCode.SESSION_INVALID, HttpStatus.UNAUTHORIZED);
        }

        boolean isRefresh = requestPath.contains(REFRESH_ENDPOINT);
        boolean isLogout = requestPath.contains(LOGOUT_ENDPOINT);

        // 2. Token Type Matching Rule
        if (isRefresh) {
            // Refresh endpoint MUST use the refresh token
            if (!Objects.equals(incomingToken, sessionToken.getRefreshToken())) {
                throw new CustomException(ErrorCode.TOKEN_REFRESH_INVALID, HttpStatus.UNAUTHORIZED);
            }
        } else if (isLogout) {
            // Logout endpoint is lenient: accept either token to ensure session cleanup succeeds
            if (!Objects.equals(incomingToken, sessionToken.getAccessToken()) && !Objects.equals(incomingToken, sessionToken.getRefreshToken())) {
                throw new CustomException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
            }
        } else {
            // All other protected APIs MUST use the access token
            if (!Objects.equals(incomingToken, sessionToken.getAccessToken())) {
                throw new CustomException(ErrorCode.TOKEN_ACCESS_INVALID, HttpStatus.UNAUTHORIZED);
            }
        }

        // 3. Device & IP Binding Check (Anti-Hijacking)
        if (!Objects.equals(sessionToken.getLoginIp(), clientIp) || !Objects.equals(sessionToken.getDeviceFingerprint(), fingerPrint)) {
            throw new CustomException(ErrorCode.DEVICE_MISMATCH, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Helper method to serialize a standardized error response and write it directly to the HTTP response.
     *
     * <p>Bypasses Spring MVC's exception handling because exceptions thrown in Filters occur
     * before the {@code DispatcherServlet} is reached.
     *
     * @param response  the HTTP response object
     * @param errorCode the specific error code enum describing the authentication failure
     * @throws IOException if writing to the response output stream fails
     */
    private void handleUnauthorized(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        Result<Void> result = messageUtils.buildErrorResponse(errorCode);
        String message = jsonMapper.writeValueAsString(result);
        HttpUtils.write(response, HttpStatus.UNAUTHORIZED.value(), message);
    }
}