package com.github.starhq.template.entity;

import java.time.OffsetDateTime;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 系统 Token 实体
 *
 * @author starhq
 */
@Data
@Alias("token")
@TableName("sys_token")
public class SysToken {
    /**
     * 主键 ID
     */
    @TableId
    private Long id;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 过期时间
     */
    private OffsetDateTime expiredAt;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    /**
     * 是否已撤销
     */
    private Boolean revoked;

    /**
     * 登录 IP
     */
    private String loginIp;

    /**
     * 设备指纹
     */
    private String deviceFingerprint;
}
