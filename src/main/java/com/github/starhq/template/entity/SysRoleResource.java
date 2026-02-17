package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色资源关联实体
 *
 * @author starhq
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_resource")
public class SysRoleResource {
    /**
     * 角色ID
     */
    @TableId
    private Long roleId;

    /**
     * 资源ID
     */
    private Long resourceId;
}
