package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.model.dto.button.ButtonDTO;
import com.github.starhq.template.model.vo.button.ButtonPageVO;
import com.github.starhq.template.model.vo.button.ButtonSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * 角色对象转换器
 *
 * @author starhq
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ButtonConverter {
    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    ButtonSimpleVO toSimpleVO(SysButton entity);

    ButtonPageVO toPageVO(SysButton entity);

    SysButton toEntity(ButtonDTO dto);

    /**
     * 更新 Entity
     *
     * @param dto    DTO
     * @param entity Entity
     */
    void updateEntity(ButtonDTO dto, @MappingTarget SysButton entity);
}
