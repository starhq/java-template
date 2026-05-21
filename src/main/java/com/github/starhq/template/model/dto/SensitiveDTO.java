package com.github.starhq.template.model.dto;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.github.starhq.template.config.json.serializer.SensitivePropertyFilter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Abstract base class for Data Transfer Objects containing sensitive information.
 * <p>
 * This class provides a marker interface for DTOs that contain sensitive data (e.g., passwords,
 * tokens, private keys) and enables automatic serialization filtering via Jackson's {@code @JsonFilter}.
 * When serialized to JSON, sensitive properties are automatically excluded from the output.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Password Protection</strong>: Prevent password hashes from appearing in API responses</li>
 *     <li><strong>Token Security</strong>: Ensure access/refresh tokens are not logged or exposed</li>
 *     <li><strong>Privacy Compliance</strong>: Meet GDPR, PIPL, or other data protection requirements</li>
 * </ul>
 * <p>
 * <strong>Serialization Filtering:</strong>
 * <p>
 * The class is annotated with {@code @JsonFilter} to enable dynamic property filtering:
 * <pre>
 * {@code
 * // Sensitive properties are automatically excluded during JSON serialization
 * SensitiveDTO dto = new ConcreteSensitiveDTO();
 * dto.setPassword("secret123");
 * dto.setToken("abc123xyz");
 *
 * // JSON output will NOT include password or token fields
 * String json = objectMapper.writeValueAsString(dto);
 * }
 * </pre>
 * <p>
 * <strong>Filter Configuration:</strong>
 * <p>
 * The actual filtering logic is implemented in {@link SensitivePropertyFilter}, which:
 * <ul>
 *     <li>Identifies sensitive properties via {@code @Sensitive} annotation or naming conventions</li>
 *     <li>Replaces sensitive values with {@code null} or masked placeholders</li>
 *     <li>Applies consistently across all DTOs extending this base class</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Data
 * @JsonFilter(SensitivePropertyFilter.FILTER_NAME)
 * public class UserLoginDTO extends SensitiveDTO {
 *     private String username;
 *     private String password;  // Automatically filtered during serialization
 *     private String email;
 * }
 *
 * @RestController
 * public class AuthController {
 *     @PostMapping("/login")
 *     public Result<UserLoginDTO> login(@RequestBody UserLoginDTO request) {
 *         // password is available for validation but excluded from response
 *         authService.validate(request);
 *         return Result.success();  // Response won't include password
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Separation of Concerns</strong>: Sensitive data handling is delegated to filter</li>
 *     <li><strong>Consistency</strong>: All sensitive DTOs inherit the same filtering behavior</li>
 *     <li><strong>Flexibility</strong>: Filter can be customized for different serialization contexts</li>
 * </ul>
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 *     <li>Never log DTO instances that extend this class without filtering</li>
 *     <li>Ensure Jackson {@code ObjectMapper} is configured with {@code SimpleModule} containing the filter</li>
 *     <li>Consider using {@code @JsonIgnore} for properties that should never be serialized</li>
 *     <li>Implement proper input validation before deserializing sensitive data</li>
 * </ul>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see SensitivePropertyFilter
 * @see com.fasterxml.jackson.annotation.JsonFilter
 * @see com.fasterxml.jackson.databind.ObjectMapper#addMixIn(Class, Class)
 */
@JsonFilter(SensitivePropertyFilter.FILTER_NAME)
public abstract class SensitiveDTO implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     * <p>
     * Note: Since this is an abstract class, subclasses should define their own
     * {@code serialVersionUID} for concrete implementations.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 1L;

}
