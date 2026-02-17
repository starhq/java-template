package com.github.starhq.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.ReportingPolicy;

import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.vo.UserVO;

/**
 * 用户对象转换器
 *
 * @author starhq
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserConverter {

    /**
     * Entity转VO
     *
     * @param entity Entity
     * @return VO
     */
    @Mappings({
            @Mapping(target = "createdAt", ignore = true),
            @Mapping(target = "creator", ignore = true),
            @Mapping(target = "updatedAt", ignore = true),
            @Mapping(target = "updater", ignore = true)
    })
    UserVO toSimpleVO(SysUser entity);

    // /**
    // * DTO转Entity
    // *
    // * @param dto DTO
    // * @return Entity
    // */
    // @Mapping(target = "id", ignore = true)
    // @Mapping(target = "status", ignore = true)
    // @Mapping(target = "createdAt", ignore = true)
    // @Mapping(target = "createdBy", ignore = true)
    // @Mapping(target = "updatedAt", ignore = true)
    // @Mapping(target = "updatedBy", ignore = true)
    // SysUser toEntity(UserCreateDTO dto);

    // /**
    // * 更新Entity
    // *
    // * @param dto DTO
    // * @param entity Entity
    // */
    // @Mapping(target = "id", ignore = true)
    // @Mapping(target = "username", ignore = true)
    // @Mapping(target = "createdAt", ignore = true)
    // @Mapping(target = "createdBy", ignore = true)
    // @Mapping(target = "updatedAt", ignore = true)
    // @Mapping(target = "updatedBy", ignore = true)
    // void updateEntity(UserUpdateDTO dto, @MappingTarget SysUser entity);
}
