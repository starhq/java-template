package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Junction entity representing the many-to-many relationship between roles and API resource permissions.
 * <p>
 * This class maps to the {@code sys_role_resource} table and serves as a bridge table
 * linking {@link SysRole} and {@link SysResource} entities. Each record grants a specific
 * API endpoint/method permission to a role, enabling fine-grained backend access control
 * and URL-based authorization evaluation by security interceptors or Spring Security filters.
 * <p>
 * <strong>Relationship Model:</strong>
 * <ul>
 *     <li>{@code SysRole 1..* SysRoleResource *..1 SysResource}</li>
 *     <li>Composite primary key: {@code (role_id, resource_id)} ensures uniqueness and prevents duplicate grants</li>
 *     <li>Deletion is cascade-safe: removing a role or resource automatically cleans up associated mappings</li>
 * </ul>
 * <p>
 * <strong>Batch Operations & Caching:</strong>
 * <p>
 * Role-resource assignments are typically updated in bulk during admin configuration. Always use
 * batch insert/update methods to minimize database round-trips. Cache the role → resource mapping
 * in Redis/Caffeine keyed by {@code roleId} to achieve O(1) permission checks during API gateway
 * or security filter evaluation.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see SysRole
 * @see SysResource
 * @see com.baomidou.mybatisplus.annotation.TableName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_resource")
public class SysRoleResource {

    /**
     * The unique identifier of the role being granted API resource access.
     * <p>
     * References {@link SysRole#getId()} to establish the "who" side of the permission grant.
     * Combined with {@link #resourceId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid role</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_role_resource (role_id, resource_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all resource IDs granted to a specific role
     * SELECT resource_id FROM sys_role_resource WHERE role_id = 1001;
     *
     * -- Check if a role has access to a specific API endpoint
     * SELECT COUNT(*) FROM sys_role_resource WHERE role_id = 1001 AND resource_id = 2002;
     * }
     * </pre>
     *
     * @see SysRole
     */
    private Long roleId;

    /**
     * The unique identifier of the API resource being granted to the role.
     * <p>
     * References {@link SysResource#getId()} to establish the "what" side of the permission grant.
     * Combined with {@link #roleId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid API resource</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_role_resource (role_id, resource_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all roles that can access a specific API endpoint
     * SELECT role_id FROM sys_role_resource WHERE resource_id = 2002;
     *
     * -- Build role-based API permission matrix for security filter
     * SELECT r.url, r.methods FROM sys_resource r
     * JOIN sys_role_resource rr ON r.id = rr.resource_id
     * WHERE rr.role_id = 1001;
     * }
     * </pre>
     *
     * @see SysResource
     */
    private Long resourceId;

}