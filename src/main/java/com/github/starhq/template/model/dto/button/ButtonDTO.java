package com.github.starhq.template.model.dto.button;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object for creating or updating button permissions in the RBAC system.
 * <p>
 * This class encapsulates user-submitted form data for button management operations,
 * with built-in validation constraints to ensure data integrity before business processing.
 * It is typically used in:
 * <ul>
 *     <li><strong>Admin Console</strong>: Button creation/edit forms in role permission configuration</li>
 *     <li><strong>API Endpoints</strong>: {@code POST /api/buttons} and {@code PUT /api/buttons/{id}} request bodies</li>
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
 * @PostMapping("/buttons")
 * public Result<Void> createButton(@Valid @RequestBody ButtonDTO dto) {
 *     // dto is guaranteed to pass validation constraints here
 *     buttonService.create(dto);
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
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-03
 * @see jakarta.validation.Valid
 * @see com.github.starhq.template.service.ButtonService
 * @see <a href="https://beanvalidation.org/2.0/">Jakarta Bean Validation 2.0 Specification</a>
 */
@Data
public class ButtonDTO implements Serializable {

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
    private static final long serialVersionUID = -6134204419617079273L;

    /**
     * The unique identifier of the parent menu that contains this button.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotNull}: Must not be {@code null}; required for all create/update operations</li>
     *     <li>Business Constraint: Must reference an existing, active {@code SysMenu} record</li>
     * </ul>
     * <p>
     * <strong>Business Semantics:</strong>
     * <ul>
     *     <li>Establishes hierarchical relationship: {@code Menu 1..* Button}</li>
     *     <li>Used for UI grouping: buttons are displayed under their parent menu in permission trees</li>
     *     <li>Enables cascade operations: deleting a menu may auto-remove associated buttons</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Create button under "User Management" menu (id=1001)
     * ButtonDTO dto = new ButtonDTO();
     * dto.setMenuId(1001L);
     * dto.setName("Export User Data");
     * dto.setCode("user:export");
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysMenu
     */
    @NotNull(message = "{error.param.blank}")
    private Long menuId;

    /**
     * The human-readable display name of the button for UI presentation.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=4, max=100)}: Length must be between 4 and 100 characters (inclusive)</li>
     *     <li>Message Key: {@code "{error.param.blank}"} / {@code "{error.param.range}"} for i18n support</li>
     * </ul>
     * <p>
     * <strong>Business Guidelines:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 20 characters recommended) for consistent button layout</li>
     *     <li>Use imperative mood: {@code "Create"}, {@code "Export"}, {@code "Delete"} for action clarity</li>
     *     <li>Avoid special characters or HTML tags to prevent XSS rendering issues</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "button.user.export"}) instead</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is typically bound to form input fields with real-time validation:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-form-item label="Button Name" prop="name">
     *   <el-input v-model="form.name" :maxlength="100" :minlength="4" show-word-limit />
     * </el-form-item>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 100, message = "{error.param.range}")
    private String name;

    /**
     * The unique permission identifier used for access control checks.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=4, max=100)}: Length must be between 4 and 100 characters (inclusive)</li>
     *     <li>Uniqueness: Must be globally unique across all buttons (enforced at service/database layer)</li>
     * </ul>
     * <p>
     * <strong>Format Convention:</strong> {@code module:resource:action}
     * <ul>
     *     <li>{@code user:create} — Create user permission</li>
     *     <li>{@code user:export} — Export user data permission</li>
     *     <li>{@code report:generate:pdf} — Generate PDF report permission</li>
     * </ul>
     * <p>
     * <strong>Security & Integration:</strong>
     * <ul>
     *     <li>Used in Spring Security expressions: {@code @PreAuthorize("hasAuthority('user:export')")}</li>
     *     <li>Used in frontend permission directives: {@code v-if="$hasPerm('user:export')"} (Vue/React)</li>
     *     <li>Never expose raw permission codes in public APIs without authorization checks</li>
     *     <li>Consider defining constants for critical permissions to avoid typos:
     *         {@code public static final String USER_EXPORT = "user:export";}</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use lowercase letters, numbers, and colons only (no spaces or special chars)</li>
     *     <li>Start with module name for logical grouping and easier auditing</li>
     *     <li>Document permission codes in a centralized registry for team reference</li>
     * </ul>
     *
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 100, message = "{error.param.range}")
    private String code;

    /**
     * Optional explanatory text for administrators to understand the button's purpose.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @Size(min=0, max=255)}: Optional field; if provided, length must not exceed 255 characters</li>
     *     <li>Allows {@code null} or empty string for buttons with self-explanatory names</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Include business impact: {@code "Allows exporting user data to CSV format for offline analysis"}</li>
     *     <li>Avoid technical jargon or internal implementation details</li>
     *     <li>Keep under 255 characters for optimal storage and UI tooltip rendering</li>
     * </ul>
     * <p>
     * <strong>Usage Scenarios:</strong>
     * <ul>
     *     <li>Admin console tooltips explaining what the permission controls</li>
     *     <li>Permission dictionary documentation for compliance audits</li>
     *     <li>Onboarding guides for new system administrators</li>
     * </ul>
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

}