package com.github.starhq.template.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.model.vo.token.TokenPageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * MyBatis-Plus mapper interface for {@link SysToken} entity with pagination and upsert support.
 * <p>
 * This interface extends {@link BaseMapper} to inherit standard CRUD operations for token
 * management, and provides specialized methods for paginated queries and efficient token
 * upsert operations in authentication and session management scenarios.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Token Management Console</strong>: Paginated listing of active/inactive tokens with filtering</li>
 *     <li><strong>Session Management</strong>: Efficient upsert of user tokens during login/logout</li>
 *     <li><strong>Security Auditing</strong>: Query token usage patterns and detect anomalies</li>
 * </ul>
 * <p>
 * <strong>Design Principles:</strong>
 * <ul>
 *     <li><strong>Pagination Support</strong>: Use {@link Page} and {@link IPage} for efficient large dataset handling</li>
 *     <li><strong>Dynamic Query</strong>: Leverage {@link Wrapper} for flexible, type-safe filtering</li>
 *     <li><strong>Upsert Efficiency</strong>: Single SQL operation for insert or update to avoid race conditions</li>
 *     <li><strong>Cache Integration</strong>: Results suitable for Redis token cache with TTL</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * @Service
 * public class TokenService {
 *     @Autowired private SysTokenMapper tokenMapper;
 *
 *     public IPage<TokenPageVO> queryTokens(PageRequest request) {
 *         Page<TokenPageVO> page = new Page<>(request.getPage(), request.getSize());
 *
 *         LambdaQueryWrapper<TokenPageVO> wrapper = new LambdaQueryWrapper<TokenPageVO>()
 *             .eq(TokenPageVO::getUserId, request.getUserId())
 *             .eq(TokenPageVO::getStatus, request.getStatus())
 *             .orderByDesc(TokenPageVO::getCreatedAt);
 *
 *         return tokenMapper.selectTokenPage(page, wrapper);
 *     }
 *
 *     @Transactional
 *     public void upsertToken(SysToken token) {
 *         tokenMapper.upsertToken(token);
 *         // Invalidate token cache
 *         cacheHelper.evict(token.getUserId(), List.of(CacheConstant.USER_TOKENS));
 *     }
 * }
 * }
 * </pre>
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see BaseMapper
 * @see SysToken
 * @see TokenPageVO
 * @see com.github.starhq.template.service.TokenService
 */
@Mapper
public interface SysTokenMapper extends BaseMapper<SysToken> {

    /**
     * Executes a paginated query for tokens, mapping results to {@link TokenPageVO}.
     * <p>
     * This method combines MyBatis-Plus pagination with custom result mapping to efficiently
     * fetch token data for admin console display. The query supports:
     * <ul>
     *     <li><strong>Dynamic Filtering</strong>: Via {@link Wrapper} parameter for type-safe conditions</li>
     *     <li><strong>Field Selection</strong>: Only selects fields required by {@code TokenPageVO} to reduce payload</li>
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
     * mapping or annotation-based query references {@code ${ew.sqlSegment}} or
     * {@code <where>${ew.sqlSegment}</where>} for proper condition injection.
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Add composite indexes on frequently filtered fields: {@code (user_id, created_at)}, {@code (status, created_at)}</li>
     *     <li>For large token datasets, consider time-range partitioning on {@code created_at} column</li>
     *     <li>Avoid {@code SELECT *} — the custom result mapping should select only required fields</li>
     *     <li>Use {@code last("LIMIT ...")} cautiously; prefer {@code Page} parameter for consistent pagination</li>
     * </ul>
     * <p>
     * <strong>XML Mapping Example:</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysTokenMapper.xml -->
     * <select id="selectTokenPage" resultType="com.github.starhq.template.model.vo.token.TokenPageVO">
     *     SELECT
     *         t.id, t.user_id, t.token, t.token_type, t.status,
     *         t.expires_at, t.created_at, t.created_by, u.username as creatorName
     *     FROM sys_token t
     *     LEFT JOIN sys_user u ON t.created_by = u.id
     *     <where>
     *         ${ew.sqlSegment}  <!-- Dynamic conditions from Wrapper -->
     *     </where>
     *     ORDER BY t.created_at DESC
     * </select>
     * }
     * </pre>
     *
     * @param page    the pagination context containing page number, size, and sort orders; must not be {@code null}
     * @param wrapper the query conditions wrapper for dynamic filtering; may be {@code null} for unfiltered queries
     * @return an {@link IPage} containing paginated {@link TokenPageVO} results with total count
     * @throws com.baomidou.mybatisplus.core.exceptions.MybatisPlusException if query execution fails
     * @see Page
     * @see Wrapper
     * @see Constants#WRAPPER
     * @see IPage#getRecords()
     * @see IPage#getTotal()
     */
    IPage<TokenPageVO> selectTokenPage(
            @Param("page") Page<TokenPageVO> page,
            @Param(Constants.WRAPPER) Wrapper<TokenPageVO> wrapper

    );

