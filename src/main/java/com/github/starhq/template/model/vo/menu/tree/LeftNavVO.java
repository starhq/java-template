package com.github.starhq.template.model.vo.menu.tree;

import com.github.starhq.template.common.enums.OpenStyle;
import com.github.starhq.template.model.vo.tree.BaseIdTreeVO;
import com.github.starhq.template.model.vo.tree.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * View object for rendering left-side navigation menus in admin console or user portal.
 * <p>
 * This class extends {@link BaseIdTreeVO} to inherit tree structure capabilities
 * ({@code id}, {@code parentId}, {@code children}) and implements {@link Tree}
 * for recursive menu hierarchy traversal. Designed for generating dynamic sidebar
 * navigation based on user permissions and menu configuration.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Admin Sidebar</strong>: Render hierarchical navigation menus with icons, URLs, and open styles</li>
 *     <li><strong>Permission-Based Rendering</strong>: Filter menu tree by user roles to show only authorized items</li>
 *     <li><strong>Frontend Routing</strong>: Provide structured data for Vue Router / React Router dynamic route generation</li>
 *     <li><strong>Responsive Layout</strong>: Support collapsible/expandable menu groups via {@code children} nesting</li>
 * </ul>
 * <p>
 * <strong>Tree Structure Integration:</strong>
 * <p>
 * By implementing {@link Tree<LeftNavVO>}, this VO supports recursive operations:
 * <pre>
 * {@code
 * // Build tree from flat list
 * List<LeftNavVO> flatList = menuService.getUserMenus(userId);
 * List<LeftNavVO> tree = TreeUtils.buildTree(flatList, 0L); // 0L = root parent ID
 *
 * // Frontend: Render recursive menu component
 * <MenuTree :nodes="tree" @select="handleMenuSelect" />
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for navigation rendering (name, url, icon, sort, openStyle)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond tree interface; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-04
 * @see BaseIdTreeVO
 * @see Tree
 * @see com.github.starhq.template.entity.SysMenu
 * @see com.github.starhq.template.service.MenuService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class LeftNavVO extends BaseIdTreeVO<LeftNavVO> implements Tree<LeftNavVO> {

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
    private static final long serialVersionUID = 4586445110027566788L;

    /**
     * The human-readable display name of the menu item for UI presentation.
     * <p>
     * This field is shown in the left navigation sidebar to help users
     * identify the purpose of each menu node. Examples:
     * <ul>
     *     <li>{@code "Dashboard"} — Main overview page</li>
     *     <li>{@code "User Management"} — Parent menu for user-related operations</li>
     *     <li>{@code "Create User"} — Leaf menu for user creation form</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent sidebar layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "menu.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     *     <li>Use title case for readability: {@code "User Management"} not {@code "user management"}</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Recursive menu component
     * <template #title="{ node }">
     *   <a-icon :type="node.icon" />
     *   <span>{{ $t(node.name) }}</span> <!-- i18n resolution -->
     * </template>
     *
     * // React: Menu item rendering
     * <Menu.Item key={node.id} icon={<Icon type={node.icon} />}>
     *   {t(node.name)}
     * </Menu.Item>
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
     * <strong>Routing Integration:</strong>
     * <pre>
     * {@code
     * // Vue Router: Add dynamic routes from menu tree
     * const menuRoutes = leftNavTree.map(node => ({
     *   path: node.url,
     *   component: () => import(`@/views/${node.component}.vue`),
     *   meta: { title: node.name, icon: node.icon }
     * }));
     * router.addRoutes(menuRoutes);
     *
     * // Click handler: Navigate to selected menu
     * const handleMenuSelect = (node) => {
     *   if (node.openStyle === 'BLANK') {
     *     window.open(node.url, '_blank');
     *   } else if (node.openStyle === 'IFRAME') {
     *     iframeRef.value.src = node.url;
     *   } else {
     *     router.push(node.url);
     *   }
     * };
     * }
     * </pre>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Validate URLs against a whitelist to prevent open redirect attacks</li>
     *     <li>Sanitize external links to prevent XSS via {@code javascript:} protocols</li>
     *     <li>Ensure route paths match frontend route definitions to avoid 404 errors</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysMenu#getUrl()
     * @see OpenStyle
     */
    private String url;

    /**
     * The icon identifier for visual representation in the navigation sidebar.
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
     * <template #icon="{ node }">
     *   <a-icon :type="node.icon" v-if="node.icon" />
     * </template>
     *
     * // React + Ant Design: Icon component
     * {node.icon && <Icon type={node.icon} />}
     *
     * // Fallback: Show default icon if not specified
     * <a-icon :type="node.icon || 'file'" />
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
     * The sort order for determining menu item display sequence.
     * <p>
     * Lower values appear first in the navigation sidebar. Typical usage:
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
     * -- Optimize menu tree queries with composite index
     * CREATE INDEX idx_menu_parent_sort ON sys_menu(parent_id, sort_order, created_at);
     * }
     * </pre>
     * <p>
     * <strong>Frontend Rendering:</strong>
     * <pre>
     * {@code
     * // Ensure tree is sorted before rendering
     * const sortedTree = treeUtils.sortTree(leftNavTree, (a, b) => a.sortOrder - b.sortOrder);
     * menuItems.value = sortedTree;
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
     * <strong>Frontend Implementation Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Handle menu selection based on openStyle
     * const handleMenuSelect = (node) => {
     *   switch (node.openStyle) {
     *     case 'COMPONENT':
     *       router.push(node.url); // SPA navigation
     *       break;
     *     case 'IFRAME':
     *       iframeRef.value.src = node.url; // Load in iframe
     *       activeMenu.value = node.id;
     *       break;
     *     case 'BLANK':
     *       window.open(node.url, '_blank'); // New tab
     *       break;
     *     case 'REDIRECT':
     *       window.location.href = node.url; // Full redirect
     *       break;
     *   }
     * };
     * }
     * </pre>
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