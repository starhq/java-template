package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysDictType;
import com.github.starhq.template.model.dto.dictType.DictTypeDTO;
import com.github.starhq.template.model.vo.dictType.DictTypePageVO;
import com.github.starhq.template.model.vo.dictType.DictTypeSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct converter interface for transforming {@link SysDictType} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for dictionary type management:
 * <ul>
 *     <li>Entity → {@link DictTypeSimpleVO}: For basic dropdowns or reference lookups</li>
 *     <li>Entity → {@link DictTypePageVO}: For admin console pagination with audit fields</li>
 *     <li>{@link DictTypeDTO} → Entity: For create/update request binding</li>
 *     <li>Partial update: {@link DictTypeDTO} → existing {@link SysDictType} via {@code @MappingTarget}</li>
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
public interface DictTypeConverter {

    /**
     * Converts a {@link SysDictType} entity to a {@link DictTypeSimpleVO} for basic reference.
     * <p>
     * Typically used in form dropdowns, filter options, or lightweight UI components where only
     * essential fields (id, typeCode, typeName) are required.
     *
     * @param entity the source {@link SysDictType} entity from persistence layer
     * @return the target {@link DictTypeSimpleVO} for lightweight display scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    DictTypeSimpleVO toSimpleVO(SysDictType entity);

    /**
     * Converts a {@link SysDictType} entity to a {@link DictTypePageVO} for pagination display.
     * <p>
     * Typically used in admin console tables where extended fields (creator, updater, timestamps, status)
     * are required for dictionary management and audit purposes.
     *
     * @param entity the source {@link SysDictType} entity from persistence layer
     * @return the target {@link DictTypePageVO} for paginated list display,
     * or {@code null} if the input entity is {@code null}
     */
    DictTypePageVO toPageVO(SysDictType entity);

    /**
     * Converts a {@link DictTypeDTO} request to a {@link SysDictType} entity for persistence.
     * <p>
     * Typically used in create operations where the DTO contains user-submitted form data.
     * Auto-generated fields (id, createTime) should be set by the service or database layer.
     *
     * @param dto the source {@link DictTypeDTO} containing user-submitted creation parameters
     * @return the target {@link SysDictType} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    SysDictType toEntity(DictTypeDTO dto);

    /**
     * Updates an existing {@link SysDictType} entity with values from a {@link DictTypeDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime). Null values
     * in the DTO are ignored due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted,
     * ensuring partial updates without overwriting unchanged data.
     *
     * @param dto    the source {@link DictTypeDTO} containing updated parameters
     * @param entity the target {@link SysDictType} entity to be updated in-place
     */
    void updateEntity(DictTypeDTO dto, @MappingTarget SysDictType entity);

}