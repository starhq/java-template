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
 * Service implementation for JWT token management with lifecycle control, caching, and session tracking.
 * <p>
 * This class extends {@link BaseServiceImpl} to provide reusable CRUD operations for {@link SysToken},
 * while implementing {@link TokenService} for business-level token issuance, refresh, revocation, and audit.
 * Designed to centralize token logic with consistent validation, distributed cache integration, and
 * device-aware session management for enhanced security.
 * <p>
 * <strong>Primary Responsibilities:</strong>
 * <ul>
 *     <li><strong>Token Issuance</strong>: Generate JWT access/refresh tokens with user claims and device fingerprinting</li>
 *     <li><strong>Token Refresh</strong>: Support seamless session renewal without re-authentication</li>
 *     <li><strong>Session Tracking</strong>: Persist token metadata (IP, device, expiry) for audit and forced logout</li>
 *     <li><strong>Cache Management</strong>: Integrate with Spring Cache for efficient token lookups and invalidation</li>
 * </ul>
 * <p>
 * <strong>Security Design Principles:</strong>
 * <ul>
 *     <li><strong>Device Binding</strong>: Associate tokens with device fingerprints to detect suspicious activity</li>
 *     <li><strong>Stateless + Stateful Hybrid</strong>: JWTs are stateless for auth, but server-side records enable revocation</li>
 *     <li><strong>Cache-First</strong>: Frequent token lookups leverage Redis/Caffeine to reduce database load</li>
 *     <li><strong>Audit-Ready</strong>: All token operations persist metadata for compliance and forensic analysis</li>
 * </ul>
 *
 * @author starhq
 * @author wangjian (contributor)
 * @version 1.0
 * @date 2026-05-20
 * @see TokenService
 * @see BaseServiceImpl
 * @see SysToken
 * @see JwtService
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl extends BaseServiceImpl<SysTokenMapper, SysToken> implements TokenService {

    /**
     * Mapper for {@link SysToken} database operations.
     * <p>Used for custom queries like {@code selectTokenPage} and upsert operations.</p>
     *
     * @see SysTokenMapper
     */
    private final SysTokenMapper tokenMapper;

    /**
     * Converter for transforming between {@link SysToken} and DTO/VO types.
     * <p>Ensures consistent field mapping and avoids boilerplate conversion code.</p>
     *
     * @see TokenConverter
     */
    private final TokenConverter tokenConverter;

    /**
     * Service for JWT generation, validation, and claim extraction.
     * <p>Handles cryptographic signing, expiry calculation, and payload serialization.</p>
     *
     * @see JwtService
     * @see JwtToken
     */
    private final JwtService jwtService;

    /**
     * Helper for efficient cache operations (put/get/evict).
     * <p>Used to store/retrieve token metadata by user ID for fast session checks.</p>
     *
     * @see CacheHelper
     * @see CacheConstant
     */
    private final CacheHelper cacheHelper;

    /**
     * Issues new JWT access and refresh tokens for an authenticated user.
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Validate that {@code user} is an instance of {@link SysUser}</li>
     *     <li>Extract device fingerprint from request context for session binding</li>
     *     <li>Delegate to {@link #generate} for JWT creation and persistence</li>
     * </ol>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Type Safety</strong>: Ensures only internal {@code SysUser} entities can issue tokens</li>
     *     <li><strong>Device Fingerprinting</strong>: Binds token to client device for anomaly detection</li>
     *     <li><strong>Claim Minimization</strong>: Only essential claims (id, username, roles) are included</li>
     * </ul>
     *
     * @param user the authenticated user details; must be an instance of {@link SysUser}
     * @return {@link JwtToken} containing access token, refresh token, and expiry
     * @throws BusinessException if user is not {@code SysUser} or token generation fails
     * @see UserDetails
     * @see SysUser
     * @see RequestContextUtil#getContext()
     */
    @Override
    public JwtToken build(UserDetails user) {
        // Ensure user is our internal entity type
        if (!(user instanceof SysUser sysUser)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        // Extract device fingerprint for session binding
        String deviceFingerprint = RequestContextUtil.getContext().deviceFingerprint();

        // Generate and persist tokens
        return generate(sysUser.getId(), sysUser.getUsername(), sysUser.getAuthorities(), deviceFingerprint);
    }

    /**
     * Refreshes expired access token using current security context.
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Extract user ID, username, and authorities from current {@link SecurityContextUtils}</li>
     *     <li>Extract device fingerprint from request context</li>
     *     <li>Delegate to {@link #generate} to issue new tokens (implicit rotation)</li>
     * </ol>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Context Validation</strong>: Relies on valid security context; throws if unauthenticated</li>
     *     <li><strong>Implicit Rotation</strong>: Generates new tokens, effectively rotating the session</li>
     *     <li><strong>Device Consistency</strong>: Uses same device fingerprint to maintain session continuity</li>
     * </ul>
     *
     * @return {@link JwtToken} with new access and refresh tokens
     * @throws CustomException if security context is invalid or user not found
     * @see SecurityContextUtils
     * @see #generate(Long, String, Collection, String)
     */
    @Override
    public JwtToken refresh() {
        // Extract current user info from security context
        Long userId = SecurityContextUtils.getRequiredUserId();
        String username = SecurityContextUtils.getCurrentUsername();
        Collection<? extends GrantedAuthority> currentAuthorities = SecurityContextUtils.getCurrentAuthorities();

        // Extract device fingerprint for session binding
        String deviceFingerprint = RequestContextUtil.getContext().deviceFingerprint();

        // Generate new tokens
        return generate(userId, username, currentAuthorities, deviceFingerprint);
    }

    /**
     * Retrieves a paginated list of token records for admin audit and monitoring.
     * <p>
     * <strong>Filter Logic:</strong>
     * <ul>
     *     <li>Delegates to {@code KeyWordPageRequest.toQueryWrapper()} for base filters</li>
     *     <li>Adds right-fuzzy match on {@code username} if {@code keyword} is present</li>
     * </ul>
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *     <li><strong>Paginated Result</strong>: Returns {@link IPage} with current page records and total count</li>
     *     <li><strong>Empty Result</strong>: Returns empty page if no tokens match criteria</li>
     * </ul>
     * <p>
     * <strong>Security & Access Control:</strong>
     * <ul>
     *     <li><strong>Admin-Only</strong>: Must be protected by {@code @PreAuthorize("hasRole('ADMIN')")}</li>
     *     <li><strong>Data Masking</strong>: Ensure sensitive fields (e.g., full IP) are masked in VO if needed</li>
     * </ul>
     *
     * @param pageInfo the pagination and filter criteria; must not be {@code null}
     * @return paginated list of {@link TokenPageVO}; never {@code null}
     * @see KeyWordPageRequest
     * @see TokenPageVO
     */
    @Override
    public IPage<TokenPageVO> page(KeyWordPageRequest pageInfo) {
        // Build base query wrapper from request
        QueryWrapper<TokenPageVO> queryWrapper = pageInfo.toQueryWrapper();

        // Add keyword filter on username (right-fuzzy match)
        if (StringUtils.hasText(pageInfo.getKeyword())) {
            queryWrapper.likeRight(QueryConstant.USERNAME, pageInfo.getKeyword());
        }

        // Convert to MyBatis-Plus page object
        Page<TokenPageVO> page = pageInfo.toPage();

        // Execute custom paginated query
        return tokenMapper.selectTokenPage(page, queryWrapper);
    }

    /**
     * Revokes all active tokens for a specific user, effectively forcing logout.
     * <p>
     * <strong>Cache Strategy:</strong>
     * <ul>
     *     <li>Annotated with {@code @CacheEvict(value = "tokens", key = "#p0")} to remove cached token metadata</li>
     *     <li>Ensures subsequent lookups for this user result in cache miss and DB check (which will find no active tokens)</li>
     * </ul>
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li>Catches generic exceptions and wraps with {@link ErrorCode#TOKEN_REVOKED} for consistent API responses</li>
     *     <li>Returns {@code true} if at least one token was deleted; {@code false} if no tokens existed</li>
     * </ul>
     *
     * @param userId the primary key of the user whose tokens to revoke
     * @return {@code true} if tokens were successfully revoked; {@code false} if none existed
     * @throws BusinessException if database operation fails
     * @see CacheEvict
     */
    @CacheEvict(value = "tokens", key = "#p0")
    @Override
    public boolean removeByUserId(Long userId) {
        try {
            // Delete all tokens for the user
            return tokenMapper.delete(
                    new LambdaQueryWrapper<SysToken>().eq(SysToken::getUserId, userId)
            ) > 0;
        } catch (Exception e) {
            // Wrap exception with business error code
            throw new BusinessException(ErrorCode.TOKEN_REVOKED, e);
        }
    }

    /**
     * Retrieves simplified token metadata for a specific user.
     * <p>
     * <strong>Cache Strategy:</strong>
     * <ul>
     *     <li>Annotated with {@code @Cacheable(value = "tokens", key = "#p0")} to cache by {@code userId}</li>
     *     <li>Cache hit returns stored {@link TokenSimpleDTO} immediately</li>
     *     <li>Cache miss triggers DB lookup for active, non-revoked, non-expired token</li>
     * </ul>
     * <p>
     * <strong>Validation Logic:</strong>
     * <ul>
     *     <li>Filters by {@code userId}, {@code revoked = false}, and {@code expiredAt > now()}</li>
     *     <li>Throws {@link CustomException} with {@link ErrorCode#UNAUTHORIZED} if no valid token found</li>
     * </ul>
     *
     * @param userId the primary key of the user to query
     * @return {@link TokenSimpleDTO} if active token exists
     * @throws CustomException if no active token found (HTTP 401)
     * @see TokenSimpleDTO
     * @see Cacheable
     */
    @Cacheable(value = "tokens", key = "#p0")
    @Override
    public TokenSimpleDTO getByUserId(Long userId) {
        // Fetch active, non-revoked, non-expired token for user
        SysToken token = tokenMapper.selectOne(
                new LambdaQueryWrapper<SysToken>()
                        .eq(SysToken::getUserId, userId)
                        .eq(SysToken::getRevoked, false)
                        .gt(SysToken::getExpiredAt, OffsetDateTime.now())
        );

        // Throw 401 if no valid token found
        if (null == token) {
            throw new CustomException(ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }

        // Convert to DTO for response
        return tokenConverter.toSimpleDTO(token);
    }

    /**
     * Generates JWT tokens, persists session metadata, and updates cache.
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Validate that user has at least one role/authority</li>
     *     <li>Build extra claims (user ID, roles) for JWT payload</li>
     *     <li>Generate JWT via {@link JwtService#build}</li>
     *     <li>Persist token metadata via {@link #saveSessionToken}</li>
     *     <li>Return {@link JwtToken} to caller</li>
     * </ol>
     * <p>
     * <strong>Error Handling:</strong>
     * <ul>
     *     <li>Throws {@link BusinessException} with {@link ErrorCode#NO_ROLES} if user has no authorities</li>
     *     <li>Throws {@link BusinessException} with {@link ErrorCode#TOKEN_ISSUED} if JWT generation or persistence fails</li>
     * </ul>
     *
     * @param userId            the user ID
     * @param username          the username
     * @param roles             the user's authorities
     * @param deviceFingerprint the client device fingerprint
     * @return {@link JwtToken} containing access and refresh tokens
     * @throws BusinessException if validation fails or token generation errors occur
     * @see JwtService#build(Map, String)
     * @see #saveSessionToken(JwtToken, Long, String)
     */
    private JwtToken generate(Long userId, String username, Collection<? extends GrantedAuthority> roles, String deviceFingerprint) {
        // Ensure user has at least one role
        if (CollectionUtils.isEmpty(roles)) {
            throw new BusinessException(ErrorCode.NO_ROLES);
        }

        // Extract role codes for JWT claims
        List<String> codes = roles.stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Build extra claims for JWT payload
        Map<String, Object> extraClaims = Map.of("id", userId, "roles", codes);

        try {
            // Generate JWT tokens
            JwtToken jwtToken = jwtService.build(extraClaims, username);

            // Persist session metadata and update cache
            saveSessionToken(jwtToken, userId, deviceFingerprint);

            return jwtToken;
        } catch (Exception e) {
            // Wrap exception with business error code
            throw new BusinessException(ErrorCode.TOKEN_ISSUED, e);
        }
    }

    /**
     * Persists token metadata to database and updates cache.
     * <p>
     * <strong>Workflow:</strong>
     * <ol>
     *     <li>Create {@link SysToken} entity with JWT data, expiry, device fingerprint, and IP</li>
     *     <li>Upsert token to database via {@code tokenMapper.upsertToken} (handles rotation)</li>
     *     <li>Put converted {@link TokenSimpleDTO} into cache via {@code cacheHelper.put}</li>
     * </ol>
     * <p>
     * <strong>Security Considerations:</strong>
     * <ul>
     *     <li><strong>Device Fingerprint</strong>: Stored for anomaly detection and session binding</li>
     *     <li><strong>Login IP</strong>: Captured from request context for audit trail</li>
     *     <li><strong>Expiry Calculation</strong>: Based on JWT {@code expiresIn} seconds for consistency</li>
     * </ul>
     *
     * @param jwtToken          the JWT token containing access/refresh strings and expiry
     * @param userId            the user ID associated with the token
     * @param deviceFingerprint the client device fingerprint
     * @see SysToken
     * @see SysTokenMapper#upsertToken(SysToken)
     * @see CacheHelper#put(Object, Object, String)
     */
    private void saveSessionToken(JwtToken jwtToken, Long userId, String deviceFingerprint) {
        // Create token entity
        SysToken token = new SysToken();
        token.setUserId(userId);
        token.setAccessToken(jwtToken.getAccessToken());
        token.setRefreshToken(jwtToken.getRefreshToken());

        // Calculate expiry time based on JWT lifespan
        token.setExpiredAt(OffsetDateTime.now().plusSeconds(jwtToken.getExpiresIn()));

        // Capture session metadata
        token.setDeviceFingerprint(deviceFingerprint);
        token.setLoginIp(RequestContextUtil.getContext().clientIp());
        token.setRevoked(false); // Active by default

        // Upsert to database (handles rotation if record exists)
        tokenMapper.upsertToken(token);

        // Update cache with new token metadata
        cacheHelper.put(userId, tokenConverter.toSimpleDTO(token), CacheConstant.TOKEN);
    }

}