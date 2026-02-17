package com.github.starhq.template.service;

import com.github.starhq.template.vo.LoginVO;

/**
 * Token服务接口
 *
 * @author starhq
 */
public interface TokenService {
    /**
     * 生成Token
     *
     * @param userId 用户ID
     * @return 登录VO
     */
    LoginVO generateToken(Long userId);

    /**
     * 刷新Token
     *
     * @param refreshToken 刷新令牌
     * @return 登录VO
     */
    LoginVO refreshToken(String refreshToken);

    /**
     * 撤销Token
     *
     * @param token 令牌
     */
    void revokeToken(String token);

    /**
     * 验证Token
     *
     * @param token 令牌
     * @return 用户ID
     */
    Long validateToken(String token);
}
