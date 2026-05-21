package com.github.starhq.template.model.dto.dictData;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data transfer object for dictionary data entry creation and update operations.
 * <p>
 * This class encapsulates the parameters required for managing individual
 * key-value pairs within a dictionary type ({@link com.github.starhq.template.entity.SysDictType}).
 * Dictionary data entries enable dynamic configuration of system enums, status codes,
 * and UI options without code deployments, supporting:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, radio buttons, tags with localized labels</li>
 *     <li><strong>Status Management</strong>: Mapping numeric/string codes to human-readable descriptions</li>
 *     <li><strong>Business Rules</strong>: Configuring thresholds, flags, or workflow states via admin console</li>
 * </ul>
 * <p>
 * <strong>Label vs. Value Separation:</strong>
 * <p>
 * This DTO enforces a clear separation between presentation and persistence:
 * <ul>
 *     <li><strong>{@code label}</strong>: Human-readable text displayed to end-users in UI components</li>
 *     <li><strong>{@code value}</strong>: Machine-readable identifier stored in business tables as foreign key or enum</li>
 * </ul>
 * Example: For "User Status" dictionary:
 * <pre>
 * {@code
 * label = "Enabled", value = "1"  → UI shows "Enabled", database stores "1"
 * label = "Disabled", value = "0" → UI shows "Disabled", database stores "0"
 * }
 * </pre>
 * <p>
 * <strong>Validation Strategy:</strong>
 * <p>
 * This DTO leverages JSR-380 Bean Validation annotations to enforce data integrity
 * at the API boundary. Validation is triggered automatically when used with
 * {@code @Valid} in Spring MVC controllers:
 * <pre>
 * {@code
 * @PostMapping("/dict-data")
 * public Result<Long> createDictData(@RequestBody @Valid DictDataDTO dto) {
 *     // dto is guaranteed to pass validation rules defined below
 *     Long id = dictDataService.create(dto);
 *     return Result.success(id);
 * }
 * }
 * </pre>
 * <p>
 * <strong>Data Consistency Notes:</strong>
 * <ul>
 *     <li><strong>Unique Constraint</strong>: The combination {@code (typeId, value)} must be unique; enforce via database {@code UNIQUE INDEX} and application-level pre-check</li>
 *     <li><strong>Value Immutability</strong>: Once a {@code value} is referenced by business data, it should NEVER be changed to avoid breaking existing records</li>
 *     <li><strong>Label Flexibility</strong>: {@code label} can be updated freely for UI improvements without affecting business logic</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see com.github.starhq.template.entity.SysDictData
 * @see com.github.starhq.template.service.DictDataService
 * @see jakarta.validation.Valid
 */
@Data
public class DictDataDTO implements Serializable {

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
    private static final long serialVersionUID = 981980663L;

    /**
     * The unique identifier of the parent dictionary type that owns this data entry.
     * <p>
     * Establishes a many-to-one relationship: {@code DictType 1..* DictData}.
     * This field groups related key-value pairs under a logical category (e.g.,
     * "User Status", "Order Type", "Payment Method").
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotNull}: Must not be {@code null}; ensures referential integrity</li>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Foreign Key: References {@code sys_dict_type.id}; validate existence before insert</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * // Fetch all data entries for a dictionary type
     * List<DictDataVO> options = dictDataService.getDataByTypeId(1001L);
     *
     * // Frontend usage: populate dropdown
     * <select v-model="form.status">
     *   <option v-for="opt in options" :value="opt.value">{{ opt.label }}</option>
     * </select>
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Validate {@code typeId} exists before creating data entries to prevent orphaned records</li>
     *     <li>Cache dictionary data by {@code typeId} for efficient UI rendering</li>
     *     <li>Use constants for critical type IDs to avoid magic numbers in code</li>
     * </ul>
     *
     * @see jakarta.validation.constraints.NotNull
     * @see com.github.starhq.template.entity.SysDictType
     */
    @NotNull(message = "{error.param.blank}")
    private Long typeId;

