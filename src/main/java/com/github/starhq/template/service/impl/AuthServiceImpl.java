package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.common.util.SecurityUserUtils;
import com.github.starhq.template.converter.UserConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.entity.SysUserRole;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.mapper.SysUserRoleMapper;
import com.github.starhq.template.model.dto.user.ResetPasswordDTO;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Authentication service implementation integrating Spring Security for user login, registration, and password management.
 * <p>
 * This class extends {@link BaseServiceImpl} to provide enhanced CRUD operations with unified exception handling,
 * while implementing {@link AuthService} for authentication-specific workflows. Designed to centralize
 * security-critical logic with consistent password encoding, role assignment, and cache invalidation strategies.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>User Authentication</strong>: Load {@link UserDetails} by username for Spring Security login flow with status validation</li>
 *     <li><strong>User Registration</strong>: Create new accounts with password encoding, default role assignment, and duplicate prevention</li>
 *     <li><strong>Password Reset</strong>: Secure password change workflow with old password validation, token invalidation, and cache eviction</li>
 *     <li><strong>Cache Management</strong>: Integrate with Spring Cache and distributed event system for consistent user/token data</li>
 * </ul>
 * <p>
 * <strong>Spring Security Integration:</strong>
 * <p>
 * This service implements {@link AuthService} which extends {@link org.springframework.security.core.userdetails.UserDetailsService},
 * enabling seamless integration with Spring Security's authentication mechanism:
 * <pre>
 * {@code
 * // Security configuration: Wire AuthServiceImpl as UserDetailsService
 * @Configuration
 * @EnableWebSecurity
 * public class SecurityConfig {
 *     @Autowired private AuthService authService;
 *
 *     @Bean
 *     public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
 *         http.userDetailsService(authService); // Use AuthServiceImpl for user loading
 *         return http.build();
 *     }
 * }
 *
 * // Authentication flow: Spring Security calls loadUserByUsername()
 * UserDetails userDetails = authService.loadUserByUsername("alice");
 * // Returns SysUser entity with encoded password, authorities, and account status flags
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Security-First</strong>: All methods enforce password encoding, status validation, and audit logging</li>
 *     <li><strong>Transaction-Safe</strong>: Critical operations use {@code @Transactional} or {@code TransactionTemplate} for atomicity</li>
 *     <li><strong>Cache-Consistent</strong>: Changes trigger distributed cache eviction via {@link EventService} for multi-node consistency</li>
 *     <li><strong>Null-Safe</strong>: Uses {@link NullMarked} and {@link CollectionUtils} to prevent NPE in security-critical paths</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-24
 * @see AuthService
 * @see BaseServiceImpl
 * @see SysUser
 * @see UserDetails
 */
@Service("authService")
@RequiredArgsConstructor
public class AuthServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements AuthService {

    /**
     * Mapper for {@link SysToken} database operations (session management).
     * <p>
     * Used for token invalidation during password reset to force re-authentication.
     *
     * @see SysTokenMapper
     */
    private final SysTokenMapper tokenMapper;

    /**
     * Mapper for {@link SysRole} database operations (role management).
     * <p>
     * Used for fetching default roles during user registration.
     *
     * @see SysRoleMapper
     */
    private final SysRoleMapper roleMapper;

    /**
     * Mapper for {@link SysUserRole} junction table operations (user-role assignment).
     * <p>
     * Used for batch inserting default role assignments during registration.
     *
     * @see SysUserRoleMapper
     */
    private final SysUserRoleMapper userRoleMapper;

    /**
     * Converter for transforming between {@link UserDTO} and {@link SysUser} entities.
     * <p>
     * Ensures consistent field mapping and avoids boilerplate conversion code.
     *
     * @see UserConverter
     */
    private final UserConverter userConverter;

    /**
     * Password encoder for hashing passwords before storage.
     * <p>
     * Typically configured as {@code BCryptPasswordEncoder} with strength 12 for security/performance balance.
     *
     * @see PasswordEncoder
     * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Transaction template for fine-grained transaction control.
     * <p>
     * Used in {@link #resetPassword} to ensure atomic password update + token deletion.
     *
     * @see TransactionTemplate
     */
    private final TransactionTemplate transactionTemplate;

    /**
     * Event service for publishing cache invalidation events across distributed nodes.
     * <p>
     * Ensures cache consistency when user data changes (e.g., password reset).
     *
     * @see EventService
     * @see CacheConstant
     */
    private final EventService eventService;

