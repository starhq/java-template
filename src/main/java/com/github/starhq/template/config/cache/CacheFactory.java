package com.github.starhq.template.config.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;

/**
 * Factory class responsible for constructing configured Caffeine cache instances.
 *
 * <p>This factory centralizes the cache building process, ensuring that all caches created
 * in the application share the same baseline configuration (TTL, eviction, monitoring)
 * as defined in the {@link CacheType} enum.
 *
 * @author starhq
 * @see CacheType
 */
@Slf4j
public class CacheFactory {

    /**
     * Builds and initializes a new Caffeine {@link Cache} instance based on the provided type specification.
     *
     * @param cacheType the enum containing the specific configuration (TTL, max size) for this cache
     * @param <K>       the type of cache keys
     * @param <V>       the type of cache values
     * @return a fully configured, ready-to-use Caffeine Cache instance
     */
    public static <K, V> Cache<K, V> build(CacheType cacheType) {
        return Caffeine
                .newBuilder()
                // Configures the Time-To-Live based on the last write operation.
                // Once a key is written or updated, it will be automatically evicted after this duration.
                .expireAfterWrite(cacheType.getTtl())

                // Sets the maximum number of entries the cache can hold.
                // When exceeded, Caffeine uses a Window TinyLFU eviction policy to remove the least frequently used items.
                .maximumSize(cacheType.getMaxSize())

                // ⚠️ WARNING: Wraps cache values in SoftReferences.
                // Allows the JVM Garbage Collector to evict entries under memory pressure to prevent OOM.
                // TRADE-OFF: Prevents application crashes, but makes cache sizes highly unpredictable (the
                // maximumSize becomes a soft limit). Should only be used if the cache is large and memory-sensitive.
                .softValues()

                // Enables Caffeine's internal statistics (hit rate, miss rate, eviction count, load time).
                // Essential for production monitoring and tuning, but adds a tiny synchronization overhead.
                .recordStats()

                // Registers a listener invoked synchronously when an entry is removed for any reason.
                // WARNING: This executes in the same thread as the cache operation. Keep logic extremely fast!
                .removalListener((key, value, cause) -> {
                    // Filter to log only active evictions (size-based or time-based), ignoring manual invalidations
                    if (cause.wasEvicted()) {
                        log.debug("Cache evicted: {} -> {}, cause: {}", key, value, cause);
                    }
                })
                .build();
    }
}