package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.model.dto.dictData.DictDataDTO;
import com.github.starhq.template.model.vo.dictData.DictDataPageVO;
import com.github.starhq.template.model.vo.dictData.DictDataSimpleVO;
import com.github.starhq.template.model.vo.dictData.DictDataVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 角色对象转换器
 *
 * @author starhq
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DictDataConverter {
    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    DictDataSimpleVO toSimpleVO(SysDictData entity);

    DictDataPageVO toPageVO(SysDictData entity);

    SysDictData toEntity(DictDataDTO dto);

    List<DictDataVO> toVO(List<SysDictData> entities);

    /**
     * 更新 Entity
     *
     * @param dto    DTO
     * @param entity Entity
     */
    void updateEntity(DictDataDTO dto, @MappingTarget SysDictData entity);
}
