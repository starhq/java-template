package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.DatabaseException;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.config.security.jwt.JwtService;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.converter.TokenConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.model.dto.RequestContext;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.token.TokenSimpleDTO;
import com.github.starhq.template.model.vo.token.TokenPageVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: Token service test cases
 * @date 2026/3/24 23:07
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private SysTokenMapper tokenMapper;

    @Mock
    private TokenConverter tokenConverter;

    @Mock
    private JwtService jwtService;

    @Mock
    private CacheHelper cacheHelper;

    @InjectMocks
    private TokenServiceImpl tokenService;

    private SysUser mockUser;
    private SysToken mockToken;
    private TokenSimpleDTO mockTokenDTO;
    private JwtToken token;

    @BeforeEach
    void setUp() {
        // 初始化通用 Mock 对象
        mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setStatus(UserStatus.ACTIVE);

        SysRole role = new SysRole();
        role.setId(1L);
        role.setCode("T_TEST");
        mockUser.setAuthorities(List.of(role));

        mockToken = new SysToken();
        mockToken.setId(100L);
        mockToken.setUserId(1L);
        mockToken.setAccessToken("mock-access-token");
        mockToken.setRefreshToken("mock-refresh-token");
        mockToken.setExpiredAt(OffsetDateTime.now().plusHours(1));
        mockToken.setRevoked(false);

        mockTokenDTO = new TokenSimpleDTO();
        mockTokenDTO.setAccessToken("mock-access-token");

        token = JwtToken.builder().accessToken("acc").refreshToken("ref").tokenType("Bear").expiresIn(3600L).build();

    }

    // ====================== build 测试 ======================

    @Test
    void testBuild_Success() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        // 2. 使用 try-with-resources Mock 静态方法
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            // 当调用 getRequestAttributes 时，返回我们模拟的 attributes
            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);

            // Mock JWT 生成
            when(jwtService.build(anyMap(), anyString())).thenReturn(token);

            // Mock Converter
            when(tokenConverter.toSimpleDTO(any(SysToken.class))).thenReturn(mockTokenDTO);

            // When
            var result = tokenService.build(mockUser);

            // Then
            assertNotNull(result);

            // 验证 Token 保存逻辑 (Upsert)
            verify(tokenMapper).upsertToken(any(SysToken.class));

            // 验证缓存更新
            verify(cacheHelper).put(eq(1L), any(TokenSimpleDTO.class), anyString());
        }
    }

    @Test
    void testBuild_WrongUserType() {
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        // 2. 使用 try-with-resources Mock 静态方法
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            // 当调用 getRequestAttributes 时，返回我们模拟的 attributes
            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);

            // When
            UserDetails userDetails = new User("name", "password", Collections.emptyList());


            // Then
            assertThrows(CustomException.class, () -> tokenService.build(userDetails));

            // 验证 Token 保存逻辑 (Upsert)
            verify(tokenMapper, never()).upsertToken(any(SysToken.class));

            verify(tokenConverter, never()).toSimpleDTO(any(SysToken.class));
        }
    }

    @Test
    void testBuild_Upset_Failure() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        // 2. 使用 try-with-resources Mock 静态方法
        try (MockedStatic<RequestContextUtil> mockedStatic = mockStatic(RequestContextUtil.class)) {
            // 当调用 getRequestAttributes 时，返回我们模拟的 attributes
            mockedStatic.when(RequestContextUtil::getContext).thenReturn(context);
            // Mock JWT 生成
            when(jwtService.build(anyMap(), anyString())).thenReturn(token);

            // Mock upset
            doThrow(new RuntimeException("DB Error")).when(tokenMapper).upsertToken(any(SysToken.class));
            // When & Then
            assertThrows(RuntimeException.class, () -> tokenService.build(mockUser));
        }
    }

    // ====================== refresh 测试 ======================

    @Test
    void testRefresh_Success() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        // 1. 准备模拟的 Request 和 Response
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100"); // 模拟 IP

        ServletRequestAttributes attributes = new ServletRequestAttributes(request);

        // Mock 静态方法 checkUserStatus (需要 mockito-inline)
        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class);
             MockedStatic<RequestContextHolder> mockedRequestStatic = mockStatic(RequestContextHolder.class);
             MockedStatic<RequestContextUtil> mockedRequestContextStatic = mockStatic(RequestContextUtil.class)) {
            // 检查用户状态
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            mockedStatic.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
            mockedStatic.when(SecurityContextUtils::getCurrentAuthorities).thenReturn(List.of(new SysRole()));
            // 当调用 getRequestAttributes 时，返回我们模拟的 attributes
            mockedRequestStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            mockedRequestContextStatic.when(RequestContextUtil::getContext).thenReturn(context);

            // Mock JWT 生成
            when(jwtService.build(anyMap(), anyString())).thenReturn(token);

            // Mock Converter
            when(tokenConverter.toSimpleDTO(any(SysToken.class))).thenReturn(mockTokenDTO);

            // When
            var result = tokenService.refresh();

            // Then
            assertNotNull(result);
            verify(tokenMapper).upsertToken(any(SysToken.class));
        }
    }

    @Test
    void testRefresh_UserWithNoRole() {
        RequestContext context = new RequestContext("device_finger_print", "192.168.1.100");

        // 1. 准备模拟的 Request 和 Response
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100"); // 模拟 IP

        ServletRequestAttributes attributes = new ServletRequestAttributes(request);

        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class);
             MockedStatic<RequestContextHolder> mockedRequestStatic = mockStatic(RequestContextHolder.class);
             MockedStatic<RequestContextUtil> mockedRequestContextStatic = mockStatic(RequestContextUtil.class)) {
            // 当调用 getRequestAttributes 时，返回我们模拟的 attributes
            mockedRequestStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);
            // Given
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            mockedStatic.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
            mockedStatic.when(SecurityContextUtils::getCurrentAuthorities).thenReturn(Collections.emptyList());

            mockedRequestContextStatic.when(RequestContextUtil::getContext).thenReturn(context);

            // When & Then

            CustomException exception = assertThrows(CustomException.class, () -> tokenService.refresh());
            assertEquals(ErrorCode.NO_ROLES, exception.getErrorCode());
        }
    }

    @Test
    void testRefresh_UserNotFound() {
        // When & Then

        CustomException exception = assertThrows(CustomException.class, () -> tokenService.refresh());
        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void testRefresh_Upset_Failure() {
        // Given
        String deviceFingerprint = "device-123";
        String ip = "192.168.1.100";

        RequestContext context = new RequestContext(deviceFingerprint, ip);

        // 1. 准备模拟的 Request 和 Response
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100"); // 模拟 IP

        ServletRequestAttributes attributes = new ServletRequestAttributes(request);

        // Mock 静态方法 checkUserStatus (需要 mockito-inline)
        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class);
             MockedStatic<RequestContextHolder> mockedRequestStatic = mockStatic(RequestContextHolder.class);
             MockedStatic<RequestContextUtil> mockedRequestContextStatic = mockStatic(RequestContextUtil.class)) {
            // 检查用户状态
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            mockedStatic.when(SecurityContextUtils::getCurrentUsername).thenReturn("admin");
            mockedStatic.when(SecurityContextUtils::getCurrentAuthorities).thenReturn(List.of(new SysRole()));


            // 当调用 getRequestAttributes 时，返回我们模拟的 attributes
            mockedRequestStatic.when(RequestContextHolder::getRequestAttributes).thenReturn(attributes);

            mockedRequestContextStatic.when(RequestContextUtil::getContext).thenReturn(context);

            // Mock JWT 生成
            when(jwtService.build(anyMap(), anyString())).thenReturn(token);

            // Mock Upset
            doThrow(new RuntimeException("Db Error")).when(tokenMapper).upsertToken(any(SysToken.class));

            // When & Then
            assertThrows(RuntimeException.class, () -> tokenService.refresh());
        }
    }

    // ====================== page 测试 ======================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_noKeyword_returnsEmptyPage() {
        // 准备
        KeyWordPageRequest request = new KeyWordPageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<TokenPageVO> emptyPage = new Page<>(1, 10, 0);
        emptyPage.setRecords(Collections.emptyList());

        when(tokenMapper.selectTokenPage(any(), any(Wrapper.class))).thenReturn(emptyPage);

        // 执行
        IPage<TokenPageVO> result = tokenService.page(request);

        // 验证
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        KeyWordPageRequest request = new KeyWordPageRequest();
        request.setPage(1L);
        request.setSize(10L);
        request.setKeyword("admin");

        IPage<TokenPageVO> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(new TokenPageVO()));

        // Mock Mapper 查询
        when(tokenMapper.selectTokenPage(any(), any(Wrapper.class))).thenReturn(mockDbPage);

        // When
        var result = tokenService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(tokenMapper).selectTokenPage(any(Page.class), any(Wrapper.class));
    }


    // ====================== removeByUserId 测试 ======================

    @Test
    void testRemoveByUserId_Success() {
        // Given
        Long userId = 1L;
        when(tokenMapper.delete(any())).thenReturn(1);

        // When
        boolean result = tokenService.removeByUserId(userId);

        // Then
        assertTrue(result);
        // 验证删除条件
        verify(tokenMapper).delete(any());
        // 注意：@CacheEvict 是 AOP，在单元测试中不生效，不需要 verify cache
    }

    @Test
    void testRemoveByUserId_NotFound() {
        // Given
        Long userId = 1L;
        when(tokenMapper.delete(any())).thenReturn(0); // 删除 0 条

        // When
        boolean result = tokenService.removeByUserId(userId);

        // Then
        assertFalse(result);
    }

    @Test
    void testRemoveByUserId_Failure() {
        // Given
        Long userId = 1L;
        when(tokenMapper.delete(any())).thenThrow(DatabaseException.class);

        // When & Then
        assertThrows(CustomException.class, () -> tokenService.removeByUserId(userId));
    }

    // ====================== getByUserId 测试 ======================

    @Test
    void testGetByUserId_Success() {
        // Given
        Long userId = 1L;
        when(tokenMapper.selectOne(any())).thenReturn(mockToken);
        when(tokenConverter.toSimpleDTO(mockToken)).thenReturn(mockTokenDTO);

        // When
        TokenSimpleDTO result = tokenService.getByUserId(userId);

        // Then
        assertNotNull(result);
        assertEquals("mock-access-token", result.getAccessToken());
        verify(tokenMapper).selectOne(any());
    }

    @Test
    void testGetByUserId_NotFound() {
        // Given
        Long userId = 1L;
        when(tokenMapper.selectOne(any())).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> tokenService.getByUserId(userId));
    }
}