package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Junction entity representing the many-to-many relationship between users and roles in the RBAC system.
 * <p>
 * This class maps to the {@code sys_user_role} table and serves as a bridge table
 * linking {@link SysUser} and {@link SysRole} entities. Each record assigns a specific
 * role to a user, enabling flexible permission inheritance: users gain access to menus,
 * buttons, and API resources through their assigned roles without direct permission grants.
 * <p>
 * <strong>Relationship Model:</strong>
 * <ul>
 *     <li>{@code SysUser 1..* SysUserRole *..1 SysRole}</li>
 *     <li>Composite primary key: {@code (user_id, role_id)} ensures uniqueness and prevents duplicate assignments</li>
 *     <li>Deletion is cascade-safe: removing a user or role automatically cleans up associated mappings</li>
 * </ul>
 * <p>
 * <strong>Batch Operations & Caching:</strong>
 * <p>
 * User-role assignments are typically updated in bulk during user provisioning or role reconfiguration.
 * Always use batch insert/update methods to minimize database round-trips. Cache the user → role
 * mapping in Redis/Caffeine keyed by {@code userId} to achieve O(1) permission resolution during
 * authentication and authorization checks.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see SysUser
 * @see SysRole
 * @see com.baomidou.mybatisplus.annotation.TableName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
public class SysUserRole {

    /**
     * The unique identifier of the user being assigned roles.
     * <p>
     * References {@link SysUser#getId()} to establish the "who" side of the role assignment.
     * Combined with {@link #roleId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid user</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_user_role (user_id, role_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all role IDs assigned to a specific user
     * SELECT role_id FROM sys_user_role WHERE user_id = 1001;
     *
     * -- Check if a user has a specific role
     * SELECT COUNT(*) FROM sys_user_role WHERE user_id = 1001 AND role_id = 2002;
     *
     * -- Build user permission matrix via role JOINs
     * SELECT r.code, r.name FROM sys_role r
     * JOIN sys_user_role ur ON r.id = ur.role_id
     * WHERE ur.user_id = 1001;
     * }
     * </pre>
     *
     * @see SysUser
     */
    private Long userId;

    /**
     * The unique identifier of the role being assigned to the user.
     * <p>
     * References {@link SysRole#getId()} to establish the "what" side of the role assignment.
     * Combined with {@link #userId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid role</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_user_role (user_id, role_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all users that have a specific role
     * SELECT user_id FROM sys_user_role WHERE role_id = 2002;
     *
     * -- Bulk role assignment audit: which users were granted a role?
     * SELECT u.username, u.email FROM sys_user u
     * JOIN sys_user_role ur ON u.id = ur.user_id
     * WHERE ur.role_id = 2002 AND ur.created_at >= '2026-01-01';
     * }
     * </pre>
     *
     * @see SysRole
     */
    private Long roleId;

}