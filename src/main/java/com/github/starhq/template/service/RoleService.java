package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.dto.RoleDTO;
import com.github.starhq.template.model.vo.RoleCheckVO;
import com.github.starhq.template.model.vo.RolePageVO;
import com.github.starhq.template.model.vo.RoleSimpleVO;

import java.io.Serializable;
import java.util.List;

/**
 * Service interface for role management with CRUD operations and permission integration.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysRole} entities, while adding business-level methods for paginated queries,
 * lightweight metadata retrieval, user-role assignment checks, and role lifecycle management.
 * Designed to centralize role logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role Management</strong>: CRUD operations for defining roles in admin console</li>
 *     <li><strong>Permission Assignment</strong>: Link roles to menus/resources for fine-grained access control</li>
 *     <li><strong>User Authorization</strong>: Query roles assigned to a user for dynamic permission resolution</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured role metadata for dropdowns and RBAC UI rendering</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service layer handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Type Safety</strong>: Use typed DTOs/VOs instead of generic maps for compile-time validation</li>
 *     <li><strong>Cache-Aware</strong>: Frequent read operations should leverage caching for performance</li>
 *     <li><strong>Access-Controlled</strong>: All write operations should enforce role-based permissions (ADMIN-only)</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysRole
 * @see PageRequest
 * @see RolePageVO
 * @see RoleCheckVO
 * @see RoleSimpleVO
 * @see RoleDTO
 */
public interface RoleService extends IService<SysRole> {

    /**
     * Retrieves a paginated list of role definitions matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient role management:
     * <ul>
     *     <li><strong>Code Filter</strong>: Exact match on role code (e.g., {@code "admin"}) for precise lookups</li>
     *     <li><strong>Name Filter</strong>: Fuzzy search on role name for admin convenience</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for bulk operations</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageInfo.getCode()}: Optional; performs exact match on role code</li>
     *     <li>{@code pageInfo.getName()}: Optional; performs right-fuzzy match on role name</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysRole} entity is converted to {@link RolePageVO} with audit fields</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code page()} calls for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_role_code_name ON sys_role(code, name);
     *         CREATE INDEX idx_role_status ON sys_role(status);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     * </ul>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link RolePageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException if {@code pageInfo} is {@code null}
     * @see PageRequest
     * @see RolePageVO
     * @see IPage
     */
    IPage<RolePageVO> page(PageRequest pageInfo);

    /**
     * Retrieves simplified role metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full role queries when only
     * basic identification fields ({@code id}, {@code name}, {@code code}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the role to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link RoleSimpleVO} with {@code id}, {@code name}, {@code code}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no role exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes audit metadata for minimal payload</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to high read frequency and low write frequency.
     * Recommended configuration:
     * <pre>
     * {@code
     * @Cacheable(value = "roles", key = "#id", unless = "#result == null")
     * public RoleSimpleVO getRoleById(Serializable id) { ... }
     * }
     * </pre>
     *
     * @param id the primary key of the role to retrieve; must not be {@code null}
     * @return {@link RoleSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see RoleSimpleVO
     */
    RoleSimpleVO getRoleById(Serializable id);

    /**
     * Retrieves a list of roles with checked state for user-based role assignment.
     * <p>
     * This method supports the "assign roles to user" workflow in admin consoles by:
     * <ol>
     *     <li>Fetching all available roles (typically filtered by status=enabled)</li>
     *     <li>Joining with user-role assignments to determine {@code checked} state</li>
     *     <li>Returning {@link RoleCheckVO} with {@code id}, {@code name}, {@code code}, {@code checked} fields</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userId}: Must not be {@code null}; the primary key of the user to configure</li>
     *     <li>Checked state logic: {@code checked = true} if role is assigned to user; {@code false} otherwise</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns list of {@link RoleCheckVO} with pre-computed {@code checked} flags</li>
     *     <li><strong>Empty Result</strong>: Returns empty list if no roles exist or user has no assignments</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <pre>
     * {@code
     * // Vue 3: Checkbox group for user role assignment
     * <a-checkbox-group v-model="checkedRoleCodes">
     *   <a-checkbox
     *     v-for="role in roles"
     *     :key="role.id"
     *     :value="role.code"
     *     :checked="role.checked"
     *   >
     *     {{ role.name }} <small class="text-gray-500">({{ role.code }})</small>
     *   </a-checkbox>
     * </a-checkbox-group>
     * }
     * </pre>
     *
     * @param userId the primary key of the user to configure; must not be {@code null}
     * @return list of {@link RoleCheckVO} with computed {@code checked} state; never {@code null}
     * @throws IllegalArgumentException if {@code userId} is {@code null}
     * @see RoleCheckVO
     */
    List<RoleCheckVO> selectCheckedRoles(Serializable userId);

    /**
     * Updates an existing role definition with validation and conflict prevention.
     * <p>
     * This method handles the complete role update workflow including:
     * <ul>
     *     <li>Existence check (ensure role with given {@code id} exists)</li>
     *     <li>Code uniqueness validation (if {@code code} is being changed)</li>
     *     <li>Field-level updates (name, code, description, sort, status, etc.)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected role metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the role to update</li>
     *     <li>{@code roleDto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code roleDto.getCode()}: If changed, must be unique across all roles (excluding current entry)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if role was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no role exists with given {@code id}</li>
     *     <li><strong>Duplicate Code</strong>: Returns {@code false} or throws exception if new {@code code} conflicts</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     * </ul>
     *
     * @param id      the primary key of the role to update; must not be {@code null}
     * @param roleDto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if role was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException if {@code id} or {@code roleDto} is {@code null}
     * @throws BusinessException        if new {@code code} conflicts with existing role
     * @see RoleDTO
     */
    boolean updateRole(Serializable id, RoleDTO roleDto);

    /**
     * Creates a new role definition with validation and duplicate prevention.
     * <p>
     * This method handles the complete role creation workflow including:
     * <ul>
     *     <li>Input validation (code uniqueness, name format, description length)</li>
     *     <li>Role code format enforcement (e.g., {@code snake_case} or {@code camelCase})</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code roleDto}: Must not be {@code null}; should include {@code name}, {@code code}, {@code description}</li>
     *     <li>{@code roleDto.getCode()}: Must be unique across all roles; format: {@code [a-z][a-z0-9_]*}</li>
     *     <li>{@code roleDto.getName()}: Human-readable name for admin UI; should be concise (≤ 50 chars)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if role was successfully created</li>
     *     <li><strong>Duplicate Code</strong>: Returns {@code false} or throws exception if {@code code} already exists</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Role creation either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Code Validation</strong>: Enforce format to prevent injection or privilege escalation</li>
     *     <li><strong>Audit Logging</strong>: Log role creation events for compliance tracking</li>
     * </ul>
     *
     * @param roleDto the role creation data; must not be {@code null}
     * @return {@code true} if role was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException if {@code roleDto} is {@code null} or missing required fields
     * @throws BusinessException        if role code already exists or validation fails
     * @see RoleDTO
     */
    boolean createRole(RoleDTO roleDto);

}