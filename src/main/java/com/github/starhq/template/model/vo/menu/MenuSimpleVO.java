package com.github.starhq.template.model.vo.menu;

import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * Lightweight view object for menu metadata in dropdowns, selectors, and internal service communication.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the menu's unique identifier
 * and provides minimal fields required for menu reference and display. Designed
 * for scenarios where full menu details are unnecessary, such as:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, cascaders, or tree selectors with menu options</li>
 *     <li><strong>Permission Hints</strong>: Providing safe metadata for frontend route guards without exposing audit fields</li>
 *     <li><strong>Internal Service Communication</strong>: Passing menu references between microservices for distributed auth</li>
 *     <li><strong>Cache Optimization</strong>: Storing menu metadata in Redis/Caffeine with minimal memory footprint</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for menu identification and display (id, parentId, name, url, icon, sort, openStyle)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond JSON serialization; usable in any Java context</li>
 * </ul>
 * <p>
 * <strong>Serialization Strategy:</strong>
 * <p>
 * The {@code parentId} field uses {@code @JsonSerialize(using = ToStringSerializer.class)}
 * to convert {@link Long} to {@code String} in JSON output. This prevents precision loss
 * when consuming APIs from JavaScript/TypeScript clients (which use 53-bit integers).
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-04
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysMenu
 * @see com.github.starhq.template.service.MenuService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MenuSimpleVO extends BaseIdVO {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -8830378623396258161L;

    /**
     * The unique identifier of the parent menu that contains this menu.
     * <p>
     * Establishes a hierarchical relationship: {@code Menu 1..* Menu}.
     * Used for grouping menus by parent in UI components and for permission
     * resolution in hierarchical navigation systems.
     * <p>
     * <strong>Serialization Strategy:</strong>
     * <p>
     * Annotated with {@code @JsonSerialize(using = ToStringSerializer.class)} to
     * convert the {@link Long} value to a {@code String} in JSON output. This prevents
     * precision loss when the API is consumed by JavaScript/TypeScript clients, which
     * represent integers as 64-bit floats with only 53 bits of precision.
     * <pre>
     * {@code
     * // Without ToStringSerializer:
     * { "parentId": 9007199254740993 }  // May be truncated in JS
     *
     * // With ToStringSerializer:
     * { "parentId": "9007199254740993" }  // Safe string representation
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_menu.id} for foreign key integrity</li>
     *     <li>Nullability: May be {@code null} for root-level menus (no parent)</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_parent_id ON sys_menu(parent_id)}</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Frontend: Group menus by parent in a cascader
     * const groupedMenus = menus.reduce((acc, menu) => {
     *   const parentId = menu.parentId ?? 'root';
     *   if (!acc[parentId]) acc[parentId] = [];
     *   acc[parentId].push(menu);
     *   return acc;
     * }, {});
     *
     * // Backend: Fetch child menus for a parent
     * List<MenuSimpleVO> childMenus = menuService.getMenusByParentId(1001L);
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu#getParentId()
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /**
     * The human-readable display name of the menu item for UI presentation.
     * <p>
     * This field is used in dropdowns, selectors, tooltips, and permission lists to help
     * users identify the purpose of each menu. Examples:
     * <ul>
     *     <li>{@code "Dashboard"} — Main overview page</li>
     *     <li>{@code "User Management"} — Parent menu for user-related operations</li>
     *     <li>{@code "Create User"} — Leaf menu for user creation form</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent UI layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "menu.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     *     <li>Use title case for readability: {@code "User Management"} not {@code "user management"}</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Populate a select dropdown
     * <a-select v-model="form.menuId" :options="menuOptions">
     *   <a-select-option v-for="menu in menuOptions" :key="menu.id" :value="menu.id">
     *     {{ menu.name }}
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render menu list with permission hints
     * {menus.map(menu => (
     *   <Button key={menu.id} disabled={!hasPerm(menu.code)}>
     *     {menu.name}
     *   </Button>
     * ))}
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu#getName()
     */
    private String name;

    /**
     * The target URL or route path for navigation when the menu is selected.
     * <p>
     * This field defines where the application should navigate when the user
     * clicks the menu item. Supports multiple routing strategies:
     * <ul>
     *     <li><strong>Frontend Route</strong>: {@code "/user/list"} — handled by Vue Router / React Router</li>
     *     <li><strong>External Link</strong>: {@code "https://example.com"} — opens in new tab if {@code openStyle = BLANK}</li>
     *     <li><strong>IFrame Embed</strong>: {@code "/report/dashboard"} — loaded in embedded iframe if {@code openStyle = IFRAME}</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Validate URLs against a whitelist to prevent open redirect attacks</li>
     *     <li>Sanitize external links to prevent XSS via {@code javascript:} protocols</li>
     *     <li>Ensure route paths match frontend route definitions to avoid 404 errors</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Frontend: Handle menu click based on openStyle
     * const handleMenuClick = (menu) => {
     *   switch (menu.openStyle) {
     *     case 'COMPONENT':
     *       router.push(menu.url); // SPA navigation
     *       break;
     *     case 'IFRAME':
     *       iframeRef.value.src = menu.url; // Load in iframe
     *       break;
     *     case 'BLANK':
     *       window.open(menu.url, '_blank', 'noopener,noreferrer'); // New tab with security
     *       break;
     *     case 'REDIRECT':
     *       window.location.href = menu.url; // Full redirect
     *       break;
     *   }
     * };
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu#getUrl()
     * @see OpenStyle
     */
    private String url;

    /**
     * The icon identifier for visual representation in menu lists and navigation.
     * <p>
     * This field references an icon from the frontend icon library (e.g.,
     * Ant Design Icons, Element Plus Icons, Font Awesome). Typical formats:
     * <ul>
     *     <li>Ant Design: {@code "user"}, {@code "setting"}, {@code "dashboard"}</li>
     *     <li>Element Plus: {@code "User"}, {@code "Setting"}, {@code "Odometer"}</li>
     *     <li>Font Awesome: {@code "fa-user"}, {@code "fa-cog"}, {@code "fa-tachometer"}</li>
     *     <li>Custom SVG: {@code "svg:custom-icon-name"}</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3 + Ant Design Vue: Dynamic icon rendering
     * <a-select-option v-for="menu in menuOptions" :key="menu.id" :value="menu.id">
     *   <a-icon :type="menu.icon" v-if="menu.icon" style="margin-right: 8px" />
     *   {{ menu.name }}
     * </a-select-option>
     *
     * // React + Ant Design: Icon component
     * {menu.icon && <Icon type={menu.icon} style={{ marginRight: 8 }} />}
     * {menu.name}
     *
     * // Fallback: Show default icon if not specified
     * <a-icon :type="menu.icon || 'file'" />
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use consistent icon naming convention across the application</li>
     *     <li>Provide fallback icons for menu items without explicit icon assignment</li>
     *     <li>Consider icon accessibility: add {@code aria-label} for screen readers</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysMenu#getIcon()
     */
    private String icon;

    /**
     * The sort order for determining menu item display sequence in lists and trees.
     * <p>
     * Lower values appear first in the menu hierarchy. Typical usage:
     * <ul>
     *     <li>{@code 1-99}: High-priority menus (Dashboard, Home)</li>
     *     <li>{@code 100-199}: Core business modules (User, Order, Report)</li>
     *     <li>{@code 200+}: Auxiliary functions (Settings, Help, About)</li>
     * </ul>
     * <p>
     * <strong>Sorting Strategy:</strong>
     * <ul>
     *     <li>Primary sort: {@code parent_id} (group items under same parent)</li>
     *     <li>Secondary sort: {@code sort_order ASC} (respect manual ordering)</li>
     *     <li>Tertiary sort: {@code created_at DESC} (newer items last within same sort order)</li>
     * </ul>
     * <p>
     * <strong>Database Index Recommendation:</strong>
     * <pre>
     * {@code
     * -- Optimize menu queries with composite index
     * CREATE INDEX idx_menu_parent_sort ON sys_menu(parent_id, sort_order);
     * }
     * </pre>
     * <p>
     * <strong>Frontend Rendering:</strong>
     * <pre>
     * {@code
     * // Ensure menu list is sorted before rendering
     * const sortedMenus = [...menus].sort((a, b) => a.sortOrder - b.sortOrder);
     * menuOptions.value = sortedMenus;
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu#getSortOrder()
     */
    private Integer sortOrder;

    /**
     * The display behavior when the menu item is selected.
     * <p>
     * Uses the {@link OpenStyle} enum to control how the target URL is opened:
     * <ul>
     *     <li>{@code COMPONENT} (default): Navigate within the current SPA frame via frontend router</li>
     *     <li>{@code IFRAME}: Load the URL in an embedded iframe within the admin layout</li>
     *     <li>{@code BLANK}: Open the URL in a new browser tab/window via {@code window.open()}</li>
     *     <li>{@code REDIRECT}: Perform a full-page redirect (bypassing SPA router)</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>For {@code BLANK} style: Add {@code rel="noopener noreferrer"} to prevent tabnabbing attacks</li>
     *     <li>For {@code IFRAME} style: Validate URL against same-origin policy or CSP headers</li>
     *     <li>For {@code REDIRECT} style: Ensure URL is whitelisted to prevent open redirect vulnerabilities</li>
     * </ul>
     *
     * @see OpenStyle
     * @see com.github.starhq.template.entity.SysMenu#getOpenStyle()
     */
    private OpenStyle openStyle;

}