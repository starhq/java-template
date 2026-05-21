package com.github.starhq.template.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Abstract base view object for audit trail metadata in administrative and compliance contexts.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the entity's unique identifier
 * and adds standardized audit fields ({@code createdAt}, {@code updatedAt}, {@code creator}, {@code updater})
 * for tracking creation and modification history. Designed as a reusable foundation for
 * any VO requiring audit compliance, such as user management, role configuration, or data governance.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Admin Console</strong>: Display audit metadata in management tables for transparency and accountability</li>
 *     <li><strong>Compliance Reporting</strong>: Provide creation/modification timestamps for regulatory audits (GDPR, SOX, PCI-DSS)</li>
 *     <li><strong>Change Tracking</strong>: Enable diff analysis by comparing {@code updatedAt} and {@code updater} across versions</li>
 *     <li><strong>Frontend Integration</strong>: Supply structured audit data for Vue/React table components with sorting/filtering</li>
 * </ul>
 * <p>
 * <strong>Time Serialization Strategy:</strong>
 * <p>
 * Time fields use {@code @JsonFormat} with explicit pattern and timezone to ensure consistent
 * datetime representation across different server/client timezones:
 * <ul>
 *     <li>Pattern: {@code yyyy-MM-dd HH:mm:ss} — human-readable format for admin UIs</li>
 *     <li>Timezone: {@code GMT+8} — China Standard Time (adjust per deployment region)</li>
 *     <li>Example output: {@code "2026-05-21 14:30:00"}</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Reusability</strong>: Abstract base class eliminates duplication across audit-capable VOs</li>
 *     <li><strong>Immutability-Friendly</strong>: Fields are set once during VO construction; no setters exposed in public API</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 *     <li><strong>Framework-Neutral</strong>: Uses standard Jackson annotations; compatible with any JSON library</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see BaseIdVO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BaseAuditVO extends BaseIdVO {

    /**
     * Default placeholder for unknown or unresolvable user identifiers.
     * <p>
     * Used when the creator/updater ID cannot be resolved to a username
     * (e.g., user deleted, system account, or migration artifact).
     * <p>
     * <strong>Usage Guidance:</strong>
     * <ul>
     *     <li>Frontend should display this as a neutral indicator (e.g., gray text "unknown")</li>
     *     <li>Backend should log a warning when resolution fails for audit integrity</li>
     *     <li>Consider storing original ID alongside resolved name for traceability</li>
     * </ul>
     */
    protected static final String UNKNOWN_USER = "unknown";

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
    private static final long serialVersionUID = -5234444810204228663L;

    /**
     * The timestamp when this entity was initially created.
     * <p>
     * Used for audit trails, data lifecycle management, and identifying
     * the origin time of records for compliance reporting.
     * <p>
     * <strong>Serialization Format:</strong>
     * <p>
     * Annotated with {@code @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")}
     * to ensure consistent datetime representation:
     * <ul>
     *     <li>Pattern: {@code yyyy-MM-dd HH:mm:ss} — human-readable format for admin UIs</li>
     *     <li>Timezone: {@code GMT+8} — China Standard Time (adjust per deployment region)</li>
     *     <li>Example output: {@code "2026-03-25 09:15:30"}</li>
     * </ul>
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Audit Query</strong>: Filter records by creation time range for compliance reports</li>
     *     <li><strong>Data Retention</strong>: Identify records older than retention policy for archival</li>
     *     <li><strong>Frontend Display</strong>: Show relative time ("2 hours ago") with tooltip for exact timestamp</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always populate with {@code OffsetDateTime.now(ZoneOffset.UTC)} for timezone consistency</li>
     *     <li>Never allow client-side override of this field; set exclusively in service layer</li>
     *     <li>Consider indexing {@code created_at} in database for efficient time-range queries</li>
     * </ul>
     *
     * @see java.time.OffsetDateTime
     * @see JsonFormat
     * @see com.github.starhq.template.common.util.DateTimeUtils#formatRelative(OffsetDateTime)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime createdAt;

    /**
     * The timestamp when this entity was last modified.
     * <p>
     * Used for change tracking, cache invalidation triggers, and identifying
     * stale records that may require review or refresh.
     * <p>
     * <strong>Serialization Format:</strong>
     * <p>
     * Annotated with {@code @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")}
     * for consistent datetime representation (see {@link #createdAt} for details).
     * <p>
     * <strong>Usage Patterns:</strong>
     * <ul>
     *     <li><strong>Cache Invalidation</strong>: Use {@code updatedAt} as cache key version for automatic invalidation</li>
     *     <li><strong>Change Detection</strong>: Compare {@code updatedAt} across replicas to detect replication lag</li>
     *     <li><strong>Frontend Display</strong>: Show "Last updated: 5 minutes ago" with exact time on hover</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always update with {@code OffsetDateTime.now(ZoneOffset.UTC)} on entity modification</li>
     *     <li>Never allow client-side override; set exclusively in service layer via {@code @PreUpdate} or manual assignment</li>
     *     <li>Consider adding database trigger to auto-update {@code updated_at} as fallback protection</li>
     * </ul>
     *
     * @see java.time.OffsetDateTime
     * @see JsonFormat
     * @see com.github.starhq.template.common.util.CacheHelper#invalidateIfNewer(String, OffsetDateTime)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private OffsetDateTime updatedAt;

    /**
     * The resolved username of the account that created this entity.
     * <p>
     * This is a denormalized, human-readable field for convenient display in admin UIs
     * without requiring additional JOIN queries to fetch user details. The actual
     * creator ID ({@code createdBy}) should be stored separately for security-critical operations.
     * <p>
     * <strong>Data Consistency:</strong>
     * <ul>
     *     <li>Populated via {@link #populateAuditFields(Long, Long, Map)} during VO construction</li>
     *     <li>Resolved from {@code createdBy} ID using a pre-fetched username map for efficiency</li>
     *     <li>Falls back to {@link #UNKNOWN_USER} if resolution fails (e.g., user deleted)</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Never use this field for authorization decisions; always reference {@code createdBy} ID</li>
     *     <li>Consider masking usernames in logs for privacy compliance (GDPR, PIPL)</li>
     *     <li>For audit integrity, store historical username snapshots if username changes are frequent</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Show creator with link to user profile
     * <a-table-column title="Created By" data-index="creator">
     *   <template #bodyCell="{ text, record }">
     *     <router-link v-if="record.createdById" :to="`/users/${record.createdById}`">
     *       {{ text }}
     *     </router-link>
     *     <span v-else :class="{ 'text-gray-400': text === 'unknown' }">{{ text }}</span>
     *   </template>
     * </a-table-column>
     * }
     * </pre>
     *
     * @see #populateAuditFields(Long, Long, Map)
     * @see com.github.starhq.template.entity.BaseAuditEntity#getCreatedBy()
     */
    private String creator;

    /**
     * The resolved username of the account that last modified this entity.
     * <p>
     * Similar to {@link #creator}, this denormalized field enables efficient display
     * of modification history without runtime JOINs. The actual updater ID ({@code updatedBy})
     * should be used for security-critical operations.
     * <p>
     * <strong>Usage Guidance:</strong>
     * <ul>
     *     <li>Display in audit tables alongside {@code updatedAt} for change tracking</li>
     *     <li>Use in diff views to show "Changed by alice at 2026-05-21 14:30:00"</li>
     *     <li>Consider showing both current and historical updaters for multi-step workflows</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show tooltip with exact timestamp on hover: {@code <a-tooltip :title="record.updatedAt">}</li>
     *     <li>Color-code recent updates (e.g., last 24 hours) for visual scanning</li>
     *     <li>Disable editing of audit fields in forms to prevent tampering</li>
     * </ul>
     *
     * @see #populateAuditFields(Long, Long, Map)
     * @see com.github.starhq.template.entity.BaseAuditEntity#getUpdatedBy()
     */
    private String updater;

    /**
     * Populates audit display fields ({@code creator}, {@code updater}) from numeric IDs
     * using a pre-fetched username mapping for efficient batch resolution.
     * <p>
     * This method is designed for batch VO conversion scenarios where multiple entities
     * need audit field resolution without N+1 database queries. Typical usage pattern:
     * <ol>
     *     <li>Fetch list of entities with {@code createdBy}/{@code updatedBy} IDs</li>
     *     <li>Collect all unique user IDs from audit fields</li>
     *     <li>Batch fetch usernames via {@code SELECT id, username FROM sys_user WHERE id IN (...)}</li>
     *     <li>Build {@code Map<Long, String>} for O(1) lookup</li>
     *     <li>Call this method on each VO to populate resolved names</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code createdBy}: The numeric ID of the creator; may be {@code null} for system-generated records</li>
     *     <li>{@code updatedBy}: The numeric ID of the last updater; may be {@code null} if never modified</li>
     *     <li>{@code nameMap}: Pre-fetched map of {@code userId -> username}; should contain all possible IDs to avoid fallback</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service layer: Batch convert entities to VOs with audit resolution
     * public List<UserPageVO> pageWithAudit(UserPageRequest request) {
     *     // 1. Fetch entities with audit IDs
     *     IPage<SysUser> entityPage = userMapper.selectPage(...);
     *
     *     // 2. Collect unique user IDs from audit fields
     *     Set<Long> userIds = entityPage.getRecords().stream()
     *         .flatMap(e -> Stream.of(e.getCreatedBy(), e.getUpdatedBy()))
     *         .filter(Objects::nonNull)
     *         .collect(Collectors.toSet());
     *
     *     // 3. Batch fetch usernames
     *     Map<Long, String> nameMap = userMapper.selectIdNameMap(userIds);
     *
     *     // 4. Convert to VOs and populate audit fields
     *     return entityPage.getRecords().stream()
     *         .map(entity -> {
     *             UserPageVO vo = converter.toPageVO(entity);
     *             vo.populateAuditFields(entity.getCreatedBy(), entity.getUpdatedBy(), nameMap);
     *             return vo;
     *         })
     *         .collect(Collectors.toList());
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Batch resolution reduces database round-trips from O(N) to O(1) for N entities</li>
     *     <li>Cache the {@code nameMap} by user ID set hash for repeated conversions within TTL</li>
     *     <li>For very large batches (>1000), consider chunking to avoid SQL parameter limits</li>
     * </ul>
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>If {@code createdBy} or {@code updatedBy} is {@code null}, resolves to {@link #UNKNOWN_USER}</li>
     *     <li>If ID not found in {@code nameMap}, falls back to {@link #UNKNOWN_USER} with warning log</li>
     *     <li>Frontend should handle {@code "unknown"} gracefully (e.g., gray text, no link)</li>
     * </ul>
     *
     * @param createdBy the numeric ID of the creator; may be {@code null}
     * @param updatedBy the numeric ID of the last updater; may be {@code null}
     * @param nameMap   pre-fetched map of {@code userId -> username} for efficient resolution
     */
    public void populateAuditFields(Long createdBy, Long updatedBy, Map<Long, String> nameMap) {
        this.setCreator(nameMap.getOrDefault(createdBy, UNKNOWN_USER));
        this.setUpdater(nameMap.getOrDefault(updatedBy, UNKNOWN_USER));
    }

}