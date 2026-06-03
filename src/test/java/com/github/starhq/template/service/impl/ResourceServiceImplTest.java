package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.converter.ResourceConverter;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysResourceMapper;
import com.github.starhq.template.mapper.SysRoleResourceMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.dto.ResourceDTO;
import com.github.starhq.template.model.vo.ResourceCheckVO;
import com.github.starhq.template.model.vo.ResourcePageVO;
import com.github.starhq.template.model.vo.ResourceSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/3 00:48
 */

@ExtendWith(MockitoExtension.class)
class ResourceServiceImplTest {

    @Mock
    private SysResourceMapper resourceMapper;
    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleResourceMapper roleResourceMapper;
    @Mock
    private ResourceConverter resourceConverter;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private EventService eventService;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private ResourceDTO resourceDto;
    private SysResource mockResource;

    @BeforeEach
    void setUp() {
        resourceDto = new ResourceDTO();
        resourceDto.setName("test-resource");
        resourceDto.setUrl("/api/test");
        resourceDto.setMethods(List.of(HttpMethod.GET));

        mockResource = new SysResource();
        Long resourceId = 1L;
        mockResource.setId(resourceId);
        mockResource.setName("test-resource");
        mockResource.setCreatedBy(1L);
        mockResource.setUpdatedBy(1L);

        ReflectionTestUtils.setField(resourceService, "baseMapper", resourceMapper);
        ReflectionTestUtils.setField(resourceService, "cacheHelper", cacheHelper);
    }

    // ==================== page 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysResource> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockResource));

        // Mock selectPage 返回结果
        when(resourceMapper.selectPage(any(), any(Wrapper.class))).thenReturn(mockDbPage);

        ResourcePageVO mockVo = new ResourcePageVO();
        mockVo.setId(1L);
        when(resourceConverter.toPageVO(mockResource)).thenReturn(mockVo);

