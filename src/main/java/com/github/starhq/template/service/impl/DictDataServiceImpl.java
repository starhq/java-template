package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.converter.DictDataConverter;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysDictDataMapper;
import com.github.starhq.template.mapper.SysDictTypeMapper;
import com.github.starhq.template.model.dto.dictData.DictDataDTO;
import com.github.starhq.template.model.dto.dictData.DictDataPageRequest;
import com.github.starhq.template.model.vo.dictData.DictDataPageVO;
import com.github.starhq.template.model.vo.dictData.DictDataSimpleVO;
import com.github.starhq.template.service.DictDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/10 09:52
 */
@Service("dictDataService")
@RequiredArgsConstructor
public class DictDataServiceImpl extends AuditBaseServiceImpl<SysDictDataMapper, SysDictData> implements DictDataService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysUserMapperHelper userMapperHelper;

    private final DictDataConverter dictDataConverter;

    @Override
    public IPage<DictDataPageVO> page(DictDataPageRequest pageInfo) {
        return pageVO(pageInfo,
                wrapper -> {
                    if (!Objects.isNull(pageInfo.getDictTypeId())) {
                        wrapper.eq(QueryConstant.TYPE_ID, pageInfo.getDictTypeId());
                    }
                },
                userMapperHelper,
                dictDataConverter::toPageVO);
    }

    @Override
    public DictDataSimpleVO getDictDataById(Serializable id) {
        SysDictData dictData = getAndCheckById(id, ErrorCode.DICT_DATA_NOT_FOUND);

        return dictDataConverter.toSimpleVO(dictData);
    }

    @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_INSERT)
    @Override
    public boolean createDictData(DictDataDTO dictDataDto) {
        validateDictType(dictDataDto.getTypeId());

        SysDictData dictData = dictDataConverter.toEntity(dictDataDto);
        insert(dictData, ErrorCode.DICT_DATA_DUPLICATE_VALUE, ErrorCode.DICT_DATA_INSERT_FAILED);

        return true;
    }

    @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_UPDATE)
    @Override
    public boolean updateDictData(Serializable id, DictDataDTO dictDataDto) {
        validateDictType(dictDataDto.getTypeId());

        SysDictData dictData = getAndCheckById(id, ErrorCode.DICT_DATA_NOT_FOUND);
        dictDataConverter.updateEntity(dictDataDto, dictData);
        update(dictData, ErrorCode.DICT_DATA_DUPLICATE_VALUE, ErrorCode.DICT_DATA_UPDATE_FAILED, ErrorCode.DICT_DATA_NOT_FOUND);
        clearCache();

        return true;
    }

    @AuditLoggable(targetType = TargetType.DICT_DATA, action = AuditLogConstant.DICT_DATA_REMOVE)
    @Override
    public boolean removeById(Serializable id) {
        delete(id, ErrorCode.DICT_DATA_NOT_FOUND, ErrorCode.DICT_DATA_DELETE_FAILED);

        clearCache();

        return true;
    }

    private void validateDictType(Serializable dictTypeId) {
        boolean exists = dictTypeMapper.exists(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getId, dictTypeId));
        if (!exists) {
            throw new NotFoundException(ErrorCode.DICT_TYPE_NOT_FOUND);
        }
    }

    private void clearCache() {
        cacheHelper.clear(CacheConstant.DICT_TYPE);
    }
}
