package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.exception.DuplicateException;
import com.github.starhq.template.common.exception.NotFoundException;
import org.springframework.dao.DuplicateKeyException;

import java.io.Serializable;
import java.util.Optional;

/**
 * Generic base service implementation providing enhanced CRUD operations with unified exception handling.
 * <p>
 * This abstract class extends {@link ServiceImpl} to provide reusable, type-safe methods for
 * common database operations (get, insert, update, delete) with consistent error semantics.
 * Designed to eliminate boilerplate code across all domain services while enforcing
 * business-level error codes and exception hierarchies.
 * <p>
 * <strong>Generic Type Parameters:</strong>
 * <ul>
 *     <li>{@code <M extends BaseMapper<T>>}: The MyBatis-Plus mapper interface for database operations</li>
 *     <li>{@code <T>}: The entity type extending domain-specific base entity</li>
 * </ul>
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Service Inheritance</strong>: Base class for all domain services (UserService, RoleService, etc.) to avoid code duplication</li>
 *     <li><strong>Unified Error Handling</strong>: Convert low-level exceptions (SQL, constraint violations) to business-level exceptions with i18n codes</li>
 *     <li><strong>Null-Safe Queries</strong>: Provide {@code getAndCheckById} to enforce "not found" semantics consistently</li>
 *     <li><strong>Constraint Protection</strong>: Automatic handling of duplicate key violations with business-friendly error messages</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Fail-Fast</strong>: Validate preconditions early and throw descriptive exceptions</li>
 *     <li><strong>Exception Hierarchy</strong>: Use typed exceptions ({@link NotFoundException}, {@link DuplicateException}) for precise error handling</li>
 *     <li><strong>i18n Ready</strong>: All error codes reference {@link ErrorCode} enum for multi-language message resolution</li>
 *     <li><strong>Transaction-Safe</strong>: Methods are designed to work within Spring {@code @Transactional} boundaries</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Concrete service: Extend base class with domain-specific types
 * @Service
 * public class UserService extends BaseServiceImpl<UserMapper, SysUser> {
 *
 *     @Transactional
 *     public UserSimpleVO createUser(UserCreateDTO dto) {
 *         SysUser entity = converter.toEntity(dto);
 *
 *         // Use enhanced insert with business error codes
 *         insert(entity,
 *             ErrorCode.USER_USERNAME_EXISTS,  // duplicate key error
 *             ErrorCode.USER_CREATE_FAILED     // general insert error
 *         );
 *
 *         return converter.toSimpleVO(entity);
 *     }
 *
 *     @Transactional
 *     public void updateUser(Long id, UserUpdateDTO dto) {
 *         // Use enhanced get with not-found check
 *         SysUser existing = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);
 *
 *         // Apply updates
 *         converter.updateEntity(existing, dto);
 *
 *         // Use enhanced update with business error codes
 *         update(existing,
 *             ErrorCode.USER_USERNAME_EXISTS,  // duplicate key on unique field update
 *             ErrorCode.USER_UPDATE_FAILED,    // general update error
 *             ErrorCode.USER_NOT_FOUND         // not found (should not happen after getAndCheck)
 *         );
 *     }
 * }
 *
 * // Global exception handler: Convert business exceptions to API responses
 * @RestControllerAdvice
 * public class GlobalExceptionHandler {
 *     @ExceptionHandler(NotFoundException.class)
 *     public Result<Void> handleNotFound(NotFoundException e) {
 *         return Result.fail(e.getErrorCode()); // Returns 404 with i18n message
 *     }
 *
 *     @ExceptionHandler(DuplicateException.class)
 *     public Result<Void> handleDuplicate(DuplicateException e) {
 *         return Result.fail(e.getErrorCode()); // Returns 409 with i18n message
 *     }
 * }
 * }
 * </pre>
 *
 * @param <M> the MyBatis-Plus mapper type extending {@link BaseMapper}
 * @param <T> the entity type for CRUD operations
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-28
 * @see ServiceImpl
 * @see ErrorCode
 * @see NotFoundException
 * @see DuplicateException
 * @see BusinessException
 */
public class BaseServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> {

