package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.converter.DictTypeConverter;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysDictDataMapper;
import com.github.starhq.template.mapper.SysDictTypeMapper;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper dictTypeMapper;
    @Mock
    private SysDictDataMapper dictDataMapper;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private DictTypeConverter dictTypeConverter;
    @Mock
    private EventService eventService;

    @InjectMocks
    private DictTypeServiceImpl dictTypeService;

    private SysDictType mockDictType;
    private DictTypeDTO dictTypeDTO;

    @BeforeEach
    void setUp() {
        mockDictType = new SysDictType();
        mockDictType.setId(1L);
        mockDictType.setName("Gender");
        mockDictType.setType("gender");
        mockDictType.setCreatedBy(1L);
        mockDictType.setUpdatedBy(1L);

        dictTypeDTO = new DictTypeDTO();
        dictTypeDTO.setName("Gender");
        dictTypeDTO.setType("gender");

        // 手动注入父类依赖
        ReflectionTestUtils.setField(dictTypeService, "cacheHelper", cacheHelper);
        ReflectionTestUtils.setField(dictTypeService, "baseMapper", dictTypeMapper);
    }

    // ==================== page 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysDictType> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockDictType));

        when(dictTypeMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(mockDbPage);
        // Mock Converter
        DictTypePageVO mockVo = new DictTypePageVO();
        when(dictTypeConverter.toPageVO(any(SysDictType.class))).thenReturn(mockVo);

        // When
        IPage<DictTypePageVO> result = dictTypeService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(dictTypeMapper).selectPage(any(), any());
    }

    @Test
    void testPage_Empty() {
        // Given
        PageRequest request = new PageRequest();
        request.setPage(1L);
        request.setSize(10L);

        IPage<SysDictType> emptyPage = new Page<>(1, 10, 0);
        when(dictTypeMapper.selectPage(any(), any())).thenReturn(emptyPage);

        // When
        IPage<DictTypePageVO> result = dictTypeService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        assertTrue(result.getRecords().isEmpty());
    }

    // ==================== getDictDataById 测试 ====================

    @Test
    void testGetDictDataById_Success() {
        // Given
        // Mock 父类 getAndCheckById 行为
        when(dictTypeMapper.selectById(1L)).thenReturn(mockDictType);
        DictTypeSimpleVO vo = new DictTypeSimpleVO();
        when(dictTypeConverter.toSimpleVO(mockDictType)).thenReturn(vo);

        // When
        DictTypeSimpleVO result = dictTypeService.getDictDataById(1L);

        // Then
        assertNotNull(result);
        verify(dictTypeMapper).selectById(1L);
    }

    @Test
    void testGetDictDataById_NotFound() {
        // Given
        when(dictTypeMapper.selectById(1L)).thenReturn(null);

        // When & Then
        // 对应父类抛出的 NotFoundException 或 CustomException (根据你实际父类的实现选用)
        var customException = assertThrows(CustomException.class, () -> dictTypeService.getDictDataById(1L));
        assertEquals(ErrorCode.DICT_TYPE_NOT_FOUND, customException.getErrorCode());
    }

    // ==================== selDictTypeAndDataResponses 测试 ====================

    @Test
    void testSelectDictTypeAndDataResponses_Success() {
        // Given
        DictTypeWithDataVO vo = new DictTypeWithDataVO();
        vo.setDictType("gender");
        when(dictTypeMapper.selectDictTypesWithData()).thenReturn(List.of(vo));

        // When
        List<DictTypeWithDataVO> result = dictTypeService.selectDictTypeAndDataResponses();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("gender", result.getFirst().getDictType());
        verify(dictTypeMapper).selectDictTypesWithData();
    }

    @Test
    void testSelectDictTypeAndDataResponses_Empty() {
        // Given
        when(dictTypeMapper.selectDictTypesWithData()).thenReturn(Collections.emptyList());

        // When
        List<DictTypeWithDataVO> result = dictTypeService.selectDictTypeAndDataResponses();

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== createDictType 测试 ====================

    @Test
    void testCreateDictType_Success() {
        // Given
        when(dictTypeConverter.toEntity(dictTypeDTO)).thenReturn(mockDictType);

        // Mock 父类 insert 方法
        when(dictTypeMapper.insert(any(SysDictType.class))).thenReturn(1);

        // When
        boolean result = dictTypeService.createDictType(dictTypeDTO);

        // Then
        assertTrue(result);
        verify(dictTypeMapper).insert(any(SysDictType.class));
    }

    @Test
    void testCreateDictType_DuplicateType() {
        // Given
        when(dictTypeConverter.toEntity(dictTypeDTO)).thenReturn(mockDictType);
        // 模拟唯一键冲突
        when(dictTypeMapper.insert(any(SysDictType.class))).thenThrow(new DuplicateKeyException("Duplicate Key"));

        // When & Then
        // 假设父类 insert 方法捕获异常并抛出 CustomException
        assertThrows(CustomException.class, () -> dictTypeService.createDictType(dictTypeDTO));
    }

    @Test
    void testCreateDictType_Failure() {
        // Given
        when(dictTypeConverter.toEntity(dictTypeDTO)).thenReturn(mockDictType);
        // 模拟其他数据库异常
        when(dictTypeMapper.insert(any(SysDictType.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        // 假设父类 insert 方法捕获异常并抛出 CustomException
        var customException = assertThrows(CustomException.class, () -> dictTypeService.createDictType(dictTypeDTO));
        assertEquals(ErrorCode.DICT_TYPE_INSERT_FAILED, customException.getErrorCode());
    }

    // ==================== updateDictType 测试 ====================

    @Test
    void testUpdateDictType_Success() {
        // Given
        Serializable id = 1L;
        when(dictTypeMapper.selectById(id)).thenReturn(mockDictType);

        // Mock 父类 update 行为
        when(dictTypeMapper.updateById(any(SysDictType.class))).thenReturn(1);

        // When
        boolean result = dictTypeService.updateDictType(id, dictTypeDTO);

        // Then
        assertTrue(result);
        verify(dictTypeConverter).updateEntity(dictTypeDTO, mockDictType);
        verify(dictTypeMapper).updateById(any(SysDictType.class));
    }

    @Test
    void testUpdateDictType_NotFound() {
        // Given
        when(dictTypeMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> dictTypeService.updateDictType(1L, dictTypeDTO));
    }

    @Test
    void testUpdateDictType_DuplicateType() {
        // Given
        when(dictTypeMapper.selectById(1L)).thenReturn(mockDictType);
        when(dictTypeMapper.updateById(any(SysDictType.class))).thenThrow(new DuplicateKeyException("dup"));

        // When & Then
        assertThrows(CustomException.class, () -> dictTypeService.updateDictType(1L, dictTypeDTO));
    }

    @Test
    void testUpdateDictType_Failed() {
        // Given
        when(dictTypeMapper.selectById(1L)).thenReturn(mockDictType);
        when(dictTypeMapper.updateById(any(SysDictType.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> dictTypeService.updateDictType(1L, dictTypeDTO));
        assertEquals(ErrorCode.DICT_TYPE_UPDATE_FAILED, customException.getErrorCode());
    }

    // ==================== removeById 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_Success() {
        // Given
        Serializable id = 1L;

        // Mock 删除关联表数据
        when(dictDataMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        // Mock 父类 delete 行为
        when(dictTypeMapper.deleteById(id)).thenReturn(1);

        // When
        boolean result = dictTypeService.removeById(id);

        // Then
        assertTrue(result);
        verify(dictDataMapper).delete(any()); // 验证删除了字典数据关联表
        verify(dictTypeMapper).deleteById(id);   // 验证删除了字典类型主表
        verify(eventService).notifyCacheEvict(anyList(), anyList());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_NotFound() {
        // Given
        Serializable id = 999L;
        // Mock 删除关联表数据返回0（不影响主流程）
        when(dictDataMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        // Mock 父类 delete 逻辑检测到删除行数为0并抛出异常
        when(dictTypeMapper.deleteById(id)).thenReturn(0);

        // When & Then
        assertThrows(CustomException.class, () -> dictTypeService.removeById(id));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testRemoveById_Failed() {
        // Given
        Serializable id = 999L;
        when(dictDataMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(0);
        // Mock 父类 delete 逻辑检测到异常并抛出
        when(dictTypeMapper.deleteById(id)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> dictTypeService.removeById(id));
        assertEquals(ErrorCode.DICT_TYPE_DELETE_FAILED, customException.getErrorCode());
    }
}