package com.github.starhq.template.entity;

import java.time.OffsetDateTime;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.enums.TargetType;

import lombok.Data;

/**
 * 系统审计日志实体
 *
 * @author starhq
 */
@Data
@Alias("auditLog")
@TableName("sys_audit_log")
public class SysAuditLog {
    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 目标类型
     */
    private TargetType targetType;

    /**
     * 操作值（JSON格式）
     */
    private String value;

    /**
     * 创建时间
     */
    private OffsetDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;
}
