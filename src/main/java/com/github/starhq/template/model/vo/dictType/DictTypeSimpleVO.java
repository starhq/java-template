package com.github.starhq.template.model.vo.dictType;

import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Lightweight view object for dictionary type metadata in dropdowns, selectors, and internal service communication.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the dictionary type's unique identifier
 * and provides minimal fields required for type reference and display. Designed for scenarios
 * where full dictionary type details are unnecessary, such as:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, cascaders, or tree selectors with dictionary type options</li>
 *     <li><strong>Configuration Hints</strong>: Providing safe metadata for frontend form binding without exposing audit fields</li>
 *     <li><strong>Internal Service Communication</strong>: Passing dictionary type references between microservices for distributed config</li>
 *     <li><strong>Cache Optimization</strong>: Storing dictionary type metadata in Redis/Caffeine with minimal memory footprint</li>
 * </ul>
 * <p>
 * <strong>Field Semantics:</strong>
 * <ul>
 *     <li><strong>{@code type}</strong>: Unique technical identifier/code for the dictionary type (e.g., "user_status")</li>
 *     <li><strong>{@code name}</strong>: Human-readable display name for administrative interfaces (e.g., "User Status")</li>
 *     <li><strong>{@code description}</strong>: Optional explanatory text for administrators</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for dictionary type identification and display (id, type, name, description)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-09
 * @see BaseIdVO
 * @see com.github.starhq.template.entity.SysDictType
 * @see com.github.starhq.template.service.DictTypeService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictTypeSimpleVO extends BaseIdVO {

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
    private static final long serialVersionUID = 2986620577899369251L;

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
     * <strong>Uniqueness & Immutability:</strong>
     * <ul>
     *     <li><strong>Global Uniqueness</strong>: Must be unique across all dictionary types; enforce via database {@code UNIQUE INDEX} and application-level pre-check</li>
     *     <li><strong>Immutability After Creation</strong>: Once a type has associated data entries or is referenced in business logic, changing its {@code type} code may break existing references. Consider making {@code type} immutable after creation</li>
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
     * // Frontend: Use type code as key to access data list
     * const dictionaries = {
     *   user_status: [
     *     { label: "Enabled", value: "1" },
     *     { label: "Disabled", value: "0" }
     *   ],
     *   order_type: [
     *     { label: "Standard", value: "STD" },
     *     { label: "Express", value: "EXP" }
     *   ]
     * };
     *
     * // Usage in form binding
     * <select v-model="form.status" :options="dictionaries.user_status" />
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictType#getType()
     */
    private String type;

    /**
     * The human-readable display name for administrative interfaces.
     * <p>
     * This field is used in admin consoles, dropdown selectors, and documentation
     * to help system administrators identify the purpose of the dictionary type.
     * Examples: {@code "User Status"}, {@code "Order Priority"}, {@code "Region List"}.
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
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Populate a select dropdown
     * <a-select v-model="form.dictType" :options="typeOptions">
     *   <a-select-option v-for="opt in typeOptions" :key="opt.type" :value="opt.type">
     *     {{ opt.name }}
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render radio group with dictionary types
     * <Radio.Group value={form.dictType} onChange={setDictType}>
     *   {types.map(t => (
     *     <Radio key={t.type} value={t.type}>{t.name}</Radio>
     *   ))}
     * </Radio.Group>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictType#getName()
     */
    private String name;

    /**
     * Optional explanatory text describing the purpose and usage rules of this dictionary type.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what the type controls</li>
     *     <li>Documenting deprecated types or migration notes</li>
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
     * @see com.github.starhq.template.entity.SysDictType#getDescription()
     */
    private String description;

}