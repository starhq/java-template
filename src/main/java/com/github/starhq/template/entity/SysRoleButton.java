package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Junction entity representing the many-to-many relationship between roles and button permissions.
 * <p>
 * This class maps to the {@code sys_role_button} table and serves as a bridge table
 * linking {@link SysRole} and {@link SysButton} entities. Each record grants a specific
 * button-level permission to a role, enabling fine-grained UI control (show/hide buttons)
 * and API endpoint access based on user role assignments.
 * <p>
 * <strong>Relationship Model:</strong>
 * <ul>
 *     <li>{@code SysRole 1..* SysRoleButton *..1 SysButton}</li>
 *     <li>Composite primary key: {@code (role_id, button_id)} ensures uniqueness and prevents duplicate grants</li>
 *     <li>Deletion is cascade-safe: removing a role or button automatically cleans up associated mappings</li>
 * </ul>
 * <p>
 * <strong>Batch Operations & Caching:</strong>
 * <p>
 * Role-button assignments are typically updated in bulk (e.g., admin configures all permissions
 * for a role at once). Always use batch insert/update methods to minimize database round-trips.
 * Cache the role → button mapping in Redis/Caffeine keyed by {@code roleId} to achieve O(1)
 * permission checks during UI rendering or API authorization.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see SysRole
 * @see SysButton
 * @see TableName
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_button")
public class SysRoleButton {

    /**
     * The unique identifier of the role being granted permissions.
     * <p>
     * References {@link SysRole#getId()} to establish the "who" side of the permission grant.
     * Combined with {@link #buttonId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid role</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_role_button (role_id, button_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all button IDs granted to a specific role
     * SELECT button_id FROM sys_role_button WHERE role_id = 1001;
     *
     * -- Check if a role has a specific button permission
     * SELECT COUNT(*) FROM sys_role_button WHERE role_id = 1001 AND button_id = 2002;
     * }
     * </pre>
     *
     * @see SysRole
     */
    private Long roleId;

    /**
     * The unique identifier of the button permission being granted.
     * <p>
     * References {@link SysButton#getId()} to establish the "what" side of the permission grant.
     * Combined with {@link #roleId}, forms the composite primary key for this junction table.
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Long} — consistent with primary key strategy across entities</li>
     *     <li>Nullability: {@code NOT NULL} — every mapping must reference a valid button</li>
     *     <li>Index Recommendation: Part of composite unique key {@code UNIQUE KEY uk_role_button (role_id, button_id)}</li>
     * </ul>
     * <p>
     * <strong>Query Pattern:</strong>
     * <pre>
     * {@code
     * -- Fetch all roles that have access to a specific button
     * SELECT role_id FROM sys_role_button WHERE button_id = 2002;
     *
     * -- Bulk check: which buttons does a role have?
     * SELECT b.code, b.name FROM sys_button b
     * JOIN sys_role_button rb ON b.id = rb.button_id
     * WHERE rb.role_id = 1001;
     * }
     * </pre>
     *
     * @see SysButton
     */
    private Long buttonId;

}