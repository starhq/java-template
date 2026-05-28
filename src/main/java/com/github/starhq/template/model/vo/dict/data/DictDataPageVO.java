package com.github.starhq.template.model.vo.dict.data;

import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * View object for paginated dictionary data responses in admin console or API clients.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit common audit trail fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and adds dictionary-specific business fields for comprehensive data management.
 * Designed for rendering dictionary entries in management interfaces with full context.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Dictionary Management</strong>: Display paginated dictionary entries with filtering by type, label, value</li>
 *     <li><strong>UI Component Population</strong>: Provide structured data for dropdowns, radio buttons, and tags</li>
 *     <li><strong>Audit & Reporting</strong>: Track dictionary data creation/modification history via inherited audit fields</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React table components with sorting/pagination</li>
 * </ul>
 * <p>
 * <strong>Label vs. Value Separation:</strong>
 * <p>
 * This VO enforces a clear separation between presentation and persistence:
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
 * <strong>Serialization Strategy:</strong>
 * <p>
 * The {@code typeId} field uses {@code @JsonSerialize(using = ToStringSerializer.class)}
 * to convert {@link Long} to {@code String} in JSON output. This prevents precision loss
 * when consuming APIs from JavaScript/TypeScript clients (which use 53-bit integers).
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-10
 * @see BaseAuditVO
 * @see com.github.starhq.template.entity.SysDictData
 * @see com.github.starhq.template.service.DictDataService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataPageVO extends BaseAuditVO {

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
    private static final long serialVersionUID = 7807113097916961890L;

    /**
     * The unique identifier of the parent dictionary type that owns this data entry.
     * <p>
     * Establishes a many-to-one relationship: {@code DictType 1..* DictData}.
     * This field groups related key-value pairs under a logical category (e.g.,
     * "User Status", "Order Type", "Payment Method").
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
     * { "typeId": 9007199254740993 }  // May be truncated in JS
     *
     * // With ToStringSerializer:
     * { "typeId": "9007199254740993" }  // Safe string representation
     * }
     * </pre>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * // Fetch all data entries for a dictionary type
     * GET /api/v1/dict-data?typeId=1001
     *
     * // Frontend usage: populate dropdown
     * <select v-model="form.status">
     *   <option v-for="opt in options" :value="opt.value">{{ opt.label }}</option>
     * </select>
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_dict_type.id} for foreign key integrity</li>
     *     <li>Nullability: Should not be {@code null} — every data entry must belong to a type</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_type_id ON sys_dict_data(type_id)}</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysDictType
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
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
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep labels concise (≤ 50 characters recommended) for consistent UI layout</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "dict.status.active"})
     *         and resolving translations at the frontend or gateway layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     *     <li>Use title case or sentence case for readability: {@code "Pending Review"} not {@code "pending review"}</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 table column
     * <a-table-column title="Label" data-index="label">
     *   <template #bodyCell="{ text }">
     *     <a-tag color="blue">{{ text }}</a-tag>
     *   </template>
     * </a-table-column>
     *
     * // React: Populate a select dropdown
     * <Select value={form.status} onChange={setStatus}>
     *   {options.map(opt => (
     *     <Select.Option key={opt.value} value={opt.value}>
     *       {opt.label}
     *     </Select.Option>
     *   ))}
     * </Select>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictData#getLabel()
     */
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
     * <strong>Usage in Business Logic:</strong>
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
     *
     * // Frontend: submit form with value (not label)
     * const submit = async () => {
     *   await api.updateUser({ ...form.value, status: form.value.status });
     *   // Database stores: { status: "1" }
     * };
     * }
     * </pre>
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
     *
     * @see com.github.starhq.template.entity.SysDictData#getValue()
     */
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
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Avoid exposing internal implementation details or sensitive business logic</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show as tooltip on hover: {@code <a-tooltip :title="record.description">}</li>
     *     <li>Truncate long descriptions with ellipsis for table layout</li>
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
     * @see com.github.starhq.template.entity.SysDictData#getDescription()
     */
    private String description;

}