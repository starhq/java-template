package com.github.starhq.template.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.starhq.template.dto.LoginDTO;
import com.github.starhq.template.dto.PageRequest;
import com.github.starhq.template.dto.UserCreateDTO;
import com.github.starhq.template.dto.UserUpdateDTO;
import com.github.starhq.template.entity.SysUser;
import com.github.starhq.template.enums.UserStatus;
import com.github.starhq.template.exception.BusinessException;
import com.github.starhq.template.mapper.SysUserMapper;
import com.github.starhq.template.service.TokenService;
import com.github.starhq.template.service.UserService;
import com.github.starhq.template.vo.LoginVO;
import com.github.starhq.template.vo.UserVO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务实现类
 *
 * @author starhq
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, loginDTO.getUsername())
        );

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("用户已被禁用");
        }

        // 生成Token
        return tokenService.generateToken(user.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(UserCreateDTO dto) {
        return null;

        // // 检查用户名是否已存在
        // SysUser existingUser = userMapper.selectOne(
        //         new LambdaQueryWrapper<SysUser>()
        //                 .eq(SysUser::getUsername, dto.getUsername())
        // );

        // if (existingUser != null) {
        //     throw new BusinessException("用户名已存在");
        // }

        // SysUser user = UserConverter.INSTANCE.toEntity(dto);
        // user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // user.setStatus(UserStatus.ACTIVE);
        // user.setCreatedAt(OffsetDateTime.now());
        // // TODO: 从SecurityContext获取当前用户ID
        // // user.setCreatedBy(getCurrentUserId());

        // userMapper.insert(user);
        // return UserConverter.INSTANCE.toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Long id, UserUpdateDTO dto) {
        // SysUser user = userMapper.selectById(id);
        // if (user == null) {
        //     throw new BusinessException("用户不存在");
        // }

        // UserConverter.INSTANCE.updateEntity(dto, user);
        // if (dto.getPassword() != null) {
        //     user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // }
        // user.setUpdatedAt(OffsetDateTime.now());
        // // TODO: 从SecurityContext获取当前用户ID
        // // user.setUpdatedBy(getCurrentUserId());

        // userMapper.updateById(user);
        // return UserConverter.INSTANCE.toVO(user);

        return null;
    }

    @Override
    public UserVO getUserById(Long id) {
        // SysUser user = userMapper.selectById(id);
        // if (user == null) {
        //     throw new BusinessException("用户不存在");
        // }
        // return UserConverter.INSTANCE.toVO(user);

        return null;
    }

    @Override
    public UserVO getUserByUsername(String username) {
        // SysUser user = userMapper.selectOne(
        //         new LambdaQueryWrapper<SysUser>()
        //                 .eq(SysUser::getUsername, username)
        // );
        // if (user == null) {
        //     throw new BusinessException("用户不存在");
        // }
        // return UserConverter.INSTANCE.toVO(user);

        return null;
    }

    @Override
    public Page<UserVO> listUsers(PageRequest pageRequest) {
        // Page<SysUser> page = new Page<>(pageRequest.getCurrent(), pageRequest.getSize());
        // Page<SysUser> userPage = userMapper.selectPage(page, null);
        // return (Page<UserVO>) userPage.convert(UserConverter.INSTANCE::toVO);

        return null;
        
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        userMapper.deleteById(id);
    }
}
