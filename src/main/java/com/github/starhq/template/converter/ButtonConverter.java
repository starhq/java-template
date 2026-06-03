package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysButton;
import com.github.starhq.template.model.dto.ButtonDTO;
import com.github.starhq.template.model.vo.ButtonPageVO;
import com.github.starhq.template.model.vo.ButtonSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct converter interface for transforming {@link SysButton} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for button permission management:
 * <ul>
 *     <li>Entity → {@link ButtonSimpleVO}: For dropdown selections or basic info display</li>
 *     <li>Entity → {@link ButtonPageVO}: For admin console pagination with extended fields</li>
 *     <li>{@link ButtonDTO} → Entity: For create/update request binding</li>
 *     <li>Partial update: {@link ButtonDTO} → existing {@link SysButton} via {@code @MappingTarget}</li>
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
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ButtonConverter {

    /**
     * Converts a {@link SysButton} entity to a {@link ButtonSimpleVO} for basic info display.
     * <p>
     * Typically used in dropdown menus, role assignment dialogs, or permission tree nodes
     * where only essential fields (id, name, code) are required.
     *
     * @param entity the source {@link SysButton} entity from persistence layer
     * @return the target {@link ButtonSimpleVO} for lightweight display scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    ButtonSimpleVO toSimpleVO(SysButton entity);

    /**
     * Converts a {@link SysButton} entity to a {@link ButtonPageVO} for pagination display.
     * <p>
     * Typically used in admin console tables where extended fields (creator, timestamps, status)
     * are required for audit and management purposes.
     *
     * @param entity the source {@link SysButton} entity from persistence layer
     * @return the target {@link ButtonPageVO} for paginated list display,
     * or {@code null} if the input entity is {@code null}
     */
    ButtonPageVO toPageVO(SysButton entity);

    /**
     * Converts a {@link ButtonDTO} request to a {@link SysButton} entity for persistence.
     * <p>
     * Typically used in create operations where the DTO contains user-submitted form data.
     * Auto-generated fields (id, createTime) should be set by the service or database layer.
     *
     * @param dto the source {@link ButtonDTO} containing user-submitted creation parameters
     * @return the target {@link SysButton} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    SysButton toEntity(ButtonDTO dto);

    /**
     * Updates an existing {@link SysButton} entity with values from a {@link ButtonDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime). Null values
     * in the DTO are ignored due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted.
     *
     * @param dto    the source {@link ButtonDTO} containing updated parameters
     * @param entity the target {@link SysButton} entity to be updated in-place
     */
    void updateEntity(ButtonDTO dto, @MappingTarget SysButton entity);

}