package com.github.starhq.template.model.dto.dict.type;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data transfer object for dictionary type creation and update operations.
 * <p>
 * This class encapsulates the parameters required for managing dictionary type
 * definitions in the system. Dictionary types serve as logical containers for
 * key-value pairs ({@link com.github.starhq.template.entity.SysDictData}), enabling
 * dynamic configuration of system enums, status codes, and UI options without
 * code deployments.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Type Registration</strong>: Create new dictionary categories (e.g., "User Status", "Order Type")</li>
 *     <li><strong>Type Update</strong>: Modify type metadata (name, description) while preserving existing data entries</li>
 *     <li><strong>Admin Console</strong>: Provide structured input validation for dictionary management UI</li>
 *     <li><strong>API Contract</strong>: Define request/response schema for RESTful dictionary management endpoints</li>
 * </ul>
 * <p>
 * <strong>Validation Strategy:</strong>
 * <p>
 * This DTO leverages JSR-380 Bean Validation annotations to enforce data integrity
 * at the API boundary. Validation is triggered automatically when used with
 * {@code @Valid} in Spring MVC controllers:
 * <pre>
 * {@code
 * @PostMapping("/dict-types")
 * public Result<Long> createDictType(@RequestBody @Valid DictTypeDTO dto) {
 *     // dto is guaranteed to pass validation rules defined below
 *     Long id = dictTypeService.create(dto);
 *     return Result.success(id);
 * }
 * }
 * </pre>
 * <p>
 * <strong>Data Consistency Notes:</strong>
 * <ul>
 *     <li><strong>Unique Constraint</strong>: The {@code type} field must be globally unique; enforce via database {@code UNIQUE INDEX} and application-level pre-check</li>
 *     <li><strong>Immutable After Use</strong>: Once a dictionary type has associated data entries, changing its {@code type} code may break existing references; consider making {@code type} immutable after creation</li>
 *     <li><strong>Cascade Behavior</strong>: Deleting a dictionary type should either cascade-delete its data entries or reject deletion if entries exist (configurable per business policy)</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see com.github.starhq.template.entity.SysDictType
 * @see com.github.starhq.template.service.DictTypeService
 * @see jakarta.validation.Valid
 */
@Data
public class DictTypeDTO implements Serializable {

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
    private static final long serialVersionUID = -3504131929015871485L;

    /**
     * Optional explanatory text describing the purpose and usage rules of this dictionary type.
     * <p>
     * This field helps system administrators understand:
     * <ul>
     *     <li>What business scenario this type supports (e.g., "Status codes for user account lifecycle")</li>
     *     <li>Any special constraints or dependencies (e.g., "Referenced by user.profile.status field")</li>
     *     <li>Deprecation notices or migration guidance for legacy types</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @Size(min=0, max=255)}: Allows empty string (optional field) but caps length to prevent storage abuse</li>
     *     <li>Message key {@code {error.param.range}} resolves to localized error: "Must be between {min} and {max} characters"</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Write descriptions in clear, imperative language targeting non-technical administrators</li>
     *     <li>Avoid exposing internal implementation details or sensitive business logic</li>
     *     <li>Update descriptions when modifying type behavior to keep documentation in sync</li>
     * </ul>
     * <p>
     * <strong>Storage Recommendation:</strong>
     * <ul>
     *     <li>Database column: {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} type if detailed documentation or markdown formatting is required</li>
     *     <li>Add full-text index if descriptions are frequently searched in admin UI</li>
     * </ul>
     *
     * @see jakarta.validation.constraints.Size
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

    /**
     * The unique technical identifier/code for this dictionary type.
     * <p>
     * This field serves as the primary key for business logic to fetch associated
     * dictionary data entries. Typically follows {@code snake_case} or {@code camelCase}
     * conventions for readability and IDE auto-completion:
     * <ul>
     *     <li>{@code "user_status"} — Status codes for user accounts</li>
     *     <li>{@code "orderType"} — Classification of order types</li>
     *     <li>{@code "payment_method"} — Supported payment options</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null, empty, or whitespace-only</li>
     *     <li>{@code @Size(min=0, max=100)}: Enforces reasonable length; note {@code min=0} is redundant with {@code @NotBlank} but kept for explicit documentation</li>
     *     <li>Message keys: {@code {error.param.blank}} → "This field cannot be empty"; {@code {error.param.range}} → "Must be between 0 and 100 characters"</li>
     * </ul>
     * <p>
     * <strong>Uniqueness & Immutability:</strong>
     * <ul>
     *     <li><strong>Global Uniqueness</strong>: Must be unique across all dictionary types; enforce via database {@code UNIQUE INDEX uk_dict_type (type)} and application-level pre-check</li>
     *     <li><strong>Immutability After Creation</strong>: Once a type has associated data entries or is referenced in business logic, changing its {@code type} code may break existing references. Consider:
     *         <ul>
     *             <li>Making {@code type} immutable after initial creation (reject updates to this field)</li>
     *             <li>Or implementing a migration workflow: create new type → migrate data → deprecate old type</li>
     *         </ul>
     *     </li>
     * </ul>
     * <p>
     * <strong>Format Recommendations:</strong>
     * <ul>
     *     <li>Use lowercase with underscores ({@code snake_case}) for consistency: {@code "user_status"}</li>
     *     <li>Avoid special characters except underscores; restrict to {@code [a-z0-9_]} via regex validation if needed</li>
     *     <li>Prefix with module name for large systems: {@code "sys_user_status"}, {@code "biz_order_type"}</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Fetch dictionary data by type code
     * List<DictDataVO> statuses = dictDataService.getDataByTypeCode("user_status");
     *
     * // Frontend API request
     * GET /api/v1/dict-types/user_status/data
     * }
     * </pre>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 100, message = "{error.param.range}")
    private String type;

    /**
     * The human-readable display name for administrative interfaces.
     * <p>
     * This field is used in admin consoles, dropdown selectors, and documentation
     * to help system administrators identify the purpose of the dictionary type.
     * Examples: {@code "User Status"}, {@code "Order Priority"}, {@code "Region List"}.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be null, empty, or whitespace-only</li>
     *     <li>{@code @Size(min=0, max=255)}: Allows flexible naming while preventing excessive length</li>
     *     <li>Message keys resolve to localized, user-friendly error messages</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 50 characters recommended) for consistent UI layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "dict.type.user_status"})
     *         and resolving translations at the frontend or gateway layer</li>
     *     <li>Avoid technical jargon; target non-technical system administrators</li>
     *     <li>Use title case or sentence case for readability: {@code "User Status"} not {@code "user status"}</li>
     * </ul>
     * <p>
     * <strong>Uniqueness Consideration:</strong>
     * <p>
     * While not enforced at the database level, it is recommended to keep {@code name}
     * unique to avoid confusion in admin UIs. If duplicate names are allowed, ensure
     * the UI displays additional context (e.g., {@code type} code) to distinguish entries.
     * <p>
     * <strong>Storage Recommendation:</strong>
     * <ul>
     *     <li>Database column: {@code VARCHAR(255)} for standard names</li>
     *     <li>Add index on {@code name} if admin UI supports searching by display name</li>
     *     <li>Consider {@code COLLATE utf8mb4_unicode_ci} for case-insensitive searches if needed</li>
     * </ul>
     *
     * @see jakarta.validation.constraints.NotBlank
     * @see jakarta.validation.constraints.Size
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String name;

}