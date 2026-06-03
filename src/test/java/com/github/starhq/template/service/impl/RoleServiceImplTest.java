package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.DatabaseException;
import com.github.starhq.template.converter.RoleConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.helper.RelationHelper;
import com.github.starhq.template.mapper.*;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.dto.RoleDTO;
import com.github.starhq.template.model.vo.RoleCheckVO;
import com.github.starhq.template.model.vo.RolePageVO;
import com.github.starhq.template.model.vo.RoleSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysResourceMapper resourceMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysButtonMapper buttonMapper;

    @Mock
    private SysRoleResourceMapper roleResourceMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private SysRoleButtonMapper roleButtonMapper;

    @Mock
    private SysUserRoleMapper userRoleMapper;

    @Mock
    private RoleConverter roleConverter;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private EventService eventService;

    @InjectMocks
    private RoleServiceImpl roleService;

    private SysRole mockRole;
    private RoleDTO roleDTO;

    @BeforeEach
    void setUp() {
        mockRole = new SysRole();
        mockRole.setId(1L);
        mockRole.setCode("ADMIN");
        mockRole.setName("Administrator");
        mockRole.setCreatedBy(2L);
        mockRole.setUpdatedBy(3L);

        roleDTO = new RoleDTO();
        roleDTO.setCode("ADMIN");
        roleDTO.setName("Administrator");
        roleDTO.setResourceIds(new HashSet<>(Arrays.asList(101L, 102L)));
        roleDTO.setMenuIds(new HashSet<>(Arrays.asList(201L, 202L)));
        roleDTO.setButtonIds(new HashSet<>(Arrays.asList(301L, 302L)));

        RelationHelper helper = new RelationHelper();
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
        ReflectionTestUtils.setField(roleService, "cacheHelper", cacheHelper);
        ReflectionTestUtils.setField(roleService, "relationHelper", helper);
    }

    // ==================== page 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysRole> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockRole));

        // Mock selectPage 返回结果
        when(roleMapper.selectPage(any(), any(Wrapper.class))).thenReturn(mockDbPage);

        RolePageVO mockVo = new RolePageVO();
        mockVo.setId(1L);
        when(roleConverter.toPageVO(mockRole)).thenReturn(mockVo);

        // When
        IPage<RolePageVO> result = roleService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());

        // 验证 Converter 被调用
        verify(roleConverter).toPageVO(mockRole);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_ReturnsEmptyPage() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysRole> emptyPage = new Page<>(1, 10, 0);

        // Mock selectPage 返回结果
        when(roleMapper.selectPage(any(), any(Wrapper.class))).thenReturn(emptyPage);

        // When
        IPage<RolePageVO> result = roleService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        // 验证查询函数被调用
        verify(roleMapper).selectPage(any(), any(Wrapper.class));
        verify(userMapper, never()).selectList(any());
    }

    // ==================== getRoleById 测试 ====================

    @Test
    void testGetRoleById_Success() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(mockRole);
        RoleSimpleVO vo = new RoleSimpleVO();
        when(roleConverter.toSimpleVO(mockRole)).thenReturn(vo);

        // When
        var result = roleService.getRoleById(1L);

        // Then
        assertNotNull(result);
        verify(roleMapper).selectById(1L);
    }

    @Test
    void testGetRoleById_NotFound() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> roleService.getRoleById(1L));
    }

    // ==================== getSelectCheckedRoles 测试 ====================

    @Test
    void getSelectCheckedRoles_Success() {
        // Given
        Long userId = 1L;
        when(roleMapper.selectRolesByUserId(userId)).thenReturn(List.of(new RoleCheckVO()));

        // When
        var result = roleService.selectCheckedRoles(userId);
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(roleMapper).selectRolesByUserId(1L);
    }

    @Test
    void getSelectCheckedRoles_ReturnsEmpty() {
        // Given
        Long userId = 1L;
        when(roleMapper.selectRolesByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        var result = roleService.selectCheckedRoles(userId);
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(roleMapper).selectRolesByUserId(1L);
    }

    // ==================== updateRole 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateRole_Success() {
        when(roleMapper.selectById(1L)).thenReturn(mockRole);
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        when(resourceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(buttonMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);


        boolean result = roleService.updateRole(1L, roleDTO);

        assertTrue(result);
        // 验证 updateEntity 被调用进行属性拷贝
        verify(roleConverter).updateEntity(roleDTO, mockRole);
        verify(roleMapper).updateById(mockRole);
    }

    @Test
    void testUpdateRole_NotFound() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> roleService.updateRole(1L, roleDTO));
    }

    @Test
    void testUpdateRole_DuplicateCode() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(mockRole);
        when(roleMapper.updateById(any(SysRole.class))).thenThrow(new DuplicateKeyException("Duplicate Key"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.updateRole(1L, roleDTO));
        assertEquals(ErrorCode.ROLE_DUPLICATE_CODE, customException.getErrorCode());
    }

    @Test
    void testUpdateRole_InvalidResourceIds() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(mockRole);
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);


        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.updateRole(1L, roleDTO));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, customException.getErrorCode());

        // 验证后续的 upsert 没有执行
        verify(roleResourceMapper, never()).upsertRoleResource(anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateRole_UpsertFailure() {
        // Given
        when(roleMapper.selectById(1L)).thenReturn(mockRole);
        when(roleMapper.updateById(any(SysRole.class))).thenReturn(1);

        when(resourceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);

        doThrow(DatabaseException.class).when(roleResourceMapper).upsertRoleResource(anyList());

        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.updateRole(1L, roleDTO));
        assertEquals(ErrorCode.ROLE_ASSIGN_RESOURCES_FAILED, customException.getErrorCode());
    }

    // ==================== createRole 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testCreateRole_Success() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        when(resourceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(buttonMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);


        // When
        boolean result = roleService.createRole(roleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).insert(any(SysRole.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateRole_NoResourceId() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
        when(buttonMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);


        // When
        roleDTO.setResourceIds(Collections.emptySet());
        boolean result = roleService.createRole(roleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).insert(any(SysRole.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateRole_NoMenuId() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        when(resourceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(buttonMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);


        // When
        roleDTO.setMenuIds(Collections.emptySet());
        boolean result = roleService.createRole(roleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).insert(any(SysRole.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateRole_NoButtonId() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        when(resourceMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(roleButtonMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);
        when(menuMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);


        // When
        roleDTO.setButtonIds(Collections.emptySet());
        boolean result = roleService.createRole(roleDTO);

        // Then
        assertTrue(result);
        verify(roleMapper).insert(any(SysRole.class));
    }

    @Test
    void testCreateRole_Duplicate() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        when(roleMapper.insert(any(SysRole.class))).thenThrow(new DuplicateKeyException("Duplicate Key"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.createRole(roleDTO));
        assertEquals(ErrorCode.ROLE_DUPLICATE_CODE, customException.getErrorCode());
    }

    @Test
    void testCreateRole_DBERROR() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        doThrow(new RuntimeException("DB Error")).when(roleMapper).insert(any(SysRole.class));


        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.createRole(roleDTO));
        assertEquals(ErrorCode.ROLE_INSERT_FAILED, customException.getErrorCode());
    }

    @Test
    void testCreateRole_InvalidResourceIds() {
        // Given
        when(roleConverter.toEntity(roleDTO)).thenReturn(mockRole);
        when(roleMapper.insert(any(SysRole.class))).thenReturn(1);

        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.createRole(roleDTO));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, customException.getErrorCode());

        // 验证后续的 upsert 没有执行
        verify(roleResourceMapper, never()).upsertRoleResource(anyList());
    }

    // ==================== removeById 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_Success() {
        // Given
        Long roleId = 1L;

        // Mock 删除关联表
        when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleMenuMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(roleButtonMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(userRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        // Mock 删除角色本身
        when(roleMapper.deleteById(roleId)).thenReturn(1);

        // Mock 查询关联用户 (为了清理缓存)

        // When
        boolean result = roleService.removeById(roleId);

        // Then
        assertTrue(result);

        // 验证所有删除操作都被调用
        verify(roleResourceMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMenuMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleButtonMapper).delete(any(LambdaQueryWrapper.class));
        verify(userRoleMapper).delete(any(LambdaQueryWrapper.class));
        verify(roleMapper).deleteById(roleId);
        verify(eventService).notifyCacheEvict(anyList(), anyList());
    }

    @Test
    void testRemoveById_NotFound() {
        // Given
        Long roleId = 1L;
        when(roleMapper.deleteById(roleId)).thenReturn(0); // 删除 0 行

        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.removeById(roleId));
        assertEquals(ErrorCode.ROLE_NOT_FOUND, customException.getErrorCode());

        // 验证角色本身没被删 (如果前面逻辑是先删关联再删角色，这里可能已经删了关联)
        // 但通常主逻辑 rollback 了，所以验证一下
        // 注意：如果测试报错，需检查 try-catch 逻辑
    }

    @Test
    void testRemoveById_Failed() {
        // Given
        Long roleId = 1L;
        when(roleMapper.deleteById(roleId)).thenThrow(new RuntimeException("DB ERROR")); // 删除 0 行

        // When & Then
        var customException = assertThrows(CustomException.class, () -> roleService.removeById(roleId));
        assertEquals(ErrorCode.ROLE_DELETE_FAILED, customException.getErrorCode());
    }
}