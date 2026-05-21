package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing a dictionary type/category that groups related key-value pairs.
 * <p>
 * This class maps to the {@code sys_dict_type} table and extends {@link BaseEntity}
 * to provide full audit trail. It serves as a logical container for {@link SysDictData}
 * entries, enabling dynamic management of system enums, status codes, and configuration
 * options without code deployments or database schema changes.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Data Grouping</strong>: Categorizing related dictionary entries (e.g., "User Status", "Order Type")</li>
 *     <li><strong>Code Reference</strong>: The {@code type} field acts as a unique technical key used by backend/frontend to fetch associated data</li>
 *     <li><strong>Dynamic Configuration</strong>: Enabling admin users to modify system options without restarting applications</li>
 * </ul>
 * <p>
 * <strong>Caching & Performance:</strong>
 * <p>
 * Dictionary types are queried frequently to load associated data. Implement application-level
 * caching (Redis/Caffeine) keyed by {@code type} to minimize database hits. The {@code type}
 * field must be globally unique to ensure deterministic data retrieval and cache consistency.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see com.github.starhq.template.entity.SysDictData
 * @see TableName
 */
@Data
@Alias("dictType")
@TableName("sys_dict_type")
@EqualsAndHashCode(callSuper = false)
public class SysDictType extends BaseEntity {

    /**
     * The unique technical identifier/code for this dictionary category.
     * <p>
     * This field serves as the primary key for business logic to fetch associated
     * dictionary data. Typically follows {@code snake_case} conventions
     * (e.g., {@code user_status}, {@code order_type}, {@code payment_method}).
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link String} — alphanumeric with underscores recommended</li>
     *     <li>Uniqueness: Must be globally unique across all dictionary types</li>
     *     <li>Index Recommendation: {@code CREATE UNIQUE INDEX uk_dict_type ON sys_dict_type(type)}</li>
     *     <li>Nullability: {@code NOT NULL} — required for type identification</li>
     * </ul>
     * <p>
     * <strong>Usage Pattern:</strong>
     * <pre>
     * {@code
     * // Backend service
     * List<DictDataVO> statuses = dictDataService.getByTypeCode("user_status");
     *
     * // Frontend API request
     * GET /api/v1/dict-types/user_status/data
     * }
     * </pre>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[a-z][a-z0-9_]*$")} for standardized format</li>
     *     <li>Length: {@code @Size(min = 3, max = 64)}</li>
     * </ul>
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
     *     <li>Keep names concise and descriptive (≤ 50 characters recommended)</li>
     *     <li>For multi-language systems, consider storing i18n keys or providing
     *         separate localization tables if dynamic translation is required</li>
     *     <li>Avoid technical jargon; target non-technical system administrators</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank}</li>
     *     <li>Uniqueness: Recommended to be unique to avoid admin UI confusion</li>
     *     <li>Pattern: Safe characters only to prevent XSS or rendering issues</li>
     * </ul>
     */
    private String name;

    /**
     * Optional explanatory text detailing the business context, usage rules,
     * or maintenance notes for this dictionary type.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what data this type manages</li>
     *     <li>Documenting dependencies (e.g., {@code "Referenced by user.profile.status"})</li>
     *     <li>Recording deprecation notices or migration plans for legacy types</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     *     <li>Avoid exposing internal implementation details or sensitive logic</li>
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