package com.github.starhq.template.event;

import java.util.Collection;

/**
 * Application event for batch cache invalidation across multiple cache regions.
 * <p>
 * This record encapsulates a cache eviction request that can be published via
 * Spring's {@link org.springframework.context.ApplicationEventPublisher} to
 * trigger coordinated cache clearing across local (Caffeine) and distributed
 * (Redis) cache layers. Designed for eventual consistency in multi-tier caching
 * architectures.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Data Update Propagation</strong>: Invalidate cached entities after database modifications</li>
 *     <li><strong>Bulk Operations</strong>: Clear multiple cache entries after batch insert/update/delete</li>
 *     <li><strong>Cross-Service Sync</strong>: Propagate cache invalidation across microservice boundaries via messaging</li>
 *     <li><strong>Admin Operations</strong>: Force cache refresh after configuration changes or data imports</li>
 * </ul>
 * <p>
 * <strong>Generic Type Parameter:</strong>
 * <p>
 * The {@code <ID>} type parameter represents the cache key type, typically:
 * <ul>
 *     <li>{@link Long} for numeric entity IDs (e.g., {@code user:123})</li>
 *     <li>{@link String} for composite or natural keys (e.g., {@code "dict:user_status"})</li>
 *     <li>Custom serializable types for complex cache key objects</li>
 * </ul>
 * Ensure the chosen type implements proper {@code equals()} and {@code hashCode()}
 * for reliable cache key matching.
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * // Publish event after updating user profile
 * @Service
 * public class UserService {
 *     @Autowired private ApplicationEventPublisher eventPublisher;
 *
 *     @Transactional
 *     public void updateUserProfile(Long userId, UserDTO dto) {
 *         // 1. Update database
 *         userMapper.updateById(converter.toEntity(dto));
 *
 *         // 2. Publish cache eviction event
 *         eventPublisher.publishEvent(new CacheEvictEvent<>(
 *             List.of(userId),                    // keys to evict
 *             List.of("user:simple", "user:full") // cache names to clear
 *         ));
 *     }
 * }
 *
 * // Listen and process eviction request
 * @Component
 * public class CacheEvictionListener {
 *     @Autowired private CacheManager cacheManager;
 *
 *     @EventListener
 *     @Async // Process asynchronously to avoid blocking business logic
 *     public void handleCacheEvict(CacheEvictEvent<Long> event) {
 *         for (String cacheName : event.cacheNames()) {
 *             Cache cache = cacheManager.getCache(cacheName);
 *             if (cache != null) {
 *                 for (Long key : event.keys()) {
 *                     cache.evict(key); // Clear specific key
 *                 }
 *             }
 *         }
 *     }
 * }
 * }
 * </pre>
 *
 * @param <ID> the type of cache keys to be evicted, must be serializable for distributed caching
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-29
 * @see org.springframework.context.ApplicationEvent
 * @see org.springframework.cache.CacheManager
 * @see java.util.Collection
 */
public record CacheEvictEvent<T>(
        Collection<T> keys,
        Collection<String> cacheNames
) {
}