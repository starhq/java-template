package com.github.starhq.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.dto.token.TokenSimpleDTO;

/**
 * 令牌对象转换器
 *
 * @author starhq
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.WARN)
public interface TokenConverter {

    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    TokenSimpleDTO toSimpleDTO(SysToken entity);
}
