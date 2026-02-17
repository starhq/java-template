package com.github.starhq.template.service.impl;

import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.exception.BusinessException;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.service.TokenService;
import com.github.starhq.template.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Token服务实现类
 *
 * @author starhq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {
    private final SysTokenMapper tokenMapper;

    @Value("${jwt.expiration:3600}")
    private Long expiration;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO generateToken(Long userId) {
        String accessToken = UUID.randomUUID().toString();
        String refreshToken = UUID.randomUUID().toString();
        OffsetDateTime expiredAt = OffsetDateTime.now().plusSeconds(expiration);

        SysToken token = new SysToken();
        token.setUserId(userId);
        token.setAccessToken(accessToken);
        token.setRefreshToken(refreshToken);
        token.setExpiredAt(expiredAt);
        token.setCreatedAt(OffsetDateTime.now());
        token.setRevoked(false);
        // TODO: 获取真实IP和设备指纹
        token.setLoginIp("127.0.0.1");
        token.setDeviceFingerprint("default");

        tokenMapper.insert(token);

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setExpiresIn(expiration);
        return loginVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO refreshToken(String refreshToken) {
        SysToken token = tokenMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysToken>()
                        .eq(SysToken::getRefreshToken, refreshToken)
                        .eq(SysToken::getRevoked, false)
        );

        if (token == null) {
            throw new BusinessException("刷新令牌无效");
        }

        if (token.getExpiredAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("刷新令牌已过期");
        }

        // 撤销旧Token
        token.setRevoked(true);
        tokenMapper.updateById(token);

        // 生成新Token
        return generateToken(token.getUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeToken(String token) {
        SysToken sysToken = tokenMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysToken>()
                        .eq(SysToken::getAccessToken, token)
                        .or()
                        .eq(SysToken::getRefreshToken, token)
        );

        if (sysToken != null) {
            sysToken.setRevoked(true);
            tokenMapper.updateById(sysToken);
        }
    }

    @Override
    public Long validateToken(String token) {
        SysToken sysToken = tokenMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysToken>()
                        .eq(SysToken::getAccessToken, token)
                        .eq(SysToken::getRevoked, false)
        );

        if (sysToken == null) {
            throw new BusinessException("令牌无效");
        }

        if (sysToken.getExpiredAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException("令牌已过期");
        }

        return sysToken.getUserId();
    }
}
