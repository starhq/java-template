package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.entity.SysUser;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class SecurityContextUtils {

    /**
     * 获取原始的 Authentication 对象（最底层，不推荐业务代码直接使用）
     */
    public static Optional<Authentication> getCurrentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // ✅ 优化1：过滤掉框架内部的匿名用户，防止匿名用户穿透到业务层
        return Optional.ofNullable(authentication)
                .filter(auth -> !(auth instanceof AnonymousAuthenticationToken));
    }

    /**
     * 获取当前登录的用户详情实体（核心方法）
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        return getCurrentAuthentication()
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getPrincipal)
                .filter(UserDetails.class::isInstance)
                .map(UserDetails.class::cast);
    }

    /**
     * 获取当前用户名（如果未登录抛出异常）
     */
    public static String getCurrentUsername() {
        return getCurrentUserDetails()
                .map(UserDetails::getUsername)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED));
    }

    /**
     * 获取当前用户名（安全获取，未登录返回 null，适用于某些不强制登录的公开接口）
     */
    public static String getUsernameOrNull() {
        return getCurrentUserDetails()
                .map(UserDetails::getUsername)
                .orElse(null);
    }

    /**
     * 获取当前用户 ID（安全获取，未登录或类型转换失败返回 null）
     * 适用场景：单元测试降级、异步线程兜底、非核心日志记录
     */
    public static Long getUserIdOrNull() {
        return getCurrentUserDetails()
                .filter(SysUser.class::isInstance) // ✅ 优化2：直接用 class::isInstance 更简洁
                .map(SysUser.class::cast)
                .map(SysUser::getId)
                .orElse(null);
    }

    /**
     * 获取当前用户 ID（强制获取，未登录抛出异常）
     * 适用场景：核心业务逻辑（如修改密码、下单等必须依赖用户 ID 的操作）
     */
    public static Long getRequiredUserId() {
        Long userId = getUserIdOrNull();
        if (userId == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 获取当前用户的权限集合（如果未登录抛出异常）
     */
    public static Collection<? extends GrantedAuthority> getCurrentAuthorities() {
        return getCurrentUserDetails()
                .map(UserDetails::getAuthorities)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED));
    }

    /**
     * 判断当前用户是否拥有某个特定角色（工具方法，常用于代码级别的权限控制）
     */
    public static boolean hasRole(String roleCode) {
        return getCurrentAuthorities().stream()
                .anyMatch(authority -> Objects.equals(authority.getAuthority(), roleCode));
    }
}
