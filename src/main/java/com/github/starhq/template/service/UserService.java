package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;

import java.io.Serializable;

/**
 * Service interface for user management with CRUD operations, pagination, and security integration.
 * <p>
 * This interface extends {@link IService} to provide standardized MyBatis-Plus operations
 * for {@link SysUser} entities, while adding business-level methods for paginated queries,
 * lightweight metadata retrieval, and user lifecycle management. Designed to centralize
 * user logic with consistent validation, caching, and audit trail support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>User Management</strong>: CRUD operations for defining user accounts in admin console</li>
 *     <li><strong>Authentication Support</strong>: Provide user details for login and permission resolution</li>
 *     <li><strong>Profile Management</strong>: Allow users to update their own profile information</li>
 *     <li><strong>Admin Audit</strong>: Paginated user queries for security monitoring and compliance</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Service layer handles business logic; controllers handle HTTP concerns</li>
 *     <li><strong>Type Safety</strong>: Use typed DTOs/VOs instead of generic maps for compile-time validation</li>
 *     <li><strong>Privacy-Aware</strong>: Sensitive fields (passwords, PII) must be masked or excluded in VOs</li>
 *     <li><strong>Access-Controlled</strong>: All write operations should enforce role-based permissions (ADMIN or Self)</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see IService
 * @see SysUser
 * @see KeyWordPageRequest
 * @see UserPageVO
 * @see UserSimpleVO
 * @see UserDTO
 */
public interface UserService extends IService<SysUser> {

    /**
     * Retrieves a paginated list of user definitions matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient user management:
     * <ul>
     *     <li><strong>Keyword Filter</strong>: Fuzzy search on username, nickname, or email for admin convenience</li>
     *     <li><strong>Status Filter</strong>: Filter by enabled/disabled status for bulk operations</li>
     *     <li><strong>Role Filter</strong>: Filter by assigned roles for targeted administration</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageRequest}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageRequest.getKeyword()}: Optional; performs right-fuzzy match on username/nickname/email</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>VO Conversion</strong>: Each {@link SysUser} entity is converted to {@link UserPageVO} with audit fields</li>
     *     <li><strong>Data Masking</strong>: Passwords and sensitive PII are excluded or masked in returned VOs</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code page()} calls for compliance tracking</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration attacks on user data</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_user_username_status ON sys_user(username, status);
     *         CREATE INDEX idx_user_email ON sys_user(email);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Query Optimization</strong>: Avoid SELECT *; fetch only required fields for pagination</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Admin endpoint for user audit
     * @GetMapping("/users")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<IPage<UserPageVO>> listUsers(KeyWordPageRequest request) {
     *     IPage<UserPageVO> page = userService.page(request);
     *     return Result.success(page.getRecords(), page.getTotal());
     * }
     *
     * // Frontend: Admin console user table with filters
     * const UserTable = () => {
     *   const [filters, setFilters] = useState({ keyword: '', status: null });
     *   const { data: userPage, loading } = useRequest(() =>
     *     api.listUsers({ ...filters, page: 1, size: 20 })
     *   );
     *
     *   return (
     *     <a-table :data-source="userPage.records" :loading={loading}>
     *       <a-table-column title="Username" dataIndex="username" />
     *       <a-table-column title="Nickname" dataIndex="nickname" />
     *       <a-table-column title="Email" dataIndex="email" />
     *       <a-table-column title="Status" dataIndex="status">
     *         <template #bodyCell="{ text }">
     *           <a-tag :color="getStatusColor(text)">{text}</a-tag>
     *         </template>
     *       </a-table-column>
     *     </a-table>
     *   );
     * };
     * }
     * </pre>
     *
     * @param pageRequest the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link UserPageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException                                  if {@code pageRequest} is {@code null}
     * @throws org.springframework.security.access.AccessDeniedException if caller lacks ADMIN permission
     * @see KeyWordPageRequest
     * @see UserPageVO
     * @see IPage
     */
    IPage<UserPageVO> page(KeyWordPageRequest pageRequest);

