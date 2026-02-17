package com.github.starhq.template.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 角色按钮关联实体
 *
 * @author starhq
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_role_button")
public class SysRoleButton {
    /**
     * 角色ID
     */
    @TableId
    private Long roleId;

    /**
     * 按钮ID
     */
    private Long buttonId;
}
