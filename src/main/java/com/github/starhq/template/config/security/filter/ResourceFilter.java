package com.github.starhq.template.config.security.filter;

import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.common.util.HttpUtils;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.config.security.WhiteListPathMatcher;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.model.vo.Result;
import com.github.starhq.template.service.ResourceService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

import static com.github.starhq.template.common.constant.FilterConstant.HTTP_METHOD_CACHE;

/**
 * Dynamic API resource authorization filter.
 *
 * <p>This filter intercepts requests after authentication to determine if the authenticated user
 * has the required permissions to access the specific API endpoint (URL + HTTP Method).
 *
 * <p><b>Why a custom Filter instead of {@code @PreAuthorize}?</b><br>
 * Standard Spring Security annotations (like {@code @PreAuthorize("hasRole('ADMIN')")}) rely on static,
 * code-level checks. This filter implements <b>database-driven, dynamic permission control</b>. It compares
 * the current request against API resource records stored in the database, allowing administrators to
 * modify permissions at runtime without redeploying the application.
 *
 * <p><b>Architecture Note:</b> This filter is placed <b>after</b> the {@code JwtAuthenticationFilter} to ensure
 * that the {@link SecurityContextHolder} is fully populated with user details before attempting authorization.
 *
 * @author starhq
 */
@Slf4j
@RequiredArgsConstructor
public class ResourceFilter extends OncePerRequestFilter {

    private final ResourceService resourceService;
    private final WhiteListPathMatcher whiteListPathMatcher;
    private final CacheHelper cacheHelper;
    private final JsonMapper jsonMapper;

    /**
     * Spring utility for matching URL patterns (e.g., "/api/users/**" against "/api/users/1").
     * Note: For production high-traffic scenarios, consider upgrading to {@code PathPatternParser}.
     */
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return whiteListPathMatcher.isWhiteListPath(request.getServletPath());
    }

    /**
     * Executes the dynamic authorization workflow:
     * Authenticate Check -> Cache Lookup -> DB Fallback -> Cache Write -> Decision.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the filter chain to proceed if authorized
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Defensive check: If JWT filter failed or didn't set context, reject immediately.
        // (Theoretically handled by JwtFilter, but prevents security bypass if filter chain order changes)
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            handle(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
            return;
        }

        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();

        // Extract the strictly validated user ID (throws exception if not present)
        Long userId = SecurityContextUtils.getRequiredUserId();

        // 1. Attempt to resolve authorization from cache
        String cacheKey = generateCacheKey(userId, requestPath, httpMethod);
        Boolean hasPermission = cacheHelper.get(cacheKey, Boolean.class, CacheConstant.PERMISSION);

        if (hasPermission != null) {
            if (hasPermission) {
                // Cache Hit: Granted. Skip database query.
                filterChain.doFilter(request, response);
                return;
            }
            // Cache Hit: Explicitly Denied. (Crucial for preventing cache penetration attacks on restricted APIs)
            handle(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
            return;
        }

        // 2. Cache Miss: Query all permissions assigned to this user from the database
        List<SysResource> resources = resourceService.selectByUserId(userId);

        // 3. Evaluate if the requested URL and Method match any of the user's assigned resources
        boolean permitted = resources.stream()
                .anyMatch(res -> PATH_MATCHER.match(res.getUrl(), requestPath) &&
                        HttpMethod.contains(res.getMethods(), HTTP_METHOD_CACHE.get(httpMethod)));

        // 4. Write the evaluated result back to the cache for future requests
        cacheHelper.put(cacheKey, permitted, CacheConstant.PERMISSION);

        // 5. Enforce the decision
        if (permitted) {
            log.debug("Access granted for {} {}", httpMethod, requestPath);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Access denied for {} {}", httpMethod, requestPath);
            handle(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
        }
    }

    /**
     * Generates a unique cache key scoped to a specific user and API endpoint.
     *
     * <p><b>Why not just use the URL as the cache key?</b>
     * Permissions are user-specific. User A might have access to {@code /api/admin/delete}, but User B does not.
     * The cache key MUST include the {@code userId} to isolate permission caches between different users.
     *
     * @param userId the ID of the currently authenticated user
     * @param path   the requested API URL (e.g., "/api/sys-user")
     * @param method the HTTP method (e.g., "POST")
     * @return a structured string combining all three elements (e.g., "1001:/api/sys-user:POST")
     */
    private String generateCacheKey(Long userId, String path, String method) {
        String id = String.valueOf(userId);
        return String.join(":", id, path, method);
    }

    /**
     * Helper method to serialize an error response and write it directly to the HTTP response stream,
     * bypassing the DispatcherServlet.
     *
     * @param response  the HTTP response
     * @param status    the HTTP status code to return
     * @param errorCode the specific error code enum
     * @throws IOException if writing to the response fails
     */
    private void handle(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        Result<Void> result = Result.fail(errorCode);
        String message = jsonMapper.writeValueAsString(result);
        HttpUtils.write(response, status.value(), message);
    }
}