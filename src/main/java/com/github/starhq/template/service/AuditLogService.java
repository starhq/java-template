package com.github.starhq.template.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.model.dto.auditlog.AuditLogPageRequest;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;

/**
 * Service interface for audit log management and compliance reporting.
 * <p>
 * This service provides centralized operations for querying, filtering, and exporting
 * system audit trails. It supports multi-dimensional filtering (action, target, time range),
 * pagination for large datasets, and structured responses for admin console integration.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Admin Console</strong>: Display paginated audit logs with filtering by action, target, operator, time range</li>
 *     <li><strong>Security Analysis</strong>: Investigate suspicious activities by reviewing detailed operation records</li>
 *     <li><strong>Compliance Reporting</strong>: Export filtered audit trails for regulatory audits (GDPR, SOX, PCI-DSS)</li>
 *     <li><strong>Troubleshooting</strong>: Reconstruct user actions leading to data inconsistencies or business errors</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Read-Only Queries</strong>: Service methods should not modify audit data; use separate commands for archival/purge</li>
 *     <li><strong>Privacy-Aware</strong>: Sensitive fields (passwords, tokens, PII) must be masked in returned VOs</li>
 *     <li><strong>Performance-Conscious</strong>: Leverage database indexes and pagination to handle large audit datasets efficiently</li>
 *     <li><strong>Access-Controlled</strong>: All query methods should enforce role-based permissions (e.g., ADMIN-only access)</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Expose audit log query endpoint (admin-only)
 * @GetMapping("/audit-logs")
 * @PreAuthorize("hasRole('ADMIN')")
 * public Result<IPage<AuditLogPageVO>> listAuditLogs(AuditLogPageRequest request) {
 *     IPage<AuditLogPageVO> page = auditLogService.page(request);
 *     return Result.success(page.getRecords(), page.getTotal());
 * }
 *
 * // Service: Build query with dynamic filters
 * public IPage<AuditLogPageVO> page(AuditLogPageRequest request) {
 *     LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>()
 *         .eq(StringUtils.hasText(request.getAction()), SysAuditLog::getAction, request.getAction())
 *         .eq(request.getTargetType() != null, SysAuditLog::getTargetType, request.getTargetType())
 *         .eq(request.getTargetId() != null, SysAuditLog::getTargetId, request.getTargetId())
 *         .ge(request.getStartTime() != null, SysAuditLog::getCreatedAt, request.getStartTime())
 *         .le(request.getEndTime() != null, SysAuditLog::getCreatedAt, request.getEndTime())
 *         .orderByDesc(SysAuditLog::getCreatedAt);
 *
 *     Page<AuditLogPageVO> page = new Page<>(request.getPage(), request.getSize());
 *     return auditLogMapper.selectPage(page, wrapper);
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see AuditLogPageRequest
 * @see AuditLogPageVO
 * @see com.github.starhq.template.entity.SysAuditLog
 * @see com.github.starhq.template.mapper.SysAuditLogMapper
 */
public interface AuditLogService {

    /**
     * Retrieves a paginated list of audit log entries matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient audit trail analysis:
     * <ul>
     *     <li><strong>Action Filter</strong>: Filter by operation type (e.g., {@code "USER:CREATE"}, {@code "ROLE:ASSIGN"})</li>
     *     <li><strong>Target Filter</strong>: Filter by target entity type and ID (e.g., {@code targetType=USER, targetId=123})</li>
     *     <li><strong>Time Range</strong>: Filter by creation time window for temporal analysis</li>
     *     <li><strong>Operator Filter</strong>: Filter by creator/updater ID for user activity tracking</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; should include pagination parameters ({@code page}, {@code size})</li>
     *     <li>{@code pageInfo.getAction()}: Optional; if set, filters logs by exact action code match</li>
     *     <li>{@code pageInfo.getTargetType()}/{@code getTargetId()}: Optional; used together to filter by specific entity</li>
     *     <li>{@code pageInfo.getStartTime()}/{@code getEndTime()}: Optional; define inclusive time range for filtering</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>Field Masking</strong>: Sensitive fields in {@code AuditLogPageVO.value} must be pre-masked before return</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: This method should be called only from endpoints protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Audit Access</strong>: Consider logging all {@code page()} calls themselves for compliance tracking</li>
     *     <li><strong>Data Masking</strong>: Ensure {@code value} field in returned VOs has sensitive data masked per privacy policy</li>
     *     <li><strong>Rate Limiting</strong>: Implement per-user rate limits to prevent enumeration or DoS attacks on audit data</li>
     * </ul>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: Ensure composite indexes exist for common filter combinations:
     *         <pre>{@code
     *         CREATE INDEX idx_audit_action_time ON sys_audit_log(action, created_at DESC);
     *         CREATE INDEX idx_audit_target_time ON sys_audit_log(target_type, target_id, created_at DESC);
     *         CREATE INDEX idx_audit_creator_time ON sys_audit_log(created_by, created_at DESC);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Time Range Validation</strong>: Reject queries with time range > 90 days to prevent full-table scans</li>
     *     <li><strong>Async Export</strong>: For large result sets, consider async export to file instead of direct pagination</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Admin endpoint for audit log query
     * @GetMapping("/audit-logs")
     * @PreAuthorize("hasRole('ADMIN')")
     * @Operation(summary = "Query audit logs", description = "Retrieve paginated audit trail with filtering")
     * public Result<IPage<AuditLogPageVO>> listAuditLogs(AuditLogPageRequest request) {
     *     // Validate time range (max 90 days)
     *     if (request.getStartTime() != null && request.getEndTime() != null) {
     *         long days = ChronoUnit.DAYS.between(request.getStartTime(), request.getEndTime());
     *         if (days > 90) {
     *             return Result.fail(ErrorCode.AUDIT_TIME_RANGE_TOO_LARGE);
     *         }
     *     }
     *
     *     // Execute query
     *     IPage<AuditLogPageVO> page = auditLogService.page(request);
     *
     *     // Return paginated result
     *     return Result.success(page.getRecords(), page.getTotal());
     * }
     *
     * // Frontend: Vue 3 table with filters
     * const { data: auditPage, loading } = useRequest(() =>
     *   api.getAuditLogs({
     *     page: pagination.current,
     *     size: pagination.pageSize,
     *     action: filters.action,
     *     targetType: filters.targetType,
     *     targetId: filters.targetId,
     *     startTime: filters.timeRange?.[0],
     *     endTime: filters.timeRange?.[1]
     *   })
     * );
     * }
     * </pre>
     * <p>
     * <strong>Exception Handling:</strong>
     * <ul>
     *     <li><strong>Validation Errors</strong>: Throw {@code IllegalArgumentException} for invalid parameters (e.g., endTime &lt; startTime)</li>
     *     <li><strong>Database Errors</strong>: Propagate to global exception handler for consistent error response</li>
     *     <li><strong>Security Violations</strong>: Throw {@code AccessDeniedException} if caller lacks required permissions</li>
     * </ul>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link AuditLogPageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException                                  if {@code pageInfo} is {@code null} or contains invalid filter combinations
     * @throws org.springframework.security.access.AccessDeniedException if caller lacks required permissions
     * @see AuditLogPageRequest
     * @see AuditLogPageVO
     * @see IPage
     * @see com.github.starhq.template.mapper.SysAuditLogMapper#selectAuditLogPage(Page, Wrapper)
     */
    IPage<AuditLogPageVO> page(AuditLogPageRequest pageInfo);

}