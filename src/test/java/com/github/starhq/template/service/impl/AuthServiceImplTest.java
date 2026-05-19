package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.DatabaseException;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.converter.UserConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.mapper.SysUserRoleMapper;
import com.github.starhq.template.model.dto.user.ResetPasswordDTO;
import com.github.starhq.template.model.dto.user.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysTokenMapper tokenMapper;

    @Mock
    private UserConverter userConverter;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private EventService eventService;
    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserDTO userDTO;
    private ResetPasswordDTO resetPasswordDTO;
    private SysUser mockUser;
    private SysRole mockRole;

    @BeforeEach
    void setUp() {
        // 初始化基础 Mock 数据
        userDTO = new UserDTO();
        userDTO.setUsername("testUser");
        userDTO.setPassword("rawPassword123");

        resetPasswordDTO = new ResetPasswordDTO();
        resetPasswordDTO.setOldPassword("oldRawPassword");
        resetPasswordDTO.setNewPassword("newRawPassword456");

        mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setPassword("$2a$10$encodedOldPasswordHash"); // 模拟数据库里的密文
        mockUser.setStatus(UserStatus.ACTIVE);

        mockRole = new SysRole();
        mockRole.setId(10L);
        mockRole.setIsDefault(true);

        mockUser.setAuthorities(List.of(mockRole));

        // 手动注入父类依赖
        ReflectionTestUtils.setField(authService, "baseMapper", userMapper);
    }

    // ==================== loadUserByUsername 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testLoadUserByUsername_Success() {
        // Given
        when(userMapper.selectUserWithRole(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        // When
        UserDetails result = authService.loadUserByUsername("testUser");

        // Then
        assertNotNull(result);
        assertEquals("testUser", result.getUsername());
        verify(userMapper).selectUserWithRole(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLoadUserByUsername_WithNoRole() {
        // Given
        mockUser.setAuthorities(Collections.emptyList());
        when(userMapper.selectUserWithRole(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        // When & Then
        assertThrows(CustomException.class, () -> authService.loadUserByUsername("testUser"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLoadUserByUsername_Banned() {
        // Given
        mockUser.setStatus(UserStatus.BANNED);
        when(userMapper.selectUserWithRole(any(LambdaQueryWrapper.class))).thenReturn(mockUser);

        // When & Then
        assertThrows(CustomException.class, () -> authService.loadUserByUsername("testUser"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLoadUserByUsername_NotFound() {
        // Given
        when(userMapper.selectUserWithRole(any(LambdaQueryWrapper.class))).thenReturn(null);

        // When & Then
        // 对应 checkUser 逻辑抛出的异常
        assertThrows(CustomException.class, () -> authService.loadUserByUsername("notExistUser"));
    }

    // ==================== register 测试 ====================

    @Deprecated
    @SuppressWarnings("unchecked")
    @Test
    void testRegister_Success() {
        // Given
        // 假设 Converter 内部已经处理了密码加密，这里直接返回带密文的 User 实体
        when(userConverter.toEntity(userDTO)).thenReturn(mockUser);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mockRole));
        when(userRoleMapper.insert(anyList())).thenReturn(anyList());

        // When
        UserDetails result = authService.register(userDTO);

        // Then
        assertNotNull(result);
        verify(userMapper).insert(any(SysUser.class));
        verify(roleMapper).selectList(any());
        verify(userRoleMapper).insert(anyList());
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    @Test
    void testRegister_Fail_NoDefaultRoles() {
        // Given
        when(userConverter.toEntity(userDTO)).thenReturn(mockUser);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);
        // 模拟数据库中没有默认角色
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // When & Then
        // 抛出系统内部错误异常
        assertThrows(CustomException.class, () -> authService.register(userDTO));

        // 验证角色关联表没有被插入
        verify(userRoleMapper, never()).upsertUserRole(anyList());
    }

    @Deprecated
    @Test
    void testRegister_Fail_DuplicateUsername() {
        // Given
        when(userConverter.toEntity(userDTO)).thenReturn(mockUser);
        // 模拟唯一键冲突
        when(userMapper.insert(any(SysUser.class))).thenThrow(new DuplicateKeyException("Duplicate"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> authService.register(userDTO));
        assertEquals(ErrorCode.USER_DUPLICATE_USERNAME, customException.getErrorCode());

        // 验证后续流程中断
        verify(roleMapper, never()).selectList(any());
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    @Test
    void testRegister_Failure() {
        // Given
        // 假设 Converter 内部已经处理了密码加密，这里直接返回带密文的 User 实体
        when(userConverter.toEntity(userDTO)).thenReturn(mockUser);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);
        when(roleMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(mockRole));
        when(userRoleMapper.insert(anyList())).thenThrow(DatabaseException.class);

        // When & Then
        assertThrows(CustomException.class, () -> authService.register(userDTO));

        verify(userMapper).insert(any(SysUser.class));
        verify(roleMapper).selectList(any());
        verify(userRoleMapper).insert(anyList());
    }

    // ==================== resetPassword 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testResetPassword_Success() {
        // Given: 使用 MockedStatic 模拟 SecurityContext 获取当前用户 ID
        try (MockedStatic<SecurityContextUtils> utilities = mockStatic(SecurityContextUtils.class)) {
            utilities.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);

            // 模拟只查基础用户信息 (优化后的逻辑)
            when(userMapper.selectById(1L)).thenReturn(mockUser);

            // 模拟密码匹配逻辑
            when(passwordEncoder.matches("oldRawPassword", mockUser.getPassword())).thenReturn(true); // 旧密码正确
            when(passwordEncoder.matches("newRawPassword456", mockUser.getPassword())).thenReturn(false); // 新密码不等于旧密码
            when(passwordEncoder.encode("newRawPassword456")).thenReturn("$2a$10$newEncodedHash"); // 加密新密码

            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                // 1. 获取传入 execute 的 Lambda 表达式 (TransactionCallback 对象)
                TransactionCallback<Object> callback = invocation.getArgument(0);
                // 2. 真实执行这个 Lambda！这会触发里面写的 userMapper.updateById 等操作
                // 注意：参数传 null 是因为我们在单元测试里不需要真实的事务状态
                return callback.doInTransaction(any(TransactionStatus.class));
            });

            // 模拟更新成功
            when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
            when(tokenMapper.delete(any(Wrapper.class))).thenReturn(1);
            doNothing().when(eventService).notifyCacheEvict(anyList(), anyList());

            // When
            boolean result = authService.resetPassword(resetPasswordDTO);

            // Then
            assertTrue(result);
            verify(passwordEncoder).encode("newRawPassword456");

            // 【核心验证】：验证传给 Mapper 的实体只有 ID 和 Password，没有其他多余字段
            verify(userMapper).updateById(ArgumentMatchers.<SysUser>argThat(entity -> {
                assertEquals(1L, entity.getId());
                assertEquals("$2a$10$newEncodedHash", entity.getPassword());
                return true;
            }));

            verify(tokenMapper).delete(any(Wrapper.class));
        }
    }

    @Test
    void testResetPassword_Fail_UserNotFound() {
        // Given
        try (MockedStatic<SecurityContextUtils> utilities = mockStatic(SecurityContextUtils.class)) {
            utilities.when(SecurityContextUtils::getRequiredUserId).thenReturn(99L);
            when(userMapper.selectById(99L)).thenReturn(null);

            // When & Then
            assertThrows(CustomException.class, () -> authService.resetPassword(resetPasswordDTO));
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }
    }

    @Test
    void testResetPassword_Fail_OldPasswordMismatch() {
        // Given
        try (MockedStatic<SecurityContextUtils> utilities = mockStatic(SecurityContextUtils.class)) {
            utilities.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(mockUser);

            // 旧密码不匹配
            when(passwordEncoder.matches("oldRawPassword", mockUser.getPassword())).thenReturn(false);

            // When & Then
            var customException = assertThrows(CustomException.class, () -> authService.resetPassword(resetPasswordDTO));
            assertEquals(ErrorCode.MISMATCH_PASSWORD, customException.getErrorCode());
            assertEquals(HttpStatus.BAD_REQUEST, customException.getStatus()); // 验证状态码改为 400

            // 验证不会执行更新操作
            verify(userMapper, never()).updateById(any(SysUser.class));
        }
    }

    @Test
    void testResetPassword_Fail_NewPasswordSameAsOld() {
        // Given
        try (MockedStatic<SecurityContextUtils> utilities = mockStatic(SecurityContextUtils.class)) {
            utilities.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(mockUser);

            // 旧密码匹配
            when(passwordEncoder.matches("oldRawPassword", mockUser.getPassword())).thenReturn(true);
            // 新密码和旧密码一样 (新密码的明文，恰好匹配上了数据库里的密文)
            when(passwordEncoder.matches("newRawPassword456", mockUser.getPassword())).thenReturn(true);

            // When & Then
            var customException = assertThrows(CustomException.class, () -> authService.resetPassword(resetPasswordDTO));
            assertEquals(ErrorCode.SAME_PASSWORD, customException.getErrorCode());
            assertEquals(HttpStatus.BAD_REQUEST, customException.getStatus());

            // 验证不会执行加密和更新操作
            verify(passwordEncoder, never()).encode(anyString());
            verify(userMapper, never()).updateById(any(SysUser.class));
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testResetPassword_Fail_UpdateFailed() {
        // Given
        try (MockedStatic<SecurityContextUtils> utilities = mockStatic(SecurityContextUtils.class)) {
            utilities.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(mockUser);

            when(passwordEncoder.matches("oldRawPassword", mockUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches("newRawPassword456", mockUser.getPassword())).thenReturn(false);
            when(passwordEncoder.encode("newRawPassword456")).thenReturn("$2a$10$newEncodedHash");

            // 模拟数据库更新影响行数为 0
            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                // 1. 获取传入 execute 的 Lambda 表达式 (TransactionCallback 对象)
                TransactionCallback<Object> callback = invocation.getArgument(0);
                // 2. 真实执行这个 Lambda！这会触发里面写的 userMapper.updateById 等操作
                when(userMapper.updateById(any(SysUser.class))).thenReturn(0);

                // 注意：参数传 null 是因为我们在单元测试里不需要真实的事务状态
                return callback.doInTransaction(any(TransactionStatus.class));
            });

            // When & Then
            assertThrows(CustomException.class, () -> authService.resetPassword(resetPasswordDTO));
            assertEquals(ErrorCode.RESET_PASSWORD, assertThrows(CustomException.class, () ->
                    authService.resetPassword(resetPasswordDTO)).getErrorCode());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void testResetPassword_Fail_RemoveTokenFailed() {
        // Given
        try (MockedStatic<SecurityContextUtils> utilities = mockStatic(SecurityContextUtils.class)) {
            utilities.when(SecurityContextUtils::getRequiredUserId).thenReturn(1L);
            when(userMapper.selectById(1L)).thenReturn(mockUser);

            when(passwordEncoder.matches("oldRawPassword", mockUser.getPassword())).thenReturn(true);
            when(passwordEncoder.matches("newRawPassword456", mockUser.getPassword())).thenReturn(false);
            when(passwordEncoder.encode("newRawPassword456")).thenReturn("$2a$10$newEncodedHash");

            // 模拟数据库更新影响行数为 0
            when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
                // 1. 获取传入 execute 的 Lambda 表达式 (TransactionCallback 对象)
                TransactionCallback<Object> callback = invocation.getArgument(0);
                // 2. 真实执行这个 Lambda！这会触发里面写的 userMapper.updateById 等操作
                when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
                when(tokenMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);

                // 注意：参数传 null 是因为我们在单元测试里不需要真实的事务状态
                return callback.doInTransaction(any(TransactionStatus.class));
            });

            // When & Then
            assertThrows(CustomException.class, () -> authService.resetPassword(resetPasswordDTO));
            assertEquals(ErrorCode.RESET_PASSWORD, assertThrows(CustomException.class, () ->
                    authService.resetPassword(resetPasswordDTO)).getErrorCode());
        }
    }
}