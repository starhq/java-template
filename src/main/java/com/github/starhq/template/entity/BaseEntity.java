package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.OffsetDateTime;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: creator and updater base entity
 * @date 2026/3/24 10:32
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BaseEntity extends BaseCreatorEntity {

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.UPDATE)
    private OffsetDateTime updatedAt;

    /**
     * 更新人 ID
     */
    @TableField(fill = FieldFill.UPDATE)
    private Long updatedBy;
}
