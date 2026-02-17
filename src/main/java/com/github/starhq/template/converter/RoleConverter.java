package com.github.starhq.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import com.github.starhq.template.dto.RoleCreateDTO;
import com.github.starhq.template.dto.RoleUpdateDTO;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.vo.RoleVO;

/**
 * 角色对象转换器
 *
 * @author starhq
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RoleConverter {
    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);

    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    RoleVO toVO(SysRole entity);

    /**
     * DTO转Entity
     *
     * @param dto DTO
     * @return Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    SysRole toEntity(RoleCreateDTO dto);

    /**
     * 更新Entity
     *
     * @param dto    DTO
     * @param entity Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntity(RoleUpdateDTO dto, @MappingTarget SysRole entity);
}
