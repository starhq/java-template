package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.model.vo.DictDataVO;

/**
 * 系统字典数据 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    IPage<DictDataVO> selectDictDataPage(@Param("page") Page<DictDataVO> page,
            @Param(Constants.WRAPPER) QueryWrapper<DictDataVO> queryWrapper);
}