    /**
     * The human-readable display text presented to end-users in UI components.
     * <p>
     * This field is used in dropdowns, radio buttons, badges, and tooltips to
     * help users understand the meaning of the underlying {@code value}.
     * Examples:
     * <ul>
     *     <li>{@code "Enabled"} / {@code "Disabled"} — For toggle switches</li>
     *     <li>{@code "Pending Review"} — For workflow status badges</li>
     *     <li>{@code "VIP Member"} — For user tier labels</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotNull}: Must not be {@code null} (empty string {@code ""} is allowed if business permits)</li>
     *     <li>{@code @Size(min=0, max=255)}: Allows flexible labeling while preventing storage abuse</li>
     *     <li>Message keys: {@code {error.param.blank}} → "This field cannot be null"; {@code {error.param.range}} → "Must be between 0 and 255 characters"</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep labels concise (≤ 50 characters recommended) for consistent UI layout</li>
     *     <li>For multi-language systems, store i18n keys (e.g., {@code "dict.status.active"})
     *         and resolve translations at the frontend or gateway layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     *     <li>Use title case or sentence case for readability: {@code "Pending Review"} not {@code "pending review"}</li>
     * </ul>
     * <p>
     * <strong>Uniqueness Consideration:</strong>
     * <p>
     * While {@code label} does not need to be unique within a {@code typeId},
     * duplicate labels may confuse end-users. If duplicates are allowed, ensure
     * the UI displays additional context (e.g., {@code value} or {@code description})
     * to distinguish entries.
     *
     * @see jakarta.validation.constraints.NotNull
     * @see jakarta.validation.constraints.Size
     */
    @NotNull(message = "{error.param.blank}")
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String label;

    /**
     * The machine-readable identifier stored in business tables as a reference value.
     * <p>
     * This is the actual value persisted in foreign business entities (e.g.,
     * {@code user.status = '1'}). While stored as {@link String} for flexibility,
     * it often represents numeric codes, enum constants, or boolean flags.
     * <p>
     * <strong>Format Convention:</strong>
     * <ul>
     *     <li>Numeric codes: {@code "0"}, {@code "1"}, {@code "100"}</li>
     *     <li>String codes: {@code "PENDING"}, {@code "APPROVED"}, {@code "USD"}</li>
     *     <li>Boolean-like: {@code "true"}, {@code "false"}, {@code "Y"}, {@code "N"}</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotNull}: Must not be {@code null} (empty string {@code ""} is allowed if business permits)</li>
     *     <li>{@code @Size(min=0, max=255)}: Enforces reasonable length for storage efficiency</li>
     * </ul>
     * <p>
     * <strong>Immutability Warning:</strong>
     * <p>
     * Once referenced by business data, the {@code value} should <strong>NEVER</strong> be changed.
     * Modifying it will break existing records and cause data inconsistency.
     * If a change is required, implement a migration workflow:
     * <ol>
     *     <li>Create a new dictionary data entry with the new {@code value}</li>
     *     <li>Update business records to reference the new {@code value}</li>
     *     <li>Deprecate or soft-delete the old entry after migration completes</li>
     * </ol>
     * <p>
     * <strong>Uniqueness Constraint:</strong>
     * <ul>
     *     <li><strong>Per-Type Uniqueness</strong>: {@code value} must be unique within the same {@code typeId}</li>
     *     <li>Enforce via database {@code UNIQUE INDEX uk_dict_data (type_id, value)}</li>
     *     <li>Pre-check in application layer to provide friendly error messages</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Business entity references dictionary value
     * @TableName("sys_user")
     * public class SysUser {
     *     private String status; // stores "1" or "0", not "Enabled"/"Disabled"
     * }
     *
     * // Service layer converts value to enum for business logic
     * UserStatus status = EnumUtils.parse(UserStatus.class, user.getStatus());
     * }
     * </pre>
     *
     * @see jakarta.validation.constraints.NotNull
     * @see jakarta.validation.constraints.Size
     */
    @NotNull(message = "{error.param.blank}")
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String value;

    /**
     * Optional explanatory text for administrators to understand the business context
     * or usage rules of this dictionary entry.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what the value controls</li>
     *     <li>Documenting deprecated entries or migration notes</li>
     *     <li>Clarifying complex business rules (e.g., {@code "Used only for enterprise accounts"})</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @Size(min=0, max=255)}: Optional field; empty string is valid</li>
     *     <li>Message key {@code {error.param.range}} resolves to localized error</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Avoid exposing internal implementation details or sensitive business logic</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     *     <li>Update descriptions when modifying entry behavior to keep documentation in sync</li>
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

}