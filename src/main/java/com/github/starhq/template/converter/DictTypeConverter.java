package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import com.github.starhq.template.model.vo.dictType.DictTypeWithNameVO;
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
public interface DictTypeConverter {
    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    DictTypeSimpleVO toSimpleVO(SysDictType entity);

    DictTypePageVO toPageVO(SysDictType entity);

    List<DictTypeWithNameVO> toNameVO(List<SysDictType> entities);

    SysDictType toEntity(DictTypeDTO dto);

    /**
     * 更新 Entity
     *
     * @param dto    DTO
     * @param entity Entity
     */
    void updateEntity(DictTypeDTO dto, @MappingTarget SysDictType entity);
}
