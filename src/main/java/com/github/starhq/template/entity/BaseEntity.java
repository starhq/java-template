package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Base entity class for tracking full audit trail (creator and updater information).
 * <p>
 * This class extends {@link BaseCreatorEntity} and adds standardized fields for
 * recording who last modified a record and when the modification occurred. These
 * fields are automatically populated by MyBatis-Plus meta object handler during
 * {@code UPDATE} operations, complementing the insert-time fields from the parent
 * class to provide complete lifecycle audit capabilities.
 * <p>
 * <strong>Auto-fill Behavior:</strong>
 * <ul>
 *     <li>{@code updatedAt}: Populated with current {@link OffsetDateTime} on every {@code UPDATE}</li>
 *     <li>{@code updatedBy}: Populated with authenticated user ID from security context on every {@code UPDATE}</li>
 *     <li>Inherited fields ({@code createdAt}, {@code createdBy}) remain immutable after initial {@code INSERT}</li>
 * </ul>
 * <p>
 * <strong>Audit Trail Summary:</strong>
 * <pre>
 * {@code
 * INSERT operation:
 *   - createdAt  → auto-filled with current timestamp
 *   - createdBy  → auto-filled with current user ID
 *   - updatedAt  → auto-filled with current timestamp (same as createdAt)
 *   - updatedBy  → auto-filled with current user ID (same as createdBy)
 *
 * UPDATE operation:
 *   - createdAt  → preserved (unchanged)
 *   - createdBy  → preserved (unchanged)
 *   - updatedAt  → auto-updated with current timestamp
 *   - updatedBy  → auto-updated with current user ID
 * }
 * </pre>
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * {@code
 * @TableName("sys_role")
 * public class SysRole extends BaseEntity {
 *     // Additional business fields...
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-24
 * @see BaseCreatorEntity
 * @see FieldFill
 * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseEntity extends BaseCreatorEntity {

    /**
     * The timestamp when this entity was last modified.
     * <p>
     * This field is automatically populated by MyBatis-Plus {@code MetaObjectHandler}
     * during both {@code INSERT} and {@code UPDATE} operations using the current system
     * time with timezone offset. On {@code INSERT}, it typically equals {@link BaseCreatorEntity#getCreatedAt()};
     * on subsequent {@code UPDATE} operations, it reflects the most recent modification time.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link OffsetDateTime} — preserves timezone information for global deployments</li>
     *     <li>Fill Strategy: {@link FieldFill#UPDATE} — populated on both INSERT and UPDATE operations</li>
     *     <li>Immutability: Mutable on UPDATE — reflects latest modification timestamp</li>
     *     <li>Default Value: {@code null} — handler sets value if not explicitly provided</li>
     * </ul>
     * <p>
     * <strong>Query Tips:</strong>
     * <ul>
     *     <li>Find recently modified records: {@code WHERE updated_at >= NOW() - INTERVAL '1 hour'}</li>
     *     <li>Audit change frequency: {@code GROUP BY DATE(updated_at)} for activity reports</li>
     * </ul>
     *
     * @see FieldFill#UPDATE
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#updateFill
     * @see BaseCreatorEntity#getCreatedAt()
     */
    @TableField(fill = FieldFill.UPDATE)
    private OffsetDateTime updatedAt;

    /**
     * The unique identifier of the user who last modified this entity.
     * <p>
     * This field is automatically populated by MyBatis-Plus {@code MetaObjectHandler}
     * during both {@code INSERT} and {@code UPDATE} operations by extracting the
     * authenticated user ID from the security context (e.g., JWT token, session,
     * or {@code SecurityContextUtils}). On {@code INSERT}, it typically equals
     * {@link BaseCreatorEntity#getCreatedBy()}; on subsequent {@code UPDATE} operations, it reflects
     * the most recent modifier.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code SysUser.id} for foreign key integrity</li>
     *     <li>Fill Strategy: {@link FieldFill#UPDATE} — populated on both INSERT and UPDATE operations</li>
     *     <li>Null Handling: May be {@code null} for system-initiated or batch operations</li>
     *     <li>Immutability: Mutable on UPDATE — reflects latest modifier ID</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * This field should NEVER be modifiable via API requests. Always populate it
     * server-side from trusted authentication context to prevent privilege escalation
     * or audit trail tampering. Client-submitted values for this field must be ignored.
     * <p>
     * <strong>Audit Use Cases:</strong>
     * <ul>
     *     <li>Track who modified sensitive configurations</li>
     *     <li>Investigate unauthorized changes via {@code updatedBy} + {@code updatedAt} correlation</li>
     *     <li>Implement data ownership rules based on last modifier</li>
     * </ul>
     *
     * @see FieldFill#UPDATE
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#updateFill
     * @see com.github.starhq.template.common.util.SecurityContextUtils
     * @see BaseCreatorEntity#getCreatedBy()
     */
    @TableField(fill = FieldFill.UPDATE)
    private Long updatedBy;

}