    /**
     * Retrieves an entity by ID with automatic "not found" exception handling.
     * <p>
     * This method provides a null-safe alternative to {@code getBaseMapper().selectById()}
     * by throwing a typed {@link NotFoundException} when the entity does not exist,
     * eliminating repetitive null checks across service methods.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: Must not be {@code null}; the primary key value to lookup</li>
     *     <li>{@code notFound}: The {@link ErrorCode} to use if entity is not found; must not be {@code null}</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Found</strong>: Returns the entity instance of type {@code <T>}</li>
     *     <li><strong>Not Found</strong>: Throws {@link NotFoundException} with provided {@code ErrorCode}</li>
     *     <li><strong>Never Null</strong>: If method returns, result is guaranteed non-null</li>
     * </ul>
     * <p>
     * <strong>Exception Strategy:</strong>
     * <ul>
     *     <li><strong>Not Found</strong>: {@link NotFoundException} with business error code for consistent API responses</li>
     *     <li><strong>Database Errors</strong>: Propagated to caller for global exception handling</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service method: Get user with not-found handling
     * public UserSimpleVO getUserById(Long id) {
     *     // Throws NotFoundException if user not found
     *     SysUser user = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);
     *     return converter.toSimpleVO(user);
     * }
     *
     * // Alternative: Use in update/delete operations
     * @Transactional
     * public void deleteUser(Long id) {
     *     // Ensure entity exists before deletion
     *     SysUser user = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);
     *     getBaseMapper().deleteById(id);
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Single database query via {@code selectById}; ensure primary key index exists</li>
     *     <li>Optional wrapper adds negligible overhead; prefer over manual null checks for readability</li>
     * </ul>
     *
     * @param id       the primary key value to lookup; must not be {@code null}
     * @param notFound the error code to use if entity is not found; must not be {@code null}
     * @return the entity instance if found
     * @throws NotFoundException        if no entity found with given {@code id}
     * @throws IllegalArgumentException if {@code id} or {@code notFound} is {@code null}
     * @see BaseMapper#selectById(Serializable)
     * @see Optional#ofNullable(Object)
     */
    protected T getAndCheckById(Serializable id, ErrorCode notFound) {
        return Optional.ofNullable(getBaseMapper().selectById(id)).orElseThrow(() -> new NotFoundException(notFound));
    }

    /**
     * Inserts a new entity with automatic duplicate key and general error handling.
     * <p>
     * This method wraps {@code getBaseMapper().insert()} to convert low-level database
     * exceptions into business-level exceptions with i18n-ready error codes.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code data}: The entity to insert; must not be {@code null} and should have required fields populated</li>
     *     <li>{@code duplicate}: The {@link ErrorCode} to use if insert fails due to unique constraint violation</li>
     *     <li>{@code insert}: The {@link ErrorCode} to use for other insert failures (e.g., connection error, validation)</li>
     * </ul>
     * <p>
     * <strong>Exception Handling Strategy:</strong>
     * <ul>
     *     <li><strong>Duplicate Key</strong>: Catches {@link DuplicateKeyException} and throws {@link DuplicateException} with {@code duplicate} code</li>
     *     <li><strong>Other Errors</strong>: Catches any {@link Exception} and throws {@link BusinessException} with {@code insert} code</li>
     *     <li><strong>Success</strong>: Returns normally; entity ID should be auto-populated if using auto-increment primary key</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Insert either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related entities (e.g., user-role mappings) can be inserted in same transaction</li>
     *     <li>Isolation: Concurrent inserts with same unique key are properly serialized by database</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service method: Create new user with error handling
     * @Transactional
     * public UserSimpleVO createUser(UserCreateDTO dto) {
     *     SysUser entity = converter.toEntity(dto);
     *
     *     // Insert with business error codes
     *     insert(entity,
     *         ErrorCode.USER_USERNAME_EXISTS,  // If username already exists
     *         ErrorCode.USER_CREATE_FAILED     // For other insert errors
     *     );
     *
     *     // Entity ID is now populated (if using auto-increment)
     *     return converter.toSimpleVO(entity);
     * }
     *
     * // Global exception handler: Convert to API response
     * @ExceptionHandler(DuplicateException.class)
     * public Result<Void> handleDuplicate(DuplicateException e) {
     *     // Returns HTTP 409 with i18n message: "Username 'alice' already exists"
     *     return Result.fail(e.getErrorCode());
     * }
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always use specific {@link ErrorCode} values for duplicate vs. general errors to enable precise frontend handling</li>
     *     <li>Validate business rules (e.g., username format) before calling {@code insert} to provide immediate feedback</li>
     *     <li>Consider pre-checking unique constraints with {@code existsByXxx} queries for more user-friendly error messages</li>
     * </ul>
     *
     * @param data      the entity to insert; must not be {@code null}
     * @param duplicate the error code for unique constraint violations
     * @param insert    the error code for other insert failures
     * @throws DuplicateException       if insert fails due to duplicate key
     * @throws BusinessException        if insert fails for other reasons
     * @throws IllegalArgumentException if {@code data}, {@code duplicate}, or {@code insert} is {@code null}
     * @see BaseMapper#insert(Object)
     * @see DuplicateKeyException
     * @see DuplicateException
     * @see BusinessException
     */
    protected void insert(T data, ErrorCode duplicate, ErrorCode insert) {
        try {
            getBaseMapper().insert(data);
        } catch (DuplicateKeyException e) {
            throw new DuplicateException(duplicate, e);
        } catch (Exception e) {
            throw new BusinessException(insert, e);
        }
    }

