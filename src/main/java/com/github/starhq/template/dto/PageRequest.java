package com.github.starhq.template.dto;

import java.io.Serializable;
import java.util.Objects;

import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分页请求DTO
 *
 * @author starhq
 */
@Data
public class PageRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // ==================== 常量定义 ====================

    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 10L;
    private static final long MAX_SIZE = 100L;
    private static final String DEFAULT_ASC = "DESC"; // 2. 习惯上列表默认倒序（最新的在前面）
    private static final String DEFAULT_SORT = "created_at"; // 3. 默认按创建时间排序

    // ==================== 字段定义 ====================

    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码不能小于1")
    private Long page;

    /**
     * 每页条数
     */
    @NotNull(message = "每页条数不能为空") // 4. 增加校验
    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    private Long size;

    /**
     * 排序字段
     */
    private String sort;

    /**
     * 是否升序
     */
    private String asc;

    // ==================== Getter 逻辑优化 ====================

    public long getPage() {
        // 兜底处理：如果前端没传 page，默认 1
        return Objects.requireNonNullElse(page, DEFAULT_PAGE);
    }

    public long getSize() {
        // 兜底处理：如果没传，用默认值；如果传了，限制最大值
        if (size == null) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    public String getSort() {
        // 兜底处理：如果没传排序列，默认按主键倒序
        return StringUtils.hasText(sort) ? sort : DEFAULT_SORT;
    }

    public String getAsc() {
        // 兜底处理：默认倒序
        return StringUtils.hasText(asc) ? asc : DEFAULT_ASC;
    }

    // ==================== 工具方法 ====================

    /**
     * 转换为 MyBatis-Plus 的 Page 对象
     */
    public <T> com.baomidou.mybatisplus.extension.plugins.pagination.Page<T> toPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(getPage(), getSize());
    }

    public <T> QueryWrapper<T> toQueryWraper() {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderBy(false, false, null)
        return queryWrapper;

    }
}
