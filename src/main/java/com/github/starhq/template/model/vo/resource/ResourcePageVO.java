package com.github.starhq.template.model.vo.resource;

import com.github.starhq.template.common.enums.HttpMethod;
import com.github.starhq.template.model.vo.BaseAuditVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * View object for paginated API resource responses in admin console or API clients.
 * <p>
 * This class extends {@link BaseAuditVO} to inherit common audit trail fields
 * ({@code createdBy}, {@code createdAt}, {@code updatedBy}, {@code updatedAt})
 * and adds API resource-specific business fields for comprehensive permission management.
 * Designed for rendering resource lists in management interfaces with filtering, sorting, and audit context.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Resource Management</strong>: Display paginated API resources with filtering by name, URL, HTTP methods</li>
 *     <li><strong>Permission Configuration</strong>: List available resources when assigning permissions to roles with method-level granularity</li>
 *     <li><strong>Audit & Reporting</strong>: Track resource creation/modification history via inherited audit fields</li>
 *     <li><strong>Frontend Integration</strong>: Provide structured data for Vue/React table components with sorting/pagination</li>
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
 *     <li><strong>Audit Integration</strong>: Inherits audit fields from {@link BaseAuditVO} for compliance tracking</li>
 *     <li><strong>Type Safety</strong>: Uses {@link HttpMethod} enum instead of raw strings for method validation</li>
 *     <li><strong>Serialization-Ready</strong>: Implements {@link java.io.Serializable} with fixed {@code serialVersionUID} for caching</li>
 *     <li><strong>Framework-Neutral</strong>: No framework-specific annotations beyond enum; usable in any Java context</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-01
 * @see BaseAuditVO
 * @see HttpMethod
 * @see com.github.starhq.template.entity.SysResource
 * @see com.github.starhq.template.service.ResourceService
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ResourcePageVO extends BaseAuditVO {

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
    private static final long serialVersionUID = 3597420852784856922L;

    /**
     * The human-readable display name of the API resource for UI presentation.
     * <p>
     * This field is shown in resource management tables and permission dialogs
     * to help administrators identify the purpose of each API endpoint. Examples:
     * <ul>
     *     <li>{@code "Create User API"} — Endpoint for user creation</li>
     *     <li>{@code "Export Data API"} — Endpoint for data export functionality</li>
     *     <li>{@code "Delete Order API"} — Endpoint for order deletion operation</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 50 characters) for consistent table layout</li>
     *     <li>Include HTTP method prefix for clarity: {@code "[POST] Create User"}</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "resource.user.create"})
     *         and resolving translations at the frontend layer</li>
     *     <li>Avoid HTML tags or special characters to prevent XSS rendering issues</li>
     * </ul>
     * <p>
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3 table column
     * <a-table-column title="Resource Name" data-index="name" width="200px">
     *   <template #bodyCell="{ text }">
     *     <a-tag color="blue">{{ text }}</a-tag>
     *   </template>
     * </a-table-column>
     *
     * // React: Table cell rendering
     * <Tag color="blue">{record.name}</Tag>
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
     * <strong>Frontend Display Example:</strong>
     * <pre>
     * {@code
     * // Vue 3: Show URL with copy-to-clipboard feature
     * <a-table-column title="URL" data-index="url">
     *   <template #bodyCell="{ text }">
     *     <a-space>
     *       <code class="text-xs bg-gray-100 px-2 py-1 rounded">{{ text }}</code>
     *       <a-tooltip title="Copy to clipboard">
     *         <a-button size="small" type="text" @click="copyToClipboard(text)">
     *           <a-icon type="copy" />
     *         </a-button>
     *       </a-tooltip>
     *     </a-space>
     *   </template>
     * </a-table-column>
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
     *     <li>Show as tooltip on hover: {@code <a-tooltip :title="record.description">}</li>
     *     <li>Truncate long descriptions with ellipsis for table layout</li>
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