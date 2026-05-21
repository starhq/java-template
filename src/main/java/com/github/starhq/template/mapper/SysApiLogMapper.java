package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.starhq.template.common.constant.ProfileConstants;
import com.github.starhq.template.entity.SysApiLog;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.context.annotation.Profile;

/**
 * MyBatis-Plus mapper interface for {@link SysApiLog} entity persistence.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations:
 * <ul>
 *     <li>{@code insert(T entity)} — Insert a new API log record</li>
 *     <li>{@code selectById(Serializable id)} — Fetch log by primary key</li>
 *     <li>{@code selectList(LambdaQueryWrapper<T>)} — Query logs with type-safe conditions</li>
 *     <li>{@code update(T entity, LambdaUpdateWrapper<T>)} — Update logs with conditional logic</li>
 *     <li>{@code deleteById(Serializable id)} — Remove log by primary key</li>
 * </ul>
 * <p>
 * <strong>Environment Isolation:</strong>
 * <p>
 * Annotated with {@code @Profile(ProfileConstants.DEV)} to activate this mapper
 * <strong>only in development environments</strong>. This design enables:
 * <ul>
 *     <li><strong>Local Testing</strong>: Persist API logs to database for debugging without affecting production</li>
 *     <li><strong>Environment Separation</strong>: Production may use alternative storage (e.g., Elasticsearch, file-based logging)</li>
 *     <li><strong>Resource Optimization</strong>: Avoid unnecessary database writes in high-traffic production environments</li>
 * </ul>
 * <p>
 * <strong>Usage Guidance:</strong>
 * <ul>
 *     <li>Inject via constructor or field injection: {@code @Autowired private SysApiLogMapper apiLogMapper;}</li>
 *     <li>Prefer {@link com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper} for type-safe queries</li>
 *     <li>For custom queries, define methods with {@code @Select}/{@code @Insert} annotations or XML mappings</li>
 *     <li>Always wrap batch operations in {@code @Transactional} for atomicity</li>
 * </ul>
 * <p>
 * <strong>Extension Example:</strong>
 * <pre>
 * {@code
 * // Custom method: Fetch logs by trace ID with pagination
 * @Select("SELECT * FROM sys_api_log WHERE trace_id = #{traceId} ORDER BY create_time DESC")
 * IPage<SysApiLog> selectByTraceId(@Param("traceId") String traceId, Page<SysApiLog> page);
 *
 * // Custom method: Batch insert for high-throughput scenarios
 * int insertBatch(@Param("list") List<SysApiLog> logs);
 * }
 * </pre>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysApiLog
 * @see ProfileConstants
 * @see org.springframework.context.annotation.Profile
 */
@Profile(ProfileConstants.DEV)
@Mapper
public interface SysApiLogMapper extends BaseMapper<SysApiLog> {

    // ========== Inherited Methods from BaseMapper<SysApiLog> ==========
    //
    // The following methods are automatically provided by MyBatis-Plus:
    //
    // // Insert
    // int insert(SysApiLog entity);
    //
    // // Select
    // SysApiLog selectById(Serializable id);
    // List<SysApiLog> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysApiLog> selectByMap(Map<String, Object> columnMap);
    // SysApiLog selectOne(LambdaQueryWrapper<SysApiLog> queryWrapper);
    // List<SysApiLog> selectList(LambdaQueryWrapper<SysApiLog> queryWrapper);
    // <E extends IPage<SysApiLog>> E selectPage(E page, LambdaQueryWrapper<SysApiLog> queryWrapper);
    //
    // // Update
    // int updateById(SysApiLog entity);
    // int update(SysApiLog entity, LambdaUpdateWrapper<SysApiLog> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysApiLog> queryWrapper);
    //
    // For detailed usage, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}