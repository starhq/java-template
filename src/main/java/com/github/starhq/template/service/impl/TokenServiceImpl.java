package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.RequestContextUtil;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.config.security.jwt.JwtService;
import com.github.starhq.template.config.security.jwt.JwtToken;
import com.github.starhq.template.converter.TokenConverter;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.helper.CacheHelper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.token.TokenSimpleDTO;
import com.github.starhq.template.model.vo.token.TokenPageVO;
import com.github.starhq.template.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Token 服务实现类
 *
 * @author starhq
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl extends BaseServiceImpl<SysTokenMapper, SysToken> implements TokenService {

    private final SysTokenMapper tokenMapper;

    private final TokenConverter tokenConverter;
    private final JwtService jwtService;
    private final CacheHelper cacheHelper;

    @Override
    public JwtToken build(UserDetails user) {
        if (!(user instanceof SysUser sysUser)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        String deviceFingerprint = RequestContextUtil.getContext().deviceFingerprint();
        return generate(sysUser.getId(), sysUser.getUsername(), sysUser.getAuthorities(), deviceFingerprint);
    }

    @Override
    public JwtToken refresh() {
        Long userId = SecurityContextUtils.getRequiredUserId();
        String username = SecurityContextUtils.getCurrentUsername();
        Collection<? extends GrantedAuthority> currentAuthorities = SecurityContextUtils.getCurrentAuthorities();

        String deviceFingerprint = RequestContextUtil.getContext().deviceFingerprint();
        return generate(userId, username, currentAuthorities, deviceFingerprint);
    }

    @Override
    public IPage<TokenPageVO> page(KeyWordPageRequest pageInfo) {
        QueryWrapper<TokenPageVO> queryWrapper = pageInfo.toQueryWrapper();
        if (StringUtils.hasText(pageInfo.getKeyword())) {
            queryWrapper.likeRight(QueryConstant.USERNAME, pageInfo.getKeyword());
        }

        Page<TokenPageVO> page = pageInfo.toPage();

        return tokenMapper.selectTokenPage(page, queryWrapper);
    }

    @CacheEvict(value = "tokens", key = "#p0")
    @Override
    public boolean removeByUserId(Long userId) {
        try {
            return tokenMapper
                    .delete(new LambdaQueryWrapper<SysToken>().eq(SysToken::getUserId, userId)) > 0;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOKEN_REVOKED, e);
        }
    }

    @Cacheable(value = "tokens", key = "#p0")
    @Override
    public TokenSimpleDTO getByUserId(Long userId) {
        SysToken token = tokenMapper.selectOne(
                new LambdaQueryWrapper<SysToken>()
                        .eq(SysToken::getUserId, userId)
                        .eq(SysToken::getRevoked, false)
                        .gt(SysToken::getExpiredAt, OffsetDateTime.now())
        ); // Fetch
        if (null == token) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }
        return tokenConverter.toSimpleDTO(token); // Convert to response DTO
    }

    private JwtToken generate(Long userId, String username, Collection<? extends GrantedAuthority> roles, String deviceFingerprint) {
        if (CollectionUtils.isEmpty(roles)) {
            throw new BusinessException(ErrorCode.NO_ROLES);
        }
        List<String> codes = roles.stream().map(GrantedAuthority::getAuthority).toList();
        Map<String, Object> extraClaims = Map.of("id", userId, "roles", codes); // Build extra claims for JWT

        try {
            JwtToken jwtToken = jwtService.build(extraClaims, username); // Generate new JWT token
            saveSessionToken(jwtToken, userId, deviceFingerprint); // Save the session token
            return jwtToken; // Return the new token
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.TOKEN_ISSUED, e);
        }
    }

    /**
     * Saves the session token to the database.
     *
     * @param jwtToken          the JWT token to save
     * @param userId            the ID of the user
     * @param deviceFingerprint the fingerprint of the device
     */
    private void saveSessionToken(JwtToken jwtToken, Long userId, String deviceFingerprint) {
        SysToken token = new SysToken();
        token.setUserId(userId);
        token.setAccessToken(jwtToken.getAccessToken());
        token.setRefreshToken(jwtToken.getRefreshToken());
        token.setExpiredAt(OffsetDateTime.now().plusSeconds(jwtToken.getExpiresIn())); // Set expiration time
        token.setDeviceFingerprint(deviceFingerprint);
        token.setLoginIp(RequestContextUtil.getContext().clientIp()); // Set the login IP
        token.setRevoked(false); // Set revoked status to false

        tokenMapper.upsertToken(token);

        cacheHelper.put(userId, tokenConverter.toSimpleDTO(token), CacheConstant.TOKEN);
    }
}
