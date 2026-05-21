package com.github.starhq.template.model.vo;

import lombok.Data;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.io.Serial;
import java.io.Serializable;

/**
 * Abstract base view object providing a unique identifier for entity representation.
 * <p>
 * This class serves as the foundational building block for all view objects (VOs)
 * in the application, ensuring consistent ID handling across different domain models.
 * By centralizing ID serialization logic, it prevents JavaScript precision loss issues
 * and enables reusable patterns for caching, API responses, and frontend integration.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>VO Inheritance</strong>: Base class for all domain-specific VOs (UserVO, RoleVO, MenuVO, etc.)</li>
 *     <li><strong>API Responses</strong>: Standardized ID format in JSON responses for frontend consumption</li>
 *     <li><strong>Cache Keys</strong>: Reliable string-based IDs for Redis/Caffeine cache key construction</li>
 *     <li><strong>Frontend Integration</strong>: Safe numeric IDs for Vue/React component keys and routing</li>
 * </ul>
 * <p>
 * <strong>Serialization Strategy:</strong>
 * <p>
 * The {@code id} field uses {@code @JsonSerialize(using = ToStringSerializer.class)}
 * to convert {@link Long} to {@code String} in JSON output. This prevents precision loss
 * when consuming APIs from JavaScript/TypeScript clients, which represent integers as
 * 64-bit floats with only 53 bits of precision (MAX_SAFE_INTEGER = 9007199254740991).
 * <pre>
 * {@code
 * // Without ToStringSerializer:
 * { "id": 9007199254740993 }  // May be truncated to 9007199254740992 in JS
 *
 * // With ToStringSerializer:
 * { "id": "9007199254740993" }  // Safe string representation, no precision loss
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes the essential {@code id} field; no business logic</li>
 *     <li><strong>Reusability</strong>: Abstract base eliminates duplication across all VO classes</li>
 *     <li><strong>Serialization-Ready</strong>: Fixed {@code serialVersionUID} ensures cross-version compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: Uses standard Jackson annotations; compatible with any JSON library</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see java.io.Serializable
 * @see ToStringSerializer
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
 */
@Data
public class BaseIdVO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., via RPC or messaging).
     * Update this value only if the class structure changes in a
     * backward-incompatible way (e.g., removing fields, changing types).
     * <p>
     * <strong>Update Guidance:</strong>
     * <ul>
     *     <li>Add field: No change needed (backward compatible)</li>
     *     <li>Remove field: Increment this value (breaks old deserialization)</li>
     *     <li>Change field type: Increment this value (breaks old deserialization)</li>
     * </ul>
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 7028930656191228294L;

    /**
     * The unique identifier of the entity represented by this view object.
     * <p>
     * This field serves as the primary key for:
     * <ul>
     *     <li>Database record identification and foreign key relationships</li>
     *     <li>API request/response payload for CRUD operations</li>
     *     <li>Cache key construction for efficient data retrieval</li>
     *     <li>Frontend component keys for Vue/React list rendering</li>
     * </ul>
     * <p>
     * <strong>Serialization Strategy:</strong>
     * <p>
     * Annotated with {@code @JsonSerialize(using = ToStringSerializer.class)} to
     * convert the {@link Long} value to a {@code String} in JSON output. This prevents
     * precision loss when the API is consumed by JavaScript/TypeScript clients, which
     * represent integers as 64-bit floats with only 53 bits of precision.
     * <pre>
     * {@code
     * // Example: Large ID beyond JS MAX_SAFE_INTEGER
     * Long id = 9007199254740993L;
     *
     * // JSON output with ToStringSerializer:
     * { "id": "9007199254740993" }
     *
     * // Frontend: Parse back to BigInt if needed
     * const id = BigInt(response.id); // Safe handling of large integers
     * }
     * </pre>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — auto-generated primary key from database sequence</li>
     *     <li>Uniqueness: Globally unique within the entity type scope</li>
     *     <li>Nullability: Should not be {@code null} for persisted entities; may be {@code null} for transient/new objects</li>
     *     <li>Generation: Typically auto-increment (MySQL) or Snowflake (distributed systems)</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Return VO with ID for frontend
     * public UserSimpleVO getUserById(Long id) {
     *     SysUser entity = userMapper.selectById(id);
     *     return converter.toSimpleVO(entity); // ID auto-serialized as string
     * }
     *
     * // Frontend: Use ID for API calls and component keys
     * <a-list
     *   :data-source="users"
     *   :row-key="user => user.id" <!-- String ID works reliably -->
     * >
     *   <template #item="{ item }">
     *     <a-button @click="viewUser(item.id)">View</a-button>
     *   </template>
     * </a-list>
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always use {@code id} for entity identification; avoid business keys (username, code) for internal references</li>
     *     <li>For cache keys, prefix with entity type: {@code "user:" + id} to avoid collisions across domains</li>
     *     <li>When comparing IDs in frontend, use string comparison or BigInt to avoid precision issues</li>
     *     <li>Document ID generation strategy (Snowflake, UUID, etc.) in system architecture docs</li>
     * </ul>
     *
     * @see ToStringSerializer
     * @see <a href="https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER">JavaScript Number.MAX_SAFE_INTEGER</a>
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

}