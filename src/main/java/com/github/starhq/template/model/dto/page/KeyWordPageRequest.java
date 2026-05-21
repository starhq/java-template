package com.github.starhq.template.model.dto.page;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * Pagination request parameters with keyword search support for generic list queries.
 * <p>
 * This class extends {@link PageRequest} to inherit standard pagination fields
 * ({@code page}, {@code size}, {@code sort}) and adds a {@code keyword} filter
 * for fuzzy text search across one or more designated columns. Designed for
 * admin console tables, user-facing search interfaces, and audit log filtering.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>Admin Console</strong>: Filter users, roles, menus by name/code/description via keyword</li>
 *     <li><strong>Audit Search</strong>: Narrow audit logs by action, target, or operator name</li>
 *     <li><strong>Public Search</strong>: Enable end-user content discovery with simple text input</li>
 * </ul>
 * <p>
 * <strong>Query Semantics:</strong>
 * <p>
 * The {@code keyword} field typically triggers {@code LIKE '%keyword%'} fuzzy matching
 * against pre-configured columns (e.g., {@code name}, {@code code}, {@code description}).
 * Exact matching behavior and target columns should be defined at the Service/Mapper layer
 * to maintain separation of concerns and prevent SQL injection.
 * <p>
 * <strong>Security Considerations:</strong>
 * <ul>
 *     <li><strong>SQL Injection Prevention</strong>: Always use parameterized queries ({@code #{keyword}} in MyBatis); never concatenate user input into SQL</li>
 *     <li><strong>Input Sanitization</strong>: Trim whitespace and validate length to prevent excessive wildcard expansion</li>
 *     <li><strong>Rate Limiting</strong>: Implement per-IP search rate limits to mitigate enumeration or DoS attacks</li>
 *     <li><strong>Result Filtering</strong>: Ensure keyword searches respect data access permissions (e.g., user can only search their own records)</li>
 * </ul>
 *
 * @author wangjian
 * @author starhq (maintainer)
 * @version 1.0
 * @date 2026-05-20
 * @see PageRequest
 * @see com.baomidou.mybatisplus.core.mapper.BaseMapper
 * @see org.springframework.util.StringUtils
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class KeyWordPageRequest extends PageRequest {

    /**
     * Serial version UID for serialization compatibility.
     * <p>
     * Required when this DTO is stored in distributed caches (Redis) or
     * transmitted across service boundaries. Update this value only if the
     * class structure changes in a backward-incompatible way.
     *
     * @see java.io.Serializable
     */
    @Serial
    private static final long serialVersionUID = 8515119070863756538L;

    /**
     * Free-text keyword for fuzzy search across designated columns.
     * <p>
     * Typical matching behavior:
     * <ul>
     *     <li><strong>Fuzzy Match</strong>: {@code LIKE CONCAT('%', :keyword, '%')} for substring search</li>
     *     <li><strong>Case-Insensitive</strong>: Recommended for user-friendly search (database collation or {@code LOWER()} function)</li>
     *     <li><strong>Multi-Column</strong>: Search across multiple fields via {@code OR} conditions (e.g., {@code name LIKE ? OR code LIKE ?})</li>
     * </ul>
     * <p>
     * <strong>Technical Details:</strong>
     * <ul>
     *     <li>Type: {@link String} — trimmed before query execution to handle user input variations</li>
     *     <li>Nullability: {@code null} or empty string means no keyword filtering (return all records)</li>
     *     <li>Length: Recommended max 100 characters to prevent performance degradation from excessive wildcard expansion</li>
     * </ul>
     * <p>
     * <strong>Query Example (MyBatis XML):</strong>
     * <pre>
     * {@code
     * <!-- resources/mapper/SysUserMapper.xml -->
     * <select id="selectUserPage" resultType="com.github.starhq.template.model.vo.user.UserPageVO">
     *     SELECT id, username, email, status, created_at
     *     FROM sys_user
     *     <where>
     *         <if test="keyword != null and keyword != ''">
     *             AND (
     *                 username LIKE CONCAT('%', #{keyword}, '%') OR
     *                 email LIKE CONCAT('%', #{keyword}, '%') OR
     *                 nickname LIKE CONCAT('%', #{keyword}, '%')
     *             )
     *         </if>
     *         <!-- Other filters... -->
     *     </where>
     *     ORDER BY created_at DESC
     * </select>
     * }
     * </pre>
     * <p>
     * <strong>Performance Optimization:</strong>
     * <ul>
     *     <li><strong>Index Strategy</strong>: For frequent keyword searches, consider full-text indexes (MySQL {@code FULLTEXT}, PostgreSQL {@code tsvector}) instead of {@code LIKE '%...%'}</li>
     *     <li><strong>Prefix Matching</strong>: If business allows, use {@code LIKE 'keyword%'} to leverage B-tree indexes</li>
     *     <li><strong>Search Scope</strong>: Limit searchable columns to 2-3 high-value fields to avoid full-table scans</li>
     *     <li><strong>Pagination</strong>: Always combine with pagination to limit result set size and response time</li>
     * </ul>
     * <p>
     * <strong>Validation Recommendations:</strong>
     * <ul>
     *     <li>Trim input: {@code keyword = keyword?.trim()} before query execution</li>
     *     <li>Length check: Reject keywords > 100 chars to prevent performance abuse</li>
     *     <li>Character whitelist: Optionally restrict to {@code [a-zA-Z0-9\u4e00-\u9fa5_@.+ -]} for safety</li>
     *     <li>Empty handling: Treat empty string as {@code null} (no filtering) for consistent behavior</li>
     * </ul>
     * <p>
     * <strong>Usage Example:</strong>
     * <pre>
     * {@code
     * // Controller: Accept keyword from query param or request body
     * @GetMapping("/users")
     * public Result<IPage<UserPageVO>> listUsers(KeyWordPageRequest request) {
     *     // Optional: sanitize keyword
     *     if (request.getKeyword() != null) {
     *         request.setKeyword(request.getKeyword().trim());
     *     }
     *     return Result.success(userService.page(request));
     * }
     *
     * // Service: Build query with keyword filter
     * public IPage<UserPageVO> page(KeyWordPageRequest request) {
     *     LambdaQueryWrapper<UserPageVO> wrapper = new LambdaQueryWrapper<>()
     *         .like(StringUtils.hasText(request.getKeyword()), UserPageVO::getUsername, request.getKeyword())
     *         .or().like(StringUtils.hasText(request.getKeyword()), UserPageVO::getEmail, request.getKeyword())
     *         .orderByDesc(UserPageVO::getCreatedAt);
     *
     *     Page<UserPageVO> page = new Page<>(request.getPage(), request.getSize());
     *     return userMapper.selectPage(page, wrapper);
     * }
     * }
     * </pre>
     *
     * @see org.springframework.util.StringUtils#hasText(CharSequence)
     * @see com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper#like(boolean, Object, Object)
     */
    private String keyword;

}