package com.github.starhq.template.converter;

import com.github.starhq.template.entity.SysMenu;
import com.github.starhq.template.model.dto.menu.MenuDTO;
import com.github.starhq.template.model.vo.menu.MenuSimpleVO;
import com.github.starhq.template.model.vo.menu.tree.LeftNavVO;
import com.github.starhq.template.model.vo.menu.tree.MenuListVO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct converter interface for transforming {@link SysMenu} entities
 * between persistence layer and presentation/DTO layers.
 * <p>
 * This converter supports bidirectional mapping for menu permission management:
 * <ul>
 *     <li>Entity → {@link MenuSimpleVO}: For basic dropdowns or reference lookups</li>
 *     <li>Entity → {@link MenuListVO}: For hierarchical tree rendering in permission configs</li>
 *     <li>Entity → {@link LeftNavVO}: For sidebar navigation structures based on user permissions</li>
 *     <li>{@link MenuDTO} → Entity: For create/update request binding</li>
 *     <li>Partial update: {@link MenuDTO} → existing {@link SysMenu} via {@code @MappingTarget}</li>
 * </ul>
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
 * @date 2026-04-06
 * @see <a href="https://mapstruct.org/documentation/stable/reference/html/">MapStruct Reference Guide</a>
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface MenuConverter {

    /**
     * Converts a {@link SysMenu} entity to a {@link MenuSimpleVO} for basic reference.
     * <p>
     * Typically used in form dropdowns, parent menu selectors, or lightweight UI components
     * where only essential fields (id, menuName, path) are required.
     *
     * @param entity the source {@link SysMenu} entity from persistence layer
     * @return the target {@link MenuSimpleVO} for lightweight display scenarios,
     * or {@code null} if the input entity is {@code null}
     */
    MenuSimpleVO toSimpleVO(SysMenu entity);


    /**
     * Converts a {@link SysMenu} entity to a {@link MenuListVO} for hierarchical tree rendering.
     * <p>
     * Typically used in role permission configuration dialogs where menus are displayed as a
     * collapsible tree structure. The {@code children} field is populated recursively if the
     * source entity contains nested child menus.
     *
     * @param entity the source {@link SysMenu} entity from persistence layer
     * @return the target {@link MenuListVO} for tree-structured UI rendering,
     * or {@code null} if the input entity is {@code null}
     */
    MenuListVO toListVO(SysMenu entity);

    /**
     * Converts a {@link SysMenu} entity to a {@link LeftNavVO} for sidebar navigation.
     * <p>
     * Typically used for rendering the left sidebar menu in frontend applications based on
     * the authenticated user's permissions. Fields like {@code icon}, {@code hidden}, and
     * {@code alwaysShow} are mapped to support dynamic route generation in Vue/React frameworks.
     *
     * @param entity the source {@link SysMenu} entity from persistence layer
     * @return the target {@link LeftNavVO} for client-side navigation rendering,
     * or {@code null} if the input entity is {@code null}
     */
    LeftNavVO toLeftNavVO(SysMenu entity);

    /**
     * Converts a {@link MenuDTO} request to a {@link SysMenu} entity for persistence.
     * <p>
     * Typically used in create operations where the DTO contains user-submitted form data.
     * Auto-generated fields (id, createTime) should be set by the service or database layer.
     *
     * @param dto the source {@link MenuDTO} containing user-submitted creation parameters
     * @return the target {@link SysMenu} entity ready for persistence,
     * or {@code null} if the input DTO is {@code null}
     */
    SysMenu toEntity(MenuDTO dto);

    /**
     * Updates an existing {@link SysMenu} entity with values from a {@link MenuDTO}.
     * <p>
     * This method performs an in-place update using {@code @MappingTarget}, preserving
     * fields not present in the DTO (e.g., auto-generated id, createTime, createBy).
     * Null values in the DTO are ignored due to {@code NullValuePropertyMappingStrategy.IGNORE}.
     * <p>
     * Typically used in update operations where only modified fields are submitted,
     * ensuring partial updates without overwriting unchanged data or system-managed fields.
     *
     * @param dto    the source {@link MenuDTO} containing updated parameters
     * @param entity the target {@link SysMenu} entity to be updated in-place
     */
    void updateEntity(MenuDTO dto, @MappingTarget SysMenu entity);

}