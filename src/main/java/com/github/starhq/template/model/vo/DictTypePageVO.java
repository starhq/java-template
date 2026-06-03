package com.github.starhq.template.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * View object for paginated dictionary type responses in admin console or API clients.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit common audit trail fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and adds dictionary type-specific business fields for comprehensive type management.
 * Designed for rendering dictionary type lists in management interfaces with full context.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Dictionary Type Management</strong>: Display paginated dictionary types with filtering by type code, name</li>
 *     <li><strong>Admin Console</strong>: Provide structured data for Vue/React table components with sorting/pagination</li>
 *     <li><strong>Audit & Reporting</strong>: Track dictionary type creation/modification history via inherited audit fields</li>
 *     <li><strong>Configuration Export</strong>: Export dictionary type definitions for backup or migration</li>
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
 *     <li><strong>Separation of Concerns</strong>: Entity persistence ({@code SysDictType}) vs. UI presentation ({@code DictTypePageVO})</li>
 *     <li><strong>Audit Integration</strong>: Inherits audit fields from {@link BaseAuditVO} for compliance tracking</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-09
 * @see BaseAuditVO
 * @see com.github.starhq.template.entity.SysDictType
 * @see com.github.starhq.template.service.DictTypeService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class DictTypePageVO extends BaseAuditVO {

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
    private static final long serialVersionUID = 2095749722209419518L;

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
     * // Fetch dictionary data by type code
     * List<DictDataVO> statuses = dictDataService.getDataByTypeCode("user_status");
     *
     * // Frontend API request
     * GET /api/v1/dict-types/user_status/data
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
     * <strong>Uniqueness Consideration:</strong>
     * <p>
     * While not enforced at the database level, it is recommended to keep {@code name}
     * unique to avoid confusion in admin UIs. If duplicate names are allowed, ensure
     * the UI displays additional context (e.g., {@code type} code) to distinguish entries.
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 table column
     * <a-table-column title="Type Name" data-index="name">
     *   <template #bodyCell="{ text, record }">
     *     <a-tooltip :title="`Code: ${record.type}`">
     *       {{ text }}
     *     </a-tooltip>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictType#getName()
     */
    private String name;

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
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Write descriptions in clear, imperative language targeting non-technical administrators</li>
     *     <li>Avoid exposing internal implementation details or sensitive business logic</li>
     *     <li>Update descriptions when modifying type behavior to keep documentation in sync</li>
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
     * @see com.github.starhq.template.entity.SysDictType#getDescription()
     */
    private String description;

}