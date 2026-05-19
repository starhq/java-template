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

@Slf4j
@RequiredArgsConstructor
public class ResourceFilter extends OncePerRequestFilter {

    private final ResourceService resourceService;
    private final WhiteListPathMatcher whiteListPathMatcher;
    private final CacheHelper cacheHelper; // 注入 CacheManager
    private final JsonMapper jsonMapper;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return whiteListPathMatcher.isWhiteListPath(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 理论上 JwtFilter 已经处理了未认证情况，这里做防御性编程
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            handle(response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
            return;
        }

        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();
        Long userId = SecurityContextUtils.getRequiredUserId();

        // 1. 尝试从缓存获取权限结果
        String cacheKey = generateCacheKey(userId, requestPath, httpMethod);

        // 使用 get 方法原子性操作缓存
        Boolean hasPermission = cacheHelper.get(cacheKey, Boolean.class, CacheConstant.PERMISSION);

        if (hasPermission != null) {
            if (hasPermission) {
                filterChain.doFilter(request, response);
                return;
            }
            // 缓存明确记录无权限
            handle(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
            return;
        }

        // 2. 缓存未命中，查询数据库
        List<SysResource> resources = resourceService.selectByUserId(userId);

        // 3. 匹配权限
        boolean permitted = resources.stream()
                .anyMatch(res -> PATH_MATCHER.match(res.getUrl(), requestPath) &&
                        HttpMethod.contains(res.getMethods(), HTTP_METHOD_CACHE.get(httpMethod)));

        // 4. 写入缓存
        cacheHelper.put(cacheKey, permitted, CacheConstant.PERMISSION);

        // 5. 决策
        if (permitted) {
            log.debug("Access granted for {} {}", httpMethod, requestPath);
            filterChain.doFilter(request, response);
        } else {
            log.warn("Access denied for {} {}", httpMethod, requestPath);
            handle(response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
        }
    }

    private String generateCacheKey(Long userId, String path, String method) {
        String id = String.valueOf(userId);
        return String.join(":", id, path, method);
    }

    private void handle(HttpServletResponse response, HttpStatus status, ErrorCode errorCode) throws IOException {
        Result<Void> result = Result.fail(errorCode);

        String message = jsonMapper.writeValueAsString(result);

        HttpUtils.write(response, status.value(), message);
    }
}
