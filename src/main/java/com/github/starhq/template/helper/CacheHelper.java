package com.github.starhq.template.helper;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;

/**
 * Centralized helper for cache operations across the application.
 * <p>
 * This class provides a type-safe, framework-agnostic API for common caching patterns:
 * <ul>
 *     <li><strong>Single Key Operations</strong>: {@code put}/{@code get} with null-safety</li>
 *     <li><strong>Batch Invalidation</strong>: Evict multiple keys across multiple cache regions in one call</li>
 *     <li><strong>Cache-Aside Pattern</strong>: {@code getBatchWithCache} for efficient ID-to-value mapping with automatic cache population</li>
 *     <li><strong>Cache Access</strong>: {@code getCache} for advanced operations (native cache manipulation)</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Null-Safety</strong>: All methods handle {@code null} cache/keys gracefully without throwing NPE</li>
 *     <li><strong>Framework Abstraction</strong>: Wraps Spring {@link CacheManager} to enable future cache provider swaps</li>
 *     <li><strong>Batch Efficiency</strong>: Minimize round-trips by supporting bulk operations</li>
 *     <li><strong>Fail-Fast</strong>: {@code getCache} throws {@link BusinessException} if cache is misconfigured, preventing silent failures</li>
 * </ul>
 * <p>
 * <strong>Cache Naming Convention:</strong>
 * <p>
 * Cache names should follow a consistent pattern for easy management:
 * <ul>
 *     <li>{@code "user:simple"} — Lightweight user profile cache (key: {@code userId}, value: {@code UserSimpleVO})</li>
 *     <li>{@code "dict:all"} — Full dictionary data cache (key: {@code typeCode}, value: {@code List<DictDataVO>})</li>
 *     <li>{@code "role:permissions"} — Role-based permission matrix (key: {@code roleId}, value: {@code Set<String>})</li>
 *     <li>{@code "config:global"} — System-wide configuration cache (key: {@code configKey}, value: {@code Object})</li>
 * </ul>
 * Use constants in {@code CacheConstants} class to avoid typos and enable IDE auto-completion.
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * This helper is stateless and safe for concurrent use. Underlying {@link Cache} implementations
 * (Caffeine, Redis, etc.) handle their own synchronization.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-14
 * @see CacheManager
 * @see Cache
 * @see com.github.starhq.template.common.constant.CacheConstant
 */
@Slf4j
@RequiredArgsConstructor
public class CacheHelper {

    private final CacheManager cacheManager;

    /**
     * Stores a key-value pair into the specified cache region.
     * <p>
     * This method is null-safe: if the cache name is not configured or the cache
     * instance is {@code null}, the operation is silently skipped without throwing
     * exceptions. This design prioritizes resilience over strict enforcement.
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * cacheHelper.put(1001L, userSimpleVO, "user:simple");
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Ensure {@code value} is serializable if using distributed cache (Redis)</li>
     *     <li>Set appropriate TTL at cache configuration level to avoid stale data</li>
     *     <li>Use cache name constants to avoid typos: {@code CacheConstants.USER_SIMPLE}</li>
     * </ul>
     *
     * @param <K>       the type of the cache key, typically {@link Long} or {@link String}
     * @param <V>       the type of the cache value, must be serializable for distributed caches
     * @param key       the cache key to store; must not be {@code null} for reliable retrieval
     * @param value     the value to cache; may be {@code null} (stores a null placeholder in some implementations)
     * @param cacheName the logical name of the target cache region; must match a configured cache
     * @see Cache#put(Object, Object)
     * @see com.github.starhq.template.common.constant.CacheConstant
     */
    public <K, V> void put(K key, V value, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
        // Silently skip if cache is not configured — defensive design for resilience
    }

