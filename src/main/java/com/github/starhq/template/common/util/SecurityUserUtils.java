package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.entity.SysUser;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/**
 * Utility class for validating the state and status of system users.
 *
 * <p>Provides a centralized security checkpoint to be called after retrieving a user from the database
 * but before granting access to protected resources or generating tokens.
 *
 * @author starhq
 */
@UtilityClass
public class SecurityUserUtils {

    /**
     * Validates the fetched user entity to ensure it is eligible for authentication.
     *
     * <p>This method implements a strict security state machine to prevent information leakage.
     * <b>Security Design Note:</b>
     * <ul>
     *   <li>If the user is {@code null} (username does not exist), it throws {@link ErrorCode#CREDENTIALS}
     *       (typically mapped to "Incorrect username or password"). It does <b>NOT</b> throw a
     *       "User Not Found" exception. This prevents malicious actors from enumerating valid usernames.</li>
     *   <li>If the user exists but their status is not {@link UserStatus#ACTIVE} (e.g., BANNED or INACTIVE),
     *       it throws {@link ErrorCode#DISABLED}. This clearly separates "wrong credentials" from
     *       "account policy violation" in the frontend and audit logs.</li>
     * </ul>
     *
     * <p><b>Typical Usage:</b> Call this in your {@code UserDetailsService.loadUserByUsername()} or
     * custom authentication provider immediately after the database query.
     *
     * @param user the {@link SysUser} entity retrieved from the database, may be {@code null}
     * @throws CustomException with {@link HttpStatus#UNAUTHORIZED} if the user is null or not active
     */
    public void checkUserStatus(SysUser user) {
        if (user == null) {
            // Fail silently with a generic credential error to prevent username enumeration attacks
            throw new CustomException(ErrorCode.CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            // Explicitly block locked, banned, or deactivated accounts
            throw new CustomException(ErrorCode.DISABLED, HttpStatus.UNAUTHORIZED);
        }
    }
}