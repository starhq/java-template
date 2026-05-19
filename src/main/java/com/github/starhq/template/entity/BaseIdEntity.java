package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: id base entity
 * @date 2026/3/24 10:32
 */
@Data
public class BaseIdEntity {
    /**
     * 主键 ID
     */
    @TableId
    private Long id;
}
