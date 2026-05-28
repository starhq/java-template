package com.github.starhq.template.event.listener;

import com.github.starhq.template.event.CacheEvictEvent;
import com.github.starhq.template.helper.CacheHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

/**
 * Asynchronous transactional event listener for batch cache invalidation.
 * <p>
 * This component subscribes to {@link CacheEvictEvent} published by business services,
 * and invalidates cached entries across multiple cache regions <strong>after the
 * originating transaction commits</strong>. By using {@link TransactionalEventListener}
 * with default {@link TransactionPhase#AFTER_COMMIT} phase, it ensures:
 * <ul>
 *     <li><strong>Tx Boundary Safety</strong>: Cache is only cleared for successfully committed operations</li>
 *     <li><strong>Non-blocking Design</strong>: Async execution decouples cache eviction from business response latency</li>
 *     <li><strong>Batch Efficiency</strong>: Single event can invalidate multiple keys across multiple cache regions</li>
 *     <li><strong>Failure Isolation</strong>: Cache eviction errors do not rollback or affect core business transactions</li>
 * </ul>
 * <p>
 * <strong>Processing Flow:</strong>
 * <ol>
 *     <li>Business service updates data and publishes {@code CacheEvictEvent} (within transaction)</li>
 *     <li>Spring transaction manager commits the business transaction</li>
 *     <li>This listener receives the event asynchronously via dedicated thread pool</li>
 *     <li>{@link CacheHelper#evict(Collection, Collection)} invalidates specified keys in target caches</li>
 *     <li>Failures are logged with DEBUG/WARN level for observability without propagation</li>
 * </ol>
 * <p>
 * <strong>Thread Safety & Performance:</strong>
 * <ul>
 *     <li>Annotated with {@code @Async} to execute in separate thread pool (configure via {@code cacheEvictExecutor})</li>
 *     <li>Stateless design: safe for concurrent event processing without synchronization</li>
 *     <li>Exception isolation: caught exceptions never propagate to avoid blocking async thread pool</li>
 * </ul>
 * <p>
 * <strong>Cache Naming Convention:</strong>
 * <p>
 * Cache names should follow a consistent pattern for easy management:
 * <ul>
 *     <li>{@code "user:simple"} — Lightweight user profile cache</li>
 *     <li>{@code "user:full"} — Complete user entity cache</li>
 *     <li>{@code "dict:all"} — Full dictionary data cache</li>
 *     <li>{@code "role:permissions"} — Role-based permission matrix</li>
 *     <li>{@code "config:global"} — System-wide configuration cache</li>
 * </ul>
 * Use constants in {@code CacheConstants} class to avoid typos and enable IDE auto-completion.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-29
 * @see CacheEvictEvent
 * @see CacheHelper
 * @see TransactionalEventListener
 * @see org.springframework.scheduling.annotation.Async
 */
@Slf4j
@RequiredArgsConstructor
public class CacheEvictListener {

    private final CacheHelper cacheHelper;

    /**
     * Handles {@link CacheEvictEvent} asynchronously after transaction commit to invalidate caches.
     * <p>
     * This method is invoked by Spring's transactional event multicaster <strong>after the
     * originating transaction successfully commits</strong>. Execution is delegated to a
     * dedicated async thread pool to avoid blocking business threads.
     * <p>
     * <strong>Processing Steps:</strong>
     * <ol>
     *     <li>Null-check: Skip processing if event is null (defensive programming)</li>
     *     <li>Validation: Skip if {@code cacheNames} or {@code keys} collections are empty</li>
     *     <li>Logging: Record received event at DEBUG level for auditability</li>
     *     <li>Eviction: Delegate to {@link CacheHelper#evict(Collection, Collection)} for batch invalidation</li>
     *     <li>Error Handling: Any exceptions from {@code CacheHelper} are logged but not propagated</li>
     * </ol>
     * <p>
     * <strong>Async Configuration:</strong>
     * <p>
     * Ensure a thread pool bean named {@code cacheEvictExecutor} is configured:
     * <pre>
     * {@code
     * @Bean("cacheEvictExecutor")
     * public Executor cacheEvictExecutor() {
     *     ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
     *     executor.setCorePoolSize(4); // Cache eviction is lower priority than business logic
     *     executor.setMaxPoolSize(16);
     *     executor.setQueueCapacity(2000); // Accommodate burst events
     *     executor.setThreadNamePrefix("cache-evict-async-");
     *     executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
     *     executor.initialize();
     *     return executor;
     * }
     * }
     * </pre>
     * <p>
     * <strong>Transaction Phase:</strong>
     * <p>
     * By default, {@code @TransactionalEventListener} uses {@link TransactionPhase#AFTER_COMMIT}.
     * This ensures cache is only invalidated for successfully committed operations.
     * If different behavior is needed (e.g., invalidate before commit for pessimistic caching),
     * specify explicitly:
     * <pre>
     * {@code
     * @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep key collections small (< 1000 items) to avoid event payload bloat and long eviction times</li>
     *     <li>For large-scale invalidation, consider pattern-based eviction (e.g., {@code "user:*"}) via custom cache operations</li>
     *     <li>Use cache name constants to avoid typos: {@code public static final String USER_SIMPLE = "user:simple";}</li>
     *     <li>Log eviction events at DEBUG level in production to avoid log volume explosion</li>
     * </ul>
     *
     * @param event the {@link CacheEvictEvent} containing keys and cache names to evict, may be {@code null}
     * @see CacheEvictEvent#keys()
     * @see CacheEvictEvent#cacheNames()
     * @see CacheHelper#evict(Collection, Collection)
     * @see Async
     * @see TransactionalEventListener
     */
    @Async
    @TransactionalEventListener
    public void handleEvictEvent(CacheEvictEvent<?> event) {
        // Defensive null check: skip if event is null (should not happen with proper publishing)
        if (event == null) {
            return;
        }

        Collection<String> cacheNames = event.cacheNames();
        // Skip if no target caches specified
        if (CollectionUtils.isEmpty(cacheNames)) {
            return;
        }

        Collection<?> keys = event.keys();
        // Skip if no keys to evict
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }

        // Log at DEBUG level to avoid production log noise; enable via logging config
        log.debug("Received cache evict event for keys: {}, caches: {}", keys, cacheNames);

        try {
            // Delegate to CacheHelper for batch invalidation across multiple cache regions
            // CacheHelper should handle both local (Caffeine) and distributed (Redis) caches
            cacheHelper.evict(keys, cacheNames);
        } catch (Exception e) {
            // Log failure with context but do not re-throw to avoid blocking async pool
            // Include cache names for correlation
            log.warn("Failed to evict cache for keys: {}, caches: {}, error: {}",
                    keys, cacheNames, e.getMessage(), e);
        }
    }
}
