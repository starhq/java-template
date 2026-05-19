package com.github.starhq.template.model.dto.token;

import java.io.Serial;
import java.time.OffsetDateTime;

import com.github.starhq.template.model.dto.SensitiveDTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TokenSimpleDTO extends SensitiveDTO {
    @Serial
    private static final long serialVersionUID = 933255569L;

    /**
     * Access token
     */
    private String accessToken;

    /**
     * Refresh token
     */
    private String refreshToken;

    /**
     * Token expiration time
     */
    private OffsetDateTime expiredAt;

    /**
     * Whether the token is revoked
     */
    private Boolean revoked;

    /**
     * Login IP address
     */
    private String loginIp;

    /**
     * Device fingerprint
     */
    private String deviceFingerprint;
}
