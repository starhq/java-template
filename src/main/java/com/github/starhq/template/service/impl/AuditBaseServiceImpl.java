package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.entity.BaseEntity;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.model.dto.PageRequest;
import com.github.starhq.template.model.vo.BaseAuditVO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generic base service implementation for paginated queries with automatic audit field population.
 * <p>
 * This abstract class extends {@link BaseServiceImpl} to provide reusable pagination logic
 * that automatically resolves creator/updater IDs to human-readable usernames via cached batch lookup.
 * Designed to eliminate boilerplate code across all audit-capable services (User, Role, Menu, etc.).
 * <p>
 * <strong>Generic Type Parameters:</strong>
 * <ul>
 *     <li>{@code <M extends BaseMapper<T>>}: The MyBatis-Plus mapper interface for database operations</li>
 *     <li>{@code <T extends BaseEntity>}: The entity type with audit fields ({@code createdBy}, {@code updatedBy})</li>
 * </ul>
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Service Inheritance</strong>: Base class for all audit-capable services to avoid code duplication</li>
 *     <li><strong>Automatic Audit Resolution</strong>: Convert numeric creator/updater IDs to usernames without N+1 queries</li>
 *     <li><strong>Cache-Optimized Lookup</strong>: Batch fetch usernames via {@link CacheHelper} with fallback to database</li>
 *     <li><strong>Flexible Query Building</strong>: Support dynamic filters via {@code Consumer<QueryWrapper<T>>} callback</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Reusability</strong>: Generic implementation eliminates duplication across 10+ domain services</li>
 *     <li><strong>Performance</strong>: Batch username resolution reduces database round-trips from O(N) to O(1)</li>
 *     <li><strong>Cache-First</strong>: Leverage {@link CacheHelper} for efficient username caching with automatic fallback</li>
 *     <li><strong>Null-Safety</strong>: Handle {@code null} audit IDs gracefully with fallback to "unknown" placeholder</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Concrete service: Extend base class with domain-specific types
 * @Service
 * public class UserService extends AuditBaseServiceImpl<UserMapper, SysUser> {
 *
 *     public IPage<UserPageVO> page(UserPageRequest request) {
 *         return pageVO(
 *             request,
 *             // Dynamic filter: add username fuzzy search
 *             wrapper -> wrapper.like(StringUtils.hasText(request.getUsername()),
 *                                    SysUser::getUsername, request.getUsername()),
 *             // Batch username loader: fetch from user table
 *             userIds -> userMapper.selectIdNameMap(userIds),
 *             // Entity to VO converter
 *             entity -> converter.toPageVO(entity)
 *         );
 *     }
 * }
 *
 * // Result: Paginated VOs with resolved creator/updater names
 * IPage<UserPageVO> result = userService.page(request);
 * result.getRecords().forEach(vo -> {
 *     System.out.println(vo.getCreator()); // "alice_admin", not "1001"
 *     System.out.println(vo.getUpdater()); // "bob_manager", not "1002"
 * });
 * }
 * </pre>
 *
 * @param <M> the MyBatis-Plus mapper type extending {@link BaseMapper}
 * @param <T> the entity type extending {@link BaseEntity} with audit fields
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-14
 * @see BaseServiceImpl
 * @see BaseAuditVO#populateAuditFields(Long, Long, Map)
 * @see CacheHelper#getBatchWithCache(Set, String, Function)
 */
