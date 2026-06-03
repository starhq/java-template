package com.github.starhq.template.model.dto;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.enums.SortEnum;
import com.github.starhq.template.common.validation.SortField;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Base class for pagination request parameters in API endpoints.
 * <p>
 * This class provides standard pagination fields ({@code page}, {@code size}, {@code sort}, {@code asc})
 * and utility methods to convert to MyBatis-Plus pagination objects. It is designed to be extended
 * by specific request DTOs that need pagination support.
 * <p>
 * <strong>Primary Use Cases:</strong>
 * <ul>
 *     <li><strong>API Endpoints</strong>: Base class for all paginated request DTOs</li>
 *     <li><strong>Database Queries</strong>: Convert to {@link Page} or {@link QueryWrapper} for MyBatis-Plus</li>
 *     <li><strong>Default Value Handling</strong>: Automatic fallback to sensible defaults</li>
 * </ul>
 * <p>
 * <strong>Default Values:</strong>
 * <p>
 * The class implements defensive getter methods that provide sensible defaults:
 * <ul>
 *     <li>{@code page}: Defaults to {@code 1} if {@code null}</li>
 *     <li>{@code size}: Defaults to {@code 10}, capped at {@code 100}</li>
 *     <li>{@code sort}: Defaults to {@code "created_at"} if {@code null} or empty</li>
 *     <li>{@code asc}: Defaults to {@link SortEnum#DESC} (latest first) if {@code null}</li>
 * </ul>
 * <p>
 * <strong>Validation Rules:</strong>
 * <ul>
 *     <li>{@code page}: Must be {@code >= 1} (minimum page number)</li>
 *     <li>{@code size}: Must be {@code >= 1} and {@code <= 100} (pagination limit)</li>
 *     <li>{@code sort}: Validated by {@link SortField} annotation for safe column names</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * <pre>
 * {@code
 * // Extend for specific request types
 * @Data
 * @EqualsAndHashCode(callSuper = false)
 * public class UserPageRequest extends PageRequest {
 *     private String username;
 *     private UserStatus status;
 * }
 *
 * @Service
 * public class UserService {
 *     public IPage<UserVO> page(UserPageRequest request) {
 *         // Convert to MyBatis-Plus Page
 *         Page<User> pageParam = request.toPage();
 *
 *         // Build query wrapper
 *         QueryWrapper<User> wrapper = request.toQueryWrapper();
 *         wrapper.like(StringUtils.hasText(request.getUsername()), "username", request.getUsername());
 *
 *         return userMapper.selectPage(pageParam, wrapper);
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <strong>MyBatis-Plus Integration:</strong>
 * <p>
 * The {@link #toPage()} and {@link #toQueryWrapper()} methods provide seamless integration:
 * <ul>
 *     <li>{@code toPage()}: Creates a {@link Page} object with pagination parameters</li>
 *     <li>{@code toQueryWrapper()}: Creates a {@link QueryWrapper} with sorting applied</li>
 * </ul>
 * <p>
 * <strong>Thread Safety:</strong>
 * <p>
 * This class is immutable after construction and thread-safe for read operations.
 * However, the {@code page} and {@code size} fields are mutable; create new instances
 * for concurrent operations.
 *
 * @author starhq
 * @version 1.0
 * @date 2026-05-20
 * @see Page
 * @see QueryWrapper
 * @see SortEnum
 * @see SortField
 */
@Data
public class PageRequest implements Serializable {

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
    private static final long serialVersionUID = -7869318821396190430L;

    // ==================== Constants ====================

    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 100L;
    private static final SortEnum DEFAULT_ASC = SortEnum.DESC; // Default to descending (newest first)
    private static final String DEFAULT_SORT = "created_at"; // Default sort column

    // ==================== Fields ====================

    @Min(value = 1, message = "{error.param.min}")
    private Long page;

    @Min(value = 1, message = "{error.param.min}")
    @Max(value = 100, message = "{error.param.max}")
    private Long size;

    @SortField
    private String sort;

    private SortEnum asc;

    private boolean searchCount = true;

    // ==================== Getter Logic with Defaults ====================

    /**
     * Gets the current page number with default fallback.
     * <p>
     * If {@code page} is {@code null}, returns {@code 1}. This ensures
     * pagination always has a valid page number even if the client doesn't provide one.
     *
     * @return the page number (1-indexed), or {@code 1} if not set
     */
    public long getPage() {
        // Fallback: if client doesn't provide page, default to 1
        return Objects.requireNonNullElse(page, DEFAULT_PAGE);
    }

    /**
     * Gets the page size with default fallback and maximum cap.
     * <p>
     * If {@code size} is {@code null}, returns {@code 10}. If provided,
     * caps the value at {@code 100} to prevent excessive data retrieval.
     *
     * @return the page size, or {@code 10} if not set, or {@code 100} if exceeded
     */
    public long getSize() {
        // Fallback: if not provided, use default; if provided, cap at maximum
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /**
     * Gets the sort column with default fallback.
     * <p>
     * If {@code sort} is {@code null} or empty, returns {@code "created_at"}.
     * This ensures consistent sorting behavior across queries.
     *
     * @return the sort column name, or {@code "created_at"} if not set
     */
    public String getSort() {
        // Fallback: if not provided, default to created_at
        return StringUtils.hasText(sort) ? sort : DEFAULT_SORT;
    }

    /**
     * Gets the sort direction with default fallback.
     * <p>
     * If {@code asc} is {@code null}, returns {@link SortEnum#DESC} (descending).
     * This means new records appear first by default, which is typical for list views.
     *
     * @return the sort direction, or {@link SortEnum#DESC} if not set
     */
    public SortEnum getAsc() {
        // Fallback: default to descending (newest first)
        return Objects.requireNonNullElse(asc, DEFAULT_ASC);
    }

    // ==================== Utility Methods ====================

    /**
     * Converts this request to a MyBatis-Plus {@link Page} object.
     * <p>
     * This method creates a pagination parameter object that can be passed directly
     * to MyBatis-Plus mapper methods. The {@code searchCount} flag controls whether
     * a count query is executed (set to {@code false} for performance in large datasets).
     *
     * @param <T> the entity type for pagination
     * @return a {@link Page} object with pagination parameters
     * @see #searchCount
     */
    public <T> Page<T> toPage() {
        Page<T> pageParam = new Page<>(getPage(), getSize());
        if (!searchCount) {
            pageParam.setSearchCount(false);
        }
        return pageParam;
    }

    /**
     * Converts this request to a MyBatis-Plus {@link QueryWrapper} with sorting applied.
     * <p>
     * This method creates a query wrapper with the sort column and direction applied.
     * Callers should add additional conditions (e.g., filters) before executing the query.
     *
     * @param <T> the entity type for the query
     * @return a {@link QueryWrapper} with sorting applied
     * @see #getSort()
     * @see #getAsc()
     */
    public <T> QueryWrapper<T> toQueryWrapper() {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderBy(StringUtils.hasText(getSort()), Objects.equals(getAsc(), SortEnum.ASC), getSort());
        return queryWrapper;
    }

}
