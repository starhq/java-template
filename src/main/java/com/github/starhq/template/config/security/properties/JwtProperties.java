package com.github.starhq.template.config.security.properties;

import java.time.Duration;

import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * | 场景 | Access Token (访问令牌) | Refresh Token (刷新令牌) | 说明 |
 * | :--- | :--- | :--- | :--- |
 * | **推荐标准** | **30 分钟 ~ 2 小时** | **7 天 ~ 14 天** | 兼顾安全与体验。 |
 * | **高安全** | 5 ~ 15 分钟 | 1 天 | 银行、金融类应用。用户活跃期间无感刷新。 |
 * | **低安全/内部** | 4 ~ 8 小时 | 30 天 | 内部管理系统，用户不想频繁登录。 |
 * | **移动端 App** | 1 ~ 2 小时 | 30 天 ~ 90 天 | 移动端输入密码麻烦，允许长久的登录状态。 |
 */
@Data
@Component
@Validated // 开启校验
@ConfigurationProperties(prefix = "star.jwt")
public class JwtProperties {

    /**
     * Base64 encoded secret key.
     * Must be at least 256 bits (32 characters) for HS256.
     */
    @NotBlank(message = "JWT signing key must not be blank")
    @Size(min = 32, message = "JWT key must be at least 32 characters")
    private String key;

    /**
     * Access token expiration time.
     */
    private Duration access = Duration.ofMinutes(30);

    /**
     * Refresh token expiration time.
     */
    private Duration refresh = Duration.ofDays(7);
}