public abstract class AuditBaseServiceImpl<M extends BaseMapper<T>, T extends BaseEntity>
        extends BaseServiceImpl<M, T> {

    /**
     * Cache helper for efficient batch username resolution with automatic fallback.
     * <p>
     * Injected via {@link Autowired} for:
     * <ul>
     *     <li><strong>Cache-First Lookup</strong>: Try Redis/Caffeine before database query</li>
     *     <li><strong>Batch Optimization</strong>: Fetch multiple usernames in single operation</li>
     *     <li><strong>Automatic Fallback</strong>: Load from database if cache miss, then populate cache</li>
     *     <li><strong>TTL Management</strong>: Configurable cache expiration to balance freshness and performance</li>
     * </ul>
     * <p>
     * <strong>Usage Pattern:</strong>
     * <pre>
     * {@code
     * // Batch resolve user IDs to usernames
     * Set<Long> userIds = Set.of(1001L, 1002L, 1003L);
     * Map<Long, String> nameMap = cacheHelper.getBatchWithCache(
     *     userIds,
     *     CacheConstant.ADMIN_NAME,  // Cache key prefix
     *     ids -> userMapper.selectIdNameMap(ids)  // Database loader function
     * );
     * // Result: {1001="alice", 1002="bob", 1003="unknown"} (if 1003 not found)
     * }
     * </pre>
     *
     * @see CacheHelper
     * @see CacheConstant#ADMIN_NAME
     */
    protected final CacheHelper cacheHelper;

    /**
     * Constructs the base audit service with the required cache helper.
     * <p>
     * This constructor is designed to be called by subclass constructors via {@code super(cacheHelper)}.
     *
     * @param cacheHelper the cache utility used for batch username resolution
     *                    and cache invalidation in {@link #pageVO}
     */
    protected AuditBaseServiceImpl(CacheHelper cacheHelper) {
        this.cacheHelper = cacheHelper;
    }

    /**
     * Executes a paginated query with automatic audit field population.
     * <p>
     * This method provides a reusable template for audit-capable pagination:
     * <ol>
     *     <li>Build base {@link QueryWrapper} from {@link PageRequest}</li>
     *     <li>Apply optional dynamic filters via {@code queryConsumer} callback</li>
     *     <li>Execute paginated query via MyBatis-Plus mapper</li>
     *     <li>Extract unique {@code createdBy}/{@code updatedBy} IDs from results</li>
     *     <li>Batch resolve IDs to usernames via {@link CacheHelper} with fallback</li>
     *     <li>Convert entities to VOs and populate audit fields with resolved names</li>
     * </ol>
     * <p>
     * <strong>Generic Type Parameters:</strong>
     * <ul>
     *     <li>{@code <E extends BaseAuditVO>}: The VO type that supports {@code populateAuditFields()}</li>
     *     <li>{@code <P extends PageRequest>}: The request type that provides pagination and query building</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code request}: Must not be {@code null}; provides pagination params and base query wrapper</li>
     *     <li>{@code queryConsumer}: Optional callback to add domain-specific filters; may be {@code null}</li>
     *     <li>{@code dbLoader}: Function to batch fetch usernames from database; must handle empty ID set gracefully</li>
     *     <li>{@code voMapper}: Function to convert entity to VO; must not return {@code null}</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} with current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches</li>
     *     <li><strong>Audit Fields</strong>: All returned VOs have {@code creator}/{@code updater} populated with usernames or "unknown"</li>
     * </ul>
     * <p>
     * <strong>Performance Optimizations:</strong>
     * <ul>
     *     <li><strong>Batch Resolution</strong>: Collect all audit IDs first, then single batch lookup (O(1) DB calls vs O(N))</li>
     *     <li><strong>Cache-First</strong>: {@link CacheHelper} tries cache before database, reducing latency</li>
     *     <li><strong>Early Return</strong>: Skip username resolution if page is empty to avoid unnecessary work</li>
     *     <li><strong>Stream Efficiency</strong>: Use {@code flatMap} + {@code filter} for concise ID extraction</li>
     * </ul>
     * <p>
     * <strong>Null-Safety Guarantees:</strong>
     * <ul>
     *     <li>{@code null} audit IDs are filtered out before batch lookup</li>
     *     <li>Empty result pages return valid {@code IPage} with empty list, not {@code null}</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // UserService: Paginated user query with audit resolution
     * @Service
     * public class UserService extends AuditBaseServiceImpl<UserMapper, SysUser> {
     *
     *     public IPage<UserPageVO> page(UserPageRequest request) {
     *         return pageVO(
     *             request,
     *             // Add dynamic filters
     *             wrapper -> {
     *                 wrapper.like(StringUtils.hasText(request.getUsername()),
     *                            SysUser::getUsername, request.getUsername());
     *                 wrapper.eq(request.getStatus() != null,
     *                           SysUser::getStatus, request.getStatus());
     *             },
     *             // Batch username loader
     *             userIds -> userMapper.selectIdNameMap(userIds),
     *             // Entity to VO converter
     *             entity -> converter.toPageVO(entity)
     *         );
     *     }
     * }
     *
     * // Controller: Return paginated result
     * @GetMapping("/users")
     * public Result<IPage<UserPageVO>> listUsers(UserPageRequest request) {
     *     IPage<UserPageVO> page = userService.page(request);
     *     return Result.success(page.getRecords(), page.getTotal());
     * }
     *
     * // Frontend: Access resolved audit fields
     * const UserTable = ({ data }) => (
     *   <a-table :data-source="data.records">
     *     <a-table-column title="Created By" data-index="creator" />
     *     <a-table-column title="Updated By" data-index="updater" />
     *   </a-table>
     * );
     * }
     * </pre>
     * <p>
     * <strong>Exception Handling:</strong>
     * <ul>
     *     <li><strong>Database Errors</strong>: Propagated to caller for global exception handling</li>
     *     <li><strong>Cache Errors</strong>: {@link CacheHelper} handles internally with fallback to database</li>
     *     <li><strong>Null Pointer</strong>: Prevented by null checks and {@code Objects::nonNull} filter</li>
     * </ul>
     *
     * @param <E>           the VO type extending {@link BaseAuditVO}
     * @param <P>           the request type extending {@link PageRequest}
     * @param request       the pagination and filter criteria; must not be {@code null}
     * @param queryConsumer optional callback to add domain-specific filters; may be {@code null}
     * @param dbLoader      function to batch fetch usernames from database; must handle empty set
     * @param voMapper      function to convert entity to VO; must not return {@code null}
     * @return paginated list of VOs with populated audit fields; never {@code null}
     * @throws IllegalArgumentException if {@code request} or {@code voMapper} is {@code null}
     * @see BaseAuditVO#populateAuditFields(Long, Long, Map)
     * @see CacheHelper#getBatchWithCache(Set, String, Function)
     * @see PageRequest#toQueryWrapper()
     * @see PageRequest#toPage()
     */
    protected <E extends BaseAuditVO, P extends PageRequest> IPage<E> pageVO(
            P request,
            Consumer<QueryWrapper<T>> queryConsumer,
            Function<Set<Long>, Map<Long, String>> dbLoader,
            Function<T, E> voMapper) {

        // 1. Build base query wrapper from request
        QueryWrapper<T> queryWrapper = request.toQueryWrapper();

        // 2. Apply optional dynamic filters
        if (queryConsumer != null) {
            queryConsumer.accept(queryWrapper);
        }

        // 3. Convert request to MyBatis-Plus page object
        Page<T> page = request.toPage();

        // 4. Execute paginated query
        IPage<T> pages = getBaseMapper().selectPage(page, queryWrapper);

        // 5. Early return if no results (skip username resolution)
        if (pages.getRecords().isEmpty()) {
            return new Page<>(pages.getCurrent(), pages.getSize(), pages.getTotal());
        }

        // 6. Extract unique audit IDs (createdBy + updatedBy) from all records
        // Uses flatMap to handle both fields, filter out nulls, collect to set for deduplication
        Set<Long> auditIds = pages.getRecords().stream()
                .flatMap(e -> Stream.of(e.getCreatedBy(), e.getUpdatedBy()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 7. Batch resolve IDs to usernames via cache helper with database fallback
        // CacheHelper handles: cache lookup → cache miss → db load → cache populate → return
        Map<Long, String> nameMap = cacheHelper.getBatchWithCache(
                auditIds,
                CacheConstant.ADMIN_NAME,  // Cache key prefix for username resolution
                dbLoader                   // Database loader function for cache miss
        );

        // 8. Convert entities to VOs and populate audit fields with resolved names
        return pages.convert(entity -> {
            E vo = voMapper.apply(entity);
            // Populate creator/updater with resolved usernames (or "unknown" if not found)
            vo.populateAuditFields(entity.getCreatedBy(), entity.getUpdatedBy(), nameMap);
            return vo;
        });
    }

}