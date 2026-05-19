package com.github.starhq.template.model.vo;

import java.io.Serializable;

import lombok.Data;

/**
 * 登录响应VO
 *
 * @author starhq
 */
@Data
public class LoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 刷新令牌
     */
    private String refreshToken;

    /**
     * 过期时间（秒）
     */
    private Long expiresIn;
}
