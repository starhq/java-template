package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.converter.ButtonConverter;
import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysButtonMapper;
import com.github.starhq.template.mapper.SysMenuMapper;
import com.github.starhq.template.mapper.SysRoleButtonMapper;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.dto.button.ButtonPageRequest;
import com.github.starhq.template.model.vo.button.ButtonCheckVO;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ButtonServiceImplTest {

    @Mock
    private SysButtonMapper buttonMapper;
    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysRoleButtonMapper roleButtonMapper;

    @Mock
    private ButtonConverter buttonConverter;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private EventService eventService;

    @InjectMocks
    private ButtonServiceImpl buttonService;

    private SysButton mockButton;
    private ButtonDTO buttonDTO;

    @BeforeEach
    void setUp() {
        mockButton = new SysButton();
        mockButton.setId(1L);
        mockButton.setCode("btn:add");
        mockButton.setName("Add Button");
        mockButton.setMenuId(100L);
        mockButton.setCreatedBy(1L);
        mockButton.setUpdatedBy(1L);

        buttonDTO = new ButtonDTO();
        buttonDTO.setCode("btn:add");
        buttonDTO.setName("Add Button");
        buttonDTO.setMenuId(100L);

        // 手动注入父类依赖
        ReflectionTestUtils.setField(buttonService, "baseMapper", buttonMapper);
        ReflectionTestUtils.setField(buttonService, "cacheHelper", cacheHelper);
    }

    // ==================== page 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        ButtonPageRequest request = new ButtonPageRequest();
        request.setPage(1L);
        request.setSize(10L);
        request.setMenuId(100L);

        IPage<SysButton> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockButton));

        // Mock父类 pageVO 逻辑所需的 Mapper 行为
        when(buttonMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(mockDbPage);
        // Mock Converter
        ButtonPageVO mockVo = new ButtonPageVO();
        when(buttonConverter.toPageVO(any(SysButton.class))).thenReturn(mockVo);

        // When
        IPage<ButtonPageVO> result = buttonService.page(request);

        // Then
        assertNotNull(result);
        assertEquals("created_at", request.getSort());
        assertEquals(1, result.getTotal());
        verify(buttonMapper).selectPage(any(), any());
    }

    @Test
    void testPage_Empty() {
        // Given
        ButtonPageRequest request = new ButtonPageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysButton> emptyPage = new Page<>(1, 10, 0);
        when(buttonMapper.selectPage(any(), any())).thenReturn(emptyPage);

        // When
        IPage<ButtonPageVO> result = buttonService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== select (Cacheable) 测试 ====================

    @Test
    void testSelect_Success() {
        // Given
        Serializable userId = 1L;
        List<SysButton> buttons = List.of(mockButton);
        when(buttonMapper.selectAssignedButtonsByUserId(userId)).thenReturn(buttons);

        // When
        List<String> result = buttonService.select(userId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("btn:add", result.getFirst());
        verify(buttonMapper).selectAssignedButtonsByUserId(userId);
    }

    @Test
    void testSelect_ReturnsEmpty() {
        // Given
        when(buttonMapper.selectAssignedButtonsByUserId(any())).thenReturn(Collections.emptyList());

        // When
        List<String> result = buttonService.select(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== getButtonById 测试 ====================

    @Test
    void testGetButtonById_Success() {
        // Given
        // Mock 父类 getAndCheckById 行为
        when(buttonMapper.selectById(1L)).thenReturn(mockButton);
        ButtonSimpleVO vo = new ButtonSimpleVO();
        when(buttonConverter.toSimpleVO(mockButton)).thenReturn(vo);

        // When
        ButtonSimpleVO result = buttonService.getButtonById(1L);

        // Then
        assertNotNull(result);
        verify(buttonMapper).selectById(1L);
    }

    @Test
    void testGetButtonById_NotFound() {
        // Given
        when(buttonMapper.selectById(1L)).thenReturn(null);

        // When & Then
        // 对应父类抛出的 NotFoundException 或 CustomException
        var customException = assertThrows(CustomException.class, () -> buttonService.getButtonById(1L));
        assertEquals(ErrorCode.BUTTON_NOT_FOUND, customException.getErrorCode());
    }

    // ==================== selectCheckedButtons 测试 ====================

    @Test
    void testSelectCheckedButtons_Success() {
        // Given
        // Mock 父类 getAndCheckById 行为
        when(buttonMapper.selectButtonsByRoleId(1L)).thenReturn(List.of(new ButtonCheckVO()));

        // When
        List<ButtonCheckVO> result = buttonService.selectCheckedButtons(1L);

        // Then
        assertNotNull(result);
        verify(buttonMapper).selectButtonsByRoleId(1L);
    }

    @Test
    void testSelectCheckedButtons_Empty() {
        // Given
        // Mock 父类 getAndCheckById 行为
        when(buttonMapper.selectButtonsByRoleId(1L)).thenReturn(Collections.emptyList());

        // When
        List<ButtonCheckVO> result = buttonService.selectCheckedButtons(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(buttonMapper).selectButtonsByRoleId(1L);
    }

    // ==================== createButton 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testCreateButton_Success() {
        // Given
        // Mock 校验菜单存在
        when(menuMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(buttonConverter.toEntity(buttonDTO)).thenReturn(mockButton);

        // Mock 父类 insert 方法
        // 假设父类 insert 内部调用 mapper.insert
        when(buttonMapper.insert(any(SysButton.class))).thenReturn(1);

        // When
        boolean result = buttonService.createButton(buttonDTO);

        // Then
        assertTrue(result);
        verify(menuMapper).exists(any());
        verify(buttonMapper).insert(any(SysButton.class));
    }

    @Test
    void testCreateButton_MenuNotFound() {
        // Given
        when(menuMapper.exists(any())).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> buttonService.createButton(buttonDTO));
        // 验证未执行插入
        verify(buttonMapper, never()).insert(any(SysButton.class));
    }

    @Test
    void testCreateButton_DuplicateCode() {
        // Given
        when(menuMapper.exists(any())).thenReturn(true);
        when(buttonConverter.toEntity(buttonDTO)).thenReturn(mockButton);
        // 模拟唯一键冲突
        when(buttonMapper.insert(any(SysButton.class))).thenThrow(new DuplicateKeyException("Duplicate Key"));

        // When & Then
        // 假设父类 insert 方法捕获异常并抛出 CustomException
        assertThrows(CustomException.class, () -> buttonService.createButton(buttonDTO));
    }

    @Test
    void testCreateButton_Failure() {
        // Given
        when(menuMapper.exists(any())).thenReturn(true);
        when(buttonConverter.toEntity(buttonDTO)).thenReturn(mockButton);
        // 模拟唯一键冲突
        when(buttonMapper.insert(any(SysButton.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        // 假设父类 insert 方法捕获异常并抛出 CustomException
        var customException = assertThrows(CustomException.class, () -> buttonService.createButton(buttonDTO));
        assertEquals(ErrorCode.BUTTON_INSERT_FAILED, customException.getErrorCode());
    }

    // ==================== updateButton 测试 ====================

    @Test
    void testUpdateButton_Success() {
        // Given
        Serializable id = 1L;
        when(menuMapper.exists(any())).thenReturn(true);
        when(buttonMapper.selectById(id)).thenReturn(mockButton);

        // Mock 父类 update 行为
        when(buttonMapper.updateById(any(SysButton.class))).thenReturn(1);

        // When
        boolean result = buttonService.updateButton(id, buttonDTO);

        // Then
        assertTrue(result);
        verify(buttonMapper).updateById(any(SysButton.class));
        // 验证发布了缓存清理事件
        verify(cacheHelper).clear(anyString());
    }

    @Test
    void testUpdateButton_NotFound() {
        // Given
        when(menuMapper.exists(any())).thenReturn(true);
        when(buttonMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> buttonService.updateButton(1L, buttonDTO));
    }

    @Test
    void testUpdateButton_DuplicateCode() {
        // Given
        when(menuMapper.exists(any())).thenReturn(true);
        when(buttonMapper.selectById(1L)).thenReturn(mockButton);
        when(buttonMapper.updateById(any(SysButton.class))).thenThrow(new DuplicateKeyException("dup"));

        // When & Then
        assertThrows(CustomException.class, () -> buttonService.updateButton(1L, buttonDTO));
    }

    @Test
    void testUpdateButton_Failed() {
        // Given
        when(menuMapper.exists(any())).thenReturn(true);
        when(buttonMapper.selectById(1L)).thenReturn(mockButton);
        when(buttonMapper.updateById(any(SysButton.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> buttonService.updateButton(1L, buttonDTO));
        assertEquals(ErrorCode.BUTTON_UPDATE_FAILED, customException.getErrorCode());
    }

    // ==================== removeById 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_Success() {
        // Given
        Serializable id = 1L;

        // Mock 删除关联表
        when(roleButtonMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        // Mock 父类 delete 行为
        when(buttonMapper.deleteById(id)).thenReturn(1);

        // When
        boolean result = buttonService.removeById(id);

        // Then
        assertTrue(result);
        verify(roleButtonMapper).delete(any()); // 验证删除了关联表
        verify(buttonMapper).deleteById(id);   // 验证删除了主表
        verify(eventService).notifyCacheEvict(anyList(), anyList()); // 验证清理缓存
    }

    @Test
    void testRemoveById_NotFound() {
        // Given
        Serializable id = 999L;
        // Mock 父类 delete 逻辑检测到删除行数为0并抛出异常
        when(buttonMapper.deleteById(id)).thenReturn(0);

        // When & Then
        assertThrows(CustomException.class, () -> buttonService.removeById(id));
    }

    @Test
    void testRemoveById_Failed() {
        // Given
        Serializable id = 999L;
        // Mock 父类 delete 逻辑检测到删除行数为0并抛出异常
        when(buttonMapper.deleteById(id)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> buttonService.removeById(id));
        assertEquals(ErrorCode.BUTTON_DELETE_FAILED, customException.getErrorCode());
    }
}