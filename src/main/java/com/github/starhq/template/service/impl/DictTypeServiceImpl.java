package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.converter.DictTypeConverter;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysDictDataMapper;
import com.github.starhq.template.mapper.SysDictTypeMapper;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;
import com.github.starhq.template.service.DictTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description:
 * @date 2026/4/9 14:36
 */
@Service("dictTypeService")
@RequiredArgsConstructor
public class DictTypeServiceImpl extends AuditBaseServiceImpl<SysDictTypeMapper, SysDictType> implements DictTypeService {

    private final SysDictDataMapper dictDataMapper;
    private final SysUserMapperHelper userMapperHelper;

    private final DictTypeConverter dictTypeConverter;
    private final EventService eventService;

    @Override
    public IPage<DictTypePageVO> page(PageRequest pageInfo) {
        return pageVO(pageInfo,
                null,
                userMapperHelper,
                dictTypeConverter::toPageVO);
    }

    @Override
    public DictTypeSimpleVO getDictDataById(Serializable id) {
        SysDictType dictType = getAndCheckById(id, ErrorCode.DICT_TYPE_NOT_FOUND);

        return dictTypeConverter.toSimpleVO(dictType);
    }

    @Cacheable(value = "dictTypes")
    @Override
    public List<DictTypeWithDataVO> selectDictTypeAndDataResponses() {
        return getBaseMapper().selectDictTypesWithData();
    }


    @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_INSERT)
    @Override
    public boolean createDictType(DictTypeDTO dictTypeDto) {
        SysDictType entity = dictTypeConverter.toEntity(dictTypeDto);
        insert(entity, ErrorCode.DICT_TYPE_DUPLICATE_TYPE, ErrorCode.DICT_TYPE_INSERT_FAILED);
        return true;
    }

    @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_UPDATE)
    @Override
    public boolean updateDictType(Serializable id, DictTypeDTO dictTypeDto) {
        SysDictType entity = getAndCheckById(id, ErrorCode.DICT_TYPE_NOT_FOUND);
        dictTypeConverter.updateEntity(dictTypeDto, entity);
        update(entity, ErrorCode.DICT_TYPE_DUPLICATE_TYPE, ErrorCode.DICT_TYPE_UPDATE_FAILED, ErrorCode.DICT_TYPE_NOT_FOUND);
        cacheHelper.clear(CacheConstant.DICT_TYPE);
        return true;
    }

    @AuditLoggable(targetType = TargetType.DICT_TYPE, action = AuditLogConstant.DICT_TYPE_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        dictDataMapper.delete(new LambdaQueryWrapper<SysDictData>().eq(SysDictData::getTypeId, id));

        delete(id, ErrorCode.DICT_TYPE_NOT_FOUND, ErrorCode.DICT_TYPE_DELETE_FAILED);

        eventService.notifyCacheEvict(Collections.emptyList(), List.of(CacheConstant.DICT_TYPE));

        return true;
    }
}
