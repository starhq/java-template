package com.github.starhq.template.model.dto.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/**
 * Data Transfer Object for creating or updating role definitions in the RBAC system.
 * <p>
 * This class encapsulates user-submitted form data for role management operations,
 * with built-in validation constraints to ensure data integrity before business processing.
 * It is typically used in:
 * <ul>
 *     <li><strong>Admin Console</strong>: Role creation/edit forms in role configuration</li>
 *     <li><strong>API Endpoints</strong>: {@code POST /api/roles} and {@code PUT /api/roles/{id}} request bodies</li>
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
 * @PostMapping("/roles")
 * public Result<Void> createRole(@Valid @RequestBody RoleDTO dto) {
 *     // dto is guaranteed to pass validation constraints here
 *     roleService.create(dto);
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
 * @see com.github.starhq.template.service.RoleService
 * @see <a href="https://beanvalidation.org/2.0/">Jakarta Bean Validation 2.0 Specification</a>
 */
@Data
public class RoleDTO implements Serializable {

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
    private static final long serialVersionUID = 972602042L;

    /**
     * The unique permission code identifier for the role.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=4, max=50)}: Length must be between 4 and 50 characters (inclusive)</li>
     *     <li>Uniqueness: Must be globally unique across all roles (enforced at service/database layer)</li>
     * </ul>
     * <p>
     * <strong>Format Convention:</strong>
     * <ul>
     *     <li>Use lowercase letters, numbers, and underscores only</li>
     *     <li>Snake case naming: {@code "admin"}, {@code "user_manager"}, {@code "report_viewer"}</li>
     *     <li>Should be descriptive of the role's purpose or scope</li>
     * </ul>
     * <p>
     * <strong>Security & Integration:</strong>
     * <ul>
     *     <li>Used in Spring Security expressions: {@code @PreAuthorize("hasRole('admin')")}</li>
     *     <li>Used in frontend role-based access control (RBAC) directives</li>
     *     <li>Never expose raw role codes in public APIs without authorization checks</li>
     *     <li>Consider defining constants for critical roles to avoid typos:
     *         {@code public static final String ADMIN = "admin";}</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use lowercase letters, numbers, and underscores only (no spaces or special chars)</li>
     *     <li>Keep codes concise but descriptive for easy reference</li>
     *     <li>Document role codes in a centralized registry for team reference</li>
     * </ul>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 50, message = "{error.param.range}")
    private String code;

    /**
     * The human-readable display name of the role for UI presentation.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=4, max=50)}: Length must be between 4 and 50 characters (inclusive)</li>
     *     <li>Message Key: {@code "{error.param.blank}"} / {@code "{error.param.range}"} for i18n support</li>
     * </ul>
     * <p>
     * <strong>Business Guidelines:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters recommended) for consistent UI layout</li>
     *     <li>Use title case: {@code "System Administrator"}, {@code "User Manager"}, {@code "Data Analyst"}</li>
     *     <li>Avoid special characters or HTML tags to prevent XSS rendering issues</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "role.admin"}) instead</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is typically bound to form input fields with real-time validation:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-form-item label="Role Name" prop="name">
     *   <el-input v-model="form.name" :maxlength="50" :minlength="4" show-word-limit />
     * </el-form-item>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 50, message = "{error.param.range}")
    private String name;

    /**
     * Optional explanatory text for administrators to understand the role's purpose and scope.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @Size(min=0, max=255)}: Optional field; if provided, length must not exceed 255 characters</li>
     *     <li>Allows {@code null} or empty string for roles with self-explanatory names</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Include scope and permissions: {@code "Can manage user accounts, assign roles, and view audit logs"}</li>
     *     <li>Avoid technical jargon or internal implementation details</li>
     *     <li>Keep under 255 characters for optimal storage and UI tooltip rendering</li>
     * </ul>
     * <p>
     * <strong>Usage Scenarios:</strong>
     * <ul>
     *     <li>Admin console tooltips explaining what the role can do</li>
     *     <li>Role dictionary documentation for compliance audits</li>
     *     <li>Onboarding guides for new system administrators</li>
     * </ul>
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

    /**
     * The set of API resource identifiers assigned to this role.
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Establishes many-to-many relationship: {@code Role 1..* Resource}</li>
     *     <li>Used for API-level permission checks via {@code @PreAuthorize("hasPermission(...))"}</li>
     *     <li>Enables fine-grained access control for RESTful endpoints</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Assign API resources to role during creation
     * RoleDTO dto = new RoleDTO();
     * dto.setCode("admin");
     * dto.setName("System Administrator");
     * dto.setResourceIds(Set.of(101L, 102L, 103L)); // User CRUD, Role Management, Audit Log
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Large resource sets may impact query performance; consider pagination for assignment UI</li>
     *     <li>Cache role-resource mappings with TTL to reduce database queries</li>
     *     <li>Validate resource IDs exist before assignment to prevent orphaned references</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysResource
     */
    private Set<Long> resourceIds;

    /**
     * The set of menu identifiers assigned to this role.
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Establishes many-to-many relationship: {@code Role 1..* Menu}</li>
     *     <li>Used for UI navigation control: menus are shown/hidden based on role assignment</li>
     *     <li>Enables role-based sidebar navigation in admin console</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Assign menus to role for navigation control
     * RoleDTO dto = new RoleDTO();
     * dto.setMenuIds(Set.of(1001L, 1002L, 1003L)); // User Management, Role Config, Menu Config
     * }
     * </pre>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * Menu assignments are typically used to dynamically render navigation:
     * <pre>
     * {@code
     * // Vue 3 example
     * <el-menu :default-active="activeMenu">
     *   <el-menu-item
     *     v-for="menu in userMenus"
     *     :key="menu.id"
     *     :index="menu.url">
     *     {{ menu.name }}
     *   </el-menu-item>
     * </el-menu>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu
     */
    private Set<Long> menuIds;

    /**
     * The set of button identifiers assigned to this role.
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Establishes many-to-many relationship: {@code Role 1..* Button}</li>
     *     <li>Used for UI button-level permission control: buttons are shown/hidden based on role</li>
     *     <li>Enables granular permission control within pages (e.g., "Create", "Export", "Delete")</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Assign button permissions to role
     * RoleDTO dto = new RoleDTO();
     * dto.setButtonIds(Set.of(201L, 202L)); // User Create, User Export
     * }
     * </pre>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * Button assignments are typically used for conditional rendering:
     * <pre>
     * {@code
     * // Vue 3 example
     * <el-button
     *   v-if="$hasPerm('user:create')"
     *   type="primary"
     *   @click="handleCreate">
     *   Create User
     * </el-button>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysButton
     */
    private Set<Long> buttonIds;

}
