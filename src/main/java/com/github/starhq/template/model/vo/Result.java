package com.github.starhq.template.model.vo;

import com.github.starhq.template.common.enums.ErrorCode;
import lombok.Data;
import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;

/**
 * Unified API response wrapper for consistent success/error handling across the application.
 * <p>
 * This generic class encapsulates business result metadata ({@code code}, {@code message}),
 * payload data ({@code data}), pagination info ({@code total}), and distributed tracing
 * context ({@code traceId}) to enable standardized API contracts, centralized error handling,
 * and end-to-end request tracing.
 * <p>
 * <strong>Generic Type Parameter:</strong>
 * <p>
 * The type parameter {@code <T>} represents the actual response payload type.
 * This enables type-safe API responses without casting:
 * <pre>
 * {@code
 * // Controller: Return typed result
 * @GetMapping("/users/{id}")
 * public Result<UserSimpleVO> getUser(@PathVariable Long id) {
 *     UserSimpleVO user = userService.getById(id);
 *     return Result.success(user); // T = UserSimpleVO
 * }
 *
 * // Frontend: Type-safe access to data
 * const response = await api.getUser(userId);
 * if (response.code === 200) {
 *   const username = response.data.username; // No cast needed
 * }
 * }
 * </pre>
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>REST API Responses</strong>: Standardize success/error format for all HTTP endpoints</li>
 *     <li><strong>Global Exception Handling</strong>: Convert exceptions to structured error responses via {@code @ControllerAdvice}</li>
 *     <li><strong>Distributed Tracing</strong>: Propagate {@code traceId} across microservices for request correlation</li>
 *     <li><strong>Frontend Integration</strong>: Provide predictable response schema for Vue/React API clients</li>
 * </ul>
 * <p>
 * <strong>Response Code Convention:</strong>
 * <ul>
 *     <li><strong>200</strong>: Business success (HTTP status may still be 4xx/5xx for validation/auth errors)</li>
 *     <li><strong>4xx</strong>: Client errors (validation failed, unauthorized, not found)</li>
 *     <li><strong>5xx</strong>: Server errors (internal error, service unavailable)</li>
 *     <li><strong>Custom Codes</strong>: Business-specific codes defined in {@link ErrorCode} enum</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Consistency</strong>: Single response format across all APIs simplifies frontend error handling</li>
 *     <li><strong>Extensibility</strong>: Generic payload type supports any response structure without modification</li>
 *     <li><strong>Observability</strong>: Built-in {@code traceId} enables distributed debugging without log correlation</li>
 *     <li><strong>i18n Ready</strong>: Error messages use i18n keys for multi-language frontend resolution</li>
 * </ul>
 *
 * @param <T> the type of the response payload (e.g., {@code UserSimpleVO}, {@code List<RolePageVO>}, {@code Void})
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see ErrorCode
 * @see org.springframework.web.bind.annotation.RestControllerAdvice
 * @see org.slf4j.MDC
 */
