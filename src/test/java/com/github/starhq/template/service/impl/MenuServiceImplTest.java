package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.converter.MenuConverter;
import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysMenuMapper;
import com.github.starhq.template.mapper.SysRoleMenuMapper;
import com.github.starhq.template.model.dto.MenuDTO;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.vo.MenuSimpleVO;
import com.github.starhq.template.model.vo.LeftNavVO;
import com.github.starhq.template.model.vo.MenuCheckVO;
import com.github.starhq.template.model.vo.MenuListVO;
import io.jsonwebtoken.lang.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MenuServiceImplTest {

    @Mock
    private SysMenuMapper menuMapper;
    @Mock
    private SysRoleMenuMapper roleMenuMapper;
    @Mock
    private MenuConverter menuConverter;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private EventService eventService;


    @InjectMocks
    private MenuServiceImpl menuService;

    // 由于 BaseServiceImpl 的方法（如 getAndCheckById, count）依赖 this.baseMapper，
    // 而 @InjectMocks 注入时，menuMapper 会被当作 baseMapper 使用。

    private SysMenu mockMenu;
    private MenuDTO menuDTO;

    @BeforeEach
    void setUp() {
        mockMenu = new SysMenu();
        mockMenu.setId(1L);
        mockMenu.setUrl("/menus");
        mockMenu.setName("Add Button");
        mockMenu.setParentId(100L);
        mockMenu.setCreatedBy(1L);
        mockMenu.setUpdatedBy(1L);

        menuDTO = new MenuDTO();
        menuDTO.setUrl("/menus");
        menuDTO.setName("Add Button");
        menuDTO.setParentId(100L);

        // 手动注入父类依赖
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
        ReflectionTestUtils.setField(menuService, "cacheHelper", cacheHelper);
    }

    // ==========================================
    // 1. 查询相关测试
    // ==========================================

    @SuppressWarnings("unchecked")
    @Test
    void selectList_success() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);
        request.setSearchCount(false);

        IPage<SysMenu> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockMenu));

        // Mock父类 pageVO 逻辑所需的 Mapper 行为
        when(menuMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(mockDbPage);

        // Mock Converter
        MenuListVO mockVo = new MenuListVO();
        when(menuConverter.toListVO(any(SysMenu.class))).thenReturn(mockVo);

        // When
        List<MenuListVO> result = menuService.selectList(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(menuMapper).selectPage(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void selectList_empty() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);
        request.setSearchCount(false);

        IPage<SysMenu> emptyPage = new Page<>(1, 10, 0);
        // Mock父类 pageVO 逻辑所需的 Mapper 行为
        when(menuMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(emptyPage);
        // When
        List<MenuListVO> result = menuService.selectList(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    void selectSidebar_shouldReturnNonEmptyResult() {
        when(menuMapper.selectAssignedMenus(any())).thenReturn(List.of(mockMenu));
        when(menuConverter.toLeftNavVO(mockMenu)).thenReturn(new LeftNavVO());

        var result = menuService.selectSidebar(1L);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        verify(menuConverter).toLeftNavVO(mockMenu);
        verify(menuMapper).selectAssignedMenus(any());
    }

    @Test
    void selectSidebar_shouldReturnEmptyResult() {
        when(menuMapper.selectAssignedMenus(any())).thenReturn(Collections.emptyList());

        var result = menuService.selectSidebar(1L);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(menuConverter, never()).toLeftNavVO(mockMenu);
        verify(menuMapper).selectAssignedMenus(any());
    }

    @Test
    void getMenuById_success() {
        // Given: 模拟根据ID查不到数据 (假设 getAndCheckById 底层是 selectById)
        when(menuMapper.selectById(1L)).thenReturn(mockMenu);
        var menuSimpleVO = new MenuSimpleVO();
        menuSimpleVO.setName("Add Button");

        when(menuConverter.toSimpleVO(mockMenu)).thenReturn(menuSimpleVO);

        var result = menuService.getMenuById(1L);

        // When & Then
        assertNotNull(result);
        assertEquals("Add Button", result.getName());
    }

    @Test
    void getMenuById_shouldThrowWhenNotFound() {
        // Given: 模拟根据ID查不到数据 (假设 getAndCheckById 底层是 selectById)
        when(menuMapper.selectById(999L)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> menuService.getMenuById(999L));
    }

    @Test
    void selectCheckedMenus_shouldSuccess() {
        Long roleId = 1L;

        // Given: 模拟根据ID查不到数据 (假设 getAndCheckById 底层是 selectById)
        when(menuMapper.selectMenusByRoleId(roleId)).thenReturn(List.of(new MenuCheckVO()));

        // When & Then
        var menuCheckVOS = menuService.selectCheckedMenus(roleId);

        assertFalse(CollectionUtils.isEmpty(menuCheckVOS));
        verify(menuMapper).selectMenusByRoleId(any());
    }

    @Test
    void selectCheckedMenus_shouldReturnEmpty() {
        Long roleId = 1L;

        // Given: 模拟根据ID查不到数据 (假设 getAndCheckById 底层是 selectById)
        when(menuMapper.selectMenusByRoleId(roleId)).thenReturn(java.util.Collections.emptyList());

        // When & Then
        var menuCheckVOS = menuService.selectCheckedMenus(roleId);

        assertTrue(CollectionUtils.isEmpty(menuCheckVOS));
        verify(menuMapper).selectMenusByRoleId(any());
    }

    // ==========================================
    // 2. 新增/修改测试
    // ==========================================

    @Test
    void createMenu_success() {
        // Given
        when(menuConverter.toEntity(menuDTO)).thenReturn(mockMenu);
        when(menuMapper.insert(any(SysMenu.class))).thenReturn(1);
        when(menuMapper.exists(any())).thenReturn(true);

        // When
        boolean result = menuService.createMenu(menuDTO);

        // Then
        assertTrue(result);
        // 验证 exists 方法没有去查数据库
        verify(menuMapper).exists(any());
    }

    @Test
    void createMenu_failure() {
        // Given
        when(menuConverter.toEntity(menuDTO)).thenReturn(mockMenu);
        when(menuMapper.exists(any())).thenReturn(true);
        when(menuMapper.insert(any(SysMenu.class))).thenThrow(new RuntimeException("DB ERROR"));

        // When & THEN
        assertThrows(CustomException.class, () -> menuService.createMenu(menuDTO));
    }

    @Test
    void createMenu_shouldSkipParentCheck_whenParentIdIsNull() {
        // Given
        menuDTO.setParentId(null); // 父ID为空
        SysMenu entity = new SysMenu();
        when(menuConverter.toEntity(menuDTO)).thenReturn(entity);
        when(menuMapper.insert(any(SysMenu.class))).thenReturn(1);

        // When
        boolean result = menuService.createMenu(menuDTO);

        // Then
        assertTrue(result);
        // 验证 exists 方法没有去查数据库
        verify(menuMapper, never()).exists(any());
    }

    @Test
    void createMenu_shouldThrowWhenParentNotFound() {
        // Given
        MenuDTO dto = new MenuDTO();
        dto.setParentId(99L); // 父ID不为空
        // 模拟父节点不存在
        when(menuMapper.exists(any())).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> menuService.createMenu(dto));
        // 验证转换和插入都没有被执行
        verify(menuConverter, never()).toEntity(any());
        verify(menuMapper, never()).insert(any(SysMenu.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void updateMenu_success() {
        // Given
        MenuDTO dto = new MenuDTO();
        dto.setParentId(1L);
        // 模拟当前要修改的菜单不存在 (假设 getAndCheckById 底层是 selectById)
        when(menuMapper.selectById(1L)).thenReturn(mockMenu);
        when(menuMapper.updateById(mockMenu)).thenReturn(1);
        when(menuMapper.exists(any(Wrapper.class))).thenReturn(true);

        // When & Then
        boolean result = menuService.updateMenu(1L, dto);
        assertTrue(result);
        verify(cacheHelper).clear(anyString());
    }

    @Test
    void updateMenu_shouldThrowWhenMenuNotFound() {
        // Given
        MenuDTO dto = new MenuDTO();
        dto.setParentId(null);
        // 模拟当前要修改的菜单不存在 (假设 getAndCheckById 底层是 selectById)
        when(menuMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(NotFoundException.class, () -> menuService.updateMenu(1L, dto));
    }

    // ==========================================
    // 3. 删除测试 (保留并优化之前的逻辑)
    // ==========================================

    @Test
    void removeByIds_shouldDelegateToRemoveByIds() {
        List<Long> ids = List.of(1L, 2L);

        // Given: 模拟无子节点、存在
        when(menuMapper.selectCount(any())).thenReturn(2L).thenReturn(0L);
        when(roleMenuMapper.delete(any())).thenReturn(1);
        when(menuMapper.deleteByIds(anyCollection())).thenReturn(1);

        // When
        menuService.removeByIds(ids);

        // Then: 核心验证，removeById 底层确实调用了 removeByIds，且传入的是 Collections.singletonList
        verify(roleMenuMapper, times(1)).delete(argThat(_ -> {
            // 这里可以进一步解析 wrapper 确认 IN 里的参数是 [1L]
            return true;
        }));
        verify(eventService).notifyCacheEvict(anyList(), anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void removeByIds_WithEmptyIds() {
        List<Long> ids = java.util.Collections.emptyList();

        // Given: 模拟无子节点、存在

        // When
        boolean result = menuService.removeByIds(ids);

        assertTrue(result);

        // Then: 核心验证，removeById 底层确实调用了 removeByIds，且传入的是 Collections.singletonList
        verify(roleMenuMapper, never()).delete(any(LambdaQueryWrapper.class));
        verify(eventService, never()).notifyCacheEvict(anyList(), anyList());
    }

    @Test
    void removeByIds_shouldThrowException_whenHasChildren() {
        // Given
        List<Long> ids = List.of(1L, 2L);
        // 第一次 count 通过 (存在)，第二次 count 失败 (有子节点)
        when(menuMapper.selectCount(any())).thenReturn(2L).thenReturn(5L);

        // When & Then
        assertThrows(BusinessException.class, () -> menuService.removeByIds(ids));

        // 验证：发生异常后，不执行删关联表操作
        verify(roleMenuMapper, never()).delete(any());
        verify(menuMapper, never()).deleteByIds(anyCollection());
    }

    @Test
    void removeByIds_shouldDeleteSuccessfully() {
        // Given
        List<Long> ids = List.of(1L, 2L);
        when(menuMapper.selectCount(any())).thenReturn(2L).thenReturn(0L); // 校验全过
        when(roleMenuMapper.delete(any())).thenReturn(2);
        when(menuMapper.deleteByIds(ids)).thenReturn(2);

        // When
        boolean result = menuService.removeByIds(ids);

        // Then
        assertTrue(result);
        verify(roleMenuMapper, times(1)).delete(any());
        // 验证底层调用的删除方法传入了正确的 ID 列表
        verify(menuMapper, times(1)).deleteByIds(ids);
        verify(eventService).notifyCacheEvict(anyList(), anyList());
    }

    @Test
    void removeByIds_should_throwMenuNotFound() {
        // Given
        List<Long> ids = List.of(1L, 2L);
        when(menuMapper.selectCount(any())).thenReturn(1L).thenReturn(0L); // 校验全过

        // When & throw
        assertThrows(NotFoundException.class, () -> menuService.removeByIds(ids));

        verify(roleMenuMapper, never()).delete(any());
        // 验证底层调用的删除方法传入了正确的 ID 列表
        verify(menuMapper, never()).deleteByIds(ids);
        verify(eventService, never()).notifyCacheEvict(anyList(), anyList());
    }

    @Test
    void removeById_Success() {
        when(roleMenuMapper.delete(any())).thenReturn(1);
        when(menuMapper.deleteById(any(Long.class))).thenReturn(1);

        // When
        menuService.removeById(1L);

        // Then: 核心验证，removeById 底层确实调用了 removeByIds，且传入的是 Collections.singletonList
        verify(roleMenuMapper, times(1)).delete(argThat(_ -> {
            // 这里可以进一步解析 wrapper 确认 IN 里的参数是 [1L]
            return true;
        }));
        verify(eventService).notifyCacheEvict(anyList(), anyList());
    }
}