package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.dto.page.PageRequest;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;

import java.io.Serializable;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: dict type service
 * @date 2026/4/9 14:34
 */
public interface DictTypeService extends IService<SysDictType> {

    /**
     * Retrieves a paginated list of dictionary types based on the provided
     * pagination info.
     *
     * @param pageInfo the pagination and sorting information
     * @return a paginated response of dictionary types
     */
    IPage<DictTypePageVO> page(PageRequest pageInfo);

    /**
     * Retrieves a dictionary type by its ID.
     *
     * @param id the ID of the dictionary type
     * @return the dictionary type's details
     */
    DictTypeSimpleVO getDictDataById(Serializable id);

    /**
     * Retrieves a list of dictionary types and their associated data.
     *
     * @return a list of dictionary types and their data
     */
    List<DictTypeWithDataVO> selectDictTypeAndDataResponses();

    /**
     * Creates a new dictionary type in the database.
     *
     * @param dictTypeDto the DTO containing new dictionary type information
     * @return true if the creation was successful
     * @throws BusinessException if the creation fails
     */
    boolean createDictType(DictTypeDTO dictTypeDto);

    /**
     * Updates an existing dictionary type in the database.
     *
     * @param dictTypeDto the DTO containing updated dictionary type information
     * @return true if the update was successful
     * @throws BusinessException if the update fails
     */
    boolean updateDictType(Serializable id, DictTypeDTO dictTypeDto);
}
