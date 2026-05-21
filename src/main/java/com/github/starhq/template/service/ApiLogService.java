package com.github.starhq.template.service;

import com.github.starhq.template.entity.SysApiLog;

/**
 * Service interface for API request logging and audit trail management.
 * <p>
 * This service provides centralized operations for capturing, storing, and retrieving
 * API invocation logs. It supports distributed tracing via {@code traceId} correlation,
 * asynchronous logging for performance, and structured audit data for compliance reporting.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Request Auditing</strong>: Record all API invocations with request/response metadata for security audits</li>
 *     <li><strong>Troubleshooting</strong>: Enable end-to-end request tracing via {@code traceId} for debugging distributed systems</li>
 *     <li><strong>Performance Monitoring</strong>: Track API latency, error rates, and usage patterns for capacity planning</li>
 *     <li><strong>Compliance Reporting</strong>: Provide structured logs for regulatory requirements (GDPR, SOX, PCI-DSS)</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Asynchronous by Default</strong>: Logging operations should not block business logic; use async execution or message queues</li>
 *     <li><strong>Trace-First</strong>: All logs must include {@code traceId} for correlation across microservices</li>
 *     <li><strong>Structured Data</strong>: Use typed entity ({@link SysApiLog}) instead of raw strings for query efficiency</li>
 *     <li><strong>Privacy-Aware</strong>: Mask sensitive fields (passwords, tokens, PII) before persistence</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Log API request asynchronously via AOP or manual call
 * @PostMapping("/users")
 * public Result<UserVO> createUser(@RequestBody UserDTO dto) {
 *     // 1. Execute business logic
 *     UserVO result = userService.create(dto);
 *
 *     // 2. Build log entity (async, non-blocking)
 *     SysApiLog log = ApiLogBuilder.fromRequest(request, response, result)
 *         .traceId(MDC.get("trace_id"))
 *         .maskSensitiveFields()
 *         .build();
 *
 *     // 3. Async persist (fire-and-forget)
 *     apiLogService.create(log);
 *
 *     return Result.success(result);
 * }
 *
 * // Debug: Retrieve full request/response by traceId
 * @GetMapping("/debug/trace/{traceId}")
 * @PreAuthorize("hasRole('ADMIN')")
 * public Result<SysApiLog> getTrace(@PathVariable String traceId) {
 *     SysApiLog log = apiLogService.getByTraceId(traceId);
 *     return Result.success(log);
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see SysApiLog
 * @see com.github.starhq.template.mapper.SysApiLogMapper
 */
public interface ApiLogService {

    /**
     * Persists an API invocation log entry asynchronously.
     * <p>
     * This method is designed for high-throughput, low-latency logging scenarios.
     * Implementations should:
     * <ul>
     *     <li><strong>Execute Async</strong>: Use {@code @Async}, message queue, or batch writer to avoid blocking business threads</li>
     *     <li><strong>Graceful Degradation</strong>: Log persistence failures should not propagate to callers; use fallback logging</li>
     *     <li><strong>Batch Optimization</strong>: For high-volume systems, consider buffering logs and flushing in batches</li>
     *     <li><strong>Privacy Protection</strong>: Ensure sensitive fields are masked before database insertion</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code apiLog}: Must not be {@code null}; should include mandatory fields ({@code traceId}, {@code requestTime}, {@code endpoint})</li>
     *     <li>{@code apiLog.getTraceId()}: Should match the MDC {@code trace_id} for end-to-end correlation</li>
     *     <li>{@code apiLog.getRequestParams()}/{@code getResponseBody()}: Should be masked if containing sensitive data</li>
     * </ul>
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li>Database errors: Log to local file as fallback, do not throw to caller</li>
     *     <li>Validation errors: Log warning with traceId, skip persistence</li>
     *     <li>System overload: Implement backpressure via bounded queue or sampling</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Avoid serializing large response bodies (>1MB); truncate or store reference only</li>
     *     <li>Use connection pooling and batch inserts for high-throughput logging</li>
     *     <li>Consider time-series database (InfluxDB, ClickHouse) for analytical queries</li>
     * </ul>
     *
     * @param apiLog the API log entity to persist; must include {@code traceId} and core metadata
     * @throws IllegalArgumentException if {@code apiLog} is {@code null} or missing required fields
     * @see SysApiLog
     * @see com.github.starhq.template.common.util.ApiLogBuilder
     */
    void create(SysApiLog apiLog);

    /**
     * Retrieves an API log entry by its distributed tracing identifier.
     * <p>
     * This method enables end-to-end request debugging by correlating logs across
     * microservices, gateways, and frontend clients using a shared {@code traceId}.
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li><strong>Exact Match</strong>: Returns the log with {@code trace_id = :traceId} (case-sensitive)</li>
     *     <li><strong>Null Handling</strong>: Returns {@code null} if no log found or {@code traceId} is invalid</li>
     *     <li><strong>Performance</strong>: Should leverage database index on {@code trace_id} for O(log N) lookup</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This endpoint should be protected by role-based access control (e.g., {@code @PreAuthorize("hasRole('ADMIN')")})</li>
     *     <li><strong>Audit Access</strong>: Log all {@code getByTraceId} calls themselves for compliance</li>
     *     <li><strong>Data Masking</strong>: Ensure sensitive fields in returned log are masked per caller permissions</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Debug controller: Retrieve full request trace for admin
     * @GetMapping("/debug/trace/{traceId}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<SysApiLog> getTrace(@PathVariable String traceId) {
     *     SysApiLog log = apiLogService.getByTraceId(traceId);
     *     if (log == null) {
     *         return Result.fail(ErrorCode.LOG_NOT_FOUND);
     *     }
     *     return Result.success(log);
     * }
     *
     * // Frontend: Display trace details in admin console
     * const TraceDetail = ({ traceId }) => {
     *   const { data: log, loading } = useRequest(() => api.getTrace(traceId));
     *
     *   return (
     *     <a-descriptions title="Request Trace">
     *       <a-descriptions-item label="Endpoint">{log?.endpoint}</a-descriptions-item>
     *       <a-descriptions-item label="Method">{log?.method}</a-descriptions-item>
     *       <a-descriptions-item label="Duration">{log?.durationMs}ms</a-descriptions-item>
     *       <a-descriptions-item label="Status">{log?.statusCode}</a-descriptions-item>
     *       <a-descriptions-item label="Request" span={3}>
     *         <JsonViewer value={JSON.parse(log?.requestParams || '{}')} />
     *       </a-descriptions-item>
     *     </a-descriptions>
     *   );
     * };
     * }
     * </pre>
     * <p>
     * <strong>Index Recommendation:</strong>
     * <pre>
     * {@code
     * -- Ensure fast traceId lookup with unique index
     * CREATE UNIQUE INDEX uk_api_log_trace_id ON sys_api_log(trace_id);
     *
     * -- For time-range queries combined with traceId
     * CREATE INDEX idx_api_log_trace_time ON sys_api_log(trace_id, request_time DESC);
     * }
     * </pre>
     *
     * @param traceId the distributed tracing identifier (typically UUID format)
     * @return the {@link SysApiLog} entity if found; {@code null} if not found or invalid traceId
     * @throws IllegalArgumentException if {@code traceId} is {@code null} or empty
     * @see SysApiLog
     */
    SysApiLog getByTraceId(String traceId);

}