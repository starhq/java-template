package com.github.starhq.template.model.dto;

import com.github.starhq.template.common.enums.HttpMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * Data Transfer Object for creating or updating API resource definitions in the RBAC system.
 * <p>
 * This class encapsulates user-submitted form data for API resource management operations,
 * with built-in validation constraints to ensure data integrity before business processing.
 * It is typically used in:
 * <ul>
 *     <li><strong>Admin Console</strong>: API resource creation/edit forms in permission configuration</li>
 *     <li><strong>API Endpoints</strong>: {@code POST /api/resources} and {@code PUT /api/resources/{id}} request bodies</li>
 *     <li><strong>Service Layer</strong>: Type-safe parameter passing with compile-time validation hints</li>
 * </ul>
 * <p>
 * <strong>Validation Strategy:</strong>
 * <p>
 * All constraints use internationalized message keys (e.g., {@code "{error.param.blank}"})
 * configured in {@code ValidationMessages.properties} for multi-language support.
 * Validation is triggered automatically by Spring's {@code @Valid} annotation in controllers:
 * <pre>
 * {@code
 * @PostMapping("/resources")
 * public Result<Void> createResource(@Valid @RequestBody ResourceDTO dto) {
 *     // dto is guaranteed to pass validation constraints here
 *     resourceService.create(dto);
 *     return Result.success();
 * }
 * }
 * </pre>
 * <p>
 * <strong>Serialization:</strong>
 * <p>
 * Implements {@link Serializable} with a fixed {@code serialVersionUID} to support:
 * <ul>
 *     <li>Caching DTO instances in distributed caches (Redis)</li>
 *     <li>Transmitting across service boundaries in microservice architectures</li>
 *     <li>Session replication in clustered deployments</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see jakarta.validation.Valid
 * @see com.github.starhq.template.service.ResourceService
 * @see <a href="https://beanvalidation.org/2.0/">Jakarta Bean Validation 2.0 Specification</a>
 */
