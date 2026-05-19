package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.common.enums.TargetType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;

/**
 * 系统审计日志实体
 *
 * @author starhq
 */
@Data
@Alias("auditLog")
@TableName("sys_audit_log")
@EqualsAndHashCode(callSuper = false)
public class SysAuditLog extends BaseCreatorEntity {


    /**
     * 操作动作
     */
    private String action;

    /**
     * 目标 ID
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


}
