package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysAuditLog;
import com.github.starhq.template.model.vo.auditlog.AuditLogPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis-Plus mapper interface for {@link SysAuditLog} entity with custom pagination support.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for audit logs,
 * and provides a specialized method {@link #selectAuditLogPage} for efficient pagination queries
 * that map entity fields to {@link AuditLogPageVO} for admin console display.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Audit Console</strong>: Paginated listing of system operations with filtering by action, user, time range</li>
 *     <li><strong>Security Analysis</strong>: Query suspicious patterns via custom {@link Wrapper} conditions</li>
 *     <li><strong>Compliance Reporting</strong>: Export filtered audit trails for regulatory audits</li>
 * </ul>
 * <p>
 * <strong>Custom Query Design:</strong>
 * <p>
 * The {@code selectAuditLogPage} method leverages MyBatis-Plus's {@code Constants.WRAPPER}
 * parameter injection to support dynamic, type-safe query conditions while maintaining
 * separation between entity persistence ({@code SysAuditLog}) and presentation ({@code AuditLogPageVO}).
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class AuditLogService {
 *     @Autowired private SysAuditLogMapper auditLogMapper;
 *
 *     public IPage<AuditLogPageVO> queryAuditLogs(PageRequest request) {
 *         Page<AuditLogPageVO> page = new Page<>(request.getPage(), request.getSize());
 *
 *         LambdaQueryWrapper<AuditLogPageVO> wrapper = new LambdaQueryWrapper<AuditLogPageVO>()
 *             .eq(AuditLogPageVO::getAction, request.getAction())  // Filter by action
 *             .ge(AuditLogPageVO::getCreatedAt, request.getStartTime())  // Time range start
 *             .le(AuditLogPageVO::getCreatedAt, request.getEndTime())    // Time range end
 *             .orderByDesc(AuditLogPageVO::getCreatedAt);  // Sort by newest first
 *
 *         return auditLogMapper.selectAuditLogPage(page, wrapper);
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysAuditLog
 * @see AuditLogPageVO
 * @see com.baomidou.mybatisplus.core.toolkit.Constants#WRAPPER
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {

    /**
     * Executes a paginated query for audit logs, mapping results to {@link AuditLogPageVO}.
     * <p>
     * This method combines MyBatis-Plus pagination with custom result mapping to efficiently
     * fetch audit trail data for admin console display. The query supports:
     * <ul>
     *     <li><strong>Dynamic Filtering</strong>: Via {@link Wrapper} parameter for type-safe conditions</li>
     *     <li><strong>Field Selection</strong>: Only selects fields required by {@code AuditLogPageVO} to reduce payload</li>
     *     <li><strong>Sorting & Pagination</strong>: Handled by {@link Page} parameter with automatic count query</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code page}: Pagination context (page number, size, sort orders); must not be {@code null}</li>
     *     <li>{@code wrapper}: Query conditions wrapper; may be {@code null} for unfiltered queries</li>
     * </ul>
     * <p>
     * <strong>MyBatis-Plus Integration:</strong>
     * <p>
     * The {@code @Param(Constants.WRAPPER)} annotation enables MyBatis-Plus to inject
     * {@link Wrapper} conditions into the SQL dynamically. Ensure the corresponding XML
     * mapping or annotation-based query references {@code ${ew.customColumnSegment} or
     * {@code <where>${ew.sqlSegment}</where>} for proper condition injection.
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add composite indexes on frequently filtered fields: {@code (action, created_at)}, {@code (created_by, created_at)}</li>
     *     <li>For large datasets, consider time-range partitioning on {@code created_at} column</li>
     *     <li>Avoid {@code SELECT *} — the custom result mapping should select only required fields</li>
     *     <li>Use {@code last("LIMIT ...")} cautiously; prefer {@code Page} parameter for consistent pagination</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysAuditLogMapper.xml -->
     * <select id="selectAuditLogPage" resultType="com.github.starhq.template.model.vo.auditlog.AuditLogPageVO">
     *     SELECT
     *         a.id, a.action, a.target_type, a.target_id, a.value,
     *         a.created_at, a.created_by, u.username as creatorName
     *     FROM sys_audit_log a
     *     LEFT JOIN sys_user u ON a.created_by = u.id
     *     <where>
     *         ${ew.sqlSegment}  <!-- Dynamic conditions from Wrapper -->
     *     </where>
     *     ORDER BY a.created_at DESC
     * </select>
     * }
     * </pre>
     *
     * @param page    the pagination context containing page number, size, and sort orders; must not be {@code null}
     * @param wrapper the query conditions wrapper for dynamic filtering; may be {@code null} for unfiltered queries
     * @return an {@link IPage} containing paginated {@link AuditLogPageVO} results with total count
     * @throws com.baomidou.mybatisplus.core.exceptions.MybatisPlusException if query execution fails
     * @see Page
     * @see Wrapper
     * @see Constants#WRAPPER
     * @see IPage#getRecords()
     * @see IPage#getTotal()
     */
    IPage<AuditLogPageVO> selectAuditLogPage(
            @Param("page") Page<AuditLogPageVO> page,
            @Param(Constants.WRAPPER) Wrapper<AuditLogPageVO> wrapper

    );

    // ========== Inherited Methods from BaseMapper<SysAuditLog> ==========
    //
    // The following standard CRUD methods are automatically provided by MyBatis-Plus:
    //
    // // Insert
    // int insert(SysAuditLog entity);
    //
    // // Select
    // SysAuditLog selectById(Serializable id);
    // List<SysAuditLog> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysAuditLog> selectByMap(Map<String, Object> columnMap);
    // SysAuditLog selectOne(LambdaQueryWrapper<SysAuditLog> queryWrapper);
    // List<SysAuditLog> selectList(LambdaQueryWrapper<SysAuditLog> queryWrapper);
    // <E extends IPage<SysAuditLog>> E selectPage(E page, LambdaQueryWrapper<SysAuditLog> queryWrapper);
    //
    // // Update
    // int updateById(SysAuditLog entity);
    // int update(SysAuditLog entity, LambdaUpdateWrapper<SysAuditLog> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysAuditLog> queryWrapper);
    //
    // For detailed usage and advanced features, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}