@Data
public class Result<T> implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this VO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Fixed to {@code 1L} as this
     * class structure is stable and backward-compatible.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Business-level status code indicating the outcome of the request.
     * <p>
     * This code is distinct from HTTP status codes and represents application-specific
     * business logic results. Typical values:
     * <ul>
     *     <li>{@code 200} — Success (business operation completed)</li>
     *     <li>{@code 400} — Bad request (validation failed, missing parameters)</li>
     *     <li>{@code 401} — Unauthorized (authentication required)</li>
     *     <li>{@code 403} — Forbidden (insufficient permissions)</li>
     *     <li>{@code 404} — Not found (resource does not exist)</li>
     *     <li>{@code 500} — Internal server error (unexpected exception)</li>
     *     <li>Custom codes — Defined in {@link ErrorCode} for business-specific scenarios</li>
     * </ul>
     * <p>
     * <strong>Usage Guidance:</strong>
     * <ul>
     *     <li>Frontend should check {@code code === 200} for business success, not HTTP status</li>
     *     <li>Use {@link ErrorCode} enum constants to avoid magic numbers: {@code ErrorCode.SUCCESS.getCode()}</li>
     *     <li>Log {@code code} with {@code traceId} for efficient error investigation</li>
     * </ul>
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * {@code
     * // Controller: Return business error
     * if (!userService.exists(username)) {
     *     return Result.fail(ErrorCode.USER_NOT_FOUND);
     * }
     *
     * // Frontend: Handle error by code
     * if (response.code === ErrorCode.USER_NOT_FOUND.getCode()) {
     *   showMessage('User does not exist');
     * }
     * }
     * </pre>
     *
     * @see ErrorCode#getCode()
     */
    private Integer code;

    /**
     * Human-readable message describing the result or error.
     * <p>
     * This field contains an i18n key (e.g., {@code "error.user.not_found"}) rather than
     * hardcoded text, enabling frontend resolution based on user locale. For development
     * or non-i18n scenarios, it may contain plain text messages.
     * <p>
     * <strong>i18n Resolution Strategy:</strong>
     * <pre>
     * {@code
     * // Frontend: Resolve i18n key to localized message
     * const getMessage = (i18nKey, params = {}) => {
     *   return i18n.t(i18nKey, params); // Vue I18n / React Intl
     * };
     *
     * // Usage
     * if (response.code !== 200) {
     *   showError(getMessage(response.message, { username: 'alice' }));
     * }
     * }
     * </pre>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Never expose internal exception details or stack traces in this field</li>
     *     <li>Avoid including user input directly to prevent XSS; sanitize or use parameterized i18n</li>
     *     <li>For error messages, use generic descriptions in production to prevent information leakage</li>
     * </ul>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Keep messages concise (≤ 200 characters) for consistent UI display</li>
     *     <li>Use imperative mood for errors: {@code "Username is required"} not {@code "The username field is required"}</li>
     *     <li>Document all i18n keys in a centralized registry for translation management</li>
     * </ul>
     *
     * @see ErrorCode#getI18nKey()
     */
    private String message;

    /**
     * The actual response payload of type {@code <T>}.
     * <p>
     * This field contains the business data returned on success, or {@code null} on error.
     * The generic type {@code <T>} enables type-safe access without casting:
     * <ul>
     *     <li>Single entity: {@code Result<UserSimpleVO>}</li>
     *     <li>List of entities: {@code Result<List<RolePageVO>>}</li>
     *     <li>Paginated result: {@code Result<IPage<UserPageVO>>}</li>
     *     <li>No content: {@code Result<Void>} or {@code Result<null>}</li>
     * </ul>
     * <p>
     * <strong>Null-Safety Contract:</strong>
     * <ul>
     *     <li>On success ({@code code === 200}): {@code data} should not be {@code null} unless explicitly documented</li>
     *     <li>On error ({@code code !== 200}): {@code data} is typically {@code null}; frontend should not access it</li>
     *     <li>Frontend should always check {@code code} before accessing {@code data}</li>
     * </ul>
     * <p>
     * <strong>Serialization Behavior:</strong>
     * <p>
     * The payload is serialized using Jackson with default configuration. For complex types:
     * <ul>
     *     <li>Ensure VO classes have no circular references or use {@code @JsonIgnore}</li>
     *     <li>Use {@code @JsonSerialize} for custom formatting (e.g., date, enum)</li>
     *     <li>Consider DTO-to-VO conversion to exclude sensitive fields before serialization</li>
     * </ul>
     *
     */
    private transient T data;

    /**
     * Total count of records for paginated responses.
     * <p>
     * This field is only populated when the response contains paginated data
     * (e.g., {@code IPage<T>} from MyBatis-Plus). For non-paginated responses,
     * it should be {@code null} or omitted.
     * <p>
     * <strong>Usage Pattern:</strong>
     * <pre>
     * {@code
     * // Controller: Return paginated result
     * @GetMapping("/users")
     * public Result<IPage<UserPageVO>> listUsers(PageRequest page) {
     *     IPage<UserPageVO> userPage = userService.page(page);
     *     return Result.success(userPage.getRecords(), userPage.getTotal());
     * }
     *
     * // Frontend: Use total for pagination component
     * <a-pagination
     *   :current="page.current"
     *   :total="response.total"
     *   @change="loadPage"
     * />
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Only set {@code total} for paginated endpoints; leave {@code null} for others</li>
     *     <li>Ensure {@code total} matches the actual count from database, not just page size</li>
     *     <li>For large datasets (>100K), consider returning approximate counts or disabling exact total</li>
     * </ul>
     */
    private Long total;

    /**
     * Distributed tracing identifier for correlating logs across microservices.
     * <p>
     * This field is automatically populated from SLF4J MDC ({@code MDC.get("trace_id")})
     * during object construction, enabling end-to-end request tracing without manual propagation.
     * <p>
     * <strong>Tracing Integration:</strong>
     * <pre>
     * {@code
     * // Backend: MDC is populated by filter/interceptor
     * public class TraceFilter implements Filter {
     *     public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) {
     *         String traceId = UUID.randomUUID().toString();
     *         MDC.put("trace_id", traceId);
     *         try {
     *             chain.doFilter(req, res);
     *         } finally {
     *             MDC.clear();
     *         }
     *     }
     * }
     *
     * // Frontend: Include traceId in error reports
     * if (response.code !== 200) {
     *   reportError({
     *     message: response.message,
     *     traceId: response.traceId, // Include for backend correlation
     *     endpoint: window.location.pathname
     *   });
     * }
     * }
     * </pre>
     * <p>
     * <strong>Best Practices:</strong>
     * <ul>
     *     <li>Always log {@code traceId} with business logs for efficient debugging</li>
     *     <li>Propagate {@code traceId} to downstream services via HTTP headers ({@code X-Trace-Id})</li>
     *     <li>Consider integrating with OpenTelemetry/Jaeger for advanced distributed tracing</li>
     * </ul>
     *
     * @see org.slf4j.MDC
     */
    private String traceId;

    /**
     * Default constructor that auto-populates {@code traceId} from MDC.
     * <p>
     * This ensures every response includes tracing context without requiring
     * explicit assignment in controller methods.
     */
    public Result() {
        this.traceId = MDC.get("trace_id");
    }

    /**
     * Constructs a success response with payload data.
     * <p>
     * Automatically sets {@code code = 200} and inherits {@code traceId} from MDC.
     *
     * @param data the response payload of type {@code <T>}
     */
    public Result(T data) {
        this();
        this.data = data;
    }

    /**
     * Constructs a success response with paginated data.
     * <p>
     * Automatically sets {@code code = 200}, populates {@code total}, and inherits {@code traceId}.
     *
     * @param data  the list of records for the current page
     * @param total the total count of records across all pages
     */
    public Result(T data, Long total) {
        this();
        this.data = data;
        this.total = total;
    }

    /**
     * Constructs an error response with custom message.
     * <p>
     * Sets {@code code = 500} (internal error) by default; use {@link #fail(ErrorCode)}
     * or {@link #fail(Integer, String)} for specific error codes.
     *
     * @param message the error message (i18n key or plain text)
     */
    public Result(String message) {
        this();
        this.message = message;
        this.code = ErrorCode.INTERNAL_ERROR.getCode();
    }

    /**
     * Constructs an error response from predefined error code.
     * <p>
     * Uses {@link ErrorCode} to ensure consistent error codes and i18n keys
     * across the application.
     *
     * @param errorCode the predefined error code enum
     * @see ErrorCode
     */
    public Result(ErrorCode errorCode) {
        this();
        this.message = errorCode.getI18nKey();
        this.code = errorCode.getCode();
    }

    /**
     * Constructs an error response with custom code and message.
     * <p>
     * Use this constructor for dynamic error scenarios not covered by {@link ErrorCode}.
     *
     * @param code    the business error code
     * @param message the error message (i18n key recommended)
     */
    public Result(Integer code, String message) {
        this();
        this.message = message;
        this.code = code;
    }

    /**
     * Creates a success response with payload data.
     * <p>
     * Static factory method for concise controller returns.
     *
     * @param data the response payload
     * @param <T>  the payload type
     * @return success result with {@code code = 200}
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(data);
    }

    /**
     * Creates a success response with paginated data.
     * <p>
     * Static factory method for paginated endpoint returns.
     *
     * @param data  the list of records for the current page
     * @param total the total count of records across all pages
     * @param <T>   the payload type (typically {@code List<VO>})
     * @return success result with {@code code = 200} and populated {@code total}
     */
    public static <T> Result<T> success(T data, Long total) {
        return new Result<>(data, total);
    }

    /**
     * Creates an error response with custom message.
     * <p>
     * Static factory method for dynamic error scenarios.
     *
     * @param message the error message (i18n key recommended)
     * @param <T>     the payload type (typically inferred as {@code Void})
     * @return error result with {@code code = 500}
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(message);
    }

    /**
     * Creates an error response from predefined error code.
     * <p>
     * Preferred method for standardized error handling.
     *
     * @param errorCode the predefined error code enum
     * @param <T>       the payload type (typically inferred as {@code Void})
     * @return error result with code and i18n message from {@code ErrorCode}
     * @see ErrorCode
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode);
    }

    /**
     * Creates an error response with custom code and message.
     * <p>
     * Use for dynamic error scenarios not covered by {@link ErrorCode}.
     *
     * @param code    the business error code
     * @param message the error message (i18n key recommended)
     * @param <T>     the payload type (typically inferred as {@code Void})
     * @return error result with specified code and message
     */
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message);
    }

}