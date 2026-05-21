package com.github.starhq.template.config.cache;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Enumeration defining configurations for all application-level caches.
 *
 * <p>This acts as the single source of truth for cache properties, ensuring that the
 * cache names used in {@code @Cacheable} annotations perfectly match the actual configurations
 * (TTL, maximum size) defined in the underlying cache provider (e.g., Caffeine or Redis).
 *
 * <p><b>Design Note:</b> Hard limits ({@code maxSize}) are intentionally set to prevent
 * unbounded memory growth (OOM) in case of unexpected traffic spikes or cache penetration.
 * When the limit is reached, the cache naturally evicts the least recently used (LRU) entries.
 *
 * @author starhq
 */
@Getter
@AllArgsConstructor
public enum CacheType {

    /**
     * System dictionary types (e.g., gender, status codes).
     * Long TTL because dictionary data rarely changes once deployed.
     */
    DICT_TYPE("dictTypes", Duration.ofHours(24), 500),

    /**
     * User basic information profiles.
     * Moderate TTL to balance performance with the need to reflect account status changes
     * (e.g., disabling a user) within a reasonable timeframe.
     */
    USER("users", Duration.ofMinutes(30), 100_000),

    /**
     * Menu tree structures.
     * Moderate TTL. Changes require an admin to clear the cache manually or restart.
     */
    MENU("menus", Duration.ofHours(2), 50_000),

    /**
     * Button-level permission definitions.
     * Moderate TTL. Tied to menu/role assignments.
     */
    BUTTON("buttons", Duration.ofHours(2), 50_000),

    /**
     * API resource definitions (URL to HTTP method mappings).
     * Moderate TTL.
     */
    RESOURCE("resources", Duration.ofHours(2), 50_000),

    /**
     * Generated captcha text/base64 images.
     * Short TTL because captchas expire quickly by design.
     */
    CAPTCHA("captchas", Duration.ofMinutes(5), 2_000),

    /**
     * Client IP addresses requesting captchas.
     * Very short TTL to enforce rate limiting (e.g., max 1 request per minute per IP).
     */
    CAPTCHA_IP("ips", Duration.ofSeconds(60), 1_000),

    /**
     * Client IP addresses that have successfully verified a captcha.
     * Short TTL to limit the time window between solving the captcha and submitting the login form.
     */
    CAPTCHA_VERIFY("verify_ips", Duration.ofMinutes(15), 1_000),

    /**
     * Active user session tokens (e.g., JWT mapping to user details).
     * Moderate TTL. Should ideally match the JWT access token expiration time.
     */
    TOKEN("tokens", Duration.ofMinutes(30), 50_000),

    /**
     * Aggregated user permission strings (e.g., "sys:user:add,sys:user:query").
     * <p><b>Note:</b> This cache is strictly read-only. It MUST be explicitly evicted when
     * an admin modifies user-role or role-menu assignments.
     */
    PERMISSION("permissions", Duration.ofMinutes(30), 20_000),

    /**
     * Mapping cache strictly used for translating database User IDs to usernames.
     * Useful for preventing N+1 queries in audit logs or list views.
     */
    ADMIN_NAME("adminNames", Duration.ofHours(2), 1_000);

    /**
     * The unique identifier used as the cache namespace (e.g., in {@code @Cacheable("users")}).
     */
    private final String cacheName;

    /**
     * Time To Live. How long an entry is allowed to live in the cache before it is automatically expired.
     */
    private final Duration ttl;

    /**
     * The absolute maximum number of entries this specific cache namespace is allowed to hold.
     * Prevents OutOfMemoryError during traffic anomalies.
     */
    private final long maxSize;
}