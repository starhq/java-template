package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.entity.SysUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityContextUtilsTest {

    // ========================================
    // 测试用的内部类，模拟 SysUser
    // ========================================
    static class TestSysUser implements UserDetails {
        @Serial
        private static final long serialVersionUID = -2556026608513345621L;
        private final Long id;
        private final String username;
        private final String password;
        private final Collection<? extends GrantedAuthority> authorities;

        TestSysUser(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.authorities = authorities;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        public Long getId() {
            return id;
        }
    }

    // 非 SysUser 的 UserDetails 实现
    static class NonSysUserDetails implements UserDetails {
        private final String username;

        NonSysUserDetails(String username) {
            this.username = username;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.emptyList();
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    // ========================================
    // // 测试数据常量
    // ========================================
    private static final Long USER_ID = 1001L;
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "encodedPassword";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_USER = "ROLE_USER";

    private final List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ROLE_ADMIN),
            new SimpleGrantedAuthority(ROLE_USER)
    );

    // ========================================
    // 每个测试后清理 SecurityContext
    // ========================================
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ========================================
    // 辅助方法
    // ========================================
    private void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Authentication createAuthenticatedSysUser() {
        TestSysUser sysUser = new TestSysUser(USER_ID, USERNAME, PASSWORD, authorities);
        return new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities);
    }

    private Authentication createAuthenticatedNonSysUser() {
        NonSysUserDetails userDetails = new NonSysUserDetails(USERNAME);
        return new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());
    }

    private Authentication createAnonymousAuthentication() {
        return new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    private Authentication createAuthenticationWithNonUserDetailsPrincipal() {
        return new UsernamePasswordAuthenticationToken("justAString", null, Collections.emptyList());
    }

    private Authentication createUnauthenticatedAuthentication() {
        TestSysUser sysUser = new TestSysUser(USER_ID, USERNAME, PASSWORD, authorities);
        // isAuthenticated = false
        return new UsernamePasswordAuthenticationToken(sysUser, PASSWORD) {
            @Override
            public boolean isAuthenticated() {
                return false;
            }
        };
    }

    // ========================================
    // getCurrentAuthentication 测试
    // ========================================
    @Nested
    @DisplayName("getCurrentAuthentication 方法测试")
    class GetCurrentAuthenticationTest {

        @Test
        @DisplayName("正常登录用户 - 应返回 Optional 包含 Authentication")
        void shouldReturnAuthenticationWhenUserIsLoggedIn() {
            // given
            Authentication auth = createAuthenticatedSysUser();
            setAuthentication(auth);

            // when
            Optional<Authentication> result = SecurityContextUtils.getCurrentAuthentication();

            // then
            assertThat(result).isPresent()
                    .containsSame(auth);
        }

        @Test
        @DisplayName("匿名用户 - 应返回 Optional.empty")
        void shouldReturnEmptyForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when
            Optional<Authentication> result = SecurityContextUtils.getCurrentAuthentication();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("未设置 Authentication - 应返回 Optional.empty")
        void shouldReturnEmptyWhenNoAuthentication() {
            // given
            SecurityContextHolder.clearContext();

            // when
            Optional<Authentication> result = SecurityContextUtils.getCurrentAuthentication();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Authentication 为 null - 应返回 Optional.empty")
        void shouldReturnEmptyWhenAuthenticationIsNull() {
            // given
            SecurityContextHolder.getContext().setAuthentication(null);

            // when
            Optional<Authentication> result = SecurityContextUtils.getCurrentAuthentication();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========================================
    // getCurrentUserDetails 测试
    // ========================================
    @Nested
    @DisplayName("getCurrentUserDetails 方法测试")
    class GetCurrentUserDetailsTest {

        @Test
        @DisplayName("SysUser 登录 - 应返回 Optional 包含 UserDetails")
        void shouldReturnUserDetailsForSysUser() {
            // given
            TestSysUser sysUser = new TestSysUser(USER_ID, USERNAME, PASSWORD, authorities);
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities));

            // when
            Optional<UserDetails> result = SecurityContextUtils.getCurrentUserDetails();

            // then
            assertThat(result).isPresent()
                    .containsSame(sysUser);
        }

        @Test
        @DisplayName("非 SysUser 的 UserDetails 登录 - 应返回 Optional 包含该 UserDetails")
        void shouldReturnUserDetailsForNonSysUser() {
            // given
            NonSysUserDetails userDetails = new NonSysUserDetails(USERNAME);
            setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList()));

            // when
            Optional<UserDetails> result = SecurityContextUtils.getCurrentUserDetails();

            // then
            assertThat(result).isPresent()
                    .containsSame(userDetails);
        }

        @Test
        @DisplayName("Principal 不是 UserDetails - 应返回 Optional.empty")
        void shouldReturnEmptyWhenPrincipalIsNotUserDetails() {
            // given
            setAuthentication(createAuthenticationWithNonUserDetailsPrincipal());

            // when
            Optional<UserDetails> result = SecurityContextUtils.getCurrentUserDetails();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("未认证的 Authentication - 应返回 Optional.empty")
        void shouldReturnEmptyWhenNotAuthenticated() {
            // given
            setAuthentication(createUnauthenticatedAuthentication());

            // when
            Optional<UserDetails> result = SecurityContextUtils.getCurrentUserDetails();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("匿名用户 - 应返回 Optional.empty")
        void shouldReturnEmptyForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when
            Optional<UserDetails> result = SecurityContextUtils.getCurrentUserDetails();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("无 Authentication - 应返回 Optional.empty")
        void shouldReturnEmptyWhenNoAuthentication() {
            // given
            SecurityContextHolder.clearContext();

            // when
            Optional<UserDetails> result = SecurityContextUtils.getCurrentUserDetails();

            // then
            assertThat(result).isEmpty();
        }
    }

    // ========================================
    // getCurrentUsername 测试
    // ========================================
    @Nested
    @DisplayName("getCurrentUsername 方法测试")
    class GetCurrentUsernameTest {

        @Test
        @DisplayName("正常登录用户 - 应返回用户名")
        void shouldReturnUsernameWhenLoggedIn() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when
            String username = SecurityContextUtils.getCurrentUsername();

            // then
            assertThat(username).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("未登录 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenNotLoggedIn() {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            assertThatThrownBy(SecurityContextUtils::getCurrentUsername)
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode", "status")
                    .containsExactly(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("匿名用户 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when & then
            assertThatThrownBy(SecurityContextUtils::getCurrentUsername)
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("未认证 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // given
            setAuthentication(createUnauthenticatedAuthentication());

            // when & then
            assertThatThrownBy(SecurityContextUtils::getCurrentUsername)
                    .isInstanceOf(CustomException.class);
        }
    }

    // ========================================
    // getUsernameOrNull 测试
    // ========================================
    @Nested
    @DisplayName("getUsernameOrNull 方法测试")
    class GetUsernameOrNullTest {

        @Test
        @DisplayName("正常登录用户 - 应返回用户名")
        void shouldReturnUsernameWhenLoggedIn() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when
            String username = SecurityContextUtils.getUsernameOrNull();

            // then
            assertThat(username).isEqualTo(USERNAME);
        }

        @Test
        @DisplayName("未登录 - 应返回 null")
        void shouldReturnNullWhenNotLoggedIn() {
            // given
            SecurityContextHolder.clearContext();

            // when
            String username = SecurityContextUtils.getUsernameOrNull();

            // then
            assertThat(username).isNull();
        }

        @Test
        @DisplayName("匿名用户 - 应返回 null")
        void shouldReturnNullForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when
            String username = SecurityContextUtils.getUsernameOrNull();

            // then
            assertThat(username).isNull();
        }

        @Test
        @DisplayName("Principal 非 UserDetails - 应返回 null")
        void shouldReturnNullWhenPrincipalIsNotUserDetails() {
            // given
            setAuthentication(createAuthenticationWithNonUserDetailsPrincipal());

            // when
            String username = SecurityContextUtils.getUsernameOrNull();

            // then
            assertThat(username).isNull();
        }
    }

    // ========================================
    // getUserIdOrNull 测试
    // ========================================
    @Nested
    @DisplayName("getUserIdOrNull 方法测试")
    class GetUserIdOrNullTest {

        @Test
        @DisplayName("SysUser 登录 - 应返回用户 ID")
        void shouldReturnUserIdForSysUser() {
            SysUser sysUser = new SysUser();
            sysUser.setId(USER_ID);
            var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities);
            // given
            setAuthentication(usernamePasswordAuthenticationToken);

            // when
            Long userId = SecurityContextUtils.getUserIdOrNull();

            // then
            assertThat(userId).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("非 SysUser 的 UserDetails - 应返回 null")
        void shouldReturnNullForNonSysUser() {
            // given
            setAuthentication(createAuthenticatedNonSysUser());

            // when
            Long userId = SecurityContextUtils.getUserIdOrNull();

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("未登录 - 应返回 null")
        void shouldReturnNullWhenNotLoggedIn() {
            // given
            SecurityContextHolder.clearContext();

            // when
            Long userId = SecurityContextUtils.getUserIdOrNull();

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("匿名用户 - 应返回 null")
        void shouldReturnNullForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when
            Long userId = SecurityContextUtils.getUserIdOrNull();

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("Principal 非 UserDetails - 应返回 null")
        void shouldReturnNullWhenPrincipalIsNotUserDetails() {
            // given
            setAuthentication(createAuthenticationWithNonUserDetailsPrincipal());

            // when
            Long userId = SecurityContextUtils.getUserIdOrNull();

            // then
            assertThat(userId).isNull();
        }

        @Test
        @DisplayName("SysUser 的 ID 为 null - 应返回 null")
        void shouldReturnNullWhenSysUserIdIsNull() {
            // given
            TestSysUser sysUserWithNullId = new TestSysUser(null, USERNAME, PASSWORD, authorities);
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUserWithNullId, PASSWORD, authorities));

            // when
            Long userId = SecurityContextUtils.getUserIdOrNull();

            // then
            assertThat(userId).isNull();
        }
    }

    // ========================================
    // getRequiredUserId 测试
    // ========================================
    @Nested
    @DisplayName("getRequiredUserId 方法测试")
    class GetRequiredUserIdTest {

        @Test
        @DisplayName("SysUser 登录 - 应返回用户 ID")
        void shouldReturnUserIdForSysUser() {
            SysUser sysUser = new SysUser();
            sysUser.setId(USER_ID);
            var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities);
            // given
            setAuthentication(usernamePasswordAuthenticationToken);

            // when
            Long userId = SecurityContextUtils.getRequiredUserId();

            // then
            assertThat(userId).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("未登录 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenNotLoggedIn() {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            assertThatThrownBy(SecurityContextUtils::getRequiredUserId)
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode", "status")
                    .containsExactly(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("匿名用户 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when & then
            assertThatThrownBy(SecurityContextUtils::getRequiredUserId)
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("非 SysUser - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionForNonSysUser() {
            // given
            setAuthentication(createAuthenticatedNonSysUser());

            // when & then
            assertThatThrownBy(SecurityContextUtils::getRequiredUserId)
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("SysUser 的 ID 为 null - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenSysUserIdIsNull() {
            // given
            TestSysUser sysUserWithNullId = new TestSysUser(null, USERNAME, PASSWORD, authorities);
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUserWithNullId, PASSWORD, authorities));

            // when & then
            assertThatThrownBy(SecurityContextUtils::getRequiredUserId)
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("未认证 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenNotAuthenticated() {
            // given
            setAuthentication(createUnauthenticatedAuthentication());

            // when & then
            assertThatThrownBy(SecurityContextUtils::getRequiredUserId)
                    .isInstanceOf(CustomException.class);
        }
    }

    // ========================================
    // getCurrentAuthorities 测试
    // ========================================
    @Nested
    @DisplayName("getCurrentAuthorities 方法测试")
    class GetCurrentAuthoritiesTest {

        @Test
        @DisplayName("正常登录用户 - 应返回权限集合")
        void shouldReturnAuthoritiesWhenLoggedIn() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when
            Collection<? extends GrantedAuthority> result = SecurityContextUtils.getCurrentAuthorities();

            // then
            assertThat(result)
                    .hasSize(2)
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly(ROLE_ADMIN, ROLE_USER);
        }

        @Test
        @DisplayName("空权限集合 - 应返回空集合")
        void shouldReturnEmptyAuthorities() {
            // given
            TestSysUser sysUser = new TestSysUser(USER_ID, USERNAME, PASSWORD, Collections.emptyList());
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, Collections.emptyList()));

            // when
            Collection<? extends GrantedAuthority> result = SecurityContextUtils.getCurrentAuthorities();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("未登录 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenNotLoggedIn() {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            assertThatThrownBy(SecurityContextUtils::getCurrentAuthorities)
                    .isInstanceOf(CustomException.class)
                    .extracting("errorCode", "status")
                    .containsExactly(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("匿名用户 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionForAnonymousUser() {
            // given
            setAuthentication(createAnonymousAuthentication());

            // when & then
            assertThatThrownBy(SecurityContextUtils::getCurrentAuthorities)
                    .isInstanceOf(CustomException.class);
        }
    }

    // ========================================
    // hasRole 测试
    // ========================================
    @Nested
    @DisplayName("hasRole 方法测试")
    class HasRoleTest {

        @Test
        @DisplayName("拥有该角色 - 应返回 true")
        void shouldReturnTrueWhenHasRole() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when
            boolean result = SecurityContextUtils.hasRole(ROLE_ADMIN);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("不拥有该角色 - 应返回 false")
        void shouldReturnFalseWhenNotHasRole() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when
            boolean result = SecurityContextUtils.hasRole("ROLE_SUPER_ADMIN");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("空权限集合 - 应返回 false")
        void shouldReturnFalseWhenNoAuthorities() {
            // given
            TestSysUser sysUser = new TestSysUser(USER_ID, USERNAME, PASSWORD, Collections.emptyList());
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, Collections.emptyList()));

            // when
            boolean result = SecurityContextUtils.hasRole(ROLE_ADMIN);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("角色名完全匹配（区分大小写）- 应返回 true")
        void shouldMatchRoleExactly() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when
            boolean result = SecurityContextUtils.hasRole("role_admin"); // 小写

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("未登录 - 应抛出 UNAUTHORIZED 异常")
        void shouldThrowExceptionWhenNotLoggedIn() {
            // given
            SecurityContextHolder.clearContext();

            // when & then
            assertThatThrownBy(() -> SecurityContextUtils.hasRole(ROLE_ADMIN))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("验证多个角色 - 逐个验证")
        void shouldCheckMultipleRoles() {
            // given
            setAuthentication(createAuthenticatedSysUser());

            // when & then
            assertThat(SecurityContextUtils.hasRole(ROLE_ADMIN)).isTrue();
            assertThat(SecurityContextUtils.hasRole(ROLE_USER)).isTrue();
            assertThat(SecurityContextUtils.hasRole("ROLE_GUEST")).isFalse();
        }
    }

    // ========================================
    // 边界场景测试
    // ========================================
    @Nested
    @DisplayName("边界场景测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("连续调用多次 - 应返回一致结果")
        void shouldReturnConsistentResultsOnMultipleCalls() {
            // given
            SysUser sysUser = new SysUser();
            sysUser.setId(USER_ID);
            sysUser.setUsername(USERNAME);
            var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities);
            setAuthentication(usernamePasswordAuthenticationToken);

            // when & then
            for (int i = 0; i < 5; i++) {
                assertThat(SecurityContextUtils.getCurrentUsername()).isEqualTo(USERNAME);
                assertThat(SecurityContextUtils.getUserIdOrNull()).isEqualTo(USER_ID);
                assertThat(SecurityContextUtils.getRequiredUserId()).isEqualTo(USER_ID);
            }
        }

        @Test
        @DisplayName("SecurityContext 被替换后 - 应返回新值")
        void shouldReturnNewValueAfterContextReplacement() {
            // given - 第一次设置
            SysUser sysUser = new SysUser();
            sysUser.setId(USER_ID);
            sysUser.setUsername(USERNAME);
            var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities);
            setAuthentication(usernamePasswordAuthenticationToken);
            assertThat(SecurityContextUtils.getCurrentUsername()).isEqualTo(USERNAME);

            // when - 替换为另一个用户
            SysUser anotherUser = new SysUser();
            anotherUser.setId(9999L);
            anotherUser.setUsername("anotherUser");
            setAuthentication(new UsernamePasswordAuthenticationToken(anotherUser, "pwd", Collections.emptyList()));

            // then
            assertThat(SecurityContextUtils.getCurrentUsername()).isEqualTo("anotherUser");
            assertThat(SecurityContextUtils.getUserIdOrNull()).isEqualTo(9999L);
        }

        @Test
        @DisplayName("从登录状态变为未登录 - 应正确处理")
        void shouldHandleTransitionFromLoggedInToLoggedOut() {
            // given - 先登录
            SysUser sysUser = new SysUser();
            sysUser.setId(USER_ID);
            sysUser.setUsername(USERNAME);
            var usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities);
            setAuthentication(usernamePasswordAuthenticationToken);
            assertThat(SecurityContextUtils.getUserIdOrNull()).isEqualTo(USER_ID);

            // when - 退出登录
            SecurityContextHolder.clearContext();

            // then
            assertThat(SecurityContextUtils.getUserIdOrNull()).isNull();
            assertThat(SecurityContextUtils.getUsernameOrNull()).isNull();
            assertThat(SecurityContextUtils.getCurrentAuthentication()).isEmpty();
        }

        @Test
        @DisplayName("用户名为空字符串 - 应正常处理")
        void shouldHandleEmptyUsername() {
            // given
            TestSysUser sysUser = new TestSysUser(USER_ID, "", PASSWORD, authorities);
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, authorities));

            // when
            String username = SecurityContextUtils.getCurrentUsername();

            // then
            assertThat(username).isEmpty();
        }

        @Test
        @DisplayName("大量权限集合 - hasRole 应正常工作")
        void shouldHandleLargeAuthoritySet() {
            // given
            List<SimpleGrantedAuthority> largeAuthorities = java.util.stream.IntStream.range(0, 1000)
                    .mapToObj(i -> new SimpleGrantedAuthority("ROLE_" + i))
                    .collect(java.util.stream.Collectors.toList());
            largeAuthorities.add(new SimpleGrantedAuthority("TARGET_ROLE"));

            TestSysUser sysUser = new TestSysUser(USER_ID, USERNAME, PASSWORD, largeAuthorities);
            setAuthentication(new UsernamePasswordAuthenticationToken(sysUser, PASSWORD, largeAuthorities));

            // when
            boolean hasTargetRole = SecurityContextUtils.hasRole("TARGET_ROLE");
            boolean hasMissingRole = SecurityContextUtils.hasRole("MISSING_ROLE");

            // then
            assertThat(hasTargetRole).isTrue();
            assertThat(hasMissingRole).isFalse();
        }
    }
}
