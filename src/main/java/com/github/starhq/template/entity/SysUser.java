package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.github.starhq.template.common.enums.UserStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.Alias;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;


/**
 * 系统用户实体
 *
 * @author starhq
 */
@Data
@Alias("user")
@TableName("sys_user")
@EqualsAndHashCode(callSuper = false)
public class SysUser extends BaseEntity implements UserDetails {

    @Serial
    private static final long serialVersionUID = -6558931765964042708L;
    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（bcrypt加密）
     */
    private String password;

    /**
     * 状态
     */
    private UserStatus status;

    /**
     * 权限集
     */
    @TableField(exist = false)
    private Collection<? extends GrantedAuthority> authorities;

}
