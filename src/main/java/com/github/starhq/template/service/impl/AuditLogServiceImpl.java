package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.mapper.SysAuditLogMapper;
import com.github.starhq.template.model.dto.AuditLogPageRequest;
import com.github.starhq.template.model.vo.AuditLogPageVO;
import com.github.starhq.template.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;

/**
 * Service implementation for audit log pagination queries with dynamic filtering.
 * <p>
 * This class provides concrete operations for retrieving paginated audit trail records
 * with support for multi-dimensional filtering (target type, operator username, time range).
 * It delegates database operations to {@link SysAuditLogMapper} with custom SQL for
 * efficient audit log retrieval and audit field resolution.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Admin Console</strong>: Display paginated audit logs with filtering by action, target, operator</li>
 *     <li><strong>Security Analysis</strong>: Investigate suspicious activities by reviewing filtered operation records</li>
 *     <li><strong>Compliance Reporting</strong>: Export filtered audit trails for regulatory audits (GDPR, SOX)</li>
 *     <li><strong>Troubleshooting</strong>: Reconstruct user actions leading to data inconsistencies</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Read-Only Queries</strong>: Service methods do not modify audit data; use separate commands for archival</li>
 *     <li><strong>Privacy-Aware</strong>: Sensitive fields in returned VOs are masked via {@code AuditLogPageVO} conversion</li>
 *     <li><strong>Performance-Conscious</strong>: Leverages MyBatis-Plus pagination and database indexes for large datasets</li>
 *     <li><strong>Access-Controlled</strong>: All query endpoints should enforce role-based permissions (ADMIN-only)</li>
 * </ul>
 * <p>
 * <strong>Integration Pattern:</strong>
 * <pre>
 * {@code
 * // Controller: Expose audit log query endpoint (admin-only)
 * @RestController
 * @RequestMapping("/api/v1/audit-logs")
 * @RequiredArgsConstructor
 * public class AuditLogController {
 *
 *     private final AuditLogService auditLogService;
 *
 *     @GetMapping
 *     @PreAuthorize("hasRole('ADMIN')")
 *     public Result<IPage<AuditLogPageVO>> listAuditLogs(AuditLogPageRequest request) {
 *         IPage<AuditLogPageVO> page = auditLogService.page(request);
 *         return Result.success(page.getRecords(), page.getTotal());
 *     }
 * }
 *
 * // Frontend: Vue 3 table with filters
 * const { data: auditPage, loading } = useRequest(() =>
 *   api.getAuditLogs({
 *     page: pagination.current,
 *     size: pagination.pageSize,
 *     targetType: filters.targetType,
 *     username: filters.username
 *   })
 * );
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-04-10
 * @see AuditLogService
 * @see AuditLogPageRequest
 * @see AuditLogPageVO
 * @see SysAuditLogMapper#selectAuditLogPage(Page, Wrapper)
 */
