package com.github.starhq.template.entity;

import java.time.LocalDateTime;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: api log entity
 * @date 2026/3/24 10:32
 */
@Data
@Alias("apiLog")
@TableName("sys_api_log")
public class SysApiLog {

    @TableId
    private Long id;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    /**
     * 请求路径
     */
    private String uri;

    /**
     * 请求方法
     */
    private String method;

    /**
     * 查询参数
     */
    private String queryString;

    /**
     * 客户端 IP
     */
    private String clientIp;

    /**
     * 请求头 (JSON格式)
     */
    private String headers;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 响应状态码
     */
    private Integer httpStatus;

    /**
     * 响应体
     */
    private String responseBody;

    /**
     * 异常消息
     */
    private String exceptionMessage;

    /**
     * 异常堆栈 (仅在非生产环境或特定配置保存)
     */
    private String exceptionStack;

    /**
     * 执行时长
     */
    private Long duration;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
