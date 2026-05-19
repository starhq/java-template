package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.vo.dictType.DictTypeWithDataVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 系统字典类型 Mapper
 *
 * @author starhq
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {

    List<DictTypeWithDataVO> selectDictTypesWithData();
}
