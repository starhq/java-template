package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.model.dto.role.RoleDTO;
import com.github.starhq.template.model.vo.role.RolePageVO;
import com.github.starhq.template.model.vo.role.RoleSimpleVO;
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
public interface RoleConverter {
    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    RoleSimpleVO toSimpleVO(SysRole entity);

    RolePageVO toPageVO(SysRole entity);

    SysRole toEntity(RoleDTO dto);

    /**
     * 更新 Entity
     *
     * @param dto    DTO
     * @param entity Entity
     */
    void updateEntity(RoleDTO dto, @MappingTarget SysRole entity);
}
