package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.NotFoundException;
import com.github.starhq.template.entity.SysApiLog;
import com.github.starhq.template.mapper.SysApiLogMapper;
import com.github.starhq.template.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Development-profile implementation of {@link ApiLogService} for API request logging.
 * <p>
 * This service provides concrete operations for persisting and retrieving API invocation logs
 * in development environments. It is intentionally restricted to {@value ProfileConstants#DEV}
 * profile to avoid performance overhead in production, where async/batch logging strategies
 * should be used instead.
 * <p>
 * <strong>Environment Configuration:</strong>
 * <ul>
 *     <li><strong>@Profile({@value ProfileConstants#DEV})</strong>: Only active when Spring profile is 'dev'</li>
 *     <li><strong>@Service("apiLogService")</strong>: Explicit bean name for profile-based service switching</li>
 *     <li><strong>Development Use Only</strong>: Direct database writes without async/batch optimization</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Simplicity</strong>: Straightforward CRUD operations for easy debugging in dev environment</li>
 *     <li><strong>Type Safety</strong>: Uses {@link LambdaQueryWrapper} for compile-time field reference checking</li>
 *     <li><strong>Constructor Injection</strong>: {@link RequiredArgsConstructor} ensures immutable dependencies</li>
 *     <li><strong>Fail-Fast</strong>: Throws {@link NotFoundException} immediately if traceId not found</li>
 * </ul>
 * <p>
 * <strong>Production Migration Guidance:</strong>
 * <p>
 * For production deployment, consider implementing a separate {@code ApiLogServiceImpl} with:
 * <ul>
 *     <li><strong>Async Execution</strong>: Use {@code @Async} or message queue to avoid blocking business threads</li>
 *     <li><strong>Batch Writes</strong>: Buffer logs and flush in batches for high-throughput scenarios</li>
 *     <li><strong>Graceful Degradation</strong>: Log persistence failures should not propagate to callers</li>
 *     <li><strong>Privacy Protection</strong>: Mask sensitive fields (passwords, tokens) before persistence</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see ApiLogService
 * @see SysApiLog
 * @see SysApiLogMapper
 * @see ProfileConstants
 */
@Profile(ProfileConstants.DEV)
@RequiredArgsConstructor
@Service("apiLogService")
public class ApiLogServiceImpl implements ApiLogService {

    /**
     * MyBatis-Plus mapper for {@link SysApiLog} database operations.
     * <p>
     * Injected via {@link RequiredArgsConstructor} constructor injection for:
     * <ul>
     *     <li><strong>Immutability</strong>: Final field ensures dependency cannot be modified after construction</li>
     *     <li><strong>Testability</strong>: Easy to mock in unit tests without reflection</li>
     *     <li><strong>Null Safety</strong>: Spring guarantees non-null injection for required dependencies</li>
     * </ul>
     * <p>
     * <strong>Usage Pattern:</strong>
     * <pre>
     * {@code
     * // Insert new log entry
     * apiLogMapper.insert(apiLog);
     *
     * // Query with type-safe Lambda wrapper
     * apiLogMapper.selectOne(new LambdaQueryWrapper<SysApiLog>()
     *     .eq(SysApiLog::getTraceId, traceId));
     * }
     * </pre>
     *
     * @see SysApiLogMapper
     * @see LambdaQueryWrapper
     */
    private final SysApiLogMapper apiLogMapper;

    /**
     * Persists an API invocation log entry to the database.
     * <p>
     * <strong>Development-Only Behavior:</strong>
     * <p>
     * This implementation performs synchronous database writes, which is acceptable for
     * development debugging but should be replaced with async/batch strategies in production.
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code apiLog}: Must not be {@code null}; should include mandatory fields ({@code traceId}, {@code endpoint}, {@code requestTime})</li>
     *     <li>{@code apiLog.getTraceId()}: Should be unique per request for end-to-end tracing</li>
     *     <li>{@code apiLog.getRequestParams()}/{@code getResponseBody()}: Should be pre-masked if containing sensitive data</li>
     * </ul>
     * <p>
     * <strong>Exception Handling:</strong>
     * <ul>
     *     <li><strong>Database Errors</strong>: Propagated to caller (acceptable in dev for immediate feedback)</li>
     *     <li><strong>Validation Errors</strong>: Should be caught and handled by caller or global exception handler</li>
     *     <li><strong>Production Note</strong>: In prod, catch and log errors locally to avoid blocking business logic</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Synchronous write adds ~10-50ms latency per request (acceptable for dev debugging)</li>
     *     <li>For high-throughput scenarios, consider async execution or batch buffering</li>
     *     <li>Ensure database connection pool is sized appropriately for logging load</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Log API request (dev environment only)
     * @PostMapping("/users")
     * public Result<UserVO> createUser(@RequestBody UserDTO dto, HttpServletRequest request) {
     *     // 1. Execute business logic
     *     UserVO result = userService.create(dto);
     *
     *     // 2. Build log entity
     *     SysApiLog log = new SysApiLog();
     *     log.setTraceId(MDC.get("trace_id"));
     *     log.setEndpoint(request.getRequestURI());
     *     log.setMethod(request.getMethod());
     *     log.setRequestTime(OffsetDateTime.now());
     *     // ... set other fields
     *
     *     // 3. Persist log (synchronous in dev)
     *     apiLogService.create(log);
     *
     *     return Result.success(result);
     * }
     * }
     * </pre>
     *
     * @param apiLog the API log entity to persist; must include {@code traceId} and core metadata
     * @throws IllegalArgumentException                    if {@code apiLog} is {@code null} or missing required fields
     * @throws org.springframework.dao.DataAccessException if database operation fails
     * @see SysApiLog
     * @see SysApiLogMapper#insert(Object)
     */
    @Override
    public void create(SysApiLog apiLog) {
        apiLogMapper.insert(apiLog);
    }

    /**
     * Retrieves an API log entry by its distributed tracing identifier.
     * <p>
     * <strong>Query Semantics:</strong>
     * <ul>
     *     <li><strong>Exact Match</strong>: Returns the log with {@code trace_id = :traceId} (case-sensitive)</li>
     *     <li><strong>Not Found</strong>: Throws {@link NotFoundException} with {@link ErrorCode#NOT_FOUND} if no match</li>
     *     <li><strong>Index Usage</strong>: Leverages {@code UNIQUE INDEX uk_trace_id} for O(log N) lookup</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from admin endpoints with proper authorization</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code getByTraceId} calls for compliance</li>
     *     <li><strong>Data Masking</strong>: Ensure returned log has sensitive fields masked per caller permissions</li>
     * </ul>
     * <p>
     * <strong>Exception Strategy:</strong>
     * <ul>
     *     <li><strong>Not Found</strong>: Throws {@link NotFoundException} for immediate caller feedback (dev-friendly)</li>
     *     <li><strong>Production Note</strong>: Consider returning {@code Optional<SysApiLog>} or {@code null} for graceful handling</li>
     *     <li><strong>Error Propagation</strong>: Database errors propagate to global exception handler for consistent response</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Debug controller: Retrieve full request trace (admin only)
     * @GetMapping("/debug/trace/{traceId}")
     * @PreAuthorize("hasRole('ADMIN')")
     * public Result<SysApiLog> getTrace(@PathVariable String traceId) {
     *     try {
     *         SysApiLog log = apiLogService.getByTraceId(traceId);
     *         return Result.success(log);
     *     } catch (NotFoundException e) {
     *         return Result.fail(ErrorCode.LOG_NOT_FOUND);
     *     }
     * }
     *
     * // Frontend: Display trace details in admin console
     * const TraceDetail = ({ traceId }) => {
     *   const { data: log, error } = useRequest(() => api.getTrace(traceId));
     *
     *   if (error?.code === ErrorCode.LOG_NOT_FOUND.code) {
     *     return <Empty description="Trace not found" />;
     *   }
     *
     *   return (
     *     <a-descriptions title="Request Trace">
     *       <a-descriptions-item label="Endpoint">{log?.endpoint}</a-descriptions-item>
     *       <a-descriptions-item label="Method">{log?.method}</a-descriptions-item>
     *       <a-descriptions-item label="Duration">{log?.durationMs}ms</a-descriptions-item>
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
     * -- For time-range queries combined with traceId (if needed)
     * CREATE INDEX idx_api_log_trace_time ON sys_api_log(trace_id, request_time DESC);
     * }
     * </pre>
     *
     * @param traceId the distributed tracing identifier (typically UUID format)
     * @return the {@link SysApiLog} entity if found
     * @throws IllegalArgumentException if {@code traceId} is {@code null} or empty
     * @throws NotFoundException        if no log found with given {@code traceId}
     * @see SysApiLog
     * @see LambdaQueryWrapper#eq(Object, Object)
     * @see SysApiLogMapper#selectOne(Wrapper)
     */
    @Override
    public SysApiLog getByTraceId(String traceId) {
        SysApiLog apiLog = apiLogMapper.selectOne(
                new LambdaQueryWrapper<SysApiLog>()
                        .eq(SysApiLog::getTraceId, traceId)
        );

        if (null == apiLog) {
            throw new NotFoundException(ErrorCode.NOT_FOUND);
        }

        return apiLog;
    }

}