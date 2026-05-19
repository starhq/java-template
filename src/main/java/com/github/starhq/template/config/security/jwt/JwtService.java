package com.github.starhq.template.config.security.jwt;

import com.github.starhq.template.config.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;

    private final SecretKey signingKey;

    // 手动构造器，用于初始化 signingKey
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getKey()));
    }

    /**
     * 生成 JWT Token 对象 (包含 Access 和 Refresh Token)
     */
    public JwtToken build(Map<String, Object> extraClaims, String subject) {
        long accessMillis = jwtProperties.getAccess().toMillis();
        long refreshMillis = jwtProperties.getRefresh().toMillis();

        String accessToken = buildToken(extraClaims, subject, accessMillis);
        String refreshToken = buildToken(extraClaims, subject, refreshMillis);

        return JwtToken.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccess().toSeconds())
                .build();
    }

    /**
     * 生成具体的 Token 字符串
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(extraClaims) // 设置自定义声明
                .subject(subject) // 设置主题
                .issuedAt(now) // 签发时间
                .expiration(expiryDate) // 过期时间
                .signWith(signingKey, Jwts.SIG.HS256) // 签名
                .compact();
    }

    /**
     * 解析 Token，返回 Claims
     * 注意：此方法会抛出具体的 JWT 异常，调用方需捕获处理
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey) // 验证签名
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中提取用户名
     */
    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * 验证 Token 是否有效
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}
