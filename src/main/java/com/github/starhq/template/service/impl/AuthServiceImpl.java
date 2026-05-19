package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.exception.BusinessException;
import com.github.starhq.template.common.exception.CustomException;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.common.util.SecurityUserUtils;
import com.github.starhq.template.converter.UserConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.entity.SysUserRole;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.mapper.SysUserRoleMapper;
import com.github.starhq.template.model.dto.user.ResetPasswordDTO;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author wangjian
 * @version v1.0.0
 * Copyright (C), 2020-2026, starimba@outlook.com
 * @description: 认证服务实现类
 * @date 2026/3/24 12:15
 */
@Service("authService")
@RequiredArgsConstructor
public class AuthServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements AuthService {

    private final SysTokenMapper tokenMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;
    private final EventService eventService;

    /**
     * 加载用户信息（Spring Security 调用）
     */
    @Cacheable(value = "users", key = "#p0", unless = "#result == null")
    @Override
    public @NullMarked UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = getBaseMapper().selectUserWithRole(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );

        SecurityUserUtils.checkUserStatus(user);
        if (CollectionUtils.isEmpty(user.getAuthorities())) {
            throw new CustomException(ErrorCode.NO_ROLES, HttpStatus.UNAUTHORIZED);
        }

        return user;
    }

    /**
     * 用户注册
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserDetails register(UserDTO userDto) {
        // 1. 转换并准备实体
        SysUser user = userConverter.toEntity(userDto);
        // todo: 把这一步提取到 controller中
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 2. 插入用户
        insert(user, ErrorCode.USER_DUPLICATE_USERNAME, ErrorCode.USER_INSERT_FAILED);

        // 3. 分配默认角色
        assignDefaultRoles(user);

        return user;
    }

    /**
     * 重置密码（当前用户）
     */
    @Override
    public boolean resetPassword(ResetPasswordDTO dto) {
        Long userId = SecurityContextUtils.getRequiredUserId();

        SysUser user = getBaseMapper().selectById(userId);
        SecurityUserUtils.checkUserStatus(user);

        validatePasswordChange(user, dto);
        String newPassword = passwordEncoder.encode(dto.getNewPassword());

        // 更新密码
        transactionTemplate.execute(_ -> {
            boolean success = updatePassword(userId, newPassword);
            if (!success) {
                throw new BusinessException(ErrorCode.RESET_PASSWORD);
            }

            if (tokenMapper.delete(new LambdaQueryWrapper<SysToken>().eq(SysToken::getUserId, user.getId())) <= 0) {
                throw new BusinessException(ErrorCode.RESET_PASSWORD);
            }
            return true;
        });

        // 清除缓存
        eventService.notifyCacheEvict(List.of(user.getUsername()), List.of(CacheConstant.USER));
        eventService.notifyCacheEvict(List.of(user.getId()), List.of(CacheConstant.TOKEN));

        return true;
    }

    // ====================== 私有辅助方法 ======================

    /**
     * 为新用户分配默认角色
     */
    private void assignDefaultRoles(SysUser user) {
        List<SysRole> defaultRoles = roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getIsDefault, true)
        );

        if (CollectionUtils.isEmpty(defaultRoles)) {
            throw new BusinessException(ErrorCode.NO_ROLES);
        }

        user.setAuthorities(defaultRoles);

        List<SysUserRole> userRoles = defaultRoles.stream()
                .map(role -> new SysUserRole(user.getId(), role.getId()))
                .toList();

        try {
            userRoleMapper.insert(userRoles);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.USER_INSERT_FAILED, e);
        }
    }

    /**
     * 验证密码修改合法性
     */
    private void validatePasswordChange(SysUser user, ResetPasswordDTO dto) {
        // 旧密码校验
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.MISMATCH_PASSWORD, HttpStatus.BAD_REQUEST);
        }

        // 新密码不能与旧密码相同
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.SAME_PASSWORD, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 更新用户密码
     */
    private boolean updatePassword(Long userId, String password) {
        SysUser updateEntity = new SysUser();
        updateEntity.setId(userId);
        updateEntity.setPassword(password);

        return getBaseMapper().updateById(updateEntity) > 0;
    }
}
