package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

/**
 * Entity class representing a button-level permission in the RBAC system.
 * <p>
 * This class maps to the {@code sys_button} table and extends {@link BaseEntity}
 * to provide full audit trail (creator/updater information). Buttons are the
 * finest granularity of permission control, typically used to show/hide UI
 * elements (buttons, icons, menu items) or guard API endpoints based on
 * user roles.
 * <p>
 * <strong>Permission Model:</strong>
 * <ul>
 *     <li>Each button is associated with a parent {@code menuId} for logical grouping</li>
 *     <li>The {@code code} field serves as the unique permission identifier used in:
 *         <ul>
 *             <li>Frontend: {@code v-if="$hasPerm('user:delete')"} for Vue/React conditional rendering</li>
 *             <li>Backend: {@code @PreAuthorize("@ss.hasPerm('user:delete')")} for method-level security</li>
 *         </ul>
 *     </li>
 *     <li>Roles grant access to buttons; users inherit permissions via role assignments</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * // Frontend (Vue 3 + Pinia)
 * <el-button v-if="$hasPerm('user:delete')" @click="handleDelete">Delete</el-button>
 *
 * // Backend (Spring Security + AOP)
 * @PostMapping("/delete")
 * @PreAuthorize("@ss.hasPerm('user:delete')")
 * public Result<Void> deleteUser(@RequestBody UserDTO dto) { ... }
 *
 * // Service layer permission check
 * if (!permissionService.hasPerm(currentUser.getId(), "user:delete")) {
 *     throw new BusinessException("Permission denied");
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see TableName
 * @see <a href="https://en.wikipedia.org/wiki/Role-based_access_control">RBAC (Wikipedia)</a>
 */
@Data
@Alias("button")
@TableName("sys_button")
@EqualsAndHashCode(callSuper = false)
public class SysButton extends BaseEntity {

    /**
     * The unique identifier of the parent menu that contains this button.
     * <p>
     * Establishes a hierarchical relationship: {@code Menu 1..* Button}.
     * Buttons are typically displayed under their parent menu in permission
     * configuration UIs for intuitive management.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_menu.id} for foreign key integrity</li>
     *     <li>Nullability: Should not be {@code null} — every button must belong to a menu</li>
     *     <li>Index Recommendation: Add index for efficient menu-based queries:
     *         {@code CREATE INDEX idx_menu_id ON sys_button(menu_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all buttons under a specific menu
     * SELECT id, name, code FROM sys_button WHERE menu_id = 1001 ORDER BY sort_order;
     *
     * -- Join with menu for permission tree rendering
     * SELECT b.*, m.name as menu_name, m.path as menu_path
     * FROM sys_button b
     * JOIN sys_menu m ON b.menu_id = m.id
     * WHERE m.status = 1 AND b.status = 1;
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu
     */
    private Long menuId;

    /**
     * The human-readable display name of the button for UI presentation.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code "Create User"} — For user creation button</li>
     *     <li>{@code "Export Data"} — For data export functionality</li>
     *     <li>{@code "Assign Role"} — For role assignment operation</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent UI layout</li>
     *     <li>Use i18n keys (e.g., {@code "button.user.create"}) for multi-language support</li>
     *     <li>Avoid special characters or HTML tags to prevent XSS risks</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Length: {@code @Size(min = 2, max = 50)}</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[\\w\\s\\-\\u4e00-\\u9fa5]+$")} for safe characters</li>
     * </ul>
     */
    private String name;

    /**
     * The unique permission identifier used for access control checks.
     * <p>
     * <strong>Format Convention:</strong> {@code module:resource:action}
     * <ul>
     *     <li>{@code user:create} — Create user permission</li>
     *     <li>{@code user:delete} — Delete user permission</li>
     *     <li>{@code report:export:excel} — Export report to Excel</li>
     *     <li>{@code system:config:update} — Update system configuration</li>
     * </ul>
     * <p>
     * <strong>Uniqueness Guarantee:</strong>
     * <p>
     * The {@code code} must be globally unique across all buttons to prevent
     * permission conflicts. Enforce via:
     * <ul>
     *     <li>Database: {@code UNIQUE INDEX uk_button_code (code)}</li>
     *     <li>Application: Pre-insert uniqueness check with optimistic locking</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Never expose raw permission codes in public APIs without authorization</li>
     *     <li>Validate permission codes against a whitelist to prevent injection attacks</li>
     *     <li>Use constant definitions for critical permissions to avoid typos:
     *         {@code public static final String USER_DELETE = "user:delete";}</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank}</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[a-z][a-z0-9]*(:[a-z][a-z0-9]*){1,3}$")} for standardized format</li>
     *     <li>Length: {@code @Size(min = 5, max = 64)}</li>
     * </ul>
     *
     */
    private String code;

    /**
     * Optional detailed description of the button's purpose and usage context.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what the permission controls</li>
     *     <li>Documentation generation for permission dictionaries</li>
     *     <li>Audit trails clarifying why a permission was granted/revoked</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in imperative mood: {@code "Allows exporting user data to CSV format"}</li>
     *     <li>Include business impact: {@code "Grants ability to modify system-wide configurations"}</li>
     *     <li>Avoid technical jargon; target non-technical administrators</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong>
     * <ul>
     *     <li>Use {@code VARCHAR(255)} for short descriptions</li>
     *     <li>Consider {@code TEXT} if detailed documentation is required</li>
     *     <li>Add full-text index for searchability if descriptions are frequently queried</li>
     * </ul>
     */
    private String description;

}