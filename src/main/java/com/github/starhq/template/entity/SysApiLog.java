package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

import java.time.LocalDateTime;

/**
 * Entity class representing an API request/response audit log entry.
 * <p>
 * This class maps to the {@code sys_api_log} table and captures comprehensive
 * telemetry data for HTTP requests, including request metadata, payload snapshots,
 * response status, execution duration, and exception details. Primarily used for:
 * <ul>
 *     <li><strong>Security Auditing</strong>: Track suspicious access patterns, unauthorized attempts</li>
 *     <li><strong>Troubleshooting</strong>: Diagnose production issues via request replay and error analysis</li>
 *     <li><strong>Performance Monitoring</strong>: Identify slow endpoints via {@code duration} metrics</li>
 *     <li><strong>Compliance</strong>: Meet regulatory requirements for API access logging</li>
 * </ul>
 * <p>
 * <strong>Security & Privacy Considerations:</strong>
 * <p>
 * This entity may contain sensitive data (authentication tokens, PII, business secrets).
 * Always apply the following safeguards:
 * <ul>
 *     <li><strong>Request/Response Body</strong>: Truncate or mask sensitive fields (password, token, idCard) before persistence</li>
 *     <li><strong>Exception Stack</strong>: Never store full stack traces in production; use {@code exceptionMessage} for alerts</li>
 *     <li><strong>Access Control</strong>: Restrict log query endpoints to admin roles with MFA enforcement</li>
 *     <li><strong>Data Retention</strong>: Implement TTL-based archival/deletion per compliance policies</li>
 * </ul>
 * <p>
 * <strong>Storage Optimization:</strong>
 * <p>
 * Large text fields ({@code requestBody}, {@code responseBody}, {@code exceptionStack})
 * should be configured with appropriate column types ({@code TEXT}/{@code LONGTEXT})
 * and consider compression or external storage (e.g., Elasticsearch, OSS) for high-volume scenarios.
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-03-24
 * @see TableName
 * @see FieldFill
 * @see <a href="https://baomidou.com/pages/223848/">MyBatis-Plus Annotation Guide</a>
 */
@Data
@Alias("apiLog")
@TableName("sys_api_log")
public class SysApiLog {

    /**
     * The unique identifier for this log entry.
     * <p>
     * Mapped to the primary key column via {@link TableId}. Uses MyBatis-Plus
     * default {@code ASSIGN_ID} strategy (snowflake algorithm) for globally
     * unique, time-ordered 64-bit numeric IDs.
     *
     * @see TableId
     * @see com.baomidou.mybatisplus.annotation.IdType#ASSIGN_ID
     */
    @TableId
    private Long id;

    /**
     * The distributed tracing identifier for correlating requests across microservices.
     * <p>
     * Typically populated from HTTP header {@code X-Trace-Id} or MDC context.
     * Enables end-to-end request flow reconstruction in observability platforms
     * (e.g., SkyWalking, Jaeger, Zipkin).
     * <p>
     * <strong>Format:</strong> UUID or hex string (e.g., {@code "a1b2c3d4e5f6"})
     *
     * @see org.slf4j.MDC
     */
    private String traceId;

    /**
     * The requested URI path (excluding query string and base context).
     * <p>
     * Example: {@code "/api/v1/users/123"} for request to
     * {@code GET https://api.example.com/api/v1/users/123?fields=name}.
     * <p>
     * <strong>Query Tips:</strong>
     * <ul>
     *     <li>Filter by endpoint: {@code WHERE uri LIKE '/api/v1/users/%'}</li>
     *     <li>Aggregate by API: {@code GROUP BY uri ORDER BY COUNT(*) DESC}</li>
     * </ul>
     */
    private String uri;

    /**
     * The HTTP method of the request.
     * <p>
     * Standard values: {@code GET}, {@code POST}, {@code PUT}, {@code DELETE},
     * {@code PATCH}, {@code HEAD}, {@code OPTIONS}.
     * <p>
     * <strong>Security Note:</strong> Unexpected methods (e.g., {@code TRACE},
     * {@code CONNECT}) may indicate probing attacks — consider alerting on anomalies.
     */
    private String method;

    /**
     * The raw query string portion of the request URL.
     * <p>
     * Example: {@code "page=1&size=20&sort=name,asc"} for
     * {@code GET /api/users?page=1&size=20&sort=name,asc}.
     * <p>
     * <strong>Privacy Note:</strong> Query strings may contain sensitive tokens
     * or PII. Consider masking parameters like {@code token}, {@code secret},
     * {@code idCard} before persistence.
     */
    private String queryString;

    /**
     * The client IP address extracted from request headers or connection metadata.
     * <p>
     * Resolution priority: {@code X-Forwarded-For} → {@code X-Real-IP} →
     * {@code Remote-Addr}. Useful for geo-analysis, rate limiting, and fraud detection.
     * <p>
     * <strong>Format:</strong> IPv4 ({@code "192.168.1.1"}) or IPv6
     * ({@code "2001:db8::1"}).
     */
    private String clientIp;

    /**
     * The serialized JSON representation of selected HTTP request headers.
     * <p>
     * Typically includes: {@code Content-Type}, {@code Authorization},
     * {@code User-Agent}, {@code Accept-Language}. Excludes sensitive headers
     * like raw {@code Cookie} or {@code Authorization} token values.
     * <p>
     * <strong>Format:</strong> JSON object string, e.g.:
     * <pre>
     * {@code {"Content-Type":"application/json","User-Agent":"Mozilla/5.0"}}
     * </pre>
     * <p>
     * <strong>Storage Tip:</strong> Use {@code VARCHAR(2048)} or {@code TEXT}
     * depending on header volume; consider JSON column type if database supports.
     */
    private String headers;

