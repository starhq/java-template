package com.github.starhq.template.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.NotFoundException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Generic helper for managing many-to-many relationship assignments in RBAC systems.
 * <p>
 * This component provides reusable, type-safe utilities for bulk association operations
 * such as assigning resources/menus/buttons to roles, or assigning roles to users.
 * By abstracting common patterns (validation, deletion, batch upsert), it reduces
 * boilerplate code and ensures consistent error handling across the application.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Role-Resource Assignment</strong>: Grant/revoke API permissions to roles</li>
 *     <li><strong>Role-Menu Assignment</strong>: Configure navigation access for roles</li>
 *     <li><strong>Role-Button Assignment</strong>: Control UI element visibility by role</li>
 *     <li><strong>User-Role Assignment</strong>: Assign multiple roles to a user account</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Functional Abstraction</strong>: Uses {@link Consumer} and {@link BiFunction} to decouple persistence logic</li>
 *     <li><strong>Type Safety</strong>: Generic methods prevent accidental type mismatches at compile time</li>
 *     <li><strong>Fail-Fast Validation</strong>: Entity existence checks before assignment prevent orphaned references</li>
 *     <li><strong>Transaction Awareness</strong>: Callers should wrap assignments in {@code @Transactional} for atomicity</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class RoleService {
 *     @Autowired private RelationHelper relationHelper;
 *     @Autowired private RoleResourceMapper roleResourceMapper;
 *
 *     @Transactional
 *     public void assignResourcesToRole(Long roleId, Set<Long> resourceIds) {
 *         relationHelper.assignRelations(
 *             roleId,
 *             resourceIds,
 *             // Delete existing assignments
 *             id -> roleResourceMapper.delete(new LambdaQueryWrapper<SysRoleResource>()
 *                 .eq(SysRoleResource::getRoleId, id)),
 *             // Create new association entities
 *             (rid, resId) -> new SysRoleResource(rid, resId),
 *             // Batch insert new associations
 *             entities -> roleResourceMapper.insertBatch(entities),
 *             // Validate resource existence before assignment
 *             ids -> relationHelper.validateEntityExists(ids, resourceMapper, SysResource::getId, ErrorCode.RESOURCE_NOT_FOUND),
 *             RelationHelper.AssociationType.RESOURCE
 *         );
 *     }
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-14
 * @see AssociationType
 * @see com.baomidou.mybatisplus.core.mapper.BaseMapper
 * @see org.springframework.transaction.annotation.Transactional
 */
@Component
@RequiredArgsConstructor
public class RelationHelper {

    /**
     * Enumeration defining supported many-to-many association types in the RBAC system.
     * <p>
     * Each constant encapsulates:
     * <ul>
     *     <li><strong>Cache Name</strong>: Logical cache region for invalidation after assignment changes</li>
     *     <li><strong>Not-Found Error</strong>: ErrorCode thrown when target entities don't exist during validation</li>
     *     <li><strong>Assignment Error</strong>: ErrorCode thrown when bulk upsert operation fails</li>
     * </ul>
     * <p>
     * <strong>Extensibility:</strong>
     * <p>
     * To add a new association type (e.g., {@code ROLE_DEPT}):
     * <ol>
     *     <li>Add a new enum constant with appropriate cache name and error codes</li>
     *     <li>Ensure corresponding cache region is configured in {@code application.yml}</li>
     *     <li>Update documentation and tests to cover the new type</li>
     * </ol>
     *
     * @see CacheConstant
     * @see ErrorCode
     */
    @Getter
    @AllArgsConstructor
    public enum AssociationType {
        /**
         * Role ↔ API Resource association.
         * <p>
         * Cache: {@code "role:resources"}, Errors: {@code RESOURCE_NOT_FOUND} / {@code ROLE_ASSIGN_RESOURCES_FAILED}
         */
        RESOURCE(CacheConstant.RESOURCE, ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.ROLE_ASSIGN_RESOURCES_FAILED),

        /**
         * Role ↔ Navigation Menu association.
         * <p>
         * Cache: {@code "role:menus"}, Errors: {@code MENU_NOT_FOUND} / {@code ROLE_ASSIGN_MENUS_FAILED}
         */
        MENU(CacheConstant.MENU, ErrorCode.MENU_NOT_FOUND, ErrorCode.ROLE_ASSIGN_MENUS_FAILED),

        /**
         * Role ↔ UI Button association.
         * <p>
         * Cache: {@code "role:buttons"}, Errors: {@code BUTTON_NOT_FOUND} / {@code ROLE_ASSIGN_BUTTONS_FAILED}
         */
        BUTTON(CacheConstant.BUTTON, ErrorCode.BUTTON_NOT_FOUND, ErrorCode.ROLE_ASSIGN_BUTTONS_FAILED),

        /**
         * User ↔ Role association.
         * <p>
         * Cache: {@code "user:roles"}, Errors: {@code USER_NOT_FOUND} / {@code USER_ASSIGN_FAILED}
         */
        USER(CacheConstant.USER, ErrorCode.USER_NOT_FOUND, ErrorCode.USER_ASSIGN_FAILED);

        /**
         * The logical cache region name for this association type.
         * <p>
         * Used for cache invalidation after assignment changes (e.g., {@code "role:resources"}).
         * Should match the cache name used in {@code @Cacheable} annotations.
         */
        private final String name;

        /**
         * Error code thrown when target entities don't exist during pre-assignment validation.
         * <p>
         * Example: Assigning non-existent resources to a role triggers {@code RESOURCE_NOT_FOUND}.
         */
        private final ErrorCode notFoundError;

        /**
         * Error code thrown when the bulk upsert operation fails during assignment.
         * <p>
         * Example: Database constraint violation during batch insert triggers {@code ROLE_ASSIGN_RESOURCES_FAILED}.
         */
        private final ErrorCode assignError;
    }

    /**
     * Generic method for bulk many-to-many relationship assignment.
     * <p>
     * This method encapsulates the common pattern for updating associations:
     * <ol>
     *     <li><strong>Empty Handling</strong>: If {@code targetIds} is empty, triggers deletion of all existing associations</li>
     *     <li><strong>Validation</strong>: Optionally validates target entity existence before proceeding</li>
     *     <li><strong>Entity Construction</strong>: Creates association entities via factory function</li>
     *     <li><strong>Bulk Upsert</strong>: Persists new associations via batch insert/update</li>
     *     <li><strong>Error Translation</strong>: Wraps persistence exceptions with business-friendly error codes</li>
     * </ol>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code deleteLogic}: Called only when {@code targetIds} is empty; should remove all associations for {@code ownerId}</li>
     *     <li>{@code entityFactory}: Must return a new association entity (e.g., {@code SysRoleResource}) for each {@code (ownerId, targetId)} pair</li>
     *     <li>{@code upsertLogic}: Should perform batch insert or replace; must be idempotent for retry safety</li>
     *     <li>{@code validator}: Optional pre-check to ensure all {@code targetIds} exist; throws {@code NotFoundException} if validation fails</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Callers should wrap this method in {@code @Transactional} to ensure atomicity:
     * <pre>
     * {@code
     * @Transactional
     * public void assignResources(Long roleId, Set<Long> resourceIds) {
     *     relationHelper.assignRelations(...);
     *     // Other business logic...
     * }
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always validate target existence before assignment to prevent orphaned references</li>
     *     <li>Use batch operations (e.g., {@code insertBatch}) for performance with large {@code targetIds} sets</li>
     *     <li>Invalidate related caches after successful assignment via {@code CacheHelper.evict()}</li>
     *     <li>Log assignment changes for audit trails (consider publishing {@code AuditLogEvent})</li>
     * </ul>
     *
     * @param <E>           the type of association entity (e.g., {@code SysRoleResource}, {@code SysUserRole})
     * @param ownerId       the unique identifier of the owner entity (e.g., role ID or user ID)
     * @param targetIds     the set of target entity IDs to associate; if empty, triggers full deletion
     * @param deleteLogic   consumer to execute when {@code targetIds} is empty; may be {@code null}
     * @param entityFactory function to create association entities from {@code (ownerId, targetId)} pairs
     * @param upsertLogic   consumer to persist the list of association entities; must handle batch operations efficiently
     * @param validator     optional consumer to validate target entity existence before assignment; may be {@code null}
     * @param type          the {@link AssociationType} defining cache name and error codes for this operation
     * @throws BusinessException if {@code upsertLogic} fails, wrapped with {@code type.assignError}
     * @throws NotFoundException if {@code validator} detects missing target entities
     * @see AssociationType
     * @see org.springframework.transaction.annotation.Transactional
     */
    public <E> void assignRelations(Long ownerId,
                                    Set<Long> targetIds,
                                    Consumer<Long> deleteLogic,
                                    BiFunction<Long, Long, E> entityFactory,
                                    Consumer<List<E>> upsertLogic,
                                    Consumer<Set<Long>> validator,
                                    AssociationType type) {

        // Case 1: Empty target set → delete all existing associations for this owner
        if (CollectionUtils.isEmpty(targetIds)) {
            if (deleteLogic != null) {
                deleteLogic.accept(ownerId);
            }
            return;
        }

        // Case 2: Validate target entities exist before proceeding (optional but recommended)
        if (validator != null) {
            validator.accept(targetIds);
        }

        // Case 3: Build association entities for batch persistence
        List<E> entities = targetIds.stream()
                .map(id -> entityFactory.apply(ownerId, id))
                .toList();

        // Case 4: Persist associations with business-friendly error translation
        try {
            upsertLogic.accept(entities);
        } catch (Exception e) {
            // Wrap technical exceptions with domain-specific error codes
            throw new BusinessException(type.getAssignError(), e);
        }
    }

    /**
     * Generic method for validating that a set of entity IDs all exist in the database.
     * <p>
     * This helper prevents assignment of non-existent entities by performing a count query
     * and comparing against the expected number of IDs. If any ID is missing, throws
     * {@link NotFoundException} with the provided error code.
     * <p>
     * <strong>Query Strategy:</strong>
     * <p>
     * Uses MyBatis-Plus {@link LambdaQueryWrapper} with {@code IN} clause for efficient
     * batch existence checking. The query counts matching records rather than fetching
     * full entities, minimizing database load.
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Validate resources exist before assigning to role
     * validateEntityExists(
     *     resourceIds,
     *     resourceMapper,
     *     SysResource::getId,  // Method reference to ID getter
     *     ErrorCode.RESOURCE_NOT_FOUND
     * );
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large ID sets (>1000), consider chunking to avoid SQL parameter limits</li>
     *     <li>Ensure the ID column has an index for efficient {@code IN} query execution</li>
     *     <li>Cache frequently validated entities if validation is high-frequency</li>
     * </ul>
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>If {@code ids} is empty/null, the method returns immediately (no validation needed)</li>
     *     <li>If {@code mapper} or {@code idGetter} is misconfigured, {@code selectCount} may throw; ensure proper DI</li>
     * </ul>
     *
     * @param <X>      the entity type to validate (e.g., {@code SysResource}, {@code SysMenu})
     * @param ids      the set of entity IDs to validate; empty set returns immediately
     * @param mapper   the MyBatis-Plus mapper for querying the entity table
     * @param idGetter method reference to extract the ID field from entity (for {@code LambdaQueryWrapper})
     * @param notFound the {@link ErrorCode} to use if validation fails
     * @throws NotFoundException if the count of existing entities doesn't match the requested ID count
     */
    public <X> void validateEntityExists(Set<Long> ids,
                                         BaseMapper<X> mapper,
                                         SFunction<X, Long> idGetter,
                                         ErrorCode notFound) {
        // Empty set requires no validation
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        // Count existing entities with IDs in the requested set
        long validCount = mapper.selectCount(new LambdaQueryWrapper<X>().in(idGetter, ids));

        // If counts don't match, at least one ID is missing → fail fast
        if (validCount != ids.size()) {
            throw new NotFoundException(notFound);
        }
    }

}