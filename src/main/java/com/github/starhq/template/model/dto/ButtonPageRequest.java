package com.github.starhq.template.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Pagination request parameters for querying button permissions with menu-based filtering.
 * <p>
 * This class extends {@link PageRequest} to inherit standard pagination fields
 * ({@code page}, {@code size}, {@code sort}) and adds a {@code menuId} filter
 * for scoped button retrieval within a specific menu context. Designed for
 * admin console button management and role permission configuration scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Menu-Based Button Management</strong>: List buttons under a specific menu for CRUD operations</li>
 *     <li><strong>Role Permission Configuration</strong>: Filter available buttons by menu when assigning permissions to roles</li>
 *     <li><strong>Audit & Reporting</strong>: Generate button usage reports scoped by menu hierarchy</li>
 * </ul>
 * <p>
 * <strong>Query Semantics:</strong>
 * <p>
 * The {@code menuId} field triggers exact-match filtering against {@code sys_button.menu_id}:
 * <ul>
 *     <li>If {@code menuId} is {@code null} or not set: Return buttons from all menus (global list)</li>
 *     <li>If {@code menuId} is set: Return only buttons directly belonging to that menu (no recursive child menus)</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Pagination logic in parent class, business filters in child class</li>
 *     <li><strong>Extensibility</strong>: Easy to add more filters (e.g., {@code keyword}, {@code status}) without modifying base class</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see PageRequest
 * @see com.github.starhq.template.entity.SysButton
 * @see com.github.starhq.template.service.ButtonService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonPageRequest extends PageRequest {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., via RPC or messaging).
     * Update this value only if the class structure changes in a
     * backward-incompatible way (e.g., removing fields, changing types).
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -4711061179951332492L;

    /**
     * Filter buttons by their parent menu identifier.
     * <p>
     * This field enables scoped queries to retrieve buttons belonging to a
     * specific menu node in the navigation hierarchy. Typical usage patterns:
     * <ul>
     *     <li><strong>Menu Management UI</strong>: Show buttons under the selected menu for editing</li>
     *     <li><strong>Role Configuration</strong>: List available buttons when assigning permissions to a role</li>
     *     <li><strong>Audit Queries</strong>: Filter button operation logs by menu context</li>
     * </ul>
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li><strong>Exact Match</strong>: {@code WHERE menu_id = :menuId} — does not include buttons from child menus</li>
     *     <li><strong>Null Handling</strong>: If {@code null} or not set, no menu filtering is applied (returns all buttons)</li>
     *     <li><strong>Index Usage</strong>: Query leverages {@code idx_menu_id} index for efficient filtering</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: Optional field; {@code null} means "no filter"</li>
     *     <li>Validation: Should be positive if set; consider adding {@code @Min(1)} if business requires</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Accept menuId from query param
     * @GetMapping("/buttons")
     * public Result<IPage<ButtonPageVO>> listButtons(ButtonPageRequest request) {
     *     // request.getMenuId() may be null for global list
     *     return Result.success(buttonService.page(request));
     * }
     *
     * // Service: Build query with optional menu filter
     * public IPage<ButtonPageVO> page(ButtonPageRequest request) {
     *     LambdaQueryWrapper<SysButton> wrapper = new LambdaQueryWrapper<>()
     *         .eq(request.getMenuId() != null, SysButton::getMenuId, request.getMenuId())
     *         .orderByAsc(SysButton::getSortOrder);
     *
     *     Page<ButtonPageVO> page = new Page<>(request.getPage(), request.getSize());
     *     return buttonMapper.selectPage(page, wrapper);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure {@code sys_button(menu_id, sort_order)} composite index exists for efficient filtered sorting</li>
     *     <li><strong>Cache Friendly</strong>: Results can be cached by {@code (menuId, page, size)} key for repeated queries</li>
     *     <li><strong>Pagination</strong>: Always combine with pagination to limit result set size for large menus</li>
     * </ul>
     * <p>
     * <strong>Extension Guidance:</strong>
     * <p>
     * If additional filters are needed (e.g., keyword search, status filter),
     * extend this class or add fields directly:
     * <pre>
     * {@code
     * @Data
     * @EqualsAndHashCode(callSuper = false)
     * public class AdvancedButtonPageRequest extends ButtonPageRequest {
     *     private String keyword;        // Fuzzy search on button name/code
     *     private Integer status;        // Filter by button status (1=active, 0=disabled)
     *     private List<Long> buttonIds;  // Filter by specific button IDs
     * }
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysButton#getMenuId()
     */
    private Long menuId;

}