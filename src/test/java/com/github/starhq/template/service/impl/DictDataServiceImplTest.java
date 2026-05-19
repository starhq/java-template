package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.converter.DictDataConverter;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysDictDataMapper;
import com.github.starhq.template.mapper.SysDictTypeMapper;
import com.github.starhq.template.model.dto.dictData.DictDataDTO;
import com.github.starhq.template.model.dto.dictData.DictDataPageRequest;
import com.github.starhq.template.model.vo.dictData.DictDataPageVO;
import com.github.starhq.template.model.vo.dictData.DictDataSimpleVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictDataServiceImplTest {

    @Mock
    private SysDictDataMapper dictDataMapper;
    @Mock
    private SysDictTypeMapper dictTypeMapper;
    @Mock
    private CacheHelper cacheHelper;
    @Mock
    private DictDataConverter dictDataConverter;

    @InjectMocks
    private DictDataServiceImpl dictDataService;

    private SysDictData mockDictData;
    private DictDataDTO dictDataDTO;

    @BeforeEach
    void setUp() {
        mockDictData = new SysDictData();
        mockDictData.setId(1L);
        mockDictData.setLabel("Male");
        mockDictData.setValue("1");
        mockDictData.setTypeId(100L);
        mockDictData.setCreatedBy(1L);
        mockDictData.setUpdatedBy(1L);

        dictDataDTO = new DictDataDTO();
        dictDataDTO.setLabel("Male");
        dictDataDTO.setValue("1");
        dictDataDTO.setTypeId(100L);

        // 手动注入父类依赖
        ReflectionTestUtils.setField(dictDataService, "baseMapper", dictDataMapper);
        ReflectionTestUtils.setField(dictDataService, "cacheHelper", cacheHelper);
    }

    // ==================== page 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testPage_Success() {
        // Given
        DictDataPageRequest request = new DictDataPageRequest();
        request.setPage(1L);
        request.setSize(10L);
        request.setDictTypeId(100L); // 带上条件

        IPage<SysDictData> mockDbPage = new Page<>(1, 10, 1);
        mockDbPage.setRecords(List.of(mockDictData));

        // Mock父类 pageVO 逻辑所需的 Mapper 行为
        when(dictDataMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(mockDbPage);
        // Mock Converter
        DictDataPageVO mockVo = new DictDataPageVO();
        when(dictDataConverter.toPageVO(any(SysDictData.class))).thenReturn(mockVo);

        // When
        IPage<DictDataPageVO> result = dictDataService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        verify(dictDataMapper).selectPage(any(), any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testPage_WithoutDictTypeId() {
        // Given
        DictDataPageRequest request = new DictDataPageRequest();
        request.setPage(1L);
        request.setSize(10L);
        request.setDictTypeId(null); // 不带条件

        IPage<SysDictData> emptyPage = new Page<>(1, 10, 0);
        when(dictDataMapper.selectPage(any(IPage.class), any(Wrapper.class))).thenReturn(emptyPage);

        // When
        IPage<DictDataPageVO> result = dictDataService.page(request);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotal());
        verify(dictDataMapper).selectPage(any(), any());
    }

    // ==================== getDictDataById 测试 ====================

    @Test
    void testGetDictDataById_Success() {
        // Given
        when(dictDataMapper.selectById(1L)).thenReturn(mockDictData);
        DictDataSimpleVO vo = new DictDataSimpleVO();
        when(dictDataConverter.toSimpleVO(mockDictData)).thenReturn(vo);

        // When
        DictDataSimpleVO result = dictDataService.getDictDataById(1L);

        // Then
        assertNotNull(result);
        verify(dictDataMapper).selectById(1L);
    }

    @Test
    void testGetDictDataById_NotFound() {
        // Given
        when(dictDataMapper.selectById(1L)).thenReturn(null);

        // When & Then
        var customException = assertThrows(CustomException.class, () -> dictDataService.getDictDataById(1L));
        assertEquals(ErrorCode.DICT_DATA_NOT_FOUND, customException.getErrorCode());
    }

    // ==================== createDictData 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testCreateDictData_Success() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataConverter.toEntity(dictDataDTO)).thenReturn(mockDictData);
        when(dictDataMapper.insert(any(SysDictData.class))).thenReturn(1);

        // When
        boolean result = dictDataService.createDictData(dictDataDTO);

        // Then
        assertTrue(result);
        verify(dictTypeMapper).exists(any());
        verify(dictDataMapper).insert(any(SysDictData.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateDictData_TypeNotFound() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> dictDataService.createDictData(dictDataDTO));
        // 验证未执行插入
        verify(dictDataMapper, never()).insert(any(SysDictData.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateDictData_DuplicateValue() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataConverter.toEntity(dictDataDTO)).thenReturn(mockDictData);
        when(dictDataMapper.insert(any(SysDictData.class))).thenThrow(new DuplicateKeyException("Duplicate Key"));

        // When & Then
        assertThrows(CustomException.class, () -> dictDataService.createDictData(dictDataDTO));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testCreateDictData_Failure() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataConverter.toEntity(dictDataDTO)).thenReturn(mockDictData);
        when(dictDataMapper.insert(any(SysDictData.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> dictDataService.createDictData(dictDataDTO));
        assertEquals(ErrorCode.DICT_DATA_INSERT_FAILED, customException.getErrorCode());
    }

    // ==================== updateDictData 测试 ====================

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateDictData_Success() {
        // Given
        Serializable id = 1L;
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataMapper.selectById(id)).thenReturn(mockDictData);
        when(dictDataMapper.updateById(any(SysDictData.class))).thenReturn(1);

        // When
        boolean result = dictDataService.updateDictData(id, dictDataDTO);

        // Then
        assertTrue(result);
        verify(dictTypeMapper).exists(any());
        verify(dictDataConverter).updateEntity(dictDataDTO, mockDictData);
        verify(dictDataMapper).updateById(any(SysDictData.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateDictData_TypeNotFound() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(false);

        // When & Then
        assertThrows(NotFoundException.class, () -> dictDataService.updateDictData(1L, dictDataDTO));
        verify(dictDataMapper, never()).selectById(any());
        verify(dictDataMapper, never()).updateById(any(SysDictData.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateDictData_DataNotFound() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataMapper.selectById(1L)).thenReturn(null);

        // When & Then
        assertThrows(CustomException.class, () -> dictDataService.updateDictData(1L, dictDataDTO));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateDictData_DuplicateValue() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataMapper.selectById(1L)).thenReturn(mockDictData);
        when(dictDataMapper.updateById(any(SysDictData.class))).thenThrow(new DuplicateKeyException("dup"));

        // When & Then
        assertThrows(CustomException.class, () -> dictDataService.updateDictData(1L, dictDataDTO));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testUpdateDictData_Failed() {
        // Given
        when(dictTypeMapper.exists(any(LambdaQueryWrapper.class))).thenReturn(true);
        when(dictDataMapper.selectById(1L)).thenReturn(mockDictData);
        when(dictDataMapper.updateById(any(SysDictData.class))).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> dictDataService.updateDictData(1L, dictDataDTO));
        assertEquals(ErrorCode.DICT_DATA_UPDATE_FAILED, customException.getErrorCode());
    }

    // ==================== removeById 测试 ====================

    @Test
    void testRemoveById_Success() {
        // Given
        Serializable id = 1L;
        when(dictDataMapper.deleteById(id)).thenReturn(1);
        doNothing().when(cacheHelper).clear(anyString());

        // When
        boolean result = dictDataService.removeById(id);

        // Then
        assertTrue(result);
        verify(dictDataMapper).deleteById(id);
    }

    @Test
    void testRemoveById_NotFound() {
        // Given
        Serializable id = 999L;
        when(dictDataMapper.deleteById(id)).thenReturn(0);

        // When & Then
        assertThrows(CustomException.class, () -> dictDataService.removeById(id));
    }

    @Test
    void testRemoveById_Failed() {
        // Given
        Serializable id = 999L;
        when(dictDataMapper.deleteById(id)).thenThrow(new RuntimeException("DB Error"));

        // When & Then
        var customException = assertThrows(CustomException.class, () -> dictDataService.removeById(id));
        assertEquals(ErrorCode.DICT_DATA_DELETE_FAILED, customException.getErrorCode());
    }
}
