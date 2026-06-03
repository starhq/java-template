package com.github.starhq.template.converter;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.model.dto.UserDTO;
import com.github.starhq.template.model.vo.UserPageVO;
import com.github.starhq.template.model.vo.UserSimpleVO;

/**
 * MapStruct converter interface for transforming {@link SysUser} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for user account management:
 * <ul>
 *     <li>Entity → {@link UserSimpleVO}: For dropdown selections, basic profile display, or user references</li>
 *     <li>Entity → {@link UserPageVO}: For admin console pagination with audit and extended fields</li>
 *     <li>{@link UserDTO} → Entity: For create/update request binding</li>
 *     <li>Partial update: {@link UserDTO} → existing {@link SysUser} via {@code @MappingTarget}</li>
 * </ul>
 * <p>
 * <strong>Security Note:</strong>
 * <p>
 * Sensitive fields such as {@code password}, {@code salt}, and {@code securityCode} must NEVER be exposed
 * via VOs returned to clients. Ensure they are explicitly ignored in mapping or excluded from DTO/VO definitions.
 * Password updates should be handled via dedicated service methods with hashing and validation.
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
public interface UserConverter {

    /**
     * Converts a {@link SysUser} entity to a {@link UserSimpleVO} for basic reference.
     * <p>
     * Typically used in form dropdowns, user assignment dialogs, or lightweight UI components
     * where only essential fields (id, username, nickname, avatar) are required.
     *
     * @param entity the source {@link SysUser} entity from persistence layer
     * @return the target {@link UserSimpleVO} for lightweight display scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    UserSimpleVO toSimpleVO(SysUser entity);

    /**
     * Converts a {@link SysUser} entity to a {@link UserPageVO} for pagination display.
     * <p>
     * Typically used in admin console tables where extended fields (email, phone, deptId,
     * roleIds, creator, timestamps, status, lockStatus) are required for user management
     * and audit purposes.
     *
     * @param entity the source {@link SysUser} entity from persistence layer
     * @return the target {@link UserPageVO} for paginated list display,
     * or {@code null} if the input entity is {@code null}
     */
    UserPageVO toPageVO(SysUser entity);

    /**
     * Converts a {@link UserDTO} request to a {@link SysUser} entity for persistence.
     * <p>
     * Typically used in create operations where the DTO contains user-submitted form data.
     * Auto-generated fields (id, createTime) and security fields (password hash, salt)
     * should be set by the service layer before persistence.
     *
     * @param dto the source {@link UserDTO} containing user-submitted creation parameters
     * @return the target {@link SysUser} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    SysUser toEntity(UserDTO dto);

    /**
     * Updates an existing {@link SysUser} entity with values from a {@link UserDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime, createBy, password).
     * Null values in the DTO are ignored due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted,
     * ensuring partial updates without overwriting unchanged data or system-managed fields.
     * <strong>Note:</strong> Password updates should be handled via dedicated endpoints/methods
     * to enforce hashing, salting, and strength validation rules.
     *
     * @param dto    the source {@link UserDTO} containing updated parameters
     * @param entity the target {@link SysUser} entity to be updated in-place
     */
    void updateEntity(UserDTO dto, @MappingTarget SysUser entity);

}