package com.github.starhq.template.config.security.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Data Transfer Object (DTO) representing the payload returned to the client upon successful authentication.
 *
 * <p>This structure strictly adheres to the standard OAuth 2.0 RFC 6749 specification for returning
 * Access Tokens (e.g., {"access_token": "...", "token_type": "Bearer", "expires_in": 3600}).
 * It also extends the standard by including a `refresh_token` for our custom dual-token architecture.
 *
 * @author starhq
 */
@Data
@Builder
public class JwtToken implements Serializable {

    /**
     * Serial version UID for serialization compatibility.
     * <p>Ensures that if this object is serialized (e.g., stored in a session, Redis, or passed
     * through a message queue), it can be reliably deserialized back into an object later,
     * even if the class structure evolves.
     */
    @Serial
    private static final long serialVersionUID = 889968112L;

    /**
     * The signed JWT string used to access protected APIs.
     * <p>Lifespan is typically short-lived (e.g., 30 minutes) to minimize damage if leaked.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The signed JWT string used exclusively to request a new Access Token when the current one expires.
     * <p>Lifespan is typically long-lived (e.g., 7 days) to improve user experience by preventing
     * frequent forced logouts.
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * The expected type of token included in the Authorization header.
     * <p>Standardized as "Bearer" according to RFC 6750. Required by frontend HTTP clients (like Axios)
     * to know how to attach the token to the request header.
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * The time-to-live of the access token, represented in seconds.
     * <p>Used by frontend clients to proactively trigger a token refresh slightly before it actually expires,
     * ensuring seamless API transitions without interrupting user operations.
     */
    @JsonProperty("expires_in")
    private Long expiresIn;
}