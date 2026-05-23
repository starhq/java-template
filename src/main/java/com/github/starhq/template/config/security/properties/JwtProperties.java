package com.github.starhq.template.config.security.properties;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration properties for JSON Web Token (JWT) generation and validation.
 *
 * <p>Binds to the {@code star.jwt} namespace in application YAML files. Controls the cryptographic
 * signature requirements and the lifespan of both Access and Refresh tokens.
 *
 * <p><b>Token Expiration Strategy Recommendations:</b>
 * <table border="1">
 * <tr><th>Scenario</th><th>Access Token</th><th>Refresh Token</th><th>Description</th></tr>
 * <tr><td><strong>Recommended Standard</strong></td><td><strong>30m - 2h</strong></td><td><strong>7 - 14 days</strong></td><td>Balances security and user experience seamlessly.</td></tr>
 * <tr><td><strong>High Security</strong></td><td><strong>5 - 15m</strong></td><td><strong>1 day</strong></td><td>Banking/financial apps. Frequent silent refresh ensures active sessions are validated.</td></tr>
 * <tr><td><strong>Low Security/Internal</strong></td><td><strong>4 - 8h</strong></td><td><strong>30 days</strong></td><td>Internal microservices. Users prefer not to log in repeatedly.</td></tr>
 * <tr><td><strong>Mobile App</strong></td><td><strong>1 - 2h</strong></td><td><strong>30 - 90 days</strong></td><td>Mobile typing is cumbersome. Allow long login sessions to reduce friction.</td></tr>
 * </table>
 *
 * @author starhq
 */
@Data
@Component
@Validated // Triggers validation of JSR-380 annotations (@NotBlank, @Size) upon startup
@ConfigurationProperties(prefix = "star.jwt")
public class JwtProperties {

    /**
     * Base64-encoded secret key used to digitally sign and verify the JWT.
     * <p><b>Security Requirement:</b> HMAC-SHA512 requires a key of exactly 512 bits (64 bytes).
     * If the string is shorter, the key generation will fail or be cryptographically weak.
     */
    @NotBlank(message = "JWT signing key must not be blank")
    private String key;

    /**
     * The lifespan of the Access Token.
     * <p>This token is short-lived and used to authorize API requests. A shorter lifespan limits the
     * window of opportunity if the token is stolen by an attacker (e.g., via XSS).
     */
    private Duration access = Duration.ofMinutes(30);

    /**
     * The lifespan of the Refresh Token.
     * <p>This token is long-lived and securely stored (e.g., in a database). It is only ever presented
     * to the dedicated refresh endpoint, never exposed to the browser network in API calls, mitigating
     * the risk of interception.
     */
    private Duration refresh = Duration.ofDays(7);

    @PostConstruct
    public void init() {
        System.out.println("JwtProperties @PostConstruct - key is: '" + key + "'");
    }
}