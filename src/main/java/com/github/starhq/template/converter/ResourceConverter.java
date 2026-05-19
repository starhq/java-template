package com.github.starhq.template.converter;

import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.entity.SysResource;
import com.github.starhq.template.model.dto.resource.ResourceDTO;
import com.github.starhq.template.model.dto.resource.ResourceSimpleDTO;
import com.github.starhq.template.model.vo.resource.ResourcePageVO;
import com.github.starhq.template.model.vo.resource.ResourceSimpleVO;
import org.mapstruct.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: resource converter
 * @date 2026/4/1 13:24
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ResourceConverter {

    @Mapping(target = "methods", qualifiedByName = "methodsToMask")
    SysResource toEntity(ResourceDTO dto);

    @Mapping(target = "methods", qualifiedByName = "maskToMethods")
    ResourceSimpleVO toSimpleVO(SysResource entity);

    @Mapping(target = "methods", qualifiedByName = "maskToMethods")
    ResourcePageVO toPageVO(SysResource entity);

    List<ResourceSimpleDTO> toSimpleDTO(List<SysResource> entities);

    @Mapping(target = "methods", qualifiedByName = "methodsToMask")
    void updateEntity(ResourceDTO dto, @MappingTarget SysResource entity);

    @Named("methodsToMask")
    default Integer methodsToMask(List<HttpMethod> methods) {
        return HttpMethod.combine(methods);
    }

    @Named("maskToMethods")
    default List<HttpMethod> maskToMethods(Integer mask) {
        if (null == mask || 0 == mask) {
            return Collections.emptyList();
        }
        return Arrays.stream(HttpMethod.values()).filter(method -> HttpMethod.contains(mask, method)).toList();
    }
}
