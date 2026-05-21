package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * Base entity class for tracking creator audit information.
 * <p>
 * This class extends {@link BaseIdEntity} and adds standardized fields for
 * recording who created a record and when it was created. These fields are
 * automatically populated by MyBatis-Plus meta object handler during
 * {@code INSERT} operations, ensuring consistent audit trail across all
 * entities that inherit from this base class.
 * <p>
 * <strong>Auto-fill Behavior:</strong>
 * <ul>
 *     <li>{@code createdAt}: Populated with current {@link OffsetDateTime} on insert</li>
 *     <li>{@code createdBy}: Populated with authenticated user ID from security context on insert</li>
 *     <li>Both fields are immutable after creation (no update on {@code UPDATE} operations)</li>
 * </ul>
 * <p>
 * <strong>Usage:</strong>
 * <pre>
 * {@code
 * @TableName("sys_user")
 * public class SysUser extends BaseCreatorEntity {
 *     // Additional fields...
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-24
 * @see BaseIdEntity
 * @see FieldFill
 * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseCreatorEntity extends BaseIdEntity {

    /**
     * The timestamp when this entity was first persisted.
     * <p>
     * This field is automatically populated by MyBatis-Plus {@code MetaObjectHandler}
     * during {@code INSERT} operations using the current system time with timezone offset.
     * The value is immutable after creation and should not be manually set by business logic.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link OffsetDateTime} — preserves timezone information for global deployments</li>
     *     <li>Fill Strategy: {@link FieldFill#INSERT} — only populated on initial persistence</li>
     *     <li>Default Value: {@code null} — handler sets value if not explicitly provided</li>
     * </ul>
     *
     * @see FieldFill#INSERT
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#insertFill
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * The unique identifier of the user who created this entity.
     * <p>
     * This field is automatically populated by MyBatis-Plus {@code MetaObjectHandler}
     * during {@code INSERT} operations by extracting the authenticated user ID from
     * the security context (e.g., JWT token, session, or {@code SecurityContextUtils}).
     * The value is immutable after creation and serves as an audit trail for accountability.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — references {@code SysUser.id} for foreign key integrity</li>
     *     <li>Fill Strategy: {@link FieldFill#INSERT} — only populated on initial persistence</li>
     *     <li>Null Handling: May be {@code null} for system-initiated or anonymous operations</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * This field should never be modifiable via API requests. Always populate it
     * server-side from trusted authentication context to prevent privilege escalation.
     *
     * @see FieldFill#INSERT
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#insertFill
     * @see com.github.starhq.template.common.util.SecurityContextUtils
     */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

}