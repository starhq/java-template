package com.github.starhq.template.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.vo.DictDataVO;

/**
 * 系统字典数据Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {

    IPage<DictDataVO> selectDictDataPage(@Param("dictTypeId") Long dictTypeId, @Param("page") Page<DictDataVO> page,
            @Param("ew") QueryWrapper<DictDataVO> queryWrapper);
}
