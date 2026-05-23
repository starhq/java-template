package com.github.starhq.template.config.security.jwt;

import com.github.starhq.template.config.security.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.EncodingException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * Centralized utility class for creating, parsing, and validating JSON Web Tokens (JWT).
 *
 * <p>This service encapsulates the JJWT library specifics (e.g., signing algorithms, Base64 key decoding)
 * and exposes clean business methods for the security components to interact with.
 *
 * @author starhq
 */
@Slf4j
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    /**
     * Configuration properties holding secret key string and token lifetimes.
     */
    private final JwtProperties jwtProperties;

    /**
     * The cryptographic signing key used to digitally sign and verify JWTs.
     * <p>Initialized in the constructor by decoding the Base64-encoded string from configuration
     * into a JCA {@link SecretKey} instance compatible with the HMAC-SHA256 algorithm.
     */
    private final SecretKey signingKey;

    /**
     * Initializes the JWT service and prepares the cryptographic signing key.
     *
     * @param jwtProperties the configuration properties containing the Base64-encoded secret key
     */
    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;

        // Decode the Base64 string into a native Java cryptography Key object
        try {
            byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getKey());

            // ✅ 关键校验：64 字节 = 512 bits = HS512
            if (keyBytes.length != 64) {
                throw new IllegalArgumentException(
                        String.format("JWT key must be 64 bytes (512 bits) for HS512, got %d bytes. " +
                                "Regenerate with: openssl rand -base64 64", keyBytes.length));
            }

            this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        } catch (EncodingException e) {
            throw new IllegalArgumentException("JWT key is not valid Base64: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience method to generate a paired JWT structure containing both Access and Refresh tokens.
     *
     * <p>Both tokens share the same payload (subject, userId, etc.) but are issued with different
     * expiration times configured in {@link JwtProperties}.
     *
     * @param extraClaims custom claims to embed in the token payload (e.g., {"userId": 123})
     * @param subject     the standard JWT subject claim (typically the username)
     * @return a {@link JwtToken} wrapper containing both token strings and metadata
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
     * Internal method to construct a raw JWT string with specific claims and expiration.
     *
     * @param extraClaims      custom payload data
     * @param subject          the identifier of the token owner (usually username)
     * @param expirationMillis absolute expiration time in milliseconds since Unix epoch
     * @return the compact, signed JWT string
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .claims(extraClaims)      // Inject custom payload (e.g., userId, roles)
                .subject(subject)          // Set the standard "sub" claim
                .issuedAt(now)              // Set "iat" (Issued At) claim
                .expiration(expiryDate)     // Set "exp" (Expiration) claim
                .signWith(signingKey, Jwts.SIG.HS512) // Sign using HMAC with SHA-512
                .compact();
    }

    /**
     * Parses a JWT string and verifies its cryptographic signature.
     *
     * <p><b>Important:</b> This method does not catch exceptions. It strictly throws specific JJWT exceptions
     * (e.g., {@link io.jsonwebtoken.ExpiredJwtException}, {@link io.jsonwebtoken.security.SignatureException})
     * if the token is malformed, expired, or tampered with. The calling filter is responsible for catching
     * these and mapping them to the correct {@link com.github.starhq.template.common.enums.ErrorCode}.
     *
     * @param token the raw JWT string (e.g., "eyJhbGciOiJIUzI1NiJ9...")
     * @return the parsed {@link Claims} payload containing all claims
     * @throws JwtException if the token is invalid, expired, or signature verification fails
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)   // Verifies HMAC-SHA256 signature
                .build()
                .parseSignedClaims(token) // Parses the 3 parts: Header, Payload, Signature
                .getPayload();             // Returns the payload as a Map-like object
    }

    /**
     * Extracts the subject claim (typically the username) from a JWT string.
     *
     * @param token the raw JWT string
     * @return the subject string
     * @throws JwtException if the token is invalid
     */
    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    /**
     * Validates a token against a known {@link UserDetails} object.
     *
     * <p>This method is typically used during token refresh flows to ensure the refresh token still
     * belongs to the user it was originally issued to.
     *
     * @param token       the raw JWT string
     * @param userDetails the expected user details to validate against
     * @return {@code true} if the token is structurally valid, not expired, and belongs to the user; {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            // 1. Verify the token belongs to the provided user (prevents token swapping)
            // 2. Verify the token hasn't expired naturally
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Catch parsing errors or bad inputs gracefully, preventing crashes
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a token's "exp" (expiration) claim is before the current system time.
     *
     * @param token the raw JWT string
     * @return {@code true} if the token is expired, {@code false} if it is still valid
     * @throws JwtException if the token cannot be parsed
     */
    private boolean isTokenExpired(String token) {
        // Uses Date.before() which implicitly calls System.currentTimeMillis()
        return parseToken(token).getExpiration().before(new Date());
    }
}