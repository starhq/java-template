package com.github.starhq.template.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.starhq.template.aop.annotation.AuditLoggable;
import com.github.starhq.template.common.constant.AuditLogConstant;
import com.github.starhq.template.common.constant.CacheConstant;
import com.github.starhq.template.common.constant.QueryConstant;
import com.github.starhq.template.common.enums.ErrorCode;
import com.github.starhq.template.common.enums.TargetType;
import com.github.starhq.template.common.util.SecurityContextUtils;
import com.github.starhq.template.converter.UserConverter;
import com.github.starhq.template.entity.SysRole;
import com.github.starhq.template.entity.SysToken;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.entity.SysUserRole;
import com.github.starhq.template.event.EventService;
import com.github.starhq.template.helper.RelationHelper;
import com.github.starhq.template.helper.SysUserMapperHelper;
import com.github.starhq.template.mapper.SysRoleMapper;
import com.github.starhq.template.mapper.SysTokenMapper;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.mapper.SysUserRoleMapper;
import com.github.starhq.template.model.dto.page.KeyWordPageRequest;
import com.github.starhq.template.model.dto.user.UserDTO;
import com.github.starhq.template.model.vo.user.UserPageVO;
import com.github.starhq.template.model.vo.user.UserSimpleVO;
import com.github.starhq.template.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 用户服务实现类
 *
 * @author starhq
 */
@Service("userService")
@RequiredArgsConstructor
public class UserServiceImpl extends AuditBaseServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final SysUserRoleMapper userRoleMapper;
    private final SysTokenMapper tokenMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapperHelper userMapperHelper;

    private final UserConverter userConverter;
    private final EventService eventService;
    private final RelationHelper relationHelper;

    @Override
    public IPage<UserPageVO> page(KeyWordPageRequest pageRequest) {
        return pageVO(pageRequest,
                wrapper -> {
                    if (StringUtils.hasText(pageRequest.getKeyword())) {
                        wrapper.likeRight(QueryConstant.USERNAME, pageRequest.getKeyword());
                    }
                },
                userMapperHelper,
                userConverter::toPageVO);
    }

    @Override
    public UserSimpleVO getUserById(Serializable id) {
        id = null != id ? id : SecurityContextUtils.getRequiredUserId();

        SysUser user = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);
        return userConverter.toSimpleVO(user);
    }

    @AuditLoggable(targetType = TargetType.USER, action = AuditLogConstant.USER_UPDATE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateUser(Serializable id, UserDTO updateUserDto) {
        SysUser existingUser = getAndCheckById(id, ErrorCode.USER_NOT_FOUND);

        userConverter.updateEntity(updateUserDto, existingUser);

        update(existingUser, ErrorCode.USER_DUPLICATE_USERNAME, ErrorCode.USER_UPDATE_FAILED, ErrorCode.USER_NOT_FOUND);

        updateUserAssociations(existingUser.getId(), updateUserDto);

        clearUserCache(id);

        return true;
    }

    @AuditLoggable(targetType = TargetType.USER, action = AuditLogConstant.USER_INSERT)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean createUser(UserDTO userDTO) {
        SysUser user = userConverter.toEntity(userDTO);

        insert(user, ErrorCode.USER_DUPLICATE_USERNAME, ErrorCode.USER_INSERT_FAILED);

        updateUserAssociations(user.getId(), userDTO);

        return true;
    }

    @AuditLoggable(targetType = TargetType.USER, action = AuditLogConstant.USER_REMOVE)
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean removeById(Serializable id) {
        deleteUserRelations(id);

        // Delete the role
        delete(id, ErrorCode.USER_NOT_FOUND, ErrorCode.USER_DELETE_FAILED);

        clearUserCache(id);

        return true;
    }

    /**
     * 更新用户的所有关联关系
     */
    private void updateUserAssociations(Long userId, UserDTO userDTO) {
        assignRolesToUser(userId, userDTO.getRoleIds());
    }

    // ==================== 关联关系分配方法 ====================

    private void assignRolesToUser(Long userId, Set<Long> roleIds) {
        Consumer<Long> delete = id -> userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));

        relationHelper.assignRelations(userId, roleIds, delete, SysUserRole::new, userRoleMapper::upsertUserRole, this::validateRoleIds, RelationHelper.AssociationType.USER);
    }

    /**
     * 校验菜单 ID 的有效性
     */
    private void validateRoleIds(Set<Long> roleIds) {
        relationHelper.validateEntityExists(roleIds, roleMapper, SysRole::getId, ErrorCode.ROLE_NOT_FOUND);
    }

    // ==================== Delete Helpers ====================

    private void deleteUserRelations(Serializable userId) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        tokenMapper.delete(new LambdaQueryWrapper<SysToken>().eq(SysToken::getUserId, userId));
    }

    private void clearUserCache(Serializable existingUser) {
        List<Serializable> userIds = List.of(existingUser);
        List<String> cacheNames = List.of(CacheConstant.ADMIN_NAME, CacheConstant.TOKEN, CacheConstant.USER, CacheConstant.BUTTON, CacheConstant.RESOURCE, CacheConstant.MENU);
        eventService.notifyCacheEvict(userIds, cacheNames);
    }
}
