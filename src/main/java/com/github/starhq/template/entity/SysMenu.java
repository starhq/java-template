package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.common.enums.OpenStyle;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing a navigation menu item in the system.
 * <p>
 * This class maps to the {@code sys_menu} table and extends {@link BaseEntity}
 * to provide full audit trail. Menus form a hierarchical tree structure that
 * drives the application's sidebar navigation, routing configuration, and
 * permission-based UI rendering.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Sidebar Navigation</strong>: Rendering collapsible menu trees in admin dashboards</li>
 *     <li><strong>Route Configuration</strong>: Mapping {@code url} to frontend router paths (Vue Router/React Router)</li>
 *     <li><strong>Permission Control</strong>: Linking menu visibility to user roles and button permissions</li>
 *     <li><strong>UI Behavior</strong>: Defining how pages are opened ({@code OpenStyle}) and ordered ({@code sortOrder})</li>
 * </ul>
 * <p>
 * <strong>Tree Structure & Caching:</strong>
 * <p>
 * Menus are queried frequently for authenticated users. Always implement
 * application-level caching (Redis/Caffeine) keyed by {@code userId} or {@code roleId}
 * to avoid repetitive recursive database queries. The {@code parentId} field
 * establishes parent-child relationships, with {@code 0L} typically representing root nodes.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see com.github.starhq.template.common.enums.OpenStyle
 * @see TableName
 */
@Data
@Alias("menu")
@TableName("sys_menu")
@EqualsAndHashCode(callSuper = false)
public class SysMenu extends BaseEntity {

    /**
     * The unique identifier of the parent menu node.
     * <p>
     * Establishes the hierarchical tree structure: {@code Menu 1..* SubMenu}.
     * Root menus typically use {@code 0L} as the parent ID.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_menu.id} for self-join relationships</li>
     *     <li>Root Indicator: {@code 0L} denotes top-level menus</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_parent_id ON sys_menu(parent_id)} for efficient tree traversal</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch direct children of a parent menu
     * SELECT id, name, url FROM sys_menu WHERE parent_id = 1001 ORDER BY sort_order;
     *
     * -- Build full tree using recursive CTE (MySQL 8.0+)
     * WITH RECURSIVE MenuTree AS (
     *   SELECT * FROM sys_menu WHERE parent_id = 0
     *   UNION ALL
     *   SELECT m.* FROM sys_menu m INNER JOIN MenuTree mt ON m.parent_id = mt.id
     * )
     * SELECT * FROM MenuTree ORDER BY sort_order;
     * }
     * </pre>
     */
    private Long parentId;

    /**
     * The human-readable display name of the menu item.
     * <p>
     * Used in sidebar navigation, breadcrumb trails, and page titles.
     * Examples: {@code "User Management"}, {@code "System Settings"}, {@code "Dashboard"}.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent sidebar layout</li>
     *     <li>For multi-language systems, store i18n keys (e.g., {@code "menu.system.settings"})
     *         and resolve translations at the frontend or gateway layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Uniqueness: Recommended to be unique under the same {@code parentId} to avoid UI confusion</li>
     * </ul>
     */
    private String name;

    /**
     * The frontend route path or external link associated with this menu.
     * <p>
     * Determines the navigation target when the menu item is clicked:
     * <ul>
     *     <li><strong>Internal Routes</strong>: Relative paths like {@code "/system/users"} or {@code "/dashboard"}
     *         mapped to Vue/React router components</li>
     *     <li><strong>External Links</strong>: Full URLs like {@code "https://docs.example.com"} for new tabs or iframes</li>
     *     <li><strong>Directory Nodes</strong>: {@code null} or {@code "#"} for parent menus that only expand/collapse children</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Validate URLs against a whitelist to prevent open redirect vulnerabilities</li>
     *     <li>Never store backend API endpoints here; use {@code url} strictly for frontend routing</li>
     *     <li>For external links, ensure {@code target="_blank"} and {@code rel="noopener noreferrer"} are applied in UI</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Optional: Allow {@code null} for directory/category nodes</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^(https?:\\/\\/)?[\\w\\-./]+$")} for safe URL format</li>
     * </ul>
     */
    private String url;

    /**
     * The CSS class or icon identifier for visual representation in the UI.
     * <p>
     * Typically references icon library classes:
     * <ul>
     *     <li>Element Plus: {@code "el-icon-setting"}</li>
     *     <li>Ant Design: {@code "anticon anticon-user"}</li>
     *     <li>FontAwesome: {@code "fa fa-dashboard"}</li>
     *     <li>Custom SVG: {@code "icon-svg-custom"}</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep consistent with the frontend icon library version</li>
     *     <li>Use semantic names (e.g., {@code "icon-user-mgmt"} instead of random strings)</li>
     *     <li>Parent menus should have icons; leaf menus may omit if UI design allows</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong>
     * <ul>
     *     <li>Use {@code VARCHAR(64)} for standard icon class names</li>
     *     <li>Consider JSON or structured format if supporting dynamic SVG/color configurations</li>
     * </ul>
     */
    private String icon;

    /**
     * The display order of this menu relative to siblings under the same parent.
     * <p>
     * Lower values appear first in the navigation tree. Typically used with
     * {@code ORDER BY sort_order ASC} in database queries or frontend sorting logic.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Integer} — supports negative values for special positioning</li>
     *     <li>Default: {@code 0} or {@code 100} if not explicitly set</li>
     *     <li>Gap Strategy: Leave gaps between values (e.g., 10, 20, 30) to allow easy reordering without updating all records</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use increments of 10 for flexible drag-and-drop reordering in admin UIs</li>
     *     <li>Index: {@code INDEX idx_parent_sort (parent_id, sort_order)} for efficient tree rendering</li>
     * </ul>
     */
    private Integer sortOrder;

    /**
     * Defines how the menu content should be opened in the frontend application.
     * <p>
     * Controlled by the {@link OpenStyle} enum, typical values include:
     * <ul>
     *     <li>{@code INTERNAL}: Rendered within the main application layout (default)</li>
     *     <li>{@code EXTERNAL_TAB}: Opens in a new browser tab/window</li>
     *     <li>{@code IFRAME}: Embedded in an inline frame within the current layout</li>
     *     <li>{@code DIALOG}: Displays as a modal/popup window</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * The frontend router/layout component should switch rendering strategy based on this field:
     * <pre>
     * {@code
     * // Vue/React pseudo-code
     * switch (menu.openStyle) {
     *   case 'INTERNAL': return <router-link :to="menu.url" />;
     *   case 'EXTERNAL_TAB': return <a :href="menu.url" target="_blank" />;
     *   case 'IFRAME': return <iframe :src="menu.url" />;
     *   case 'DIALOG': return <Dialog :url="menu.url" />;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotNull} in DTO layer</li>
     *     <li>Default: {@code OpenStyle.INTERNAL} for backward compatibility</li>
     *     <li>Extensibility: Add new enum values without database schema changes</li>
     * </ul>
     *
     * @see OpenStyle
     */
    private OpenStyle openStyle;

}