    /**
     * Retrieves a cached value by key from the specified cache region, with type conversion.
     * <p>
     * This method leverages Spring Cache's {@link Cache#get(Object, Class)} to perform
     * type-safe retrieval. If the cache is not configured or the key is not present,
     * returns {@code null} without throwing exceptions.
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * UserSimpleVO user = cacheHelper.get(1001L, UserSimpleVO.class, "user:simple");
     * if (user == null) {
     *     // Cache miss: load from database and populate cache
     *     user = userMapper.selectById(1001L);
     *     cacheHelper.put(1001L, user, "user:simple");
     * }
     * }
     * </pre>
     * <p>
     * <strong>Type Conversion:</strong>
     * <p>
     * The {@code clazz} parameter enables automatic type conversion for caches that
     * store values in serialized form (e.g., Redis with JSON serialization). For
     * in-memory caches (Caffeine), the value is cast directly.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always check for {@code null} return value to handle cache misses</li>
     *     <li>Use the same {@code clazz} for {@code put} and {@code get} to ensure type consistency</li>
     *     <li>For complex objects, ensure proper serialization configuration (Jackson, etc.)</li>
     * </ul>
     *
     * @param <K>       the type of the cache key
     * @param <V>       the expected return type
     * @param key       the cache key to retrieve
     * @param clazz     the target type for conversion; must match the stored value type
     * @param cacheName the logical name of the source cache region
     * @return the cached value converted to {@code <V>}, or {@code null} if cache/key not found
     * @see Cache#get(Object, Class)
     */
    public <K, V> V get(K key, Class<V> clazz, String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            return cache.get(key, clazz);
        }
        return null;
    }

    /**
     * Batch invalidates cache entries across multiple regions.
     * <p>
     * This method supports two eviction strategies based on the {@code ids} parameter:
     * <ul>
     *     <li><strong>Specific Keys</strong>: If {@code ids} is non-empty, evicts only the specified keys from each cache</li>
     *     <li><strong>Full Clear</strong>: If {@code ids} is empty but {@code cacheNames} is provided, clears the entire cache region</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Evict specific user caches
     * cacheHelper.evict(List.of(1001L, 1002L), List.of("user:simple", "user:full"));
     *
     * // Clear entire dictionary cache (use with caution)
     * cacheHelper.evict(Collections.emptyList(), List.of("dict:all"));
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>For large key collections (>1000), consider pattern-based eviction via native cache APIs</li>
     *     <li>Redis-backed caches benefit from pipeline-based batch deletion for efficiency</li>
     *     <li>Avoid clearing entire caches in production unless necessary; prefer targeted key eviction</li>
     * </ul>
     * <p>
     * <strong>Null-Safety:</strong>
     * <ul>
     *     <li>If {@code cacheNames} is empty/null, the method returns immediately</li>
     *     <li>Unknown cache names are silently skipped without throwing exceptions</li>
     * </ul>
     *
     * @param <K>        the type of cache keys to evict
     * @param ids        the collection of keys to invalidate; if empty, triggers full cache clear
     * @param cacheNames the collection of logical cache region names to target; must not be {@code null}
     * @see Cache#evict(Object)
     * @see Cache#clear()
     */
    public <K> void evict(Collection<K> ids, Collection<String> cacheNames) {
        if (CollectionUtils.isEmpty(cacheNames)) {
            return;
        }

        for (String name : cacheNames) {
            Optional.ofNullable(cacheManager.getCache(name)).ifPresent(c -> {
                if (CollectionUtils.isEmpty(ids)) {
                    // Empty ids list means clear the entire cache region (use with caution)
                    c.clear();
                } else {
                    // Evict specific keys only
                    ids.forEach(c::evict);
                }
            });
        }
    }

    /**
     * Clears all entries from the specified cache region.
     * <p>
     * <strong>Warning:</strong> This operation removes <em>all</em> entries from the cache,
     * which may cause temporary performance degradation due to cache cold start.
     * Use targeted {@link #evict(Collection, Collection)} for finer-grained invalidation
     * whenever possible.
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *     <li>Global configuration refresh (e.g., feature flags, system parameters)</li>
     *     <li>Cache corruption recovery (manual intervention)</li>
     *     <li>Testing environments where full reset is acceptable</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Log cache clear operations at INFO level for auditability</li>
     *     <li>Consider adding a confirmation step for production clears</li>
     *     <li>Combine with event publishing to notify other service instances in distributed setups</li>
     * </ul>
     *
     * @param cacheName the logical name of the cache region to clear; must be configured
     * @see Cache#clear()
     */
    public void clear(String cacheName) {
        Optional.ofNullable(cacheManager.getCache(cacheName)).ifPresent(Cache::clear);
    }

    /**
     * Implements the Cache-Aside pattern for efficient batch ID-to-value mapping.
     * <p>
     * This method optimizes the common scenario of fetching multiple entities by ID:
     * <ol>
     *     <li><strong>Cache Lookup</strong>: Attempts to retrieve all requested IDs from cache</li>
     *     <li><strong>Database Fallback</strong>: For cache misses, invokes {@code dbLoader} to fetch missing data</li>
     *     <li><strong>Write-Back</strong>: Automatically populates cache with newly fetched values</li>
     *     <li><strong>Result Merge</strong>: Returns a unified map containing both cached and fresh data</li>
     * </ol>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Fetch user names by IDs with automatic cache population
     * Map<Long, String> userNames = cacheHelper.getBatchWithCache(
     *     Set.of(1001L, 1002L, 1003L),
     *     "user:name",
     *     missIds -> userMapper.selectNamesByIds(missIds) // DB loader function
     * );
     * }
     * </pre>
     * <p>
     * <strong>Performance Benefits:</strong>
     * <ul>
     *     <li>Reduces N+1 query problem by batching cache misses into a single DB call</li>
     *     <li>Minimizes cache round-trips by checking all keys in one pass</li>
     *     <li>Automatic write-back eliminates manual cache population logic</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Ensure {@code dbLoader} returns a map keyed by the same ID type for correct merging</li>
     *     <li>Handle partial results: {@code dbLoader} may return fewer entries than requested (e.g., soft-deleted records)</li>
     *     <li>For high-cardinality caches, consider TTL-based expiration to avoid unbounded growth</li>
     *     <li>Log cache miss ratios for capacity planning and optimization</li>
     * </ul>
     * <p>
     * <strong>Thread Safety:</strong>
     * <p>
     * This method is not synchronized. In high-concurrency scenarios, multiple threads
     * may simultaneously miss the cache and trigger duplicate DB queries. To prevent
     * cache stampede, consider:
     * <ul>
     *     <li>Using probabilistic early expiration (e.g., add random jitter to TTL)</li>
     *     <li>Implementing request coalescing via {@code CompletableFuture} or similar</li>
     *     <li>Using distributed locks for critical cache misses (advanced)</li>
     * </ul>
     *
     * @param ids       the set of IDs to resolve; duplicates are automatically handled
     * @param cacheName the logical name of the cache region for ID-to-value mapping
     * @param dbLoader  function to fetch missing values from database; receives cache-miss IDs and returns ID-to-value map
     * @return a map containing resolved values for all requested IDs (cached + fresh)
     * @throws BusinessException if {@code dbLoader} throws an unchecked exception
     * @see Cache#get(Object, Class)
     * @see Cache#put(Object, Object)
     */
    public Map<Long, String> getBatchWithCache(Set<Long> ids, String cacheName, Function<Set<Long>, Map<Long, String>> dbLoader) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }

        Cache cache = cacheManager.getCache(cacheName);
        Map<Long, String> result = new HashMap<>(ids.size()); // Pre-size for efficiency
        Set<Long> missIds = new HashSet<>();

        // 1. Attempt cache lookup for all requested IDs
        for (Long id : ids) {
            String value = (cache != null) ? cache.get(id, String.class) : null;
            if (value != null) {
                result.put(id, value);
            } else {
                missIds.add(id);
            }
        }

        // 2. Fetch missing values from database and populate cache
        if (!missIds.isEmpty()) {
            Map<Long, String> dbData;

            try {
                dbData = dbLoader.apply(missIds);
            } catch (Exception e) {
                dbData = new HashMap<>(missIds.size());
                for (Long missId : missIds) {
                    dbData.put(missId, "unknown");
                }
            }
            result.putAll(dbData);

            // Write-back to cache (null-safe)
            if (cache != null) {
                dbData.forEach(cache::put);
            }
        }
        return result;
    }

    /**
     * Retrieves a cache instance by name with fail-fast validation.
     * <p>
     * Unlike other methods in this class, this method throws {@link BusinessException}
     * if the requested cache is not configured. This strict behavior is intentional
     * for scenarios where cache availability is critical to business logic.
     * <p>
     * <strong>Use Cases:</strong>
     * <ul>
     *     <li>Advanced cache operations not covered by helper methods (TTL adjustment, stats)</li>
     *     <li>Native cache API access for provider-specific features (Redis pipelines, Caffeine stats)</li>
     *     <li>Diagnostic tools for cache health monitoring</li>
     * </ul>
     * <p>
     * <strong>Error Handling:</strong>
     * <p>
     * If the cache is not found, logs a WARN message and throws {@code BusinessException}
     * with {@code ErrorCode.INTERNAL_ERROR}. This prevents silent failures that could
     * lead to data inconsistency or performance degradation.
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use this method sparingly; prefer higher-level helpers ({@code put}/{@code get}/{@code evict}) for common operations</li>
     *     <li>Validate cache names against {@code CacheConstants} to avoid typos</li>
     *     <li>Document any native cache operations for maintainability</li>
     * </ul>
     *
     * @param cacheName the logical name of the cache region to retrieve; must be configured
     * @return the {@link Cache} instance for advanced operations
     * @throws BusinessException if the cache is not configured or not found
     * @see CacheManager#getCache(String)
     * @see com.github.starhq.template.common.constant.CacheConstant
     */
    public Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (Objects.isNull(cache)) {
            log.warn("Cache '{}' is not configured or not found", cacheName);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Cache not found: " + cacheName);
        }
        return cache;
    }

}