    /**
     * Updates an existing entity with automatic not-found, duplicate key, and general error handling.
     * <p>
     * This method wraps {@code getBaseMapper().updateById()} to provide consistent error semantics
     * for update operations, including detection of "no rows affected" scenarios.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code data}: The entity with updated fields; must include valid {@code id} for targeting</li>
     *     <li>{@code duplicate}: The {@link ErrorCode} to use if update fails due to unique constraint violation</li>
     *     <li>{@code update}: The {@link ErrorCode} to use for other update failures (e.g., connection error, optimistic lock)</li>
     *     <li>{@code notFound}: The {@link ErrorCode} to use if no entity found with given {@code id} (update affected 0 rows)</li>
     * </ul>
     * <p>
     * <strong>Update Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: {@code updateById} returns {@code > 0}; method returns normally</li>
     *     <li><strong>Not Found</strong>: {@code updateById} returns {@code 0}; throws {@link NotFoundException} with {@code notFound} code</li>
     *     <li><strong>Duplicate Key</strong>: Catches {@link DuplicateKeyException} and throws {@link DuplicateException} with {@code duplicate} code</li>
     *     <li><strong>Other Errors</strong>: Catches any {@link Exception} and throws {@link BusinessException} with {@code update} code</li>
     * </ul>
     * <p>
     * <strong>Exception Preservation:</strong>
     * <p>
     * If the caught exception is already a {@link CustomException} (or subclass), it is re-thrown
     * unchanged to preserve the original error context and avoid wrapping business exceptions unnecessarily.
     * <pre>
     * {@code
     * catch (Exception e) {
     *     if (e instanceof CustomException ce) {
     *         throw ce; // Preserve original business exception
     *     }
     *     throw new BusinessException(update, e); // Wrap unexpected exceptions
     * }
     * }
     * </pre>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Update either succeeds completely or rolls back on error</li>
     *     <li>Consistency: Related entity updates can be performed in same transaction</li>
     *     <li>Isolation: Concurrent updates to same entity are properly serialized</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service method: Update user with comprehensive error handling
     * @Transactional
     * public void updateUser(Long id, UserUpdateDTO dto) {
     *     // Optional: Pre-check existence for better error message
     *     SysUser existing = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);
     *
     *     // Apply updates to entity
     *     converter.updateEntity(existing, dto);
     *
     *     // Update with business error codes
     *     update(existing,
     *         ErrorCode.USER_USERNAME_EXISTS,  // If updating to existing username
     *         ErrorCode.USER_UPDATE_FAILED,    // For other update errors
     *         ErrorCode.USER_NOT_FOUND         // If entity was deleted concurrently
     *     );
     * }
     *
     * // Frontend: Handle update errors by code
     * if (response.code === ErrorCode.USER_USERNAME_EXISTS.code) {
     *   showMessage('Username already taken');
     * } else if (response.code === ErrorCode.USER_NOT_FOUND.code) {
     *   showMessage('User no longer exists');
     * }
     * }
     * </pre>
     * <p>
     * <strong>Optimistic Locking Consideration:</strong>
     * <p>
     * If entity uses version-based optimistic locking (e.g., {@code @Version} field),
     * a failed update due to version mismatch will result in {@code updateById} returning {@code 0},
     * which triggers the {@code notFound} exception. Consider using a distinct {@link ErrorCode}
     * like {@code OPTIMISTIC_LOCK_FAILED} for clearer error semantics.
     *
     * @param data      the entity with updated fields; must include valid {@code id}
     * @param duplicate the error code for unique constraint violations
     * @param update    the error code for other update failures
     * @param notFound  the error code if no entity found with given {@code id}
     * @throws NotFoundException        if update affected 0 rows (entity not found or version mismatch)
     * @throws DuplicateException       if update fails due to duplicate key
     * @throws BusinessException        if update fails for other reasons
     * @throws IllegalArgumentException if any parameter is {@code null}
     * @see BaseMapper#updateById(Object)
     * @see CustomException
     * @see DuplicateException
     * @see BusinessException
     */
    protected void update(T data, ErrorCode duplicate, ErrorCode update, ErrorCode notFound) {
        try {
            if (getBaseMapper().updateById(data) <= 0) {
                throw new NotFoundException(notFound);
            }
        } catch (DuplicateKeyException e) {
            throw new DuplicateException(duplicate, e);
        } catch (Exception e) {
            if (e instanceof CustomException ce) {
                throw ce;
            }
            throw new BusinessException(update, e);
        }
    }

