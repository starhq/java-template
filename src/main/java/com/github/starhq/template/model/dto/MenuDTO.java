package com.github.starhq.template.model.dto;

import com.github.starhq.template.common.enums.OpenStyle;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object for creating or updating menu entries in the RBAC system.
 * <p>
 * This class encapsulates user-submitted form data for menu management operations,
 * with built-in validation constraints to ensure data integrity before business processing.
 * It is typically used in:
 * <ul>
 *     <li><strong>Admin Console</strong>: Menu creation/edit forms in navigation configuration</li>
 *     <li><strong>API Endpoints</strong>: {@code POST /api/menus} and {@code PUT /api/menus/{id}} request bodies</li>
 *     <li><strong>Service Layer</strong>: Type-safe parameter passing with compile-time validation hints</li>
 * </ul>
 * <p>
 * <strong>Validation Strategy:</strong>
 * <p>
 * All constraints use internationalized message keys (e.g., {@code "{error.param.blank}"})
 * configured in {@code ValidationMessages.properties} for multi-language support.
 * Validation is triggered automatically by Spring's {@code @Valid} annotation in controllers:
 * <pre>
 * {@code
 * @PostMapping("/menus")
 * public Result<Void> createMenu(@Valid @RequestBody MenuDTO dto) {
 *     // dto is guaranteed to pass validation constraints here
 *     menuService.create(dto);
 *     return Result.success();
 * }
 * }
 * </pre>
 * <p>
 * <strong>Serialization:</strong>
 * <p>
 * Implements {@link Serializable} with a fixed {@code serialVersionUID} to support:
 * <ul>
 *     <li>Caching DTO instances in distributed caches (Redis)</li>
 *     <li>Transmitting across service boundaries in microservice architectures</li>
 *     <li>Session replication in clustered deployments</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see jakarta.validation.Valid
 * @see com.github.starhq.template.service.MenuService
 * @see <a href="https://beanvalidation.org/2.0/">Jakarta Bean Validation 2.0 Specification</a>
 */