    /**
     * The serialized JSON representation of request parameters (form/query/path).
     * <p>
     * Merges parameters from all sources for unified audit view. Sensitive values
     * (password, token, phone) should be masked before serialization.
     * <p>
     * <strong>Format:</strong> JSON object string, e.g.:
     * <pre>
     * {@code {"username":"alice","page":1,"filters":{"status":"active"}}}
     * </pre>
     * <p>
     * <strong>Privacy Note:</strong> Always apply field-level masking in the
     * logging interceptor to comply with GDPR/PIPL regulations.
     */
    private String params;

    /**
     * The raw request body content (for POST/PUT/PATCH requests).
     * <p>
     * <strong>Warning:</strong> This field may contain highly sensitive data:
     * <ul>
     *     <li>Authentication credentials (password, refresh token)</li>
     *     <li>Personal identifiable information (idCard, phone, address)</li>
     *     <li>Business secrets (payment details, contract terms)</li>
     * </ul>
     * <p>
     * <strong>Mandatory Safeguards:</strong>
     * <ul>
     *     <li>Truncate to max length (e.g., 4KB) to prevent storage abuse</li>
     *     <li>Mask sensitive fields via JSON path filtering before persistence</li>
     *     <li>Disable logging for sensitive endpoints (e.g., {@code /auth/login})</li>
     * </ul>
     * <p>
     * <strong>Storage Tip:</strong> Use {@code TEXT} or {@code LONGTEXT} column type;
     * consider external storage (Elasticsearch/OSS) for high-volume scenarios.
     */
    private String requestBody;

    /**
     * The HTTP response status code returned to the client.
     * <p>
     * Standard ranges:
     * <ul>
     *     <li>{@code 2xx}: Success</li>
     *     <li>{@code 3xx}: Redirection</li>
     *     <li>{@code 4xx}: Client error (authentication, validation)</li>
     *     <li>{@code 5xx}: Server error (bugs, infrastructure issues)</li>
     * </ul>
     * <p>
     * <strong>Monitoring Tip:</strong> Alert on {@code 5xx} spikes or
     * {@code 401/403} patterns indicating brute-force attempts.
     */
    private Integer httpStatus;

    /**
     * The serialized response body content returned to the client.
     * <p>
     * <strong>Warning:</strong> Similar to {@code requestBody}, this field may
     * expose sensitive data (user profiles, internal IDs, business metrics).
     * Apply the same masking/truncation strategies.
     * <p>
     * <strong>Best Practice:</strong> Log only error responses ({@code 4xx/5xx})
     * in production to reduce storage overhead and privacy risks. Success responses
     * can be sampled (e.g., 1%) for debugging purposes.
     * <p>
     * <strong>Format:</strong> JSON string or truncated plain text.
     */
    private String responseBody;

    /**
     * The exception message captured when request processing fails.
     * <p>
     * Contains the top-level error description (e.g., {@code "User not found"},
     * {@code "Database connection timeout"}). Suitable for alerting and
     * high-level error categorization.
     * <p>
     * <strong>Security Note:</strong> Ensure exception messages do not leak
     * internal implementation details (SQL syntax, stack paths, config values)
     * that could aid attackers.
     */
    private String exceptionMessage;

    /**
     * The full exception stack trace for deep debugging.
     * <p>
     * <strong>Production Warning:</strong> This field should ONLY be populated in:
     * <ul>
     *     <li>Non-production environments (dev/test/staging)</li>
     *     <li>Production with explicit debug flag + short retention (e.g., 1 hour)</li>
     *     <li>When error rate exceeds threshold and auto-capture is enabled</li>
     * </ul>
     * <p>
     * <strong>Storage Impact:</strong> Stack traces can be very large (10KB+).
     * Consider:
     * <ul>
     *     <li>Storing in separate {@code LONGTEXT} column or external system</li>
     *     <li>Compressing with GZIP before persistence</li>
     *     <li>Using {@code exceptionMessage} + {@code traceId} for correlation instead</li>
     * </ul>
     *
     * @see #exceptionMessage
     * @see #traceId
     */
    private String exceptionStack;

    /**
     * The total execution time of the request in milliseconds.
     * <p>
     * Calculated as {@code endTime - startTime} at the filter/interceptor layer.
     * Critical for:
     * <ul>
     *     <li>Identifying slow endpoints ({@code duration > 1000ms})</li>
     *     <li>Capacity planning and auto-scaling triggers</li>
     *     <li>SLA compliance monitoring</li>
     * </ul>
     * <p>
     * <strong>Query Tips:</strong>
     * <ul>
     *     <li>P95 latency: {@code SELECT PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY duration) FROM sys_api_log}</li>
     *     <li>Slow queries: {@code WHERE duration > 2000 AND uri LIKE '/api/%'}</li>
     * </ul>
     */
    private Long duration;

    /**
     * The timestamp when this log entry was persisted to the database.
     * <p>
     * Automatically populated by MyBatis-Plus {@code MetaObjectHandler} during
     * {@code INSERT} operations. Uses {@link LocalDateTime} without timezone —
     * ensure application server and database use consistent timezone (preferably UTC).
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Fill Strategy: {@link FieldFill#INSERT} — immutable after creation</li>
     *     <li>Precision: Millisecond (depends on database column type)</li>
     *     <li>Index Recommendation: Add index for time-range queries: {@code CREATE INDEX idx_create_time ON sys_api_log(create_time)}</li>
     * </ul>
     *
     * @see FieldFill#INSERT
     * @see com.baomidou.mybatisplus.core.handlers.MetaObjectHandler#insertFill
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

}