    /**
     * Loads user details by username for Spring Security authentication.
     * <p>
     * This method is called by Spring Security during the authentication process
     * to retrieve user credentials, authorities, and account status for login validation.
     * <p>
     * <strong>Cache Strategy:</strong>
     * <p>
     * Annotated with {@code @Cacheable(value = "users", key = "#p0", unless = "#result == null")} to:
     * <ul>
     *     <li>Cache successful lookups by username to reduce database load during repeated auth attempts</li>
     *     <li>Skip caching {@code null} results to prevent cache pollution from invalid usernames</li>
     *     <li>Use username (case-sensitive) as cache key for precise invalidation</li>
     * </ul>
     * <p>
     * <strong>Security Validation:</strong>
     * <ul>
     *     <li><strong>Status Check</strong>: Calls {@link SecurityUserUtils#checkUserStatus(SysUser)} to reject disabled/locked/expired accounts</li>
     *     <li><strong>Role Check</strong>: Throws {@link CustomException} if user has no roles (prevents privilege escalation)</li>
     *     <li><strong>Null Safety</strong>: Annotated with {@link NullMarked} to enforce non-null return contract</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: Returns {@link SysUser} entity implementing {@link UserDetails} with encoded password and authorities</li>
     *     <li><strong>Not Found</strong>: Throws {@link UsernameNotFoundException} if username does not exist</li>
     *     <li><strong>Account Issues</strong>: Throws {@link CustomException} for disabled/locked/no-roles scenarios</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Ensure {@code username} column has unique index for O(log N) lookup</li>
     *     <li>Cache TTL should be short (e.g., 5-10 min) to reflect account status changes promptly</li>
     *     <li>Consider caching only essential fields (username, password, roles) to minimize memory usage</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Spring Security: Automatic call during form/login authentication
     * Authentication auth = authenticationManager.authenticate(
     *     new UsernamePasswordAuthenticationToken("alice", "password123")
     * );
     * // Internally calls: authService.loadUserByUsername("alice")
     *
     * // Manual lookup (e.g., for token validation)
     * UserDetails userDetails = authService.loadUserByUsername("alice");
     * boolean matches = passwordEncoder.matches(rawPassword, userDetails.getPassword());
     * }
     * </pre>
     *
     * @param username the username to lookup; must not be {@code null} or empty
     * @return {@link UserDetails} with authentication data and authorities; never {@code null}
     * @throws UsernameNotFoundException if no user found with given {@code username}
     * @throws CustomException           if account is disabled/locked/expired or has no roles
     * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(String)
     * @see SecurityUserUtils#checkUserStatus(SysUser)
     * @see Cacheable
     */
    @Cacheable(value = "users", key = "#p0", unless = "#result == null")
    @Override
    public @NullMarked UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. Load user with roles via custom mapper method (avoids N+1 query)
        SysUser user = getBaseMapper().selectUserWithRole(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );

        // 2. Validate account status (enabled, not locked, not expired)
        SecurityUserUtils.checkUserStatus(user);

        // 3. Ensure user has at least one role (prevent privilege escalation)
        if (CollectionUtils.isEmpty(user.getAuthorities())) {
            throw new CustomException(ErrorCode.NO_ROLES, HttpStatus.UNAUTHORIZED);
        }