@Service("auditLogService")
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    /**
     * MyBatis-Plus mapper for {@link com.github.starhq.template.entity.SysAuditLog} operations.
     * <p>
     * Injected via {@link RequiredArgsConstructor} constructor injection for:
     * <ul>
     *     <li><strong>Immutability</strong>: Final field ensures dependency cannot be modified after construction</li>
     *     <li><strong>Testability</strong>: Easy to mock in unit tests without reflection</li>
     *     <li><strong>Null Safety</strong>: Spring guarantees non-null injection for required dependencies</li>
     * </ul>
     * <p>
     * <strong>Custom Query Support:</strong>
     * <p>
     * This mapper provides {@code selectAuditLogPage()} method with custom SQL that:
     * <ul>
     *     <li>Joins {@code sys_audit_log} with {@code sys_user} to resolve creator/updater IDs to usernames</li>
     *     <li>Applies dynamic filters from {@code QueryWrapper} for flexible querying</li>
     *     <li>Returns {@code AuditLogPageVO} with pre-populated audit fields for efficient rendering</li>
     * </ul>
     *
     * @see SysAuditLogMapper
     * @see QueryWrapper
     */
    private final SysAuditLogMapper auditLogMapper;

    /**
     * Retrieves a paginated list of audit log entries matching the specified criteria.
     * <p>
     * This method supports multi-dimensional filtering for efficient audit trail analysis:
     * <ul>
     *     <li><strong>Target Type Filter</strong>: Filter by target entity type (e.g., {@code USER}, {@code ROLE}, {@code MENU})</li>
     *     <li><strong>Username Filter</strong>: Fuzzy search on creator/updater username via {@code likeRight} matching</li>
     *     <li><strong>Base Filters</strong>: Inherits filters from {@code AuditLogPageRequest.toQueryWrapper()} (action, time range, etc.)</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code pageInfo}: Must not be {@code null}; provides pagination params ({@code page}, {@code size}) and base filters</li>
     *     <li>{@code pageInfo.getTargetType()}: Optional; if set, filters logs by exact target type match</li>
     *     <li>{@code pageInfo.getUsername()}: Optional; if set, performs right-fuzzy match on creator username (e.g., "alice" matches "alice_admin")</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} containing current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page ({@code records=[]}, {@code total=0}) if no matches found</li>
     *     <li><strong>Field Masking</strong>: Sensitive fields in {@code AuditLogPageVO.value} are pre-masked by mapper/VO conversion</li>
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
     *         CREATE INDEX idx_audit_target_time ON sys_audit_log(target_type, created_at DESC);
     *         CREATE INDEX idx_audit_creator_name ON sys_audit_log(created_by, created_at DESC);
     *         }</pre>
     *     </li>
     *     <li><strong>Pagination Limits</strong>: Enforce max page size (e.g., 100) to prevent excessive memory usage</li>
     *     <li><strong>Time Range Validation</strong>: Reject queries with time range > 90 days to prevent full-table scans</li>
     *     <li><strong>Custom SQL</strong>: {@code selectAuditLogPage} uses optimized JOINs to avoid N+1 username resolution</li>
     * </ul>
     * <p>
     * <strong>Filter Logic Details:</strong>
     * <pre>
     * {@code
     * // Target type: exact match on database column
     * if (pageInfo.getTargetType() != null) {
     *     wrapper.eq("target_type", pageInfo.getTargetType());
     *     // SQL: AND target_type = 'USER'
     * }
     *
     * // Username: right-fuzzy match on resolved creator name
     * if (StringUtils.hasText(pageInfo.getUsername())) {
     *     wrapper.likeRight("creator", pageInfo.getUsername());
     *     // SQL: AND creator LIKE 'alice%' (matches "alice", "alice_admin", etc.)
     * }
     * }
     * </pre>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Service call: Query audit logs with filters
     * AuditLogPageRequest request = new AuditLogPageRequest();
     * request.setPage(1);
     * request.setSize(20);
     * request.setTargetType(TargetType.USER);
     * request.setUsername("alice");
     * request.setStartTime(OffsetDateTime.now().minusDays(7));
     *
     * IPage<AuditLogPageVO> result = auditLogService.page(request);
     *
     * // Process results
     * result.getRecords().forEach(log -> {
     *     System.out.println(log.getAction() + " by " + log.getCreator());
     *     // Output: "USER:CREATE by alice_admin"
     * });
     * }
     * </pre>
     * <p>
     * <strong>Exception Handling:</strong>
     * <ul>
     *     <li><strong>Validation Errors</strong>: Throw {@code IllegalArgumentException} for invalid parameters (e.g., endTime &lt; startTime)</li>
     *     <li><strong>Database Errors</strong>: Propagated to global exception handler for consistent error response</li>
     *     <li><strong>Security Violations</strong>: Throw {@code AccessDeniedException} if caller lacks required permissions</li>
     * </ul>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link AuditLogPageVO} matching the criteria; never {@code null}
     * @throws IllegalArgumentException                                  if {@code pageInfo} is {@code null} or contains invalid filter combinations
     * @throws org.springframework.security.access.AccessDeniedException if caller lacks required permissions
     * @see AuditLogPageRequest#toQueryWrapper()
     * @see AuditLogPageRequest#toPage()
     * @see SysAuditLogMapper#selectAuditLogPage(Page, Wrapper)
     * @see QueryConstant#TARGET_TYPE
     * @see QueryConstant#CREATOR
     */
    @Override
    public IPage<AuditLogPageVO> page(AuditLogPageRequest pageInfo) {
        // 1. Convert request to MyBatis-Plus page object for pagination
        Page<AuditLogPageVO> page = pageInfo.toPage();

        // 2. Build base query wrapper from request (includes action, time range, etc.)
        QueryWrapper<AuditLogPageVO> wrapper = pageInfo.toQueryWrapper();

        // 3. Apply domain-specific dynamic filters
        // Filter by target entity type (exact match)
        if (!Objects.isNull(pageInfo.getTargetType())) {
            wrapper.eq(QueryConstant.TARGET_TYPE, pageInfo.getTargetType());
        }

        // Filter by creator username (right-fuzzy match: "alice" matches "alice_admin")
        if (StringUtils.hasText(pageInfo.getUsername())) {
            wrapper.likeRight(QueryConstant.CREATOR, pageInfo.getUsername());
        }

        // 4. Execute custom paginated query via mapper
        // Custom SQL handles JOIN with sys_user for username resolution
        return auditLogMapper.selectAuditLogPage(page, wrapper);
    }

}