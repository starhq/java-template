package com.github.starhq.template.model.vo.dict.type;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Lightweight view object for dictionary key-value pairs in UI components and internal service communication.
 * <p>
 * This class provides the minimal representation of a dictionary data entry: a human-readable
 * {@code dictLabel} for display and a machine-readable {@code dictValue} for persistence.
 * It is designed for scenarios where only the label/value pair is needed, without metadata
 * like IDs, type references, or audit fields.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, radio buttons, tags with label/value options</li>
 *     <li><strong>Form Binding</strong>: Providing safe metadata for frontend form submission without exposing internal IDs</li>
 *     <li><strong>Service-to-Service Communication</strong>: Passing dictionary references between microservices for distributed config</li>
 *     <li><strong>Cache Optimization</strong>: Storing label/value pairs in Redis/Caffeine with minimal memory footprint</li>
 * </ul>
 * <p>
 * <strong>Label vs. Value Separation:</strong>
 * <p>
 * This VO enforces a clear separation between presentation and persistence:
 * <ul>
 *     <li><strong>{@code dictLabel}</strong>: Human-readable text displayed to end-users in UI components</li>
 *     <li><strong>{@code dictValue}</strong>: Machine-readable identifier stored in business tables as foreign key or enum</li>
 * </ul>
 * Example: For "User Status" dictionary:
 * <pre>
 * {@code
 * dictLabel = "Enabled", dictValue = "1"  → UI shows "Enabled", database stores "1"
 * dictLabel = "Disabled", dictValue = "0" → UI shows "Disabled", database stores "0"
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for dictionary identification and display (dictLabel, dictValue)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-09
 * @see java.io.Serializable
 * @see com.github.starhq.template.entity.SysDictData
 */
@Data
public class DictDataVO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., via RPC or messaging).
     * Update this value only if the class structure changes in a
     * backward-incompatible way (e.g., removing fields, changing types).
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 5724782551937991433L;

    /**
     * The human-readable display text presented to end-users in UI components.
     * <p>
     * This field is used in dropdowns, radio buttons, badges, and tooltips to
     * help users understand the meaning of the underlying {@code dictValue}.
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
     *   <a-select-option v-for="opt in statusOptions" :key="opt.dictValue" :value="opt.dictValue">
     *     {{ opt.dictLabel }}
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render radio group with dictionary data
     * <Radio.Group value={form.status} onChange={setStatus}>
     *   {options.map(opt => (
     *     <Radio key={opt.dictValue} value={opt.dictValue}>{opt.dictLabel}</Radio>
     *   ))}
     * </Radio.Group>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictData#getLabel()
     */
    private String dictLabel;

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
     * Once referenced by business data, the {@code dictValue} should <strong>NEVER</strong> be changed.
     * Modifying it will break existing records and cause data inconsistency.
     * If a change is required, implement a migration workflow:
     * <ol>
     *     <li>Create a new dictionary data entry with the new {@code dictValue}</li>
     *     <li>Update business records to reference the new {@code dictValue}</li>
     *     <li>Deprecate or soft-delete the old entry after migration completes</li>
     * </ol>
     *
     * @see com.github.starhq.template.entity.SysDictData#getValue()
     */
    private String dictValue;

}