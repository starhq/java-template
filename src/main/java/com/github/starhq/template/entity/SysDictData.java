package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

/**
 * Entity class representing a key-value pair within a dictionary type.
 * <p>
 * This class maps to the {@code sys_dict_data} table and extends {@link BaseEntity}
 * to provide full audit trail. It decouples hardcoded UI labels, status codes,
 * and configuration options from business logic, enabling dynamic system behavior
 * without requiring code deployments or database schema changes.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, radio buttons, checkboxes, and tags</li>
 *     <li><strong>Status Management</strong>: Mapping numeric/string status codes to human-readable labels</li>
 *     <li><strong>System Configuration</strong>: Storing adjustable parameters (e.g., timeout thresholds, feature flags)</li>
 *     <li><strong>i18n Support</strong>: Using {@code label} as a localization key for multi-language interfaces</li>
 * </ul>
 * <p>
 * <strong>Data Integrity & Caching:</strong>
 * <p>
 * Dictionary data is read-heavy and write-rare. Always implement application-level caching
 * (e.g., Redis/Caffeine) to avoid repetitive database queries. The combination of
 * {@code typeId} and {@code value} must be unique within a dictionary type to prevent
 * ambiguous mappings.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see com.github.starhq.template.entity.SysDictType
 * @see TableName
 */
@Data
@Alias("dictData")
@TableName("sys_dict_data")
@EqualsAndHashCode(callSuper = false)
public class SysDictData extends BaseEntity {

    /**
     * The unique identifier of the parent dictionary type.
     * <p>
     * Establishes a hierarchical relationship: {@code DictType 1..* DictData}.
     * This field groups related key-value pairs under a logical category (e.g.,
     * "User Status", "Order Type", "Payment Method").
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code sys_dict_type.id}</li>
     *     <li>Nullability: {@code NOT NULL} — every data entry must belong to a type</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_type_id ON sys_dict_data(type_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all status options for user management
     * SELECT label, value FROM sys_dict_data
     * WHERE type_id = (SELECT id FROM sys_dict_type WHERE type_code = 'user_status')
     * AND status = 1 ORDER BY sort_order;
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysDictType
     */
    private Long typeId;

    /**
     * The human-readable display text presented to end-users in UI components.
     * <p>
     * Examples:
     * <ul>
     *     <li>{@code "Enabled"} / {@code "Disabled"} — For toggle switches</li>
     *     <li>{@code "Pending Review"} — For workflow status badges</li>
     *     <li>{@code "VIP Member"} — For user tier labels</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep labels concise and consistent (≤ 50 characters recommended)</li>
     *     <li>For multi-language systems, store i18n keys (e.g., {@code "dict.status.active"})
     *         and resolve translations at the frontend or gateway layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Uniqueness: Must be unique within the same {@code typeId}</li>
     * </ul>
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
     * <strong>Immutability Warning:</strong>
     * <p>
     * Once referenced by business data, the {@code value} should NEVER be changed.
     * Modifying it will break existing records and cause data inconsistency.
     * If a change is required, create a new entry and migrate data via a scheduled job.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Uniqueness: Must be unique within the same {@code typeId}</li>
     *     <li>Index Recommendation: {@code CREATE UNIQUE INDEX uk_type_value ON sys_dict_data(type_id, value)}</li>
     *     <li>Type Conversion: Parse to {@code Integer}, {@code Enum}, or {@code Boolean} in Service layer</li>
     * </ul>
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
     *     <li>Avoid exposing internal implementation details or sensitive logic</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong>
     * <ul>
     *     <li>Use {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} if detailed documentation or markdown formatting is required</li>
     * </ul>
     */
    private String description;

}