@Data
public class ResourceDTO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = -4807247444100943891L;

    /**
     * The URL path pattern for the API resource.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=5, max=100)}: Length must be between 5 and 100 characters (inclusive)</li>
     *     <li>Uniqueness: Must be globally unique across all resources (enforced at service/database layer)</li>
     * </ul>
     * <p>
     * <strong>Format Convention:</strong>
     * <ul>
     *     <li>RESTful path format: {@code "/api/v1/users"}, {@code "/api/v1/orders/{id}"}</li>
     *     <li>May include path parameters: {@code "/api/v1/users/{userId}/posts"}</li>
     *     <li>Use lowercase letters, numbers, slashes, and curly braces for parameters</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Validate URL patterns to prevent injection attacks</li>
     *     <li>Avoid exposing sensitive information in URL paths (e.g., {@code /api/users/123/password})</li>
     *     <li>Consider implementing Content Security Policy (CSP) headers for API responses</li>
     * </ul>
     * <p>
     * <strong>Spring MVC Integration:</strong>
     * <p>
     * This value is typically matched against controller request mappings:
     * <pre>
     * {@code
     * @RestController
     * @RequestMapping("/api/v1/users")
     * public class UserController {
     *     @GetMapping
     *     public List<User> listUsers() { ... }
     *
     *     @PostMapping
     *     public User createUser(@RequestBody User user) { ... }
     * }
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 5, max = 100, message = "{error.param.range}")
    private String url;

    /**
     * The human-readable display name of the API resource for UI presentation.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotBlank}: Must not be {@code null} or empty/whitespace-only string</li>
     *     <li>{@code @Size(min=4, max=30)}: Length must be between 4 and 30 characters (inclusive)</li>
     *     <li>Message Key: {@code "{error.param.blank}"} / {@code "{error.param.range}"} for i18n support</li>
     * </ul>
     * <p>
     * <strong>Business Guidelines:</strong>
     * <ul>
     *     <li>Keep names concise (≤ 15 characters recommended) for consistent UI layout</li>
     *     <li>Use title case: {@code "User Management API"}, {@code "Order Processing"}</li>
     *     <li>Avoid special characters or HTML tags to prevent XSS rendering issues</li>
     *     <li>For multi-language systems, consider storing i18n keys (e.g., {@code "resource.user.api"}) instead</li>
     * </ul>
     * <p>
     * <strong>Frontend Integration:</strong>
     * <p>
     * This value is typically bound to form input fields with real-time validation:
     * <pre>
     * {@code
     * <!-- Vue 3 + Element Plus example -->
     * <el-form-item label="Resource Name" prop="name">
     *   <el-input v-model="form.name" :maxlength="30" :minlength="4" show-word-limit />
     * </el-form-item>
     * }
     * </pre>
     */
    @NotBlank(message = "{error.param.blank}")
    @Size(min = 4, max = 30, message = "{error.param.range}")
    private String name;

    /**
     * The list of HTTP methods allowed for this API resource.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @NotEmpty}: Must not be {@code null} or empty; at least one HTTP method required</li>
     *     <li>Valid values: {@link HttpMethod#GET}, {@link HttpMethod#POST}, {@link HttpMethod#PUT}, {@link HttpMethod#DELETE}, {@link HttpMethod#PATCH}</li>
     *     <li>Uniqueness: Each HTTP method should be defined only once per URL pattern</li>
     * </ul>
     * <p>
     * <strong>HTTP Method Semantics:</strong>
     * <ul>
     *     <li>{@code GET}: Retrieve resource(s); idempotent and safe</li>
     *     <li>{@code POST}: Create new resource; not idempotent</li>
     *     <li>{@code PUT}: Update existing resource; idempotent</li>
     *     <li>{@code DELETE}: Remove resource; idempotent</li>
     *     <li>{@code PATCH}: Partial update; idempotent</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Restrict HTTP methods to only those required for the endpoint</li>
     *     <li>Prevent dangerous methods (e.g., {@code TRACE}, {@code CONNECT}) unless explicitly needed</li>
     *     <li>Implement proper CORS headers for cross-origin requests</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // User CRUD resource with full CRUD operations
     * ResourceDTO dto = new ResourceDTO();
     * dto.setUrl("/api/v1/users");
     * dto.setName("User Management");
     * dto.setMethods(List.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
     *
     * // Read-only resource
     * ResourceDTO readOnlyDto = new ResourceDTO();
     * readOnlyDto.setUrl("/api/v1/reports");
     * readOnlyDto.setName("Report Generation");
     * readOnlyDto.setMethods(List.of(HttpMethod.GET));
     * }
     * </pre>
     *
     * @see HttpMethod
     */
    @NotEmpty(message = "{error.param.blank}")
    private List<HttpMethod> methods;

    /**
     * Optional explanatory text for administrators to understand the resource's purpose.
     * <p>
     * <strong>Validation Rules:</strong>
     * <ul>
     *     <li>{@code @Size(min=0, max=255)}: Optional field; if provided, length must not exceed 255 characters</li>
     *     <li>Allows {@code null} or empty string for resources with self-explanatory names</li>
     * </ul>
     * <p>
     * <strong>Content Guidelines:</strong>
     * <ul>
     *     <li>Write in clear, imperative language targeting system administrators</li>
     *     <li>Include API version and scope: {@code "User management endpoints for CRUD operations (v1)"}</li>
     *     <li>Avoid technical jargon or internal implementation details</li>
     *     <li>Keep under 255 characters for optimal storage and UI tooltip rendering</li>
     * </ul>
     * <p>
     * <strong>Usage Scenarios:</strong>
     * <ul>
     *     <li>Admin console tooltips explaining what the API resource controls</li>
     *     <li>API documentation generation and management</li>
     *     <li>Compliance audits for API access control</li>
     * </ul>
     */
    @Size(min = 0, max = 255, message = "{error.param.range}")
    private String description;

}