    /**
     * Deletes an entity by ID with automatic not-found and general error handling.
     * <p>
     * This method wraps {@code getBaseMapper().deleteById()} to provide consistent error semantics
     * for delete operations, including detection of "no rows affected" scenarios.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code id}: The primary key value of the entity to delete; must not be {@code null}</li>
     *     <li>{@code notFound}: The {@link ErrorCode} to use if no entity found with given {@code id} (delete affected 0 rows)</li>
     *     <li>{@code delete}: The {@link ErrorCode} to use for other delete failures (e.g., foreign key constraint, connection error)</li>
     * </ul>
     * <p>
     * <strong>Delete Semantics:</strong>
     * <ul>
     *     <li><strong>Success</strong>: {@code deleteById} returns {@code > 0}; method returns normally</li>
     *     <li><strong>Not Found</strong>: {@code deleteById} returns {@code 0}; throws {@link NotFoundException} with {@code notFound} code</li>
     *     <li><strong>Other Errors</strong>: Catches any {@link Exception} and throws {@link BusinessException} with {@code delete} code</li>
     * </ul>
     * <p>
     * <strong>Exception Preservation:</strong>
     * <p>
     * Similar to {@link #update}, if the caught exception is already a {@link CustomException},
     * it is re-thrown unchanged to preserve original error context.
     * <p>
     * <strong>Soft Delete Consideration:</strong>
     * <p>
     * If entity uses logical deletion (e.g., {@code deleted} flag with MyBatis-Plus {@code @TableLogic}),
     * {@code deleteById} performs a soft delete (UPDATE with {@code deleted=1}) rather than physical DELETE.
     * In this case, "not found" may indicate the entity was already soft-deleted.
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * This method should be called within a {@code @Transactional} boundary to ensure:
     * <ul>
     *     <li>Atomicity: Delete either succeeds completely or rolls back on error</li>
     *     <li>Cascading: Related entity cleanup (e.g., user-role mappings) can be performed in same transaction</li>
     *     <li>Audit: Delete operation can be logged in same transaction for consistency</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service method: Delete user with error handling
     * @Transactional
     * public void deleteUser(Long id) {
     *     // Delete with business error codes
     *     delete(id,
     *         ErrorCode.USER_NOT_FOUND,      // If user already deleted or not exists
     *         ErrorCode.USER_DELETE_FAILED   // For other delete errors (e.g., foreign key constraint)
     *     );
     *
     *     // Optional: Log audit trail in same transaction
     *     auditLogService.record("USER_DELETED", TargetType.USER, id, ...);
     * }
     *
     * // Frontend: Handle delete errors by code
     * if (response.code === ErrorCode.USER_NOT_FOUND.code) {
     *   showMessage('User already deleted');
     * } else if (response.code === ErrorCode.USER_DELETE_FAILED.code) {
     *   showMessage('Cannot delete: user has associated data');
     * }
     * }
     * </pre>
     * <p>
     * <strong>Foreign Key Constraint Handling:</strong>
     * <p>
     * If delete fails due to foreign key constraints (e.g., user has orders),
     * the database throws {@code SQLException} which is caught and wrapped as
     * {@link BusinessException} with {@code delete} error code. Consider:
     * <ul>
     *     <li>Pre-checking for dependent records before delete for user-friendly messages</li>
     *     <li>Using database-level {@code ON DELETE CASCADE} or {@code ON DELETE SET NULL} for automatic cleanup</li>
     *     <li>Implementing application-level cascade logic for complex dependency graphs</li>
     * </ul>
     *
     * @param id       the primary key value to delete; must not be {@code null}
     * @param notFound the error code if no entity found with given {@code id}
     * @param delete   the error code for other delete failures
     * @throws NotFoundException        if delete affected 0 rows (entity not found or already deleted)
     * @throws BusinessException        if delete fails for other reasons
     * @throws IllegalArgumentException if {@code id}, {@code notFound}, or {@code delete} is {@code null}
     * @see BaseMapper#deleteById(Serializable)
     * @see CustomException
     * @see BusinessException
     */
    protected void delete(Serializable id, ErrorCode notFound, ErrorCode delete) {
        try {
            if (getBaseMapper().deleteById(id) <= 0) {
                throw new NotFoundException(notFound);
            }
        } catch (Exception e) {
            if (e instanceof CustomException ce) {
                throw ce;
            }
            throw new BusinessException(delete, e);
        }
    }

}