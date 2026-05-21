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
 * MapStruct converter interface for transforming {@link SysRole} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for role-based access control (RBAC):
 * <ul>
 *     <li>Entity → {@link RoleSimpleVO}: For dropdown selections or basic role references</li>
 *     <li>Entity → {@link RolePageVO}: For admin console pagination with audit fields</li>
 *     <li>{@link RoleDTO} → Entity: For create/update request binding</li>
 *     <li>Partial update: {@link RoleDTO} → existing {@link SysRole} via {@code @MappingTarget}</li>
 * </ul>
 * <p>
 * Configuration:
 * <ul>
 *     <li>{@code componentModel = "spring"}: Managed as a Spring bean for dependency injection</li>
 *     <li>{@code nullValuePropertyMappingStrategy = IGNORE}: Skip null source properties to preserve target defaults</li>
 *     <li>{@code unmappedTargetPolicy = IGNORE}: Allow partial field mapping for flexible VO/DTO evolution</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see <a href="https://mapstruct.org/documentation/stable/reference/html/">MapStruct Reference Guide</a>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleConverter {

    /**
     * Converts a {@link SysRole} entity to a {@link RoleSimpleVO} for basic reference.
     * <p>
     * Typically used in user assignment dialogs, role dropdowns, or permission tree nodes
     * where only essential fields (id, roleName, roleCode) are required.
     *
     * @param entity the source {@link SysRole} entity from persistence layer
     * @return the target {@link RoleSimpleVO} for lightweight display scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    RoleSimpleVO toSimpleVO(SysRole entity);

    /**
     * Converts a {@link SysRole} entity to a {@link RolePageVO} for pagination display.
     * <p>
     * Typically used in admin console tables where extended fields (dataScope, creator,
     * updater, timestamps, status, sortOrder) are required for role management and audit purposes.
     *
     * @param entity the source {@link SysRole} entity from persistence layer
     * @return the target {@link RolePageVO} for paginated list display,
     * or {@code null} if the input entity is {@code null}
     */
    RolePageVO toPageVO(SysRole entity);

    /**
     * Converts a {@link RoleDTO} request to a {@link SysRole} entity for persistence.
     * <p>
     * Typically used in create operations where the DTO contains user-submitted form data.
     * Auto-generated fields (id, createTime) should be set by the service or database layer.
     *
     * @param dto the source {@link RoleDTO} containing user-submitted creation parameters
     * @return the target {@link SysRole} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    SysRole toEntity(RoleDTO dto);

    /**
     * Updates an existing {@link SysRole} entity with values from a {@link RoleDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime, createBy).
     * Null values in the DTO are ignored due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted,
     * ensuring partial updates without overwriting unchanged data or system-managed fields.
     *
     * @param dto    the source {@link RoleDTO} containing updated parameters
     * @param entity the target {@link SysRole} entity to be updated in-place
     */
    void updateEntity(RoleDTO dto, @MappingTarget SysRole entity);

}