@Data
public class MenuDTO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 995675792L;

    /**
     * The unique identifier of the parent menu for hierarchical menu structure.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Optional: May be {@code null} for root-level menus</li>
     *     <li>Business Constraint: If provided, must reference an existing, active {@code SysMenu} record</li>
     *     <li>Circular Reference Prevention: Must not reference itself or any of its descendants</li>
     * </ul>
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Establishes hierarchical relationship: {@code Menu 1..* Submenu}</li>
     *     <li>Used for UI tree rendering: menus are displayed under their parent in navigation trees</li>
     *     <li>Enables cascade operations: deleting a parent menu may auto-remove child menus</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Create root menu (no parent)
     * MenuDTO rootDto = new MenuDTO();
     * rootDto.setParentId(null);
     * rootDto.setName("System Management");
     *
     * // Create child menu under "System Management" (id=1001)
     * MenuDTO childDto = new MenuDTO();
     * childDto.setParentId(1001L);
     * childDto.setName("User Management");
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu
     */
    private Long parentId;

    /**
     * The human-readable display name of the menu for UI presentation.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=0, max=30)}: Length must be between 0 and 30 characters (inclusive)</li>
     *     <li>Message Key: {@code "{error.param.blank}"} / {@code "{error.param.range}"} for i18n support</li>
     * </ul>
     * <p>
     * <strong>Business Guidelines:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 15 characters recommended) for consistent menu layout</li>
     *     <li>Use title case: {@code "User Management"}, {@code "Role Configuration"}</li>
     *     <li>Avoid special characters or HTML tags to prevent XSS rendering issues</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "menu.system.user"}) instead</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is typically bound to form input fields with real-time validation:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-form-item label="Menu Name" prop="name">
     *   <el-input v-model="form.name" :maxlength="30" show-word-limit />
     * </el-form-item>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 30, message = "{error.param.range}")
    private String name;

    /**
     * The URL path or route identifier for the menu entry.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=0, max=100)}: Length must be between 0 and 100 characters (inclusive)</li>
     *     <li>Uniqueness: Must be globally unique across all menus (enforced at service/database layer)</li>
     * </ul>
     * <p>
     * <strong>Format Convention:</strong>
     * <ul>
     *     <li>Internal routes: {@code "/system/user"} (Vue Router path)</li>
     *     <li>External links: {@code "https://external.example.com"}</li>
     *     <li>Empty string {@code ""} for parent-only menus (container nodes)</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Validate URLs to prevent open redirect vulnerabilities</li>
     *     <li>Sanitize external URLs to prevent XSS attacks</li>
     *     <li>Consider implementing Content Security Policy (CSP) headers for embedded content</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is used for route matching and navigation:
     * <pre>
     * {@code
     * // Vue Router configuration
     * {
     *   path: '/system/user',
     *   component: () => import('@/views/system/User.vue'),
     *   meta: { title: 'User Management', icon: 'user' }
     * }
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 100, message = "{error.param.range}")
    private String url;

    /**
     * The icon identifier for the menu entry (font icon or SVG name).
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=0, max=50)}: Length must be between 0 and 50 characters (inclusive)</li>
     *     <li>Common formats: {@code "el-icon-user"}, {@code "mdi-account"}, {@code "dashboard-outline"}</li>
     * </ul>
     * <p>
     * <strong>Business Guidelines:</strong>
     * <ul>
     *     <li>Use consistent icon naming convention across the application</li>
     *     <li>Keep names short (≤ 20 characters) for clean configuration</li>
     *     <li>Consider using icon libraries (Element Plus Icons, Material Design Icons)</li>
     *     <li>For multi-language systems, icons are typically language-agnostic</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is bound to icon components in the navigation:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-icon><User /></el-icon>
     * <span>{{ menu.name }}</span>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 50, message = "{error.param.range}")
    private String icon;

    /**
     * The sort order for menu entries within the same parent level.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotNull}: Must not be {@code null}; required for all create/update operations</li>
     *     <li>{@code @Min(0)}: Must be greater than or equal to 0</li>
     *     <li>{@code @Max(9999)}: Must be less than or equal to 9999</li>
     *     <li>Message Keys: {@code "{error.param.min}"} / {@code "{error.param.max}"} for i18n support</li>
     * </ul>
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Lower values appear first in the menu list</li>
     *     <li>Menus with the same {@code parentId} are sorted by {@code sortOrder} ascending</li>
     *     <li>Use increments of 10 (e.g., 10, 20, 30) to allow easy insertion of new items</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Menu structure under "System Management"
     * // - User Management (sortOrder=10)
     * // - Role Configuration (sortOrder=20)
     * // - Menu Management (sortOrder=30)
     * // - Audit Logs (sortOrder=40)
     * }
     * </pre>
     */
    @NotNull(message = "{error.param.blank}")
    @Min(value = 0, message = "{error.param.min}")
    @Max(value = 9999, message = "{error.param.max}")
    private Integer sortOrder;

    /**
     * The open style for the menu entry (how child menus are displayed).
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotNull}: Must not be {@code null}; required for all create/update operations</li>
     *     <li>Valid values: {@link OpenStyle#INTERNAL}, {@link OpenStyle#EXTERNAL}</li>
     * </ul>
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>{@code LINK}: Menu opens in the main content area (standard navigation)</li>
     *     <li>{@code POPUP}: Menu opens in a modal/dialog window (overlay navigation)</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Standard navigation menu
     * MenuDTO standardDto = new MenuDTO();
     * standardDto.setOpenStyle(OpenStyle.LINK);
     *
     * // Modal dialog menu (e.g., "Add User" dialog)
     * MenuDTO dialogDto = new MenuDTO();
     * dialogDto.setOpenStyle(OpenStyle.POPUP);
     * }
     * </pre>
     *
     * @see OpenStyle
     */
    @NotNull(message = "{error.param.blank}")
    private OpenStyle openStyle;

}
