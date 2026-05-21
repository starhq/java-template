package com.github.starhq.template.model.vo.dictData;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;

/**
 * Lightweight view object for dictionary data metadata in dropdowns, selectors, and internal service communication.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the dictionary data entry's unique identifier
 * and provides minimal fields required for data reference and display. Designed for scenarios
 * where full dictionary details are unnecessary, such as:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, radio buttons, tags with label/value pairs</li>
 *     <li><strong>Configuration Hints</strong>: Providing safe metadata for frontend form binding without exposing audit fields</li>
 *     <li><strong>Internal Service Communication</strong>: Passing dictionary references between microservices for distributed config</li>
 *     <li><strong>Cache Optimization</strong>: Storing dictionary metadata in Redis/Caffeine with minimal memory footprint</li>
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
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for dictionary identification and display (id, typeId, label, value)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond JSON serialization; usable in any Java context</li>
 * </ul>
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
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysDictData
 * @see com.github.starhq.template.service.DictDataService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictDataSimpleVO extends BaseIdVO {

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
    private static final long serialVersionUID = -1167008823137757214L;

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
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_dict_type.id} for foreign key integrity</li>
     *     <li>Nullability: Should not be {@code null} — every data entry must belong to a type</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_type_id ON sys_dict_data(type_id)}</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Frontend: Group dictionary data by type in a cascader
     * const groupedData = dictItems.reduce((acc, item) => {
     *   const typeId = item.typeId;
     *   if (!acc[typeId]) acc[typeId] = [];
     *   acc[typeId].push(item);
     *   return acc;
     * }, {});
     *
     * // Backend: Fetch data by type for form initialization
     * List<DictDataSimpleVO> statusOptions = dictDataService.getDataByTypeCode("user_status");
     * }
     * </pre>
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
     * // Vue 3: Populate a select dropdown
     * <a-select v-model="form.status" :options="statusOptions">
     *   <a-select-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
     *     {{ opt.label }}
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render radio group with dictionary data
     * <Radio.Group value={form.status} onChange={setStatus}>
     *   {options.map(opt => (
     *     <Radio key={opt.value} value={opt.value}>{opt.label}</Radio>
     *   ))}
     * </Radio.Group>
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
     *     <li>Show as tooltip on hover: {@code <a-tooltip :title="item.description">}</li>
     *     <li>Truncate long descriptions with ellipsis for dropdown layout</li>
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