        // When
        IPage<ResourcePageVO> result = resourceService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());

        // 验证 Converter 被调用
        verify(resourceConverter).toPageVO(mockResource);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_ReturnsEmptyPage() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysResource> emptyPage = new Page<>(1, 10, 0);

        // Mock selectPage 返回结果
        when(resourceMapper.selectPage(any(), any(Wrapper.class))).thenReturn(emptyPage);

        // When
        IPage<ResourcePageVO> result = resourceService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());

        // 验证查询函数被调用
        verify(resourceMapper).selectPage(any(), any(Wrapper.class));
        verify(userMapper, never()).selectList(any());
    }

    // ==================== getResourceById 测试 ====================

    @Test
    void testGetResourceById_Success() {
        // Given
        when(resourceMapper.selectById(1L)).thenReturn(mockResource);
        ResourceSimpleVO vo = new ResourceSimpleVO();
        when(resourceConverter.toSimpleVO(mockResource)).thenReturn(vo);

        // When
        var result = resourceService.getResourceById(1L);

        // Then
        assertNotNull(result);
        verify(resourceMapper).selectById(1L);
    }

    @Test
    void testGetResourceById_NotFound() {
        // Given
        when(resourceMapper.selectById(1L)).thenReturn(null);

        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.getResourceById(1L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, customException.getErrorCode());
    }

    // ==================== selectCheckedResources 测试 ====================

    @Test
    void testGetSelectCheckedResources_Success() {
        // Given
        Long roleId = 1L;
        when(resourceMapper.selectResourcesByRoleId(roleId)).thenReturn(List.of(new ResourceCheckVO()));

        // When
        var result = resourceService.selectCheckedResources(roleId);
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(resourceMapper).selectResourcesByRoleId(1L);
    }

    @Test
    void testSelectCheckedResources_ReturnsEmpty() {
        // Given
        Long roleId = 1L;
        when(resourceMapper.selectResourcesByRoleId(roleId)).thenReturn(Collections.emptyList());

        // When
        var result = resourceService.selectCheckedResources(roleId);
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(resourceMapper).selectResourcesByRoleId(1L);
    }

    // ==================== selectByUserId 测试 ====================

    @Test
    void testSelectByUserId_Success() {
        // Given
        Long userId = 1L;
        when(resourceMapper.selectAssignedResourceByUserId(userId)).thenReturn(List.of(new SysResource()));

        // When
        var result = resourceService.selectByUserId(userId);
        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(resourceMapper).selectAssignedResourceByUserId(userId);
    }

    @Test
    void testSelectByUserId_ReturnsEmpty() {
        // Given
        Long userId = 1L;
        when(resourceMapper.selectAssignedResourceByUserId(userId)).thenReturn(Collections.emptyList());

        // When
        var result = resourceService.selectByUserId(userId);
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(resourceMapper).selectAssignedResourceByUserId(userId);
    }

    // ==================== updateResource 测试 ====================

    @Test
    void testUpdateResource_Success() {
        // Given
        when(resourceMapper.selectById(1L)).thenReturn(mockResource);
        when(resourceMapper.updateById(any(SysResource.class))).thenReturn(1);

        // When
        boolean result = resourceService.updateResource(1L, resourceDto);

        // Then
        assertTrue(result);
        verify(resourceMapper).updateById(any(SysResource.class));
        verify(cacheHelper).clear(anyString());
    }

    @Test
    void testUpdateResource_NotFound() {
        // Given
        when(resourceMapper.selectById(1L)).thenReturn(null);

        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.updateResource(1L, resourceDto));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, customException.getErrorCode());
    }

    @Test
    void testUpdateResource_DuplicateCode() {
        // Given
        when(resourceMapper.selectById(1L)).thenReturn(mockResource);
        when(resourceMapper.updateById(any(SysResource.class))).thenThrow(new DuplicateKeyException("Duplicate Key"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.updateResource(1L, resourceDto));
        assertEquals(ErrorCode.RESOURCE_DUPLICATE_URL_METHOD, customException.getErrorCode());
    }

    @Test
    void testUpdateResource_Failure() {
        // Given
        when(resourceMapper.selectById(1L)).thenReturn(mockResource);
        when(resourceMapper.updateById(any(SysResource.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var businessException = assertThrows(BusinessException.class, () -> resourceService.updateResource(1L, resourceDto));
        assertEquals(ErrorCode.RESOURCE_UPDATE_FAILED, businessException.getErrorCode());
    }

    // ==================== createRole 测试 ====================

    @Test
    void testCreateRole_Success() {
        // Given
        when(resourceConverter.toEntity(resourceDto)).thenReturn(mockResource);
        when(resourceMapper.insert(any(SysResource.class))).thenReturn(1);

        // When
        boolean result = resourceService.createResource(resourceDto);

        // Then
        assertTrue(result);
        verify(resourceMapper).insert(any(SysResource.class));
    }

    @Test
    void testCreateRole_Duplicate() {
        // Given
        when(resourceConverter.toEntity(resourceDto)).thenReturn(mockResource);
        when(resourceMapper.insert(any(SysResource.class))).thenThrow(new DuplicateKeyException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.createResource(resourceDto));
        assertEquals(ErrorCode.RESOURCE_DUPLICATE_URL_METHOD, customException.getErrorCode());
    }

    @Test
    void testCreateRole_DBERROR() {
        // Given
        when(resourceConverter.toEntity(resourceDto)).thenReturn(mockResource);
        doThrow(new RuntimeException("DB Error")).when(resourceMapper).insert(any(SysResource.class));


        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.createResource(resourceDto));
        assertEquals(ErrorCode.RESOURCE_INSERT_FAILED, customException.getErrorCode());
    }

    // ==================== removeById 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_Success() {
        // Given
        Long resourceId = 1L;

        // Mock 删除关联表
        when(roleResourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        when(resourceMapper.deleteById(resourceId)).thenReturn(1);

        // When
        boolean result = resourceService.removeById(resourceId);

        // Then
        assertTrue(result);

        // 验证所有删除操作都被调用
        verify(roleResourceMapper).delete(any(LambdaQueryWrapper.class));
        verify(resourceMapper).deleteById(resourceId);
    }

    @Test
    void testRemoveById_NotFound() {
        // Given
        Long resourceId = 1L;
        when(resourceMapper.deleteById(resourceId)).thenReturn(0); // 删除 0 行

        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.removeById(resourceId));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, customException.getErrorCode());
    }

    @Test
    void testRemoveById_Failed() {
        // Given
        Long resourceId = 1L;
        when(resourceMapper.deleteById(resourceId)).thenThrow(new RuntimeException("DB ERROR")); // 删除 0 行

        // When & Then
        var customException = assertThrows(CustomException.class, () -> resourceService.removeById(resourceId));
        assertEquals(ErrorCode.RESOURCE_DELETE_FAILED, customException.getErrorCode());
    }
}
