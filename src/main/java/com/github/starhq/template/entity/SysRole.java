package com.github.starhq.template.entity;

import org.apache.ibatis.type.Alias;

import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;

/**
 * 系统角色实体
 *
 * @author starhq
 */
@Data
@Alias("role")
@TableName("sys_role")
@EqualsAndHashCode(callSuper = false)
public class SysRole extends BaseEntity implements GrantedAuthority {

    @Serial
    private static final long serialVersionUID = -4242770371889322444L;
    /**
     * 角色代码
     */
    private String code;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 是否为默认角色
     */
    private Boolean isDefault;

    @Override
    public @Nullable String getAuthority() {
        return this.code;
    }
}
