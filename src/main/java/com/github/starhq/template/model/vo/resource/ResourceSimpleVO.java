package com.github.starhq.template.model.vo.resource;

import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.model.vo.BaseIdVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * Lightweight view object for API resource metadata in dropdowns, selectors, and internal service communication.
 * <p>
 * This class extends {@link BaseIdVO} to inherit the resource's unique identifier
 * and provides minimal fields required for resource reference and display. Designed
 * for scenarios where full resource details are unnecessary, such as:
 * <ul>
 *     <li><strong>UI Components</strong>: Populating dropdowns, multi-selects, or permission lists with resource options</li>
 *     <li><strong>Permission Hints</strong>: Providing safe metadata for frontend route guards without exposing audit fields</li>
 *     <li><strong>Internal Service Communication</strong>: Passing resource references between microservices for distributed auth</li>
 *     <li><strong>Cache Optimization</strong>: Storing resource metadata in Redis/Caffeine with minimal memory footprint</li>
 * </ul>
 * <p>
 * <strong>HTTP Methods Handling:</strong>
 * <p>
 * The {@code methods} field uses {@link HttpMethod} enum to represent allowed HTTP verbs
 * for this API resource. Jackson automatically serializes enum values to their {@code name()}
 * (e.g., {@code "GET"}, {@code "POST"}), enabling type-safe frontend handling:
 * <pre>
 * {@code
 * // JSON output example
 * {
 *   "id": 1001,
 *   "name": "Create User API",
 *   "url": "/api/v1/users",
 *   "methods": ["POST"],
 *   "description": "Endpoint for user registration"
 * }
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Minimalism</strong>: Only includes fields essential for resource identification and display (id, name, url, methods, description)</li>
 *     <li><strong>Immutability-Friendly</strong>: Stateless VO suitable for caching and concurrent access</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for cross-JVM compatibility</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond enum; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-01
 * @see BaseIdVO
 * @see HttpMethod
 * @see com.github.starhq.template.entity.SysResource
 * @see com.github.starhq.template.service.ResourceService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResourceSimpleVO extends BaseIdVO {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -6544229209011213164L;

    /**
     * The human-readable display name of the API resource for UI presentation.
     * <p>
     * This field is used in dropdowns, selectors, tooltips, and permission lists to help
     * users identify the purpose of each API resource. Examples:
     * <ul>
     *     <li>{@code "Create User API"} — Endpoint for user creation</li>
     *     <li>{@code "Export Data API"} — Endpoint for data export functionality</li>
     *     <li>{@code "Delete Order API"} — Endpoint for order deletion operation</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 50 characters) for consistent UI layout</li>
     *     <li>Include HTTP method prefix for clarity: {@code "[POST] Create User"}</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "resource.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Populate a select dropdown
     * <a-select v-model="form.resourceId" :options="resourceOptions">
     *   <a-select-option v-for="res in resourceOptions" :key="res.id" :value="res.id">
     *     {{ res.name }} <small class="text-gray-500">({{ res.url }})</small>
     *   </a-select-option>
     * </a-select>
     *
     * // React: Render resource list with permission hints
     * {resources.map(res => (
     *   <Button key={res.id} disabled={!hasPerm(res.code)}>
     *     {res.name}
     *   </Button>
     * ))}
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysResource#getName()
     */
    private String name;

    /**
     * The URL pattern used to match incoming HTTP requests for this API resource.
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
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Backend: Check if request matches resource pattern
     * boolean matches = PathMatcherUtils.matches(resource.getUrl(), request.getRequestURI());
     *
     * // Frontend: Show URL with copy-to-clipboard feature
     * <code class="text-xs bg-gray-100 px-2 py-1 rounded">{{ resource.url }}</code>
     * <a-button size="small" @click="copyToClipboard(resource.url)">Copy</a-button>
     * }
     * </pre>
     *
     * @see com.github.starhq.template.entity.SysResource#getUrl()
     * @see org.springframework.util.AntPathMatcher
     */
    private String url;

    /**
     * The list of allowed HTTP methods for this API resource.
     * <p>
     * Uses the {@link HttpMethod} enum to ensure type safety and enable efficient
     * permission checks without string matching. Common values include:
     * <ul>
     *     <li>{@code GET} — Read-only operations (fetch data)</li>
     *     <li>{@code POST} — Create operations (submit new data)</li>
     *     <li>{@code PUT} — Full update operations (replace entire resource)</li>
     *     <li>{@code PATCH} — Partial update operations (modify specific fields)</li>
     *     <li>{@code DELETE} — Delete operations (remove resource)</li>
     * </ul>
     * <p>
     * <strong>Serialization Behavior:</strong>
     * <p>
     * Jackson automatically serializes enum values to their {@code name()} (uppercase string):
     * <pre>
     * {@code
     * // Java: List<HttpMethod> methods = List.of(HttpMethod.GET, HttpMethod.POST);
     * // JSON: { "methods": ["GET", "POST"] }
     * }
     * </pre>
     * <p>
     * <strong>Permission Check Strategy:</strong>
     * <pre>
     * {@code
     * // Backend: Check if request method is allowed for resource
     * boolean isAllowed = resource.getMethods().contains(HttpMethod.valueOf(request.getMethod()));
     *
     * // Frontend: Display method badges
     * <a-space>
     *   <a-tag v-for="method in resource.methods" :key="method" :color="getMethodColor(method)">
     *     {{ method }}
     *   </a-tag>
     * </a-space>
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Use enum constants for method checks to avoid typos: {@code HttpMethod.GET} not {@code "get"}</li>
     *     <li>Document allowed methods in API documentation for consumer clarity</li>
     *     <li>Consider method-level auditing: log which HTTP methods were used for each resource access</li>
     * </ul>
     *
     * @see HttpMethod
     * @see com.github.starhq.template.entity.SysResource#getMethods()
     */
    private List<HttpMethod> methods;

    /**
     * Optional explanatory text describing the purpose and usage rules of this API resource.
     * <p>
     * Useful for:
     * <ul>
     *     <li>Admin console tooltips explaining what the API endpoint controls</li>
     *     <li>Documenting deprecated endpoints or migration notes</li>
     *     <li>Clarifying complex business rules (e.g., {@code "Requires admin role and MFA"})</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Avoid exposing internal implementation details or sensitive business logic</li>
     *     <li>Keep under 255 characters for optimal storage and UI rendering</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Strategy:</strong>
     * <ul>
     *     <li>Show as tooltip on hover: {@code <a-tooltip :title="item.description">}</li>
     *     <li>Truncate long descriptions with ellipsis for dropdown layout</li>
     *     <li>Support markdown formatting if rich text descriptions are enabled</li>
     * </ul>
     * <p>
     * <strong>Storage Recommendation:</strong>
     * <ul>
     *     <li>Database column: {@code VARCHAR(255)} for standard descriptions</li>
     *     <li>Consider {@code TEXT} type if detailed documentation or markdown formatting is required</li>
     *     <li>Add full-text index if descriptions are frequently searched in admin UI</li>
     * </ul>
     *
     * @see com.github.starhq.template.entity.SysResource#getDescription()
     */
    private String description;

}