    /**
     * Retrieves simplified user metadata by ID for dropdowns, selectors, or internal references.
     * <p>
     * This method provides a lightweight alternative to full user queries when only
     * basic identification fields ({@code id}, {@code username}, {@code nickname}) are needed.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the user to retrieve</li>
     *     <li>Lookup strategy: Direct {@code SELECT} by primary key for O(1) performance</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns {@link UserSimpleVO} with {@code id}, {@code username}, {@code nickname}, etc.</li>
     *     <li><strong>Not Found</strong>: Returns {@code null} if no user exists with given {@code id}</li>
     *     <li><strong>Field Selection</strong>: Only includes essential fields; excludes password and sensitive PII</li>
     * </ul>
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * This method is a prime candidate for caching due to:
     * <ul>
     *     <li><strong>High Read Frequency</strong>: User metadata is referenced frequently in UI rendering and permission checks</li>
     *     <li><strong>Low Write Frequency</strong>: User profiles change infrequently</li>
     *     <li><strong>Small Payload</strong>: {@link UserSimpleVO} contains minimal fields for efficient cache storage</li>
     * </ul>
     * <p>
     * Recommended cache configuration:
     * <pre>
     * {@code
     * // Spring Cache annotation on implementation
     * @Cacheable(value = "users", key = "#id", unless = "#result == null")
     * public UserSimpleVO getUserById(Serializable id) { ... }
     *
     * // Cache invalidation on user update/delete
     * @CacheEvict(value = "users", key = "#id")
     * public boolean updateUser(Serializable id, UserDTO dto) { ... }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service: Fetch user for dropdown population
     * UserSimpleVO user = userService.getUserById(1001L);
     * if (user != null) {
     *     // Use for UI rendering
     *     dropdownOptions.add(new SelectOption(user.getId(), user.getUsername()));
     * }
     *
     * // Frontend: Populate select with user options
     * const { data: userOptions } = useRequest(() => api.getUserOptions());
     * const userOptions = computed(() =>
     *   userOptions.value?.map(opt => ({ label: opt.username, value: opt.id })) || []
     * );
     * }
     * </pre>
     *
     * @param id the primary key of the user to retrieve; must not be {@code null}
     * @return {@link UserSimpleVO} if found; {@code null} if not found
     * @throws IllegalArgumentException if {@code id} is {@code null}
     * @see UserSimpleVO
     */
    UserSimpleVO getUserById(Serializable id);

    /**
     * Updates an existing user profile with validation and conflict prevention.
     * <p>
     * This method handles the complete user update workflow including:
     * <ul>
     *     <li>Existence check (ensure user with given {@code id} exists)</li>
     *     <li>Username uniqueness validation (if {@code username} is being changed)</li>
     *     <li>Email uniqueness validation (if {@code email} is being changed)</li>
     *     <li>Field-level updates (nickname, avatar, phone, description, etc.)</li>
     *     <li>Password update handling (if new password is provided, it must be encoded)</li>
     *     <li>Audit field update ({@code updatedBy}, {@code updatedAt})</li>
     *     <li>Cache invalidation for affected user metadata</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key of the user to update</li>
     *     <li>{@code dto}: Must not be {@code null}; contains fields to update (partial updates supported)</li>
     *     <li>{@code dto.getUsername()}: If changed, must be unique across all users (excluding current entry)</li>
     *     <li>{@code dto.getEmail()}: If changed, must be unique across all users (excluding current entry)</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if user was successfully updated</li>
     *     <li><strong>Not Found</strong>: Returns {@code false} if no user exists with given {@code id}</li>
     *     <li><strong>Duplicate Username/Email</strong>: Returns {@code false} or throws exception if new values conflict</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Cache invalidation and audit logging can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same user are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Self-Update vs Admin</strong>: Users can update their own profile; admins can update any user</li>
     *     <li><strong>Password Encoding</strong>: If password is updated, it must be encoded via {@code PasswordEncoder} before persistence</li>
     *     <li><strong>Privilege Escalation Prevention</strong>: Non-admin users cannot change their own roles or status</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle profile update request
     * @PutMapping("/profile")
     * public Result<Void> updateProfile(@Valid @RequestBody UserDTO dto) {
     *     Long userId = SecurityContextUtils.getRequiredUserId();
     *     boolean success = userService.updateUser(userId, dto);
     *
     *     if (success) {
     *         return Result.success("Profile updated successfully");
     *     } else {
     *         return Result.fail(ErrorCode.USER_UPDATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Update logic
     * @Transactional
     * @Override
     * public boolean updateUser(Serializable id, UserDTO dto) {
     *     // 1. Check existence
     *     SysUser existing = getById(id);
     *     if (existing == null) {
     *         return false;
     *     }
     *
     *     // 2. Validate uniqueness if changing username/email
     *     if (!existing.getUsername().equals(dto.getUsername()) &&
     *         lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).ne(SysUser::getId, id).exists()) {
     *         throw new DuplicateException(ErrorCode.USER_USERNAME_EXISTS);
     *     }
     *
     *     // 3. Apply updates
     *     converter.updateEntity(existing, dto);
     *
     *     // 4. Encode password if provided
     *     if (StringUtils.hasText(dto.getPassword())) {
     *         existing.setPassword(passwordEncoder.encode(dto.getPassword()));
     *     }
     *
     *     existing.setUpdatedBy(SecurityContextUtils.getUserId());
     *     existing.setUpdatedAt(OffsetDateTime.now());
     *
     *     // 5. Persist changes
     *     boolean success = updateById(existing);
     *
     *     // 6. Invalidate caches
     *     if (success) {
     *         cacheHelper.evict(List.of(id), List.of(CacheConstant.USER));
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param id  the primary key of the user to update; must not be {@code null}
     * @param dto the update data with fields to modify; must not be {@code null}
     * @return {@code true} if user was successfully updated; {@code false} if not found or validation failed
     * @throws IllegalArgumentException                                       if {@code id} or {@code dto} is {@code null}
     * @throws com.github.starhq.template.common.exception.DuplicateException if new username/email conflicts
     * @see UserDTO
     */
    boolean updateUser(Serializable id, UserDTO dto);

