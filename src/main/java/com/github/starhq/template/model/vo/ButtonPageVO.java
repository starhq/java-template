package com.github.starhq.template.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * View object for paginated button permission responses in admin console or API clients.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit common audit trail fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and adds button-specific business fields for comprehensive permission management.
 * Designed for rendering button lists in management interfaces with full context.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Button Management</strong>: Display paginated button definitions with filtering by menu, name, code</li>
 *     <li><strong>Role Configuration</strong>: List available buttons when assigning permissions to roles</li>
 *     <li><strong>Audit & Reporting</strong>: Track button creation/modification history via inherited audit fields</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React table components with sorting/pagination</li>
 * </ul>
 * <p>
 * <strong>Serialization Strategy:</strong>
 * <p>
 * The {@code menuId} field uses {@code @JsonSerialize(using = ToStringSerializer.class)}
 * to convert {@link Long} to {@code String} in JSON output. This prevents precision loss
 * when consuming APIs from JavaScript/TypeScript clients (which use 53-bit integers).
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-03
 * @see BaseAuditVO
 * @see com.github.starhq.template.entity.SysButton
 * @see com.github.starhq.template.service.ButtonService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ButtonPageVO extends BaseAuditVO {

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
    private static final long serialVersionUID = -4855817067297408727L;

    /**
     * The unique identifier of the parent menu that contains this button.
     * <p>
     * Establishes a hierarchical relationship: {@code Menu 1..* Button}.
     * Buttons are typically displayed under their parent menu in permission
     * configuration UIs for intuitive management.
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
     * { "menuId": 9007199254740993 }  // May be truncated in JS
     *
     * // With ToStringSerializer:
     * { "menuId": "9007199254740993" }  // Safe string representation
     * }
     * </pre>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * // Fetch all buttons under a specific menu
     * GET /api/v1/buttons?menuId=1001
     *
     * // Join with menu for permission tree rendering
     * SELECT b.*, m.name as menu_name, m.path as menu_path
     * FROM sys_button b
     * JOIN sys_menu m ON b.menu_id = m.id
     * WHERE m.status = 1 AND b.status = 1;
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_menu.id} for foreign key integrity</li>
     *     <li>Nullability: Should not be {@code null} — every button must belong to a menu</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_menu_id ON sys_button(menu_id)}</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysMenu
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long menuId;

    /**
     * The human-readable display name of the button for UI presentation.
     * <p>
     * Used in permission configuration dialogs, button lists, and audit reports
     * to help administrators identify the purpose of each button permission.
     * Examples:
     * <ul>
     *     <li>{@code "Create User"} — For user creation button</li>
     *     <li>{@code "Export Data"} — For data export functionality</li>
     *     <li>{@code "Assign Role"} — For role assignment operation</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters) for consistent table layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "button.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 table column
     * <a-table-column title="Button Name" data-index="name">
     *   <template #bodyCell="{ text }">
     *     <a-tag color="blue">{{ text }}</a-tag>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysButton#getName()
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
     * <strong>Usage in Authorization:</strong>
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
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[a-z][a-z0-9]*(:[a-z][a-z0-9]*){1,3}$")} for standardized format</li>
     *     <li>Length: {@code @Size(min = 5, max = 64)}</li>
     * </ul>
     *
     */
    private String code;

    /**
     * Optional explanatory text describing the button's purpose and usage context.
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
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show in table column with ellipsis truncation for long descriptions</li>
     *     <li>Provide tooltip on hover for full text: {@code <a-tooltip :title="record.description">}</li>
     *     <li>Support markdown formatting if rich text descriptions are enabled</li>
     * </ul>
     * <p>
     * <strong>Storage Recommendation:</strong>
     * <ul>
     *     <li>Database column: {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} type if detailed documentation or markdown formatting is required</li>
     *     <li>Add full-text index if descriptions are frequently searched in admin UI</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysButton#getDescription()
     */
    private String description;

}