package com.github.starhq.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;

/**
 * 用户对象转换器
 *
 * @author starhq
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {

    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    UserSimpleVO toSimpleVO(SysUser entity);

    UserPageVO toPageVO(SysUser entity);

    SysUser toEntity(UserDTO dto);

    /**
     * 更新 Entity
     *
     * @param dto    DTO
     * @param entity Entity
     */
    void updateEntity(UserDTO dto, @MappingTarget SysUser entity);
}
