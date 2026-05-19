package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.model.dto.dictData.DictDataDTO;
import com.github.starhq.template.model.dto.dictData.DictDataPageRequest;
import com.github.starhq.template.model.vo.dictData.DictDataPageVO;
import com.github.starhq.template.model.vo.dictData.DictDataSimpleVO;

import java.io.Serializable;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: dict data service
 * @date 2026/4/10 09:43
 */
public interface DictDataService extends IService<SysDictData> {

    /**
     * Retrieves a paginated list of dictionary data based on the provided
     * pagination info.
     *
     * @param pageInfo the pagination and sorting information
     * @return a paginated response of dictionary data
     */
    IPage<DictDataPageVO> page(DictDataPageRequest pageInfo);

    /**
     * Retrieves a dictionary data entry by its ID.
     *
     * @param id the ID of the dictionary data
     * @return the dictionary data's details
     */
    DictDataSimpleVO getDictDataById(Serializable id);

    /**
     * Creates a new dictionary data entry in the database.
     *
     * @param dictDataDto the DTO containing new dictionary data information
     * @return true if the creation was successful
     * @throws BusinessException if the creation fails
     */
    boolean createDictData(DictDataDTO dictDataDto);

    /**
     * Updates an existing dictionary data entry in the database.
     *
     * @param dictDataDto the DTO containing updated dictionary data information
     * @return true if the update was successful
     * @throws BusinessException if the update fails
     */
    boolean updateDictData(Serializable id, DictDataDTO dictDataDto);

}