    /**
     * Performs insert or update of a token entity.
     * <p>
     * This method uses database-specific upsert syntax to efficiently handle token creation
     * or refresh in a single operation. Designed for authentication flows where tokens
     * need to be created on first login or updated on refresh.
     * <p>
     * <strong>Operation Semantics:</strong>
     * <ul>
     *     <li><strong>Insert</strong>: If token doesn't exist, create new record</li>
     *     <li><strong>Update</strong>: If token exists, update fields (e.g., expires_at, status)</li>
     *     <li><strong>Atomic</strong>: Single SQL operation ensures no race conditions</li>
     *     <li><strong>Fail-Fast</strong>: Constraint violations abort the operation immediately</li>
     * </ul>
     * <p>
     * <strong>Parameter Contracts:</strong>
     * <ul>
     *     <li>{@code token}: {@link SysToken} entity to upsert; must not be {@code null}</li>
     *     <li>Entity must have valid {@code userId} referencing existing user</li>
     *     <li>Token value should be properly hashed/encoded before storage</li>
     * </ul>
     * <p>
     * <strong>Transaction Guidance:</strong>
     * <p>
     * Callers should wrap this method in {@code @Transactional} to ensure atomicity with related operations:
     * <pre>
     * {@code
     * @Transactional
     * public void refreshToken(Long userId, String newToken) {
     *     // 1. Invalidate old token
     *     tokenMapper.updateStatus(userId, TokenStatus.INVALID);
     *
     *     // 2. Insert new token (this method)
     *     SysToken token = new SysToken(userId, newToken);
     *     tokenMapper.upsertToken(token);
     *
     *     // 3. Update cache
     *     cacheHelper.put(buildTokenKey(userId), newToken, token.getExpiresAt());
     * }
     * }
     * </pre>
     * <p>
     * <strong>Performance Considerations:</strong>
     * <ul>
     *     <li>Ensure unique key {@code UNIQUE KEY uk_user_token (user_id, token)} exists for efficient upsert</li>
     *     <li>Add index on {@code (user_id, status)} for quick token status queries</li>
     *     <li>Monitor execution time; optimize with database-specific batch syntax if needed</li>
     * </ul>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li>Token values should be hashed (e.g., SHA-256) before storage to protect against data breaches</li>
     *     <li>Consider implementing token rotation to limit exposure window</li>
     *     <li>Log token upsert operations for security audit trails</li>
     * </ul>
     *
     * @param token the token entity to insert or update; must not be {@code null}
     * @throws com.baomidou.mybatisplus.core.exceptions.MybatisPlusException if operation fails
     * @throws org.springframework.dao.DataIntegrityViolationException       if foreign key constraints are violated
     * @see SysToken
     */
    void upsertToken(SysToken token);

    // ========== Inherited Methods from BaseMapper<SysToken> ==========
    //
    // The following standard CRUD methods are automatically provided by MyBatis-Plus:
    //
    // // Insert
    // int insert(SysToken entity);
    //
    // // Select
    // SysToken selectById(Serializable id);
    // List<SysToken> selectBatchIds(Collection<? extends Serializable> idList);
    // List<SysToken> selectByMap(Map<String, Object> columnMap);
    // SysToken selectOne(LambdaQueryWrapper<SysToken> queryWrapper);
    // List<SysToken> selectList(LambdaQueryWrapper<SysToken> queryWrapper);
    // <E extends IPage<SysToken>> E selectPage(E page, LambdaQueryWrapper<SysToken> queryWrapper);
    //
    // // Update
    // int updateById(SysToken entity);
    // int update(SysToken entity, LambdaUpdateWrapper<SysToken> updateWrapper);
    //
    // // Delete
    // int deleteById(Serializable id);
    // int deleteByMap(Map<String, Object> columnMap);
    // int delete(LambdaQueryWrapper<SysToken> queryWrapper);
    //
    // For detailed usage and advanced features, refer to:
    // @see <a href="https://baomidou.com/pages/49cc81/">MyBatis-Plus BaseMapper Guide</a>

}
