package com.github.starhq.template.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.vo.DictTypeVO;

/**
 * 系统字典类型Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    IPage<DictTypeVO> selectDictTypePage(@Param("page") Page<DictTypeVO> page,
            @Param("ew") QueryWrapper<DictTypeVO> queryWrapper);

    List<DictTypeVO> selectDictTypesWithDictDatas();
}
