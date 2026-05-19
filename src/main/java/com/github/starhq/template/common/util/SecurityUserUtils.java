package com.github.starhq.template.common.util;

import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.UserStatus;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.entity.SysUser;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class SecurityUserUtils {

    /**
     * Helper method to check user status.
     *
     * @param user the User object to check
     */
    public void checkUserStatus(SysUser user) {
        if (user == null) {
            throw new CustomException(ErrorCode.CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new CustomException(ErrorCode.DISABLED, HttpStatus.UNAUTHORIZED);
        }
    }
}
