package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysDictData;
import com.github.starhq.template.model.dto.dictData.DictDataDTO;
import com.github.starhq.template.model.vo.dictData.DictDataPageVO;
import com.github.starhq.template.model.vo.dictData.DictDataSimpleVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct converter interface for transforming {@link SysDictData} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for dictionary data management:
 * <ul>
 *     <li>Entity → {@link DictDataSimpleVO}: For dropdown options or basic label-value pairs</li>
 *     <li>Entity → {@link DictDataPageVO}: For admin console pagination with audit fields</li>
 *     <li>{@link DictDataDTO} → Entity: For create/update request binding</li>
 *     <li>Partial update: {@link DictDataDTO} → existing {@link SysDictData} via {@code @MappingTarget}</li>
 *     <li>Collection mapping: {@code List<SysDictData>} → {@code List<DictDataVO>} for batch operations</li>
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
public interface DictDataConverter {

    /**
     * Converts a {@link SysDictData} entity to a {@link DictDataSimpleVO} for basic display.
     * <p>
     * Typically used in form dropdowns, filter options, or label-value mappings where only
     * essential fields (dictCode, dictLabel, sortOrder) are required.
     *
     * @param entity the source {@link SysDictData} entity from persistence layer
     * @return the target {@link DictDataSimpleVO} for lightweight UI scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    DictDataSimpleVO toSimpleVO(SysDictData entity);

    /**
     * Converts a {@link SysDictData} entity to a {@link DictDataPageVO} for pagination display.
     * <p>
     * Typically used in admin console tables where extended fields (dictType, creator, timestamps, status)
     * are required for dictionary management and audit purposes.
     *
     * @param entity the source {@link SysDictData} entity from persistence layer
     * @return the target {@link DictDataPageVO} for paginated list display,
     * or {@code null} if the input entity is {@code null}
     */
    DictDataPageVO toPageVO(SysDictData entity);

    /**
     * Converts a {@link DictDataDTO} request to a {@link SysDictData} entity for persistence.
     * <p>
     * Typically used in create operations where the DTO contains user-submitted form data.
     * Auto-generated fields (id, createTime) should be set by the service or database layer.
     *
     * @param dto the source {@link DictDataDTO} containing user-submitted creation parameters
     * @return the target {@link SysDictData} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    SysDictData toEntity(DictDataDTO dto);

    /**
     * Updates an existing {@link SysDictData} entity with values from a {@link DictDataDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime). Null values
     * in the DTO are ignored due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted,
     * ensuring partial updates without overwriting unchanged data.
     *
     * @param dto    the source {@link DictDataDTO} containing updated parameters
     * @param entity the target {@link SysDictData} entity to be updated in-place
     */
    void updateEntity(DictDataDTO dto, @MappingTarget SysDictData entity);

}