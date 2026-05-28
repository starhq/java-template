package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.converter.UserConverter;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.RelationHelper;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.mapper.SysUserRoleMapper;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private SysTokenMapper tokenMapper;

    @Mock
    private SysRoleMapper roleMapper;


    @Mock
    private UserConverter userConverter;

    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private EventService eventService;


    @InjectMocks
    private UserServiceImpl userService;

    private SysUser mockUser;
    private UserDTO request;

    @BeforeEach
    void setUp() {
        mockUser = new SysUser();
        mockUser.setId(1L);
        mockUser.setUsername("testUser");
        mockUser.setCreatedBy(2L);
        mockUser.setUpdatedBy(3L);

        request = new UserDTO();
        request.setRoleIds(Set.of(1L, 2L));

        // @Mock
        RelationHelper relationHelper = new RelationHelper();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
        ReflectionTestUtils.setField(userService, "cacheHelper", cacheHelper);
        ReflectionTestUtils.setField(userService, "relationHelper", relationHelper);
    }

    // ---------------------- page 测试 ----------------------

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        KeyWordPageRequest pageRequest = new KeyWordPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);
        pageRequest.setSort("id");
        pageRequest.setKeyword("test");

        IPage<SysUser> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockUser));


        // Mock selectPage 返回结果
        when(userMapper.selectPage(any(), any(Wrapper.class))).thenReturn(mockDbPage);


        UserPageVO mockVo = new UserPageVO();
        mockVo.setId(1L);
        when(userConverter.toPageVO(mockUser)).thenReturn(mockVo);

        // When
        IPage<UserPageVO> result = userService.page(pageRequest);

        // Then
        assertNotNull(result);
        assertEquals("id", pageRequest.getSort());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());

        // 验证 Converter 被调用
        verify(userConverter).toPageVO(mockUser);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_WithNoKeyword_Success() {
        // Given
        KeyWordPageRequest pageRequest = new KeyWordPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);

        IPage<SysUser> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockUser));


        // Mock selectPage 返回结果
        when(userMapper.selectPage(any(), any(Wrapper.class))).thenReturn(mockDbPage);


        UserPageVO mockVo = new UserPageVO();
        mockVo.setId(1L);
        when(userConverter.toPageVO(mockUser)).thenReturn(mockVo);

        // When
        IPage<UserPageVO> result = userService.page(pageRequest);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());

        // 验证 Converter 被调用
        verify(userConverter).toPageVO(mockUser);
    }


    @SuppressWarnings("unchecked")
    @Test
    void testPage_ReturnsEmptyPage() {
        // Given
        KeyWordPageRequest pageRequest = new KeyWordPageRequest();
        pageRequest.setPage(1L);
        pageRequest.setSize(10L);
        pageRequest.setKeyword("test");

        IPage<SysUser> mockDbPage = new Page<>(1, 10, 0);

        // Mock selectPage 返回结果
        when(userMapper.selectPage(any(), any(Wrapper.class))).thenReturn(mockDbPage);

        // When
        IPage<UserPageVO> result = userService.page(pageRequest);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        // 验证 Converter 被调用
        verify(userMapper).selectPage(any(), any(Wrapper.class));
        verify(userMapper, never()).selectList(any());
    }

    // ---------------------- getUserById 测试 ----------------------

    @Test
    void testGetUserById_Success() {
        // Given
        Long id = 1L;
        when(userMapper.selectById(id)).thenReturn(mockUser);
        UserSimpleVO mockVo = new UserSimpleVO();
        when(userConverter.toSimpleVO(mockUser)).thenReturn(mockVo);

        // When
        UserSimpleVO result = userService.getUserById(id);

        // Then
        assertNotNull(result);
        assertEquals(mockVo, result);
        verify(userMapper).selectById(id);
        verify(userConverter).toSimpleVO(mockUser);
    }

    @Test
    void testGetUserById_NullId_UseSecurityContext() {
        // 模拟 SecurityContext
        try (MockedStatic<SecurityContextUtils> mockedStatic = mockStatic(SecurityContextUtils.class)) {
            mockedStatic.when(SecurityContextUtils::getRequiredUserId).thenReturn(99L);
            when(userMapper.selectById(99L)).thenReturn(mockUser);

            UserSimpleVO mockVo = new UserSimpleVO();
            when(userConverter.toSimpleVO(mockUser)).thenReturn(mockVo);

            // When
            UserSimpleVO result = userService.getUserById(null);

            // Then
            assertNotNull(result);
            verify(userMapper).selectById(99L);
        }
    }

    @Test
    void testGetUserById_NotFound() {
        // Given
        Long id = 1L;
        when(userMapper.selectById(id)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> userService.getUserById(id));
    }

    // ---------------------- createUser 测试 ----------------------

    @Test
    void createUser_Success_WithRoles() {
        // Given: 模拟转换和数据库插入成功
        when(userConverter.toEntity(request)).thenReturn(mockUser);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1); // 假设底层 insert 影响行数为 1

        // 模拟角色校验通过：传了 2 个角色 ID，查出来也是 2 个
        when(roleMapper.selectCount(any())).thenReturn(2L);

        // When: 执行创建方法
        boolean result = userService.createUser(request);

        // Then: 验证结果和行为
        assertTrue(result);

        // 验证转换被调用
        verify(userConverter).toEntity(request);
        // 验证插入被调用
        verify(userMapper).insert(any(SysUser.class));

        // 验证角色关联逻辑被触发：校验被调用
        verify(roleMapper, times(1)).selectCount(any());
        // 验证角色关联逻辑被触发：批量插入被调用 (根据你实际 upsertUserRole 的参数类型可能需要调整为 anyList() 等)
        verify(userRoleMapper, times(1)).upsertUserRole(anyList());
    }

    @Test
    void createUser_Fail_DuplicateUsername() {
        // Given: 模拟插入时发生异常（比如触发唯一索引冲突）
        // 根据你的 insert 封装逻辑，如果抛出 BusinessException，这里就模拟抛出异常
        when(userConverter.toEntity(request)).thenReturn(mockUser);
        when(userMapper.insert(any(SysUser.class)))
                .thenThrow(new DuplicateKeyException("Same username"));

        // When & Then: 预期抛出业务异常
        CustomException exception = assertThrows(CustomException.class, () -> userService.createUser(request));

        // 可选：验证异常类型是否正确
        assertEquals(ErrorCode.USER_DUPLICATE_USERNAME, exception.getErrorCode());

        // 验证：如果前面插入失败了，后续的分配角色逻辑绝不应该被执行
        verify(roleMapper, never()).selectCount(any());
        verify(userRoleMapper, never()).upsertUserRole(anyList());
    }

    @Test
    void createUser_Fail_RoleNotFound() {
        // Given: 用户插入成功，但是分配了一个不存在的角色 ID
        when(userConverter.toEntity(request)).thenReturn(mockUser);
        when(userMapper.insert(any(SysUser.class))).thenReturn(1);

        // 模拟校验失败：传了 2 个 ID，数据库只查出 1 个
        when(roleMapper.selectCount(any())).thenReturn(1L);

        // When & Then: 预期在分配角色时抛出 NotFoundException (这里假设 validateEntityExists 抛出的是 NotFoundException 或其子类)
        // 如果抛出的是 BusinessException，请改成 assertThrows(BusinessException.class)
        assertThrows(Exception.class, () -> userService.createUser(request));
    }

    // ---------------------- updateUser 测试 ----------------------

    @Test
    void testUpdateUser_Success() {
        // Given
        Long id = 1L;
        when(userMapper.selectById(id)).thenReturn(mockUser);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        when(roleMapper.selectCount(any())).thenReturn(2L);

        // When
        boolean result = userService.updateUser(id, request);

        // Then
        assertTrue(result);
        verify(userMapper).updateById(any(SysUser.class));
        verify(eventService).notifyCacheEvict(anyList(), anyList());
    }

    @Test
    void testUpdateUser_ClearAssociations() {
        // Given
        request.setRoleIds(Collections.emptySet());
        when(userMapper.selectById(1L)).thenReturn(mockUser);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // When
        var result = userService.updateUser(1L, request);

        // Then: 应该删除旧角色，但不插入新角色
        assertTrue(result);
    }

    @Test
    void testUpdateUser_NotFound() {
        // Given
        Long id = 1L;
        when(userMapper.selectById(id)).thenReturn(mockUser);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(0);

        // When & Then
        // 假设 DuplicateException 继承自 RuntimeException
        var customException = assertThrows(CustomException.class, () -> userService.updateUser(1L,
                request));
        assertEquals(ErrorCode.USER_NOT_FOUND, customException.getErrorCode());
    }

    @Test
    void testUpdateUser_DuplicateUsername() {
        // Given
        request.setRoleIds(Collections.emptySet());
        when(userMapper.selectById(1L)).thenReturn(mockUser);
        when(userMapper.updateById(any(SysUser.class))).thenThrow(new DuplicateKeyException("Duplicate entry"));

        // When & Then
        // 假设 DuplicateException 继承自 RuntimeException
        var runtimeException = assertThrows(CustomException.class, () -> userService.updateUser(1L,
                request));
        assertEquals(ErrorCode.USER_DUPLICATE_USERNAME, runtimeException.getErrorCode());
    }

    @Test
    void testUpdateUser_InvalidRoleIds() {
        // Given
        Long id = 1L;
        when(userMapper.selectById(id)).thenReturn(mockUser);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // When & Then
        // 假设 DuplicateException 继承自 RuntimeException
        var runtimeException = assertThrows(CustomException.class, () -> userService.updateUser(1L,
                request));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, runtimeException.getErrorCode());
    }

    @Test
    void testUpdateUser_UpsertFailure() {
        // Given
        Long id = 1L;
        when(userMapper.selectById(id)).thenReturn(mockUser);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        // When & Then
        var businessException = assertThrows(NotFoundException.class, () -> userService.updateUser(1L, request));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, businessException.getErrorCode());
    }

    // ---------------------- removeById 测试 ----------------------

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_Success() {
        // Given
        Long id = 1L;
        when(tokenMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userMapper.deleteById(id)).thenReturn(1);

        // When
        boolean result = userService.removeById(id);

        // Then
        assertTrue(result);
        verify(tokenMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(userMapper).deleteById(id);

    }

    @Test
    void testRemoveById_NotFound() {
        // Given
        Long id = 1L;
        when(tokenMapper.delete(any())).thenReturn(1);
        when(userRoleMapper.delete(any())).thenReturn(1);
        when(userMapper.deleteById(id)).thenReturn(0); // 返回 0 表示没删到

        // When & Then
        var notFoundException = assertThrows(NotFoundException.class, () -> userService.removeById(id));
        assertEquals(ErrorCode.USER_NOT_FOUND, notFoundException.getErrorCode());
    }

    @Test
    void testRemoveById_DbException() {
        // Given
        Long id = 1L;
        when(tokenMapper.delete(any())).thenReturn(1);
        when(userRoleMapper.delete(any())).thenReturn(1);
        when(userMapper.deleteById(id)).thenThrow(new RuntimeException(
                "DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> userService.removeById(id));
        assertEquals(ErrorCode.USER_DELETE_FAILED, customException.getErrorCode());
    }
}
