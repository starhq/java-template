package com.github.starhq.template.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Lightweight data transfer object for API resource metadata in permission checks and caching.
 * <p>
 * This class encapsulates the minimal information required to evaluate API access permissions:
 * the resource URL pattern and allowed HTTP methods (encoded as a bitmask). It is designed for
 * high-frequency, low-latency scenarios such as:
 * <ul>
 *     <li><strong>Gateway Authorization</strong>: Fast URL + method matching without loading full entity</li>
 *     <li><strong>Permission Cache</strong>: Store resource definitions in Redis/Caffeine with minimal memory footprint</li>
 *     <li><strong>Internal Service Communication</strong>: Pass resource references between microservices for distributed auth</li>
 *     <li><strong>Client-side Permission Hints</strong>: Provide safe metadata for frontend route guards (without exposing sensitive details)</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for permission evaluation (url + method bitmask)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless DTO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations; usable in any Java context</li>
 * </ul>
 * <p>
 * <strong>HTTP Method Bitmask Strategy:</strong>
 * <p>
 * The {@code method} field uses bitwise encoding to efficiently store multiple HTTP verbs in a single integer:
 * <pre>
 * {@code
 * // HttpMethod enum flag definitions (example)
 * GET    = 0b0001 (1)
 * POST   = 0b0010 (2)
 * PUT    = 0b0100 (4)
 * DELETE = 0b1000 (8)
 *
 * // Example: Allow GET + POST + PUT
 * int mask = GET | POST | PUT; // 0b0111 = 7
 *
 * // Permission check: is DELETE allowed?
 * boolean canDelete = (mask & DELETE) != 0; // false
 * }
 * </pre>
 * This approach reduces memory usage, enables O(1) permission checks via bitwise AND, and simplifies cache key design.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see com.github.starhq.template.common.enums.HttpMethod
 * @see com.github.starhq.template.converter.ResourceConverter
 * @see java.io.Serializable
 */
@Data
public class ResourceSimpleDTO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries (e.g., via RPC or messaging).
     * Update this value only if the class structure changes in a
     * backward-incompatible way (e.g., removing fields, changing types).
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 662183419069882855L;

    /**
     * The URL pattern used to match incoming HTTP requests for permission evaluation.
     * <p>
     * Supports matching strategies:
     * <ul>
     *     <li><strong>Exact Match</strong>: {@code /api/v1/users/profile} matches only that exact path</li>
     *     <li><strong>Ant-style Wildcards</strong>: {@code /api/v1/users/**} matches all sub-paths (e.g., {@code /api/v1/users/123})</li>
     *     <li><strong>Path Variables</strong>: {@code /api/v1/orders/{id}} should be normalized to base pattern for matching</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link String} — case-sensitive matching recommended for URL paths</li>
     *     <li>Nullability: Should not be {@code null}; empty string may represent catch-all (use with caution)</li>
     *     <li>Length: Typically ≤ 255 characters; validate to prevent excessive pattern complexity</li>
     * </ul>
     * <p>
     * <strong>Security Notes:</strong>
     * <ul>
     *     <li><strong>Never include query parameters</strong> (e.g., {@code ?token=xxx}) in the pattern — match path only</li>
     *     <li><strong>Validate patterns</strong> against a whitelist to prevent path traversal or injection attacks</li>
     *     <li><strong>Use {@code AntPathMatcher}</strong> or equivalent for consistent, secure pattern matching</li>
     *     <li><strong>Log pattern matches</strong> at DEBUG level for auditability without performance impact</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Gateway filter: check if request matches resource
     * AntPathMatcher pathMatcher = new AntPathMatcher();
     * boolean urlMatches = pathMatcher.match(resource.getUrl(), request.getRequestURI());
     *
     * // Combined with method check
     * boolean methodAllowed = HttpMethodBitmaskUtils.isAllowed(resource.getMethod(), requestMethodFlag);
     *
     * if (urlMatches && methodAllowed) {
     *     // Permission granted
     * }
     * }
     * </pre>
     *
     * @see org.springframework.util.AntPathMatcher
     */
    private String url;

    /**
     * Bitmask integer encoding the allowed HTTP methods for this resource.
     * <p>
     * Uses bitwise OR operations to store multiple verbs compactly:
     * <pre>
     * {@code
     * GET(1)    = 0b0001
     * POST(2)   = 0b0010
     * PUT(4)    = 0b0100
     * DELETE(8) = 0b1000
     *
     * Example: method = 7 (0b0111) → Allows GET | POST | PUT
     * Example: method = 9 (0b1001) → Allows GET | DELETE
     * }
     * </pre>
     * <p>
     * <strong>Performance Benefit:</strong>
     * <ul>
     *     <li>Reduces memory footprint vs. storing {@code List<HttpMethod>} or {@code Set<String>}</li>
     *     <li>Enables O(1) permission checks via bitwise AND: {@code (allowedMask & requestMethod) != 0}</li>
     *     <li>Simplifies cache key design and serialization for distributed systems</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Integer} — supports up to 31 distinct HTTP methods (32-bit signed int)</li>
     *     <li>Default: {@code 0} or {@code 1} depending on security policy (deny-all vs. GET-only default)</li>
     *     <li>Encoding/Decoding: Handled by {@link com.github.starhq.template.converter.ResourceConverter}
     *         via {@code methodsToMask()} and {@code maskToMethods()} named mappings</li>
     * </ul>
     * <p>
     * <strong>Permission Check Example:</strong>
     * <pre>
     * {@code
     * // Utility method for bitmask check
     * public static boolean isAllowed(Integer allowedMask, int requestMethodFlag) {
     *     return allowedMask != null && (allowedMask & requestMethodFlag) != 0;
     * }
     *
     * // Usage in filter
     * int requestFlag = HttpMethodUtils.toFlag(request.getMethod()); // e.g., GET → 1
     * if (isAllowed(resource.getMethod(), requestFlag)) {
     *     // Method allowed for this URL
     * }
     * }
     * </pre>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Should not be {@code null} — at least one HTTP method must be allowed</li>
     *     <li>Range: {@code >= 1} — bitmask zero means "no methods allowed" (deny-all)</li>
     *     <li>Sanity check: Ensure only valid HttpMethod flags are combined (prevent arbitrary bit setting)</li>
     * </ul>
     *
     * @see com.github.starhq.template.common.enums.HttpMethod
     */
    private Integer method;

}