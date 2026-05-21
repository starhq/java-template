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
 * MapStruct converter interface for transforming {@link SysResource} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for resource permission management,
 * with special handling for HTTP method bitmask conversion:
 * <ul>
 *     <li>Entity → {@link ResourceSimpleVO}: For lightweight dropdowns or reference lookups</li>
 *     <li>Entity → {@link ResourcePageVO}: For admin console pagination with audit fields</li>
 *     <li>{@link ResourceDTO} → Entity: For create/update request binding with method mask encoding</li>
 *     <li>Partial update: {@link ResourceDTO} → existing {@link SysResource} via {@code @MappingTarget}</li>
 *     <li>Collection mapping: {@code List<SysResource>} → {@code List<ResourceSimpleDTO>} for batch operations</li>
 * </ul>
 * <p>
 * <strong>HTTP Method Bitmask Strategy:</strong>
 * <p>
 * To efficiently store multiple HTTP methods (GET, POST, PUT, DELETE, etc.) in a single
 * integer field, this converter uses bitwise operations:
 * <ul>
 *     <li>{@code methodsToMask}: Combines {@link List<HttpMethod>} into an integer mask via {@link HttpMethod#combine(List)}</li>
 *     <li>{@code maskToMethods}: Decomposes an integer mask back to {@link List<HttpMethod>} via {@link HttpMethod#contains(int, HttpMethod)}</li>
 * </ul>
 * This approach reduces database storage overhead and enables fast bitwise permission checks.
 * <p>
 * Configuration:
 * <ul>
 *     <li>{@code componentModel = "spring"}: Managed as a Spring bean for dependency injection</li>
 *     <li>{@code nullValuePropertyMappingStrategy = IGNORE}: Skip null source properties to preserve target defaults</li>
 *     <li>{@code unmappedTargetPolicy = IGNORE}: Allow partial field mapping for flexible VO/DTO evolution</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-01
 * @see HttpMethod
 * @see <a href="https://mapstruct.org/documentation/stable/reference/html/">MapStruct Reference Guide</a>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ResourceConverter {

    /**
     * Converts a {@link ResourceDTO} request to a {@link SysResource} entity for persistence.
     * <p>
     * The {@code methods} field (list of {@link HttpMethod}) is encoded into an integer bitmask
     * via {@link #methodsToMask(List)} for efficient storage. Auto-generated fields (id, createTime)
     * should be set by the service or database layer.
     *
     * @param dto the source {@link ResourceDTO} containing user-submitted creation parameters
     * @return the target {@link SysResource} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    @Mapping(target = "methods", qualifiedByName = "methodsToMask")
    SysResource toEntity(ResourceDTO dto);

    /**
     * Converts a {@link SysResource} entity to a {@link ResourceSimpleVO} for basic reference.
     * <p>
     * The integer {@code methods} mask is decoded back to a {@link List<HttpMethod>} via
     * {@link #maskToMethods(Integer)} for frontend display. Typically used in dropdowns or
     * lightweight UI components where only essential fields (id, name, path, methods) are required.
     *
     * @param entity the source {@link SysResource} entity from persistence layer
     * @return the target {@link ResourceSimpleVO} for lightweight display scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    @Mapping(target = "methods", qualifiedByName = "maskToMethods")
    ResourceSimpleVO toSimpleVO(SysResource entity);

    /**
     * Converts a {@link SysResource} entity to a {@link ResourcePageVO} for pagination display.
     * <p>
     * The integer {@code methods} mask is decoded back to a {@link List<HttpMethod>} via
     * {@link #maskToMethods(Integer)}. Typically used in admin console tables where extended
     * fields (creator, updater, timestamps, status, perms) are required for resource management
     * and audit purposes.
     *
     * @param entity the source {@link SysResource} entity from persistence layer
     * @return the target {@link ResourcePageVO} for paginated list display,
     * or {@code null} if the input entity is {@code null}
     */
    @Mapping(target = "methods", qualifiedByName = "maskToMethods")
    ResourcePageVO toPageVO(SysResource entity);

    /**
     * Converts a list of {@link SysResource} entities to a list of {@link ResourceSimpleDTO}.
     * <p>
     * Typically used for batch export, caching, or internal service communication where
     * lightweight DTOs without method decoding are sufficient. MapStruct automatically
     * iterates the source list and applies element-wise mapping.
     *
     * @param entities the source list of {@link SysResource} entities
     * @return the target list of {@link ResourceSimpleDTO} for batch operations,
     * or {@code null} if the input list is {@code null}
     */
    List<ResourceSimpleDTO> toSimpleDTO(List<SysResource> entities);

    /**
     * Updates an existing {@link SysResource} entity with values from a {@link ResourceDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime). The {@code methods}
     * field is re-encoded via {@link #methodsToMask(List)}. Null values in the DTO are ignored
     * due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted,
     * ensuring partial updates without overwriting unchanged data or system-managed fields.
     *
     * @param dto    the source {@link ResourceDTO} containing updated parameters
     * @param entity the target {@link SysResource} entity to be updated in-place
     */
    @Mapping(target = "methods", qualifiedByName = "methodsToMask")
    void updateEntity(ResourceDTO dto, @MappingTarget SysResource entity);

    /**
     * Encodes a list of {@link HttpMethod} enums into an integer bitmask for efficient storage.
     * <p>
     * Uses {@link HttpMethod#combine(List)} to perform bitwise OR operations on each method's
     * flag value. For example:
     * <pre>
     *   GET(1) | POST(2) | PUT(4) = 7
     * </pre>
     * This enables compact storage of multiple HTTP methods in a single {@code INT} column
     * and supports fast bitwise permission checks at runtime.
     *
     * @param methods the list of {@link HttpMethod} to encode, may be {@code null} or empty
     * @return the combined integer mask, or {@code 0} if the input is {@code null} or empty
     * @see HttpMethod#combine(List)
     */
    @Named("methodsToMask")
    default Integer methodsToMask(List<HttpMethod> methods) {
        return HttpMethod.combine(methods);
    }

    /**
     * Decodes an integer bitmask back to a list of {@link HttpMethod} enums for frontend display.
     * <p>
     * Uses {@link HttpMethod#contains(int, HttpMethod)} to perform bitwise AND checks against
     * each enum's flag value. For example:
     * <pre>
     *   mask = 7 (binary: 111)
     *   → contains GET(001), POST(010), PUT(100)
     *   → returns [GET, POST, PUT]
     * </pre>
     * Returns an empty list if the mask is {@code null} or {@code 0}.
     *
     * @param mask the integer bitmask to decode, may be {@code null}
     * @return the list of {@link HttpMethod} represented by the mask,
     * or {@link Collections#emptyList()} if mask is {@code null} or {@code 0}
     */
    @Named("maskToMethods")
    default List<HttpMethod> maskToMethods(Integer mask) {
        if (null == mask || 0 == mask) {
            return Collections.emptyList();
        }
        return Arrays.stream(HttpMethod.values())
                .filter(method -> HttpMethod.contains(mask, method))
                .toList();
    }

}