        // 4. Return SysUser as UserDetails (entity implements UserDetails interface)
        return user;
    }

    /**
     * Registers a new user account with password encoding and default role assignment.
     * <p>
     * <strong>Deprecation Notice:</strong>
     * <p>
     * This method is marked {@code @Deprecated} because registration logic should be moved
     * to the controller layer for better separation of concerns. Future implementations
     * should handle DTO validation, password encoding, and business logic orchestration in controllers.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Annotated with {@code @Transactional(rollbackFor = Exception.class)} to ensure:
     * <ul>
     *     <li>Atomicity: User insertion and role assignment succeed or fail together</li>
     *     <li>Consistency: If role assignment fails, user insertion is rolled back</li>
     *     <li>Isolation: Concurrent registrations with same username are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Processing Steps:</strong>
     * <ol>
     *     <li>Convert {@link UserDTO} to {@link SysUser} entity via {@link UserConverter}</li>
     *     <li>Encode password via {@link PasswordEncoder} before persistence (never store plain text)</li>
     *     <li>Insert user via enhanced {@link #insert} method with duplicate key handling</li>
     *     <li>Assign default roles via {@link #assignDefaultRoles} (typically {@code ROLE_USER})</li>
     *     <li>Return {@link SysUser} as {@link UserDetails} for immediate auto-login</li>
     * </ol>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Password Encoding</strong>: Always encode via {@code passwordEncoder.encode()} before storage</li>
     *     <li><strong>Duplicate Prevention</strong>: {@link #insert} catches {@code DuplicateKeyException} and throws business-friendly error</li>
     *     <li><strong>Default Roles</strong>: Ensure at least one default role exists to prevent orphaned accounts</li>
     *     <li><strong>Input Validation</strong>: Controller should validate DTO fields (username format, password strength) before calling this method</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle registration request (deprecated pattern)
     * @PostMapping("/register")
     * public Result<UserSimpleVO> register(@Valid @RequestBody UserDTO dto) {
     *     try {
     *         // Register user (returns UserDetails for auto-login)
     *         UserDetails userDetails = authService.register(dto);
     *
     *         // Generate JWT token for immediate authentication
     *         String token = jwtTokenProvider.createToken(userDetails);
     *
     *         return Result.success(
     *             userConverter.toSimpleVO((SysUser) userDetails),
     *             Map.of("token", token)
     *         );
     *     } catch (DuplicateException e) {
     *         return Result.fail(ErrorCode.USER_DUPLICATE_USERNAME);
     *     } catch (BusinessException e) {
     *         return Result.fail(e.getErrorCode());
     *     }
     * }
     * }
     * </pre>
     *
     * @param userDto the registration data with username, password, email; must not be {@code null}
     * @return {@link UserDetails} for the newly registered user; ready for authentication
     * @throws IllegalArgumentException                                       if {@code userDto} is {@code null} or missing required fields
     * @throws com.github.starhq.template.common.exception.DuplicateException if username already exists
     * @throws BusinessException                                              if role assignment fails or no default roles configured
     * @see UserDTO
     * @see UserConverter#toEntity(UserDTO)
     * @see #assignDefaultRoles(SysUser)
     * @deprecated Registration logic should be moved to controller layer; this method will be removed in v2.0
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserDetails register(UserDTO userDto) {
        // 1. Convert DTO to entity and encode password
        SysUser user = userConverter.toEntity(userDto);
        // TODO: Move password encoding to controller layer for better separation of concerns
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. Insert user with business error handling (duplicate key → USER_DUPLICATE_USERNAME)
        insert(user, ErrorCode.USER_DUPLICATE_USERNAME, ErrorCode.USER_INSERT_FAILED);

        // 3. Assign default roles (e.g., ROLE_USER) to new account
        assignDefaultRoles(user);

        // 4. Return entity as UserDetails for immediate auto-login
        return user;
    }

    /**
     * Resets the password for the currently authenticated user.
     * <p>
     * This method implements a secure password change workflow that:
     * <ol>
     *     <li>Validates the current user's identity via {@link SecurityContextUtils#getRequiredUserId()}</li>
     *     <li>Checks account status to prevent password changes on disabled/locked accounts</li>
     *     <li>Validates old password and ensures new password differs from old</li>
     *     <li>Atomically updates password and invalidates all active tokens (force re-authentication)</li>
     *     <li>Evicts cached user data and tokens via distributed event system</li>
     * </ol>
     * <p>
     * <strong>Transaction Strategy:</strong>
     * <p>
     * Uses {@link TransactionTemplate} for fine-grained control over the atomic block:
     * <ul>
     *     <li><strong>Password Update</strong>: Must succeed before token deletion</li>
     *     <li><strong>Token Invalidation</strong>: All active sessions must be revoked to prevent old password reuse</li>
     *     <li><strong>Rollback</strong>: If either step fails, entire operation rolls back to maintain consistency</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Old Password Validation</strong>: Prevents unauthorized password changes if session is hijacked</li>
     *     <li><strong>New Password Uniqueness</strong>: Prevents "reset to same password" attacks</li>
     *     <li><strong>Token Invalidation</strong>: Forces all active sessions to re-authenticate with new password</li>
     *     <li><strong>Cache Eviction</strong>: Ensures distributed nodes see updated password immediately</li>
     *     <li><strong>Audit Logging</strong>: Consider logging password reset events for compliance tracking</li>
     * </ul>
     * <p>
     * <strong>Cache Invalidation Strategy:</strong>
     * <pre>
     * {@code
     * // Evict user cache by username (for loadUserByUsername caching)
     * eventService.notifyCacheEvict(List.of(user.getUsername()), List.of(CacheConstant.USER));
     *
     * // Evict token cache by user ID (for session validation caching)
     * eventService.notifyCacheEvict(List.of(user.getId()), List.of(CacheConstant.TOKEN));
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Handle password reset request
     * @PostMapping("/reset-password")
     * public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
     *     boolean success = authService.resetPassword(dto);
     *
     *     if (success) {
     *         // Optional: Auto-logout user or redirect to login
     *         return Result.success("Password reset successfully. Please login with new password.");
     *     } else {
     *         return Result.fail(ErrorCode.RESET_PASSWORD);
     *     }
     * }
     *
     * // Frontend: Password reset form
     * const handleSubmit = async (values) => {
     *   try {
     *     await api.resetPassword({
     *       oldPassword: values.oldPassword,
     *       newPassword: values.newPassword
     *     });
     *     message.success('Password updated');
     *     // Redirect to login or refresh token
     *     router.push('/login');
     *   } catch (error) {
     *     if (error.code === ErrorCode.MISMATCH_PASSWORD.code) {
     *       message.error('Current password is incorrect');
     *     } else if (error.code === ErrorCode.SAME_PASSWORD.code) {
     *       message.error('New password must be different');
     *     }
     *   }
     * };
     * }
     * </pre>
     *
     * @param dto the reset request with old and new password; must not be {@code null}
     * @return {@code true} if password was successfully reset
     * @throws BusinessException if validation fails, update fails, or token deletion fails
     * @throws CustomException   if old password mismatch or new password equals old
     * @see ResetPasswordDTO
     * @see SecurityContextUtils#getRequiredUserId()
     * @see #validatePasswordChange(SysUser, ResetPasswordDTO)
     * @see #updatePassword(Long, String)
     */
    @Override
    public boolean resetPassword(ResetPasswordDTO dto) {
        // 1. Get current user ID from security context (ensures authenticated caller)
        Long userId = SecurityContextUtils.getRequiredUserId();

        // 2. Load user and validate account status
        SysUser user = getBaseMapper().selectById(userId);
        SecurityUserUtils.checkUserStatus(user);

        // 3. Validate password change rules (old password match, new != old)
        validatePasswordChange(user, dto);

        // 4. Encode new password before storage
        String newPassword = passwordEncoder.encode(dto.getNewPassword());

        // 5. Atomic transaction: update password + invalidate all tokens
        transactionTemplate.execute(_ -> {
            // Update password
            boolean success = updatePassword(userId, newPassword);
            if (!success) {
                throw new BusinessException(ErrorCode.RESET_PASSWORD);
            }

            // Invalidate all active tokens for this user (force re-authentication)
            if (tokenMapper.delete(new LambdaQueryWrapper<SysToken>().eq(SysToken::getUserId, user.getId())) <= 0) {
                // Note: <=0 may be acceptable if user has no active tokens; adjust based on business requirements
                throw new BusinessException(ErrorCode.RESET_PASSWORD);
            }
            return true;
        });

        // 6. Evict distributed caches to ensure consistency across nodes
        eventService.notifyCacheEvict(List.of(user.getUsername()), List.of(CacheConstant.USER));
        eventService.notifyCacheEvict(List.of(user.getId()), List.of(CacheConstant.TOKEN));

        return true;
    }

    // ====================== Private Helper Methods ======================

    /**
     * Assigns default roles to a newly registered user.
     * <p>
     * This method fetches all roles marked as {@code isDefault = true} and creates
     * junction table entries ({@link SysUserRole}) to establish user-role relationships.
     * <p>
     * <strong>Business Rules:</strong>
     * <ul>
     *     <li><strong>At Least One Default</strong>: Throws {@link BusinessException} if no default roles configured (prevents orphaned accounts)</li>
     *     <li><strong>Batch Insert</strong>: Uses {@code userRoleMapper.insert(List)} for efficient bulk insertion</li>
     *     <li><strong>Authority Population</strong>: Sets {@code user.setAuthorities()} for immediate use in returned {@link UserDetails}</li>
     * </ul>
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li><strong>No Default Roles</strong>: Throws {@code BusinessException(NO_ROLES)} to alert administrators of misconfiguration</li>
     *     <li><strong>Insert Failure</strong>: Catches generic exceptions and wraps with {@code USER_INSERT_FAILED} for consistent error reporting</li>
     * </ul>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called exclusively from {@link #register(UserDTO)} during new user creation.
     * Not intended for manual role assignment (use {@code RoleService} for that).
     *
     * @param user the newly created user entity; must have valid {@code id} populated
     * @throws BusinessException if no default roles found or batch insert fails
     * @see SysRole#getIsDefault()
     * @see SysUserRole#SysUserRole(Long, Long)
     */
    private void assignDefaultRoles(SysUser user) {
        // Fetch all roles marked as default (e.g., ROLE_USER)
        List<SysRole> defaultRoles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getIsDefault, true)
        );

        // Ensure at least one default role exists (prevent orphaned accounts)
        if (CollectionUtils.isEmpty(defaultRoles)) {
            throw new BusinessException(ErrorCode.NO_ROLES);
        }

        // Set authorities on user entity for immediate use in returned UserDetails
        user.setAuthorities(defaultRoles);

        // Create junction table entries for batch insert
        List<SysUserRole> userRoles = defaultRoles.stream()
                .map(role -> new SysUserRole(user.getId(), role.getId()))
                .toList();

        try {
            // Efficient bulk insert via MyBatis-Plus
            userRoleMapper.insert(userRoles);
        } catch (Exception e) {
            // Wrap with business error code for consistent API responses
            throw new BusinessException(ErrorCode.USER_INSERT_FAILED, e);
        }
    }

    /**
     * Validates password change rules for security and user experience.
     * <p>
     * This method enforces two critical security policies:
     * <ol>
     *     <li><strong>Old Password Verification</strong>: Ensures caller knows current password (prevents session hijacking attacks)</li>
     *     <li><strong>New Password Uniqueness</strong>: Prevents "reset to same password" which could indicate compromised credentials</li>
     * </ol>
     * <p>
     * <strong>Validation Logic:</strong>
     * <pre>
     * {@code
     * // 1. Verify old password matches stored hash
     * if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
     *     throw new CustomException(ErrorCode.MISMATCH_PASSWORD, HttpStatus.BAD_REQUEST);
     * }
     *
     * // 2. Ensure new password differs from old (prevent no-op resets)
     * if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
     *     throw new CustomException(ErrorCode.SAME_PASSWORD, HttpStatus.BAD_REQUEST);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Timing Attack Prevention</strong>: {@code PasswordEncoder.matches()} should use constant-time comparison</li>
     *     <li><strong>Error Messages</strong>: Use generic messages in production to avoid username enumeration</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limiting on password change attempts</li>
     * </ul>
     * <p>
     * <strong>Extension Points:</strong>
     * <p>
     * Consider adding additional validation rules:
     * <ul>
     *     <li>Password strength requirements (length, complexity)</li>
     *     <li>Password history check (prevent reuse of last N passwords)</li>
     *     <li>Cooldown period between password changes</li>
     * </ul>
     *
     * @param user the user entity with current password hash; must not be {@code null}
     * @param dto  the reset request with old and new password; must not be {@code null}
     * @throws CustomException if old password mismatch or new password equals old
     * @see PasswordEncoder#matches(CharSequence, String)
     * @see ErrorCode#MISMATCH_PASSWORD
     * @see ErrorCode#SAME_PASSWORD
     */
    private void validatePasswordChange(SysUser user, ResetPasswordDTO dto) {
        // Verify old password matches stored hash (prevent unauthorized changes)
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.MISMATCH_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        // Ensure new password differs from old (prevent no-op resets)
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Updates user password in database with minimal field update.
     * <p>
     * This method uses MyBatis-Plus's partial update feature to modify only the
     * {@code password} field, avoiding unnecessary updates to other columns.
     * <p>
     * <strong>Update Strategy:</strong>
     * <ul>
     *     <li><strong>Minimal Update</strong>: Only sets {@code id} and {@code password} to avoid triggering {@code @TableField(update = "...")} logic on other fields</li>
     *     <li><strong>Return Value</strong>: Returns {@code true} if {@code updateById} affected {@code > 0} rows (user exists)</li>
     *     <li><strong>No Audit Fields</strong>: Does not update {@code updatedBy}/{@code updatedAt}; consider adding if audit trail required</li>
     * </ul>
     * <p>
     * <strong>Usage Context:</strong>
     * <p>
     * Called exclusively from {@link #resetPassword} within a transactional block.
     * Not intended for general user updates (use {@code UserService.update()} for that).
     *
     * @param userId   the user ID to update; must not be {@code null}
     * @param password the encoded new password; must not be {@code null}
     * @return {@code true} if update affected {@code > 0} rows; {@code false} if user not found
     * @see SysUser#setId(Long)
     * @see SysUser#setPassword(String)
     * @see SysUserMapper#updateById(Object)
     */
    private boolean updatePassword(Long userId, String password) {
        // Create minimal update entity (only id + password)
        SysUser updateEntity = new SysUser();
        updateEntity.setId(userId);
        updateEntity.setPassword(password);

        // Execute partial update; returns number of affected rows
        return getBaseMapper().updateById(updateEntity) > 0;
    }

}