    /**
     * Creates a new user account with validation and duplicate prevention.
     * <p>
     * This method handles the complete user creation workflow including:
     * <ul>
     *     <li>Input validation (username uniqueness, email format, password strength)</li>
     *     <li>Username and email uniqueness enforcement</li>
     *     <li>Password encoding via {@link org.springframework.security.crypto.password.PasswordEncoder}</li>
     *     <li>Default status assignment (typically {@code enabled = true})</li>
     *     <li>Default role assignment (e.g., {@code ROLE_USER})</li>
     *     <li>Audit field population ({@code createdBy}, {@code createdAt})</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code userDTO}: Must not be {@code null}; should include {@code username}, {@code password}, {@code email}</li>
     *     <li>{@code userDTO.getUsername()}: Must be unique across all users; format: {@code [a-zA-Z0-9_]{3,20}}</li>
     *     <li>{@code userDTO.getEmail()}: Must be unique across all users; valid email format</li>
     *     <li>{@code userDTO.getPassword()}: Must meet password policy (min length, complexity); will be encoded before storage</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@code true} if user was successfully created</li>
     *     <li><strong>Duplicate Username/Email</strong>: Returns {@code false} or throws exception if values already exist</li>
     *     <li><strong>Validation Error</strong>: Returns {@code false} or throws exception if input fails validation</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: User creation and default role assignment succeed or fail together</li>
     *     <li>Consistency: Related audit log entries can be recorded in same transaction</li>
     *     <li>Isolation: Concurrent registrations with same username are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Password Encoding</strong>: Always encode password via {@code PasswordEncoder} before persistence</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-IP rate limiting to prevent registration spam</li>
     *     <li><strong>Audit Logging</strong>: Log user creation events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle user creation request
     * @PostMapping
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<Long> createUser(@Valid @RequestBody UserDTO dto) {
     *     boolean success = userService.createUser(dto);
     *
     *     if (success) {
     *         // Optional: Return new user ID for frontend reference
     *         return Result.success(dto.getId());
     *     } else {
     *         return Result.fail(ErrorCode.USER_CREATE_FAILED);
     *     }
     * }
     *
     * // Service implementation: Creation logic
     * @Transactional
     * @Override
     * public boolean createUser(UserDTO dto) {
     *     // 1. Validate input
     *     validateUserDto(dto);
     *
     *     // 2. Check uniqueness
     *     if (lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).exists()) {
     *         throw new DuplicateException(ErrorCode.USER_USERNAME_EXISTS);
     *     }
     *     if (lambdaQuery().eq(SysUser::getEmail, dto.getEmail()).exists()) {
     *         throw new DuplicateException(ErrorCode.USER_EMAIL_EXISTS);
     *     }
     *
     *     // 3. Convert DTO to entity
     *     SysUser entity = converter.toEntity(dto);
     *     entity.setPassword(passwordEncoder.encode(dto.getPassword()));
     *     entity.setStatus(1); // Default enabled
     *     entity.setCreatedBy(SecurityContextUtils.getUserId());
     *     entity.setCreatedAt(OffsetDateTime.now());
     *
     *     // 4. Persist entity
     *     boolean success = save(entity);
     *
     *     // 5. Assign default role
     *     if (success) {
     *         roleService.assignDefaultRole(entity.getId());
     *     }
     *
     *     return success;
     * }
     * }
     * </pre>
     *
     * @param userDTO the user creation data; must not be {@code null}
     * @return {@code true} if user was successfully created; {@code false} otherwise
     * @throws IllegalArgumentException                                       if {@code userDTO} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.DuplicateException if username/email already exists
     * @see UserDTO
     */
    boolean createUser(UserDTO userDTO);

}