package com.github.starhq.template.vo;

import java.io.Serializable;

import com.github.starhq.template.enums.UserStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户视图对象
 *
 * @author starhq
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class UserVO extends BaseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 状态
     */
    private UserStatus status;

}
