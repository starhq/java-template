package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing a protected API resource/endpoint for access control.
 * <p>
 * This class maps to the {@code sys_resource} table and extends {@link BaseEntity}
 * to provide full audit trail. Resources define the fine-grained permissions required
 * to access specific API endpoints, typically evaluated by security interceptors
 * or Spring Security filters during request processing.
 * <p>
 * <strong>Permission Matching Model:</strong>
 * <ul>
 *     <li><strong>URL Pattern</strong>: Matches request paths using Ant-style ({@code /api/**}) or exact matching</li>
 *     <li><strong>HTTP Methods</strong>: Encoded as a bitmask in the {@code methods} field to efficiently store
 *         multiple allowed verbs (GET, POST, PUT, DELETE, etc.) in a single integer column</li>
 *     <li><strong>Role Binding</strong>: Resources are assigned to roles; users inherit access via role membership</li>
 * </ul>
 * <p>
 * <strong>Caching & Performance:</strong>
 * <p>
 * Resource definitions are evaluated on every protected request. Always preload and cache
 * the full resource-permission matrix (URL + methods + roles) in memory or Redis to achieve
 * O(1) permission checks without database round-trips.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseEntity
 * @see com.github.starhq.template.common.enums.HttpMethod
 * @see com.github.starhq.template.converter.ResourceConverter
 * @see TableName
 */
@Data
@Alias("resource")
@TableName("sys_resource")
@EqualsAndHashCode(callSuper = false)
public class SysResource extends BaseEntity {

    /**
     * The human-readable identifier for this API resource.
     * <p>
     * Typically follows the pattern {@code module:resource} or {@code feature:action}
     * for administrative clarity (e.g., {@code "user:profile"}, {@code "order:export"}).
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 50 characters) and consistent across environments</li>
     *     <li>Use {@code UPPER_SNAKE_CASE} or {@code camelCase} for readability</li>
     *     <li>Avoid technical jargon; target security auditors and system administrators</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotBlank} in DTO layer</li>
     *     <li>Uniqueness: Recommended globally unique to prevent permission ambiguity</li>
     *     <li>Pattern: {@code @Pattern(regexp = "^[\\w:-]+$")} for safe identifier format</li>
     * </ul>
     */
    private String name;

    /**
     * The API path pattern used to match incoming HTTP requests.
     * <p>
     * Supports matching strategies:
     * <ul>
     *     <li><strong>Exact Match</strong>: {@code /api/v1/users/profile}</li>
     *     <li><strong>Ant-style Wildcards</strong>: {@code /api/v1/users/**} (matches all sub-paths)</li>
     *     <li><strong>Path Variables</strong>: {@code /api/v1/orders/{id}} (normalized to base pattern)</li>
     * </ul>
     * <p>
     * <strong>Security Note:</strong>
     * <ul>
     *     <li>Never include query parameters, tokens, or sensitive data in the URL pattern</li>
     *     <li>Validate against a strict whitelist to prevent path traversal or injection attacks</li>
     *     <li>Use {@code AntPathMatcher} or equivalent for consistent matching behavior</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Nullability: {@code NOT NULL} — every resource must define a target path</li>
     *     <li>Index Recommendation: {@code CREATE INDEX idx_url ON sys_resource(url)} for pattern lookup</li>
     *     <li>Length: {@code @Size(max = 255)} to accommodate complex path patterns</li>
     * </ul>
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
     * Example: methods = 7 (0b0111) → Allows GET | POST | PUT
     * Example: methods = 9 (0b1001) → Allows GET | DELETE
     * }
     * </pre>
     * <p>
     * <strong>Performance Benefit:</strong>
     * <ul>
     *     <li>Reduces database storage from multiple rows/joins to a single integer column</li>
     *     <li>Enables O(1) permission checks via bitwise AND: {@code (allowedMethods & requestMethod) != 0}</li>
     *     <li>Avoids N+1 query problems when loading role-permission mappings</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link Integer} — supports up to 31 distinct HTTP methods</li>
     *     <li>Default: {@code 1} (GET only) or {@code 0} (all methods, depending on security policy)</li>
     *     <li>Encoding/Decoding: Handled by {@link com.github.starhq.template.converter.ResourceConverter}
     *         via {@code methodsToMask()} and {@code maskToMethods()} named mappings</li>
     * </ul>
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>Required: {@code @NotNull}</li>
     *     <li>Range: {@code @Min(1)} — at least one HTTP method must be allowed</li>
     * </ul>
     *
     * @see com.github.starhq.template.common.enums.HttpMethod
     * @see com.github.starhq.template.converter.ResourceConverter
     */
    private Integer methods;

    /**
     * Optional explanatory text detailing the business purpose, access level,
     * or maintenance notes for this API resource.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what data or action this endpoint exposes</li>
     *     <li>Documenting deprecated endpoints or migration paths</li>
     *     <li>Clarifying rate limits, audit requirements, or compliance constraints</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in imperative mood targeting security administrators and auditors</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     *     <li>Avoid exposing internal implementation details, stack traces, or credentials</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong>
     * <ul>
     *     <li>Use {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} if detailed API documentation or markdown formatting is required</li>
     * </ul>
     */
    private String description;

}