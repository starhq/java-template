package com.github.starhq.template.helper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Functional helper for batch fetching user ID-to-username mappings.
 * <p>
 * This component implements {@link Function}{@code <Set<Long>, Map<Long, String>>}
 * to enable concise, reusable user name resolution by ID. It is designed for
 * scenarios where multiple user references need to be enriched with display names,
 * such as audit logs, activity feeds, or admin console tables.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Audit Log Enrichment</strong>: Convert {@code createdBy} IDs to usernames in {@link com.github.starhq.template.entity.SysAuditLog}</li>
 *     <li><strong>Activity Timeline</strong>: Display actor names for user action histories</li>
 *     <li><strong>Admin Console</strong>: Show operator names in management tables without N+1 queries</li>
 *     <li><strong>Cache Population</strong>: Serve as {@code dbLoader} for {@link CacheHelper#getBatchWithCache(Set, String, Function)}</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Batch Efficiency</strong>: Single query with {@code IN} clause avoids N+1 problem</li>
 *     <li><strong>Minimal Fetch</strong>: Selects only {@code id} and {@code username} to reduce payload</li>
 *     <li><strong>Null-Safety</strong>: Returns empty map for empty input or no results; never {@code null}</li>
 *     <li><strong>Functional Composition</strong>: Implements {@link Function} for easy integration with streams and cache helpers</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class AuditLogService {
 *     @Autowired private SysUserMapperHelper userMapperHelper;
 *
 *     public List<AuditLogVO> enrichWithUsernames(List<SysAuditLog> logs) {
 *         // Extract unique creator IDs
 *         Set<Long> creatorIds = logs.stream()
 *             .map(SysAuditLog::getCreatedBy)
 *             .filter(Objects::nonNull)
 *             .collect(Collectors.toSet());
 *
 *         // Batch fetch username map
 *         Map<Long, String> usernameMap = userMapperHelper.apply(creatorIds);
 *
 *         // Enrich logs with usernames
 *         return logs.stream()
 *             .map(log -> {
 *                 AuditLogVO vo = convert(log);
 *                 vo.setCreatorName(usernameMap.get(log.getCreatedBy()));
 *                 return vo;
 *             })
 *             .collect(Collectors.toList());
 *     }
 * }
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-14
 * @see SysUserMapper
 * @see CacheHelper#getBatchWithCache(Set, String, Function)
 * @see java.util.function.Function
 */
@Component
@RequiredArgsConstructor
public class SysUserMapperHelper implements Function<Set<Long>, Map<Long, String>> {

    private final SysUserMapper userMapper;

    /**
     * Fetches a map of user ID to username for the given set of user IDs.
     * <p>
     * This method performs a single database query with an {@code IN} clause to
     * efficiently retrieve usernames for multiple users at once. Only the {@code id}
     * and {@code username} fields are selected to minimize network and memory overhead.
     * <p>
     * <strong>Query Behavior:</strong>
     * <ul>
     *     <li>If {@code ids} is empty or {@code null}, returns {@link Collections#emptyMap()} immediately</li>
     *     <li>If no users match the requested IDs, returns empty map (partial results are allowed)</li>
     *     <li>Duplicate IDs in input are automatically deduplicated by the {@code Set} type</li>
     *     <li>Missing IDs in result are silently omitted (caller should handle {@code null} lookups)</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large ID sets (>1000), consider chunking to avoid SQL parameter limits</li>
     *     <li>Ensure {@code sys_user.id} has an index for efficient {@code IN} query execution</li>
     *     <li>Combine with {@link CacheHelper#getBatchWithCache} for automatic cache population</li>
     * </ul>
     * <p>
     * <strong>Integration with CacheHelper:</strong>
     * <pre>
     * {@code
     * // Use as dbLoader for cached ID-to-name mapping
     * Map<Long, String> usernames = cacheHelper.getBatchWithCache(
     *     userIds,
     *     "user:name",
     *     userMapperHelper  // Method reference to this Function
     * );
     * }
     * </pre>
     *
     * @param ids the set of user IDs to resolve; may be empty but not {@code null} for typical usage
     * @return a map of user ID to username for found users; empty map if no matches; never {@code null}
     * @see Collectors#toMap(Function, Function)
     */
    @Override
    public Map<Long, String> apply(Set<Long> ids) {
        // Fast-path: empty input requires no database query
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        // Query only necessary fields (id, username) for efficiency
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .select(SysUser::getId, SysUser::getUsername)
                        .in(SysUser::getId, ids)
        );

        // Return empty map if no users found (partial results are acceptable)
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }

        // Collect into ID -> username map; duplicate IDs are handled by Set input
        return